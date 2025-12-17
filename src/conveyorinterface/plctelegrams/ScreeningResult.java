//// Base                 : Conveyor Sortaion Controller
//// Class                : ScreeningResult Class
//// Programmer           : Giresh
//// Release Date         : 2025-06-19
//// Revision Number      : 1.0
//// Description          : code module 
//// ================================================================================
//// Change history 
//// Rev.     Date         Programmer    Description                               
//// --------------------------------------------------------------------------------
////01.00    2025.06.19    Giresh         Initial Version
//
//package conveyorinterface.plctelegrams;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.charset.StandardCharsets;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.logging.Logger;
//import java.util.stream.Collectors;
//import java.util.Objects ;
//
//import platform.core.Repository.BSMRepository;
//import platform.core.Repository.TelegramRepository;
//import platform.core.log.Log;
//import platform.core.util.DBConnection;
//
//public class ScreeningResult {
//    private static final Logger log = Logger.getLogger("ScannerTelegramHandler");
//    private static final String logPrefix = "[SCANNER] ";
//    
//
//    public void handleScreeningResultTelegram(
//            OutputStream outputStream,
//            InputStream inputStream,
//            Log log,
//            String logPrefix
//    ) throws IOException {
//        // === Step 1: Read TPKT Header (4 bytes) ===
//        byte[] tpkt = new byte[4];
//        readFully(inputStream, tpkt, 0, 4);
//        if ((tpkt[0] & 0xFF) != 0x03) {
//            throw new IOException("Invalid TPKT version");
//        }
//        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);
//
//        // === Step 2: Read COTP Header (3 bytes) ===
//        byte[] cotp = new byte[3];
//        readFully(inputStream, cotp, 0, 3);
//        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80) {
//            throw new IOException("Invalid COTP header");
//        }
//
//        // === Step 3: Read Telegram Header (22 bytes) ===
//        byte[] header = new byte[22];
//        readFully(inputStream, header, 0, 22);
//        int index = 0;
//
//        int telegramType = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF); // TT
//        int telegramLength = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF); // LL
//        int subsystemId = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);   // SS
//        int component = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);     // CC
//        long globalId = ((long)(header[index++] & 0xFF) << 24) |
//                        ((long)(header[index++] & 0xFF) << 16) |
//                        ((long)(header[index++] & 0xFF) << 8)  |
//                        ((long)(header[index++] & 0xFF));      // GGGG
//        int plcIndex = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);       // XX
//        int location = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF);       // DD
//        int screeningLevel = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF); // JJ
//        int screeningResult = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF); // RR
//        int responseLength = ((header[index++] & 0xFF) << 8) | (header[index++] & 0xFF); // KK
//
//        // === Step 4: Read Optional Response Data ===
//        String responseAscii = "";
//        if (responseLength > 0) {
//            byte[] responseData = new byte[responseLength];
//            readFully(inputStream, responseData, 0, responseLength);
//            if (responseLength % 2 != 0 && responseData[responseLength - 1] == (byte) 0xFF) {
//                responseAscii = new String(responseData, 0, responseLength - 1, StandardCharsets.US_ASCII);
//            } else {
//                responseAscii = new String(responseData, StandardCharsets.US_ASCII);
//            }
//        }
//
//        // === Step 5: Log Telegram Details ===
//        String rxLog = String.format(
//            "telegram=<ScreeningResult>, length=<%d>, subsystemId=<%d>, component=<%d>, globalId=<%d>, plcIndex=<%d>, location=<%d>, level=<%d>, result=<%d>, responseLength=<%d>, responseAscii=<%s>",
//            telegramLength, subsystemId, component, globalId, plcIndex,
//            location, screeningLevel, screeningResult, responseLength, responseAscii
//        );
//        log.info(logPrefix, "Telegram [RX]: " + rxLog);
//
//        // === Step 6: Translate Result ===
//        String resultMeaning = translateResult(screeningResult);
//        String levelMeaning = translateLevel(screeningLevel);
//        log.info(logPrefix, String.format("Result interpreted: <%s>, Level interpreted: <%s>", resultMeaning, levelMeaning));
//
//        // === Step 7: Special Handling for Level 4 Push Button ===
//        if (screeningLevel / 10 == 4 && screeningResult == 2) {
//            log.info(logPrefix, "✅ Level 4 Clear by Operator Detected.");
//            // You may now respond, store, or take other action
//        }
//
//        // You can insert to database or respond back as needed
//    }
//
//    private String translateResult(int code) {
//        switch (code) {
//            case 0:   return "HBS Level Undefined";
//            case 11:  return "HBS Level 1";
//            case 12:  return "HBS Level 1a";
//            case 13:  return "HBS Level 1b";
//            case 21:  return "HBS Level 2";
//            case 22:  return "HBS Level 2a";
//            case 23:  return "HBS Level 2b";
//            case 31:  return "HBS Level 3";
//            case 32:  return "HBS Level 3a";
//            case 33:  return "HBS Level 3b";
//            case 41:  return "HBS Level 4";
//            case 42:  return "HBS Level 4a";
//            case 43:  return "HBS Level 4b";
//            case 51:  return "HBS Level 5";
//            case 52:  return "HBS Level 5a";
//            case 53:  return "HBS Level 5b";
//            case 100: return "Customs Screening";
//            default:  return "Unknown Screening Level: " + code;
//        }
//    }
//
//
//
//    private String translateLevel(int code) {
//        switch (code) {
//            case 0:   return "Undefined";
//            case 1:   return "Machine Alarm / Item Cleared";
//            case 2:   return "Machine Clear / Item Rejected";
//            case 3:   return "Error/unknown / Item Not Screened";
//            case 4:   return "Timeout of screening result (Rejected)";
//            case 5:   return "Machine Timeout / Decision Point Reached";
//            case 6:   return "Test Mode (Rejected)";
//            case 7:   return "Screening Fault (Rejected)";
//            case 8:   return "Reserved (Internal Use)";
//            case 9:   return "Obvious Threat / Screening Level Mode OFF (Cleared)";
//            case 10:  return "Obvious Threat (BLR T2 specific)";
//            case 11:  return "Level 2A Alarm / Send for Reconciliation (BLR T2 Level 3A)";
//            case 12:  return "Level 2A Clear / Send to TCU (BLR T2 Level 3B)";
//            case 13:  return "Level 2A Error";
//            case 15:  return "Level 2A Timeout";
//            case 21:  return "Level 2B Alarm";
//            case 22:  return "Level 2B Clear";
//            case 23:  return "Level 2B Error";
//            case 25:  return "Level 2B Timeout";
//            case 31:  return "Customs Alarm";
//            case 32:  return "Customs Clear";
//            case 33:  return "Customs Error";
//            case 35:  return "Customs Timeout";
//            case 91:  return "Alarm status from Level 3A ETD, Level 3B EDS and Customs recheck area";
//            case 92:  return "Clear status from Level 3A ETD, Level 3B EDS and Customs recheck area";
//            case 93:  return "Error status from Level 3A ETD, Level 3B EDS and Customs recheck area";
//            case 95:  return "Timeout status from Level 3A ETD, Level 3B EDS and Customs recheck area";
//            case 96:  return "Send for Reconciliation from Level 3A ETD";
//            case 97:  return "Send to TCU status from Level 3A ETD, Level 3B EDS";
//            case -1:  return "Unknown";
//            default:  return "Other/Unhandled Result: " + code;
//        }
//    }
//
//
//    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
//        while (length > 0) {
//            int read = in.read(buffer, offset, length);
//            if (read == -1) throw new IOException("Connection closed unexpectedly");
//            offset += read;
//            length -= read;
//        }
//    }
//
//
//
//
//}

