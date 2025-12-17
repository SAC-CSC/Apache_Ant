// Base                 : Conveyor Sortaion Controller
// Class                : TelegramDispatcher Class
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

import platform.core.Repository.AirlineAllocationRepository;
import platform.core.log.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import Entity.AirlineAllocation;
import rfc.RFC1006TSAPHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import conveyorinterface.frontend.TelegramSenderUI;
import conveyorinterface.plctelegrams.ScannerResultTlgWithVersionNo;

public class TelegramDispatcher {

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Log log;
    private final String logPrefix;
    static final byte TPKT_VERSION = (byte) 0x03;
    static final byte TPKT_RESERVE = (byte) 0x00;
    private final Runnable reconnectCallback;
    public TelegramDispatcher(InputStream inputStream, OutputStream outputStream, Log log, String logPrefix ,  Runnable reconnectCallback) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.log = log;
        this.logPrefix = logPrefix;
        this.reconnectCallback = reconnectCallback;
    }

    public void startDispatchLoop() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    dispatchOneTelegram();
                } catch (IOException e) {
                    log.error(logPrefix, "❌ Telegram dispatch failed: " + e.getMessage(), e);

                    // Trigger reconnection
                    reconnectCallback.run();

                    // Exit this loop/thread after failure
                    break;
                } catch (Exception e) {
                    log.error(logPrefix, "❌ Unexpected error in dispatch loop: " + e.getMessage(), e);
                    reconnectCallback.run();
                    break;
                }
            }
        }, "TelegramDispatcherThread").start();
    }

    protected String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
    
    private void dispatchOneTelegram() throws IOException {
        // === Step 1: Read TPKT Header ===
    	readAvailableFromPLC(inputStream);
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);
        if ((tpkt[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: " + (tpkt[0] & 0xFF));

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);
        if (totalLength < 25) {	
        	  byte[] KeepAliveTelegram = new byte[] {
        	            TPKT_VERSION, TPKT_RESERVE, 0x00, 0x08, 0x02, (byte) 0xf0, (byte) 0x80, 0x00
        	        };
          outputStream.write(KeepAliveTelegram);  
          log.info(logPrefix, "Sending KeepAliveTelegram telegram: " + bytesToHex(KeepAliveTelegram));
          return ;
        }
           // throw new IOException("Corrupt telegram. Length too short: " + totalLength);

        log.info(logPrefix, "📦 Incoming telegram total length: " + totalLength + " bytes");

        // === Step 2: Read Remaining Data ===
        byte[] buffer = new byte[totalLength - 4]; // Already read 4 bytes for TPKT
        readFully(inputStream, buffer, 0, buffer.length);

        // === Step 3: Validate COTP ===
        if ((buffer[0] & 0xFF) != 0x02 || (buffer[1] & 0xFF) != 0xF0 || (buffer[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header"); 

        // === Step 4: Extract Channel Header & Telegram ===
        int channelId = ((buffer[3] & 0xFF) << 8) | (buffer[4] & 0xFF);
        int version = ((buffer[5] & 0xFF) << 8) | (buffer[6] & 0xFF);
        int sequenceNumber = ((buffer[7] & 0xFF) << 24) | ((buffer[8] & 0xFF) << 16)
                           | ((buffer[9] & 0xFF) << 8) | (buffer[10] & 0xFF);

        int telegramType = ((buffer[11] & 0xFF) << 8) | (buffer[12] & 0xFF);
        int telegramLength = ((buffer[13] & 0xFF) << 8) | (buffer[14] & 0xFF);

        // === Step 5: Log Info ===
        log.info(logPrefix, String.format(
            "📥 Received telegram: ChannelID=%d | Version=%d | SeqNo=%d | Type=%d | Length=%d words",
            channelId, version, sequenceNumber, telegramType, telegramLength));

        // === Step 6: Handle Type ===
        switch (telegramType) {
            case 26: log.info(logPrefix, "SCANNER RESULT telegram received"); 
            ScannerResultTlgWithVersionNo ScannerResult = new ScannerResultTlgWithVersionNo() ;
            ScannerResult.handleScannerTelegram(outputStream , inputStream, log , logPrefix) ;       
            break;
            case 40: log.info(logPrefix, "ITEM ENTER telegram received");
            ItemEnter itemEnter = new ItemEnter() ;
            itemEnter.validate( inputStream, log , logPrefix) ;
            break;
            case 46: log.info(logPrefix, "ITEM LOST telegram received");
            ItemLost itemLost = new ItemLost() ;
            itemLost.validate( inputStream, log , logPrefix) ;
            break;
            case 47: log.info(logPrefix, "ITEM STRAY telegram received");
            ItemStray itemStray = new ItemStray() ;
            itemStray.validate( inputStream, log , logPrefix) ;
            break;
            case 50: log.info(logPrefix, "Item Transfer telegram received");
            ItemTransfer itemTransfer = new ItemTransfer() ;
            itemTransfer.validate( inputStream, log , logPrefix) ;
            break;
            case 51: log.info(logPrefix, "Item Exit telegram received");
            ItemExit itemExit = new ItemExit() ;
            itemExit.validate( inputStream, log , logPrefix) ;
            break;
            case 44: log.info(logPrefix, "ITEM DESTINATION ACKNOWLEDGEMENT telegram received");
            ScannerResultTlgWithVersionNo AckDestination = new ScannerResultTlgWithVersionNo() ;
            AckDestination.validate_AckDestination( inputStream, log , logPrefix) ;
            break;           
            case 164: log.info(logPrefix, "ITEM INFO REQUEST  telegram received");
            ItemInfoRequest handler = new ItemInfoRequest();
             // Automatically parse request and send response
             handler.respondToRequest(inputStream, outputStream, log, logPrefix, 
                                  11, 11, 31, 12, 1, 1); // example screening/customs/EBS values
            break; 
            case 112: log.info(logPrefix, "Key Switch result telegram received"); 
            KeySwitch keySwitch = new KeySwitch() ;
            keySwitch.validate(inputStream , log , logPrefix) ;      
            break;
            case 60: log.info(logPrefix, "ScreeningResult telegram received"); 
            ScreeningResult screeningResult = new ScreeningResult() ;
            screeningResult.validate(inputStream , outputStream, log , logPrefix) ;   
            break;
            case 161: log.info(logPrefix, "AirlineCodeTableComplete telegram received"); 
            AirlineCodeTableComplete airlineCodeTableComplete = new AirlineCodeTableComplete() ;
            airlineCodeTableComplete.validate(inputStream, log , logPrefix) ;   
            break;
            case 160: log.info(logPrefix, "FallBackTagTableComplete telegram received"); 
            FallBackTagTableComplete fallBackTagTableComplete = new FallBackTagTableComplete() ;
            fallBackTagTableComplete.validate(inputStream, log , logPrefix) ;   
            break;
            default:
                log.info(logPrefix, "⚠️ Unknown telegram type: " + telegramType);
        }
        // === Step 7: Always send ACK ===
        sendAck(sequenceNumber, channelId, version);
    }
      
    public void startTriggerServer() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(6000)) {
                System.out.println("Trigger server listening on port 6000...");
                while (true) {
                    Socket client = server.accept();
                    byte[] buf = new byte[256];
                    int read = client.getInputStream().read(buf);
                    String message = new String(buf, 0, read).trim();
                    if ("START_SENDING".equals(message)) {
                        System.out.println("Trigger received, sending airline telegrams...");
                        sendAllEnabledEntries();
                    }
                    client.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendAllEnabledEntries() {

        AirlineAllocationRepository repo = new AirlineAllocationRepository();
        List<AirlineAllocation> entries = repo.getEnabledEntries();

        log.info(logPrefix, "Sending " + entries.size() + " enabled airline entries...");

        sendAirlineCodeTableStart(1);   // Start telegram only once

        int count = 0;

        for (AirlineAllocation a : entries) {
            try {
                // Skip if disabled or deleted
                if (!Boolean.TRUE.equals(a.getEnableStatus()) || Boolean.TRUE.equals(a.getDeleted())) {
                    log.info(logPrefix, "Skipping disabled/deleted airline: " + a.getAirlineCode());
                    continue; // skip this entry, but DO NOT stop loop
                }

                // Send entry telegram
                sendAirlineCodeTableEntry(a);
                count++;

            } catch (Exception e) {
                log.error(logPrefix, "Failed to send telegram for " + a.getAirlineCode(), e);
            }
        }

        sendAirlineCodeTableEnd(count);     // End telegram

        log.info(logPrefix, "✅ Finished sending all airline telegrams (" + count + " entries sent)");
    }


    
    public void sendAirlineCodeTableStart(int id) {
    	int subsystemId = id;         // your subsystem ID
    	int channelId = 7;            // fixed channel ID
    	int version = 1;              // fixed protocol version
    	// Generate a random sequence number or use an incrementing counter
    	int sequenceNumber = (int) (Math.random() * 1000000); // random 0 to 999999
    	AirlineCodeTableStartTlg airlineCodeTableStartTlg = 
    	        new AirlineCodeTableStartTlg(subsystemId, channelId, version, sequenceNumber);
    	try {
			airlineCodeTableStartTlg.send(outputStream ,log , logPrefix);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public void sendAirlineCodeTableEntry(AirlineAllocation a) {

        String airlineCode = a.getAirlineCode();       // from DB
        int destination = a.getSortPosition();          // from DB
        int numAltDest = 2;            // from DB
        int alt1 = 101;                         // from DB
        int alt2 = 102;                         // from DB
        int alt3 = 0;                         // from DB
        int subsystemId = 1;                            // fixed or configurable
        int channelId = 7;                              // fixed
        int version = 1;                                // fixed
        int sequenceNumber = (int) (Math.random() * 1000000); // random sequence

        AirlineCodeEntryTlg airlineCodeEntryTlg = new AirlineCodeEntryTlg(
                subsystemId,
                channelId,
                version,
                sequenceNumber,
                airlineCode,
                destination,
                numAltDest,
                alt1,
                alt2,
                alt3
        );
        try {
            airlineCodeEntryTlg.send(outputStream, log, logPrefix);
            log.info(logPrefix, "✅ Sent AirlineCodeEntry for: " + airlineCode + " → Dest: " + destination);
        } catch (IOException e) {
            log.error(logPrefix, "❌ Failed to send AirlineCodeEntry for: " + airlineCode, e);
        }
    }
 
    public void sendAirlineCodeTableEnd(int count_entry) {
    	int subsystemId = 1;         // your subsystem ID
    	int channelId = 7;            // fixed channel ID
    	int version = 1;              // fixed protocol version
    	// Generate a random sequence number or use an incrementing counter
    	int sequenceNumber = (int) (Math.random() * 1000000); // random 0 to 999999
    	int count = count_entry ;
    	AirlineCodeTableEndTlg AirlineCodeTableEndTlg = 
    	        new AirlineCodeTableEndTlg(subsystemId, channelId, version, sequenceNumber, count);
    	try {
    		AirlineCodeTableEndTlg.send(outputStream ,log , logPrefix);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    // Fall back tabel
    public void sendFallBackTableStart(int id) {
    	int subsystemId = id;         // your subsystem ID
    	int channelId = 7;            // fixed channel ID
    	int version = 1;              // fixed protocol version
    	// Generate a random sequence number or use an incrementing counter
    	int sequenceNumber = (int) (Math.random() * 1000000); // random 0 to 999999
    	FallbackTagTableStartTlg fallBackTableStartTlg = 
    	        new FallbackTagTableStartTlg(subsystemId, channelId, version, sequenceNumber);
    	try {
    		fallBackTableStartTlg.send(outputStream ,log , logPrefix);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void sendFallbackTagEntryTlg(int id) {
        String fallbackTag = "CHAITANYA1"; // 10 ASCII chars, pad with zeros if needed
        int destination = 101;
        int screeningLevel = 11; // Example: HBS Level 1
        int numAltDest = 3;
        int alt1 = 101;
        int alt2 = 100;
        int alt3 = 0;
        int subsystemId = 1;
        int channelId = 7;
        int version = 1;
        int sequenceNumber = (int) (Math.random() * 1000000); // random sequence

        FallbackTagEntryTlg fallbackTagEntryTlg = new FallbackTagEntryTlg(
                subsystemId,
                channelId,
                version,
                sequenceNumber,
                fallbackTag,
                destination,
                screeningLevel,
                numAltDest,
                alt1,
                alt2,
                alt3
        );

        try {
            fallbackTagEntryTlg.send(outputStream, log, logPrefix);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

 
    public void sendFallBackTableEnd(int id) {
    	int subsystemId = id;         // your subsystem ID
    	int channelId = 7;            // fixed channel ID
    	int version = 1;              // fixed protocol version
    	// Generate a random sequence number or use an incrementing counter
    	int sequenceNumber = (int) (Math.random() * 1000000); // random 0 to 999999
    	int count = 10 ;
    	FallbackTagTableEndTlg fallbackTagTableEndTlg = 
    	        new FallbackTagTableEndTlg(subsystemId, channelId, version, sequenceNumber, count);
    	try {
    		fallbackTagTableEndTlg.send(outputStream ,log , logPrefix);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void sendAck(int sequenceNumber, int channelId, int version) throws IOException {
        byte[] channelHeader = new byte[]{
            (byte)(channelId >> 8), (byte)channelId,
            (byte)(version >> 8), (byte)version,
            (byte)(sequenceNumber >> 24), (byte)(sequenceNumber >> 16),
            (byte)(sequenceNumber >> 8), (byte)sequenceNumber
        };

        byte[] ackPayload = new byte[]{
            0x00, (byte) 0x66, // TT
            0x00, 0x04,        // LL
            (byte)(sequenceNumber >> 24), (byte)(sequenceNumber >> 16),
            (byte)(sequenceNumber >> 8), (byte)sequenceNumber
        };

        byte[] fullPayload = new byte[channelHeader.length + ackPayload.length];
        System.arraycopy(channelHeader, 0, fullPayload, 0, channelHeader.length);
        System.arraycopy(ackPayload, 0, fullPayload, channelHeader.length, ackPayload.length);

        int totalLength = fullPayload.length + 7;
        byte[] telegram = new byte[totalLength];
        telegram[0] = 0x03;
        telegram[1] = 0x00;
        telegram[2] = (byte)(totalLength >> 8);
        telegram[3] = (byte)(totalLength);
        telegram[4] = 0x02;
        telegram[5] = (byte) 0xF0;
        telegram[6] = (byte) 0x80;

        System.arraycopy(fullPayload, 0, telegram, 7, fullPayload.length);

        log.info(logPrefix, "📤 Sending ACK (TT=102) for ChannelID=" + channelId + " | SeqNo=" + sequenceNumber);
        outputStream.write(telegram);
        outputStream.flush();
    }
    

    private void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1) throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }
    
    private byte[] readAvailableFromPLC(InputStream in) throws IOException {
        int available = in.available(); // Check how many bytes are available to read
        if (available == 0) return new byte[0]; // No data to read

        byte[] buffer = new byte[available];
        int bytesRead = in.read(buffer);
        if (bytesRead == -1) throw new IOException("PLC connection closed unexpectedly");

        // Log raw data in hex format
        StringBuilder sb = new StringBuilder("📥 Raw buffer from PLC (" + bytesRead + " bytes): ");
        for (int i = 0; i < bytesRead; i++) {
            sb.append(String.format("%02X ", buffer[i]));
        }
        log.info(logPrefix ,"@randomedata@" + sb.toString().trim());


        return buffer;
    }

}
