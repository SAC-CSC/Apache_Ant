// Base                 : Conveyor Sortaion Controller
// Class                : ScannerResultTlgWithVersionNo Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : code module 
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version

package conveyorinterface.plctelegrams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Objects ;

import platform.core.Repository.BSMRepository;
import platform.core.Repository.TelegramRepository;
import platform.core.log.Log;
import platform.core.util.DBConnection;

public class ScannerResultTlgWithVersionNo {
    private static final Logger log = Logger.getLogger("ScannerTelegramHandler");
    private static final String logPrefix = "[SCANNER] ";
    

    public void handleScannerTelegram(
            OutputStream outputStream,
            InputStream inputStream,
            Log log,
            String logPrefix
    ) throws IOException {

        // Read TPKT header (4 bytes)
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);
        if ((tpkt[0] & 0xFF) != 0x03) {
            throw new IOException("Invalid TPKT version");          
        }

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);

        // Read COTP header (3 bytes)
        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);
        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80) {
            throw new IOException("Invalid COTP header");
        }

        // Total header: 8 (channel + version + sequence) + 22 (telegram header) = 30 bytes
        byte[] header = new byte[30];
        readFully(inputStream, header, 0, 30);

        int index = 0;

        // === Channel + Version + Sequence ===
        int channelId = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);
        int version = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);
        int sequenceNumber = ((header[index++] & 0xFF) << 24) |
                             ((header[index++] & 0xFF) << 16) |
                             ((header[index++] & 0xFF) << 8)  |
                             (header[index++] & 0xFF);

        // === Telegram Header ===
        int telegramType = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);
        int telegramLength = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);
        int subsystemId = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);
        int component = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);

        long globalId = ((long)(header[index++] & 0xFF) << 24) |
                        ((long)(header[index++] & 0xFF) << 16) |
                        ((long)(header[index++] & 0xFF) << 8)  |
                        ((long)(header[index++] & 0xFF));

        int plcIndex = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);
        int scannerNumber = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);

        String telegramVersion = new String(header, index, 2, StandardCharsets.US_ASCII); index += 2;

        int status = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);
        int responseLength = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);

        // === Response Data ===
        List<String> barcodes = new ArrayList<>();
        String responseAscii = "";

        if (responseLength > 0 && status == 0) {
            byte[] responseData = new byte[responseLength];
            readFully(inputStream, responseData, 0, responseLength);

            if (responseLength % 2 != 0 && responseData[responseLength - 1] == (byte) 0xFF) {
                responseAscii = new String(responseData, 0, responseLength - 1, StandardCharsets.US_ASCII);
            } else {
                responseAscii = new String(responseData, StandardCharsets.US_ASCII);
            }

            int sepIndex = responseAscii.indexOf("#");
            if (sepIndex != -1 && sepIndex + 1 < responseAscii.length()) {
                String barcodeData = responseAscii.substring(sepIndex + 1);
                barcodes = extractCodesFrom(barcodeData);
            }
        }


        // Format log line
        String barcodeList = barcodes.toString();
        
        String rxLog = String.format(
            "telegram=<ScannerResultTlgWithVersionNo>, length=<%d>, subsystemId=<%d>, component=<%d>, globalId=<%d>, cmcIndex=<%d>, conveyorScannerId=<%d: %s>, scannerProtocolVersion=<%s>, status=<%s>, responseLength=<%d>, barcodesAsStr=<%s>, barcodesInASCII=<%s>",
            telegramLength, subsystemId, component, globalId, plcIndex,
            scannerNumber, resolveScannerName(scannerNumber),
            telegramVersion, (status == 0 ? "OK" : "ERROR"), responseLength,
            barcodeList, responseAscii
        );
        log.info(logPrefix, "Scanner Result telegram: " + bytesToHex(header));

        log.info(logPrefix, "Telegram [RX]: " + rxLog + " ##[");
      
        // Extract IATA codes
        String iata1 = barcodes.size() > 0 ? barcodes.get(0) : "";
        String iata2 = barcodes.size() > 1 ? barcodes.get(1) : "";
        String iata3 = barcodes.size() > 2 ? barcodes.get(2) : "";
        
        
        //After fetching IATA we need to find out and take value from the db of BSM and match this 
        // will present or not it is present then we need to make the IATA true and also destination from the 
        // db and then send the valid destination telegram to the plc with destination int the form.
        // Send processed telegram back (COTP wrapped)
        