// =================================================================================
// Base                 : Conveyor Sortation Controller
// Class                : ScreeningResult
// Programmer           : Giresh
// Release Date         : 2025-11-04
// Revision Number      : 2.0
// Description          : Full implementation of ITEM SCREENED telegram (TT=60)
// =================================================================================

package conveyorinterface.plctelegrams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import platform.core.log.Log;

public class ScreeningResult {

    /** Default Constructor */
    public ScreeningResult() {}

    /** Validate and handle Screening Result telegrams */
    public void validate(InputStream inputStream, OutputStream outputStream, Log log, String logPrefix) throws IOException {

        // === Step 1: Read TPKT Header (4 bytes) ===
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);
        if ((tpkt[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (tpkt[0] & 0xFF));

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);
        if (totalLength < 30)
            throw new IOException("Total telegram length too short: " + totalLength);

        log.info(logPrefix, "📦 Incoming ScreeningResult telegram total length: " + totalLength + " bytes");

        // === Step 2: Read COTP Header (3 bytes) ===
        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);
        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header. Expected [0x02, 0xF0, 0x80]");

        // === Step 3: Read telegram payload ===
        byte[] buffer = new byte[totalLength - 7];
        readFully(inputStream, buffer, 0, buffer.length);
        int index = 0;

        // === Step 4: Common Header Fields ===
        int channelId = getWord(buffer, index); index += 2;
        int version = getWord(buffer, index); index += 2;
        int sequenceNumber = getDWord(buffer, index); index += 4;
        int telegramType = getWord(buffer, index); index += 2;
        int telegramLength = getWord(buffer, index); index += 2;
        int subsystemId = getWord(buffer, index); index += 2;
        int component = getWord(buffer, index); index += 2;
        long globalId = getDWord(buffer, index); index += 4;
        int plcIndex = getWord(buffer, index); index += 2;

        // === Step 5: Read DD - Location ===
        int location = getWord(buffer, index); index += 2;

        // === Step 6: Read JJ - Screening Level ===
        int screeningLevel = getWord(buffer, index); index += 2;

        // === Step 7: Read RR - Screening Result ===
        int screeningResult = getWord(buffer, index); index += 2;

        // === Step 8: Read AAAAAAAAAA - IATA (10 bytes ASCII) ===
        byte[] iataBytes = new byte[10];
        System.arraycopy(buffer, index, iataBytes, 0, 10);
        String iata = new String(iataBytes, StandardCharsets.US_ASCII).trim();
        index += 10;

        // === Step 9: Read EE - Screening Result Status ===
        int screeningStatus = getWord(buffer, index); index += 2;

        // === Step 10: Read KK - Response Length ===
        int responseLength = getWord(buffer, index); index += 2;

        // === Step 11: Read a1a2...all (ASCII Data) ===
        String responseAscii = "";
        if (responseLength > 0 && index + responseLength <= buffer.length) {
            responseAscii = new String(buffer, index, responseLength, StandardCharsets.US_ASCII).trim();
            index += responseLength;

            // Append 0xFF if odd length
            if (responseLength % 2 != 0)
                responseAscii += "ÿ"; // 0xFF ASCII representation
        }

        // === Step 12: Log and Interpret ===
        log.info(logPrefix, "📥 ScreeningResult Raw Telegram: " + bytesToHex(buffer));
        log.info(logPrefix, String.format(
            "Parsed -> CH=%d | VER=%d | SEQ=%d | TYPE=%d | LEN=%d | SS=%d | CC=%d | GID=%d | PLC=%d | LOC=%d | LVL=%d | RES=%d | IATA=%s | STATUS=%d | RESP_LEN=%d | RESP_ASCII=%s",
            channelId, version, sequenceNumber, telegramType, telegramLength,
            subsystemId, component, globalId, plcIndex,
            location, screeningLevel, screeningResult, iata, screeningStatus, responseLength, responseAscii
        ));

        // === Step 13: Validate Telegram Type ===
        if (telegramType != 60)
            throw new IOException("Unexpected telegram type. Expected 60 (ScreeningResult) but got " + telegramType);

        // === Step 14: Interpret Levels & Results ===
        String levelDesc = translateLevel(screeningLevel);
        String resultDesc = translateResult(screeningResult);
        String trackStatus = (screeningStatus == 0) ? "Good Track" : "Lost Track";

        // === Step 15: IATA Handling ===
        if (iata.equals("0000000000"))
            log.info(logPrefix, "🧳 No ATR Read — IATA = 0000000000 (Untracked Bag)");
        else if (iata.equals("9999999999"))
            log.info(logPrefix, "🧳 Multi-label detected — IATA = 9999999999");