//        try {
//			TelegramRepository.insertTelegram(
//				    telegramLength,
//				    subsystemId,
//				    component,
//				    globalId,
//				    plcIndex,
//				    scannerNumber,
//				    resolveScannerName(scannerNumber),
//				    telegramVersion,
//				    (status == 0 ? "OK" : "ERROR"),
//				    responseLength,
//				    barcodeList,
//				    responseAscii
//				);
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        // === IATA Matching Logic ===
        List<String> iataList = Arrays.asList(iata1, iata2, iata3)
            .stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList());

        int matchCount = 0;
        String matchedIATA = null;
        int destination = 1;

        for (String iata : iataList) {
            if (BSMRepository.existsInBSM(iata)) {
                matchCount++;
                if (matchedIATA == null) {
                    matchedIATA = iata;
                }
            }
        }

        if (matchCount >= 0) {
            log.info(logPrefix, "✅ " + matchCount + " IATA barcodes matched in BSM.");
            // Send processed telegram back (COTP wrapped)
            send_ValidBarcodeTelegram(channelId, version, sequenceNumber, outputStream,
                 subsystemId, component, globalId, plcIndex,
                 iata1, iata2, iata3, "False", "22", log, logPrefix);
            
            
            // this i wirte only test purpose...
            send_DestinationTelegram(
                    channelId, version, sequenceNumber, outputStream,
                    subsystemId, component, globalId, plcIndex,
                    iata1, iata2, iata3,destination, log, logPrefix
                );
             //   return; // ✅ Prevent sending duplicate telegrams
            Optional<Integer> destOpt = BSMRepository.getDestination(matchedIATA);
            if (destOpt.isPresent()) {
                destination = destOpt.get();

                send_DestinationTelegram(
                    channelId, version, sequenceNumber, outputStream,
                    subsystemId, component, globalId, plcIndex,
                    iata1, iata2, iata3,destination, log, logPrefix
                );
                return; // ✅ Prevent sending duplicate telegrams
            } else {
                log.warn(logPrefix, "⚠️ Destination not found for IATA: " + matchedIATA);
            }

            // Optional: check second table if needed
            if (matchCount >= 2) {
                // implementSecondaryRoutingRules(iataList);
            	
            }
        }       
       
    }

    
    public static void insertTelegram(
    	    int telegramLength, int subsystemId, int component, long globalId,
    	    int plcIndex, int scannerNumber, String scannerName,
    	    String telegramVersion, String status, int responseLength,
    	    String barcodeList, String responseAscii
    	) {
    	    String sql = "INSERT INTO scanner_telegram (telegram_length, subsystem_id, component, global_id, plc_index, scanner_number, scanner_name, telegram_version, status, response_length, barcodes, response_ascii) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    	    try (Connection conn = DBConnection.getConnection();
    	         PreparedStatement stmt = conn.prepareStatement(sql)) {

    	        stmt.setInt(1, telegramLength);
    	        stmt.setInt(2, subsystemId);
    	        stmt.setInt(3, component);
    	        stmt.setLong(4, globalId);
    	        stmt.setInt(5, plcIndex);
    	        stmt.setInt(6, scannerNumber);
    	        stmt.setString(7, scannerName);
    	        stmt.setString(8, telegramVersion);
    	        stmt.setString(9, status);
    	        stmt.setInt(10, responseLength);
    	        stmt.setString(11, barcodeList);
    	        stmt.setString(12, responseAscii);

    	        stmt.executeUpdate();
    	        System.out.println("✔️ Data inserted successfully!");

    	    } catch (SQLException e) {
    	        System.err.println("❌ DB Insert Error: " + e.getMessage());
    	    }
    	}
    


    private static List<String> extractCodesFrom(String barcodeData) {
        List<String> codes = new ArrayList<>();
        int i = 0;
        while (i + 2 < barcodeData.length()) {
            String type = barcodeData.substring(i, i + 2);
            i += 2;

            int codeLength = 10; // Default to 10 for both 0e and 0R
            if (i + codeLength > barcodeData.length()) break;

            String code = barcodeData.substring(i, i + codeLength);
            codes.add(code);
            i += codeLength;
        }
        return codes;
    }
    private static String resolveScannerName(int id) {
        switch (id) {
            case 5: return "2AR04_AT19";
            case 6: return "2AR04_BT20";
            // Add more mappings
            default: return "Unknown";
        }
    }

    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1) throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }



    public static byte[] build_BLRT2ValidBarcodeTelegram(
            int channelId, int version, int sequenceNumber,
            int subsystemId, int component, long globalId, int plcIndex,
            String iata1, String iata2, String iata3,
            boolean customsRequired, int minScreeningLevel) {

        int cnt = 0;

        // 1️⃣ Payload according to BLRT2 spec (TT + LL + fields)
        byte[] payload = new byte[47]; // 47 bytes payload including TT + LL
        // Telegram Type TT = 56 decimal -> 0x38
        payload[cnt++] = 0x00;
        payload[cnt++] = 0x38; // TT=56

        // Telegram Length LL = 24 bytes as per spec
        payload[cnt++] = 0x00;
        payload[cnt++] = 0x18; // 24 decimal

        // Subsystem ID SS
        payload[cnt++] = (byte)((subsystemId >> 8) & 0xFF);
        payload[cnt++] = (byte)(subsystemId & 0xFF);

        // Component CC
        payload[cnt++] = (byte)((component >> 8) & 0xFF);
        payload[cnt++] = (byte)(component & 0xFF);

        // Global ID GGGG
        payload[cnt++] = (byte)((globalId >> 24) & 0xFF);
        payload[cnt++] = (byte)((globalId >> 16) & 0xFF);
        payload[cnt++] = (byte)((globalId >> 8) & 0xFF);
        payload[cnt++] = (byte)(globalId & 0xFF);

        // PLC Index XX
        payload[cnt++] = (byte)((plcIndex >> 8) & 0xFF);
        payload[cnt++] = (byte)(plcIndex & 0xFF);

        // IATA Codes (10 bytes each) — pad with 0x00 if null or shorter
        System.arraycopy(padIATA(iata1), 0, payload, cnt, 10); cnt += 10;
        System.arraycopy(padIATA(iata2), 0, payload, cnt, 10); cnt += 10;
        System.arraycopy(padIATA(iata3), 0, payload, cnt, 10); cnt += 10;

        // Customs Required Status RR (1 byte)
        payload[cnt++] = (byte)(customsRequired ? 1 : 0);

        payload[cnt++] = (byte) minScreeningLevel;
        // 2️⃣ TPKT + COTP Header (7 bytes)
        int totalLength = payload.length + 7 + 8; // + header + channel fields
        byte[] header = new byte[7];
        cnt = 0;
        header[cnt++] = 0x03;             // TPKT Version
        header[cnt++] = 0x00;             // Reserved
        header[cnt++] = (byte)((totalLength >> 8) & 0xFF);
        header[cnt++] = (byte)(totalLength & 0xFF);
        header[cnt++] = 0x02;             // COTP DT PDU
        header[cnt++] = (byte)0xF0;       // EOT + flags
        header[cnt++] = (byte)0x80;       // Flags

        // 3️⃣ Channel Fields (8 bytes)
        byte[] channelFields = new byte[8];
        cnt = 0;
        channelFields[cnt++] = (byte)(channelId >> 8);
        channelFields[cnt++] = (byte)(channelId & 0xFF);
        channelFields[cnt++] = (byte)(version >> 8);
        channelFields[cnt++] = (byte)(version & 0xFF);
        channelFields[cnt++] = (byte)(sequenceNumber >> 24);
        channelFields[cnt++] = (byte)(sequenceNumber >> 16);
        channelFields[cnt++] = (byte)(sequenceNumber >> 8);
        channelFields[cnt++] = (byte)(sequenceNumber & 0xFF);

        // 4️⃣ Combine header + channel fields + payload
        byte[] telegram = new byte[header.length + channelFields.length + payload.length];
        cnt = 0;
        for (byte b : header) telegram[cnt++] = b;
        for (byte b : channelFields) telegram[cnt++] = b;
        for (byte b : payload) telegram[cnt++] = b;

        return telegram;
    }

    private static byte[] padIATA(String iata) {
        byte[] raw = iata.getBytes(StandardCharsets.US_ASCII);
        byte[] padded = new byte[10];
        Arrays.fill(padded, (byte) '0');
        System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, 10));
        return padded;
    }

    public static void  send_ValidBarcodeTelegram(int channelId, int version, int sequenceNumber ,OutputStream outputStream , int subsystemId, int component, long globalId, int plcIndex,
            String iata1, String iata2, String iata3,
            String customsRequiredStatus, String minScreeningLevelStr,Log log, String logPrefix) throws IOException {

//        byte[] telegram = build_NMIALValidBarcodeTelegram( channelId,  version,sequenceNumber,subsystemId, component, globalId, plcIndex,
//                iata1, iata2, iata3, "False", "22");
    	byte[] telegram = build_BLRT2ValidBarcodeTelegram(
    	        channelId,        // Channel ID
    	        version,          // Version
    	        sequenceNumber,   // Sequence Number
    	        subsystemId,      // Subsystem ID
    	        component,        // Component
    	        globalId,         // Global ID
    	        plcIndex,         // PLC Index
    	        iata1,            // IATA code 1
    	        iata2,            // IATA code 2
    	        iata3,            // IATA code 3
    	        false,            // Customs Required Status ("False")
    	        22                // Minimum Screening Level (default)
    	);


        log.info(logPrefix, "Sending VALID BARCODE telegram: " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();
    }
    public static void  send_DestinationTelegram(int channelId, int version, int sequenceNumber ,OutputStream outputStream , int subsystemId, int component, long globalId, int plcIndex,
            String iata1, String iata2, String iata3,int destination,
            Log log, String logPrefix) throws IOException {

    	
    	// for testing
     //   byte[] telegram = buildDestinationRequestTelegram( channelId,  version,sequenceNumber,subsystemId, component, globalId, plcIndex,
    //            iata1, iata2, iata3 , destination);
        byte[] telegram = build_BLRT2ItemDestinationTelegram(
                channelId,       // Channel ID
                version,         // Version
                sequenceNumber,  // Sequence Number
                subsystemId,     // Subsystem ID
                component,       // Component
                globalId,        // Global ID
                plcIndex,        // PLC Index
                destination,     // Destination DD
                0                // Alternative Destination EE (use 0 if not required)
        );

        log.info(logPrefix, "Sending DESTINATION REQUEST TELEGRAM BARCODE : " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();
    }
    
    /**
     * Builds a Destination Request telegram to send to the PLC.
     *
     * @param barcodeData the raw barcode data (e.g., "0E12345678900R0987654321")
     * @param plcIndex the index of the item in the PLC
     * @param destination the primary destination (can be logical like 1=sorter1)
     * @param altDestination optional alternate destination (0 if not used)
     * @return a byte[] telegram ready to send
     */
//    public static byte[] buildDestinationRequestTelegram(
//            int channelId, int version, int sequenceNumber,
//            int subsystemId, int component, long globalId,
//            int plcIndex, String IATA1, String IATA2, String IATA3, int destination) {
//
//        List<String> iataCodes = new ArrayList<>(Arrays.asList(IATA1, IATA2, IATA3));
//        iataCodes.removeIf(Objects::isNull); // Remove null entries
//
//        int iataBytes = iataCodes.size() * 10;
//        int payloadLength = 16 + iataBytes + 1; // header + IATA + possible padding
//        if ((payloadLength % 2) != 0) {
//            payloadLength++; // Ensure even number of bytes
//        }
//
//        byte[] payload = new byte[payloadLength];
//        int i = 0;
//
//        // === Application Payload ===
//
//        // Telegram Type (2 bytes)
//        payload[i++] = 0x00;
//        payload[i++] = 0x42;
//
//        // Telegram Length (2 bytes)
//        payload[i++] = (byte) ((payloadLength >> 8) & 0xFF);
//        payload[i++] = (byte) (payloadLength & 0xFF);
//
//        // Subsystem ID (2 bytes)
//        payload[i++] = (byte) ((subsystemId >> 8) & 0xFF);
//        payload[i++] = (byte) (subsystemId & 0xFF);
//
//        // Component ID (2 bytes)
//        payload[i++] = (byte) ((component >> 8) & 0xFF);
//        payload[i++] = (byte) (component & 0xFF);
//
//        // PLC Index (2 bytes)
//        payload[i++] = (byte) ((plcIndex >> 8) & 0xFF);
//        payload[i++] = (byte) (plcIndex & 0xFF);
//
//        // Global ID (4 bytes)
//        payload[i++] = (byte) ((globalId >> 24) & 0xFF);
//        payload[i++] = (byte) ((globalId >> 16) & 0xFF);
//        payload[i++] = (byte) ((globalId >> 8) & 0xFF);
//        payload[i++] = (byte) (globalId & 0xFF);
//
//        // Destination (1 byte)
//        payload[i++] = (byte) (destination & 0xFF); // Properly set LSB only (1 byte)
//
//        // Alt Destination (1 byte)
//        payload[i++] = 0x00;
//
//        // Reserved (2 bytes)
//        payload[i++] = 0x00;
//        payload[i++] = 0x00;
//
//        // IATA Codes (each padded to 10 bytes)
//        for (String iata : iataCodes) {
//            byte[] padded = padIATA(iata);
//            System.arraycopy(padded, 0, payload, i, 10);
//            i += 10;
//        }
//
//        // Padding byte if needed
//        if (i % 2 != 0) {
//            payload[i++] = (byte) 0xFF;
//        }
//
//        int totalLength = 7 + 8 + i; // TPKT + COTP + channelFields + payload
//
//        // === TPKT Header (4 bytes) ===
//        byte[] tpkt = {
//            0x03, 0x00,
//            (byte) (totalLength >> 8),
//            (byte) (totalLength & 0xFF)
//        };
//
//        // === COTP Header (3 bytes) ===
//        byte[] cotp = { 0x02, (byte) 0xF0, (byte) 0x80 };
//
//        // === Channel Info (8 bytes) ===
//        byte[] channelFields = {
//            (byte) (channelId >> 8), (byte) (channelId & 0xFF),
//            (byte) (version >> 8), (byte) (version & 0xFF),
//            (byte) (sequenceNumber >> 24), (byte) (sequenceNumber >> 16),
//            (byte) (sequenceNumber >> 8), (byte) (sequenceNumber)
//        };
//
//        // === Final Telegram ===
//        byte[] finalTelegram = new byte[tpkt.length + cotp.length + channelFields.length + i];
//        int pos = 0;
//        System.arraycopy(tpkt, 0, finalTelegram, pos, tpkt.length); pos += tpkt.length;
//        System.arraycopy(cotp, 0, finalTelegram, pos, cotp.length); pos += cotp.length;
//        System.arraycopy(channelFields, 0, finalTelegram, pos, channelFields.length); pos += channelFields.length;
//        System.arraycopy(payload, 0, finalTelegram, pos, i);
//
//        return finalTelegram;
//    }

    public static byte[] build_BLRT2ItemDestinationTelegram(
            int channelId, int version, int sequenceNumber,
            int subsystemId, int component, long globalId, int plcIndex,
            int destination, int altDestination) {

        int cnt = 0;

        // Payload = 16 bytes fixed
        byte[] payload = new byte[16];

        // Telegram Type TT
        payload[cnt++] = 0x00;
        payload[cnt++] = 0x2A; // 42 decimal

        // Telegram Length LL = 10 decimal
        payload[cnt++] = 0x00;
        payload[cnt++] = 0x0A;

        // Subsystem ID
        payload[cnt++] = (byte)((subsystemId >> 8) & 0xFF);
        payload[cnt++] = (byte)(subsystemId & 0xFF);

        // Component
        payload[cnt++] = (byte)((component >> 8) & 0xFF);
        payload[cnt++] = (byte)(component & 0xFF);

        // Global ID
        payload[cnt++] = (byte)((globalId >> 24) & 0xFF);
        payload[cnt++] = (byte)((globalId >> 16) & 0xFF);
        payload[cnt++] = (byte)((globalId >> 8) & 0xFF);
        payload[cnt++] = (byte)(globalId & 0xFF);

        // PLC Index
        payload[cnt++] = (byte)((plcIndex >> 8) & 0xFF);
        payload[cnt++] = (byte)(plcIndex & 0xFF);

        // Destination
        payload[cnt++] = (byte)(destination & 0xFF);

        // Alternative Destination
        payload[cnt++] = (byte)(altDestination & 0xFF);

        // Now wrap with TPKT + COTP + channel fields as usual
        int totalLength = 7 + 8 + payload.length;

        byte[] header = new byte[7];
        cnt = 0;
        header[cnt++] = 0x03;
        header[cnt++] = 0x00;
        header[cnt++] = (byte)((totalLength >> 8) & 0xFF);
        header[cnt++] = (byte)(totalLength & 0xFF);
        header[cnt++] = 0x02;
        header[cnt++] = (byte)0xF0;
        header[cnt++] = (byte)0x80;

        byte[] channelFields = new byte[8];
        cnt = 0;
        channelFields[cnt++] = (byte)(channelId >> 8);
        channelFields[cnt++] = (byte)(channelId & 0xFF);
        channelFields[cnt++] = (byte)(version >> 8);
        channelFields[cnt++] = (byte)(version & 0xFF);
        channelFields[cnt++] = (byte)(sequenceNumber >> 24);
        channelFields[cnt++] = (byte)(sequenceNumber >> 16);
        channelFields[cnt++] = (byte)(sequenceNumber >> 8);
        channelFields[cnt++] = (byte)(sequenceNumber & 0xFF);

        byte[] telegram = new byte[header.length + channelFields.length + payload.length];
        cnt = 0;
        for (byte b : header) telegram[cnt++] = b;
        for (byte b : channelFields) telegram[cnt++] = b;
        for (byte b : payload) telegram[cnt++] = b;

        return telegram;
    }






    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    
    
    public static void validate_AckDestination(InputStream inputStream, Log log, String logPrefix) throws IOException {
        byte[] header = new byte[4];
        readFully(inputStream, header, 0, 4);

        if ((header[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (header[0] & 0xFF));

        int totalLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
        if (totalLength < 25)
            throw new IOException("Total telegram length too short: expected at least 25 bytes but got " + totalLength);

        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);
        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header. Expected [0x02, 0xF0, 0x80]");

        // Total header: 8 (channel + version + sequence) + 22 (telegram header) = 30 bytes
        byte[] payload = new byte[25];
        readFully(inputStream, payload, 0, 25);

        int index = 0;

        // === Channel + Version + Sequence ===
        int channelId = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
        int version = ((payload[2] & 0xFF) << 8) | (payload[3] & 0xFF);
        int sequenceNumber = ((payload[4] & 0xFF) << 24) |
                             ((payload[5] & 0xFF) << 16) |
                             ((payload[6] & 0xFF) << 8)  |
                             (payload[7] & 0xFF);

        // === Telegram payload ===
        int telegramType = ((payload[8] & 0xFF) << 8) | (payload[9] & 0xFF);
        int telegramLength = ((payload[10] & 0xFF) << 8) | (payload[11] & 0xFF);
        int subsystemId = ((payload[12] & 0xFF) << 8) | (payload[13] & 0xFF);
        int component = ((payload[14] & 0xFF) << 8) | (payload[15] & 0xFF);

        long globalId = ((long)(payload[16] & 0xFF) << 24) |
                        ((long)(payload[17] & 0xFF) << 16) |
                        ((long)(payload[18] & 0xFF) << 8)  |
                        ((long)(payload[19] & 0xFF));

        int plcIndex = ((payload[20] & 0xFF) << 8) | (payload[21] & 0xFF);
        int destination = ((payload[22] & 0xFF) << 8) | (payload[23] & 0xFF);
        int status = payload[24] & 0xFF;

        String statusDescription = switch (status) {
            case 0 -> "0(OK)";
            case 1 -> "1(Index error)";
            case 2 -> "2(Destination invalid)";
            case 3 -> "3(Item discharged / gone)";
            case 4 -> "4(Destination not reachable)";
            default -> status + "(Unknown)";
        };

        log.info(logPrefix, String.format(
            "**********PLC to SAC*********************\n" +
            "%s %s ConveyorPlcChannel_10Telegram [RX]:\n" +
            "telegram=<ItemDestinationAckTlg>,\n" +
            "length=<9>,\n" +
            "subsystemId=<%d>,\n" +
            "component=<%d>,\n" +
            "globalId=<%d>,\n" +
            "cmcIndex=<%d>,\n" +
            "destination=<%d: MES??>,\n" +
            "status=<%s> ##",
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")),
            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss")),
            subsystemId, component, globalId, plcIndex, destination, statusDescription
        ));
    }

    private static String resolveDestinationName(int code) {
        return switch (code) {
            case 41 -> "MES01";
            // Add more as needed
            default -> "Unknown";
        };
    }
}