        // === Step 16: Conditional Behavior ===
        if (screeningLevel / 10 == 4 && screeningResult == 2)
            log.info(logPrefix, "✅ Level 4 Clear by Operator (Manual Intervention)");
        if (screeningResult == 9)
            log.info(logPrefix, "🚨 Obvious Threat Detected!");
        if (screeningResult == 91)
            log.info(logPrefix, "🚨 Alarm from Level 3A ETD / 3B EDS / Customs Area");
        if (screeningResult == 96)
            log.info(logPrefix, "🔄 Send for Reconciliation (Level 3A ETD)");
        if (screeningResult == 99)
            log.info(logPrefix, "🔁 Send to Rescreening by Customs");

        // === Step 17: Final Interpretation Log ===
        log.info(logPrefix, String.format(
            "🧩 Interpreted -> Level: %s | Result: %s | Track: %s",
            levelDesc, resultDesc, trackStatus
        ));

        log.info(logPrefix, "✅ ScreeningResult telegram parsed successfully.");
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private static int getWord(byte[] buf, int idx) {
        return ((buf[idx] & 0xFF) << 8) | (buf[idx + 1] & 0xFF);
    }

    private static int getDWord(byte[] buf, int idx) {
        return ((buf[idx] & 0xFF) << 24)
             | ((buf[idx + 1] & 0xFF) << 16)
             | ((buf[idx + 2] & 0xFF) << 8)
             | (buf[idx + 3] & 0xFF);
    }

    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1)
                throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    private String translateLevel(int code) {
        return switch (code) {
            case 0 -> "HBS Level Undefined";
            case 11 -> "HBS Level 1";
            case 12 -> "HBS Level 1a";
            case 13 -> "HBS Level 1b";
            case 21 -> "HBS Level 2";
            case 22 -> "HBS Level 2a";
            case 23 -> "HBS Level 2b";
            case 31 -> "HBS Level 3";
            case 32 -> "HBS Level 3a";
            case 33 -> "HBS Level 3b";
            case 41 -> "HBS Level 4";
            case 42 -> "HBS Level 4a";
            case 43 -> "HBS Level 4b";
            case 51 -> "HBS Level 5";
            case 52 -> "HBS Level 5a";
            case 53 -> "HBS Level 5b";
            case 100 -> "Customs Screening Level 2";
            case 101 -> "Customs Screening Level 3";
            case 102 -> "Customs Screening Level 4 (MES L4)";
            default -> "Other/Unknown Level: " + code;
        };
    }

    private String translateResult(int code) {
        return switch (code) {
            case 0 -> "Undefined";
            case 1 -> "Machine Alarm";
            case 2 -> "Machine Clear";
            case 3 -> "Error/Unknown";
            case 5 -> "Machine Timeout";
            case 9 -> "Obvious Threat";
            case 11 -> "Level 2A Alarm";
            case 12 -> "Level 2A Clear";
            case 13 -> "Level 2A Error";
            case 15 -> "Level 2A Timeout";
            case 21 -> "Level 2B Alarm";
            case 22 -> "Level 2B Clear";
            case 23 -> "Level 2B Error";
            case 25 -> "Level 2B Timeout";
            case 31 -> "Customs Alarm";
            case 32 -> "Customs Clear";
            case 33 -> "Customs Error";
            case 35 -> "Customs Timeout";
            case 91 -> "Alarm from L3A ETD / L3B EDS / Customs";
            case 92 -> "Clear from L3A ETD / L3B EDS / Customs";
            case 93 -> "Error from L3A ETD / L3B EDS / Customs";
            case 95 -> "Timeout from L3A ETD / L3B EDS / Customs";
            case 96 -> "Send for Reconciliation (3A ETD)";
            case 97 -> "Send to TCU (3A ETD / 3B EDS)";
            case 98 -> "Send to Customs (3A ETD)";
            case 99 -> "Send to Rescreening by Customs";
            case -1 -> "Unknown";
            default -> "Unknown code: " + code;
        };
    }
}
