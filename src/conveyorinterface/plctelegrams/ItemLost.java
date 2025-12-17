// Base                 : Conveyor Sortation Controller
// Class                : ItemTlg Class
// Programmer           : Giresh
// Release Date         : 2025-07-23
// Revision Number      : 1.0
// Description          : Code module to construct and parse ITEM telegrams
// =================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// ---------------------------------------------------------------------------------
//01.00    2025.07.23    Giresh        Initial Version
//
//package conveyorinterface.plctelegrams;
//
//import platform.io.telegraph.AbstractTelegram;
//import platform.core.log.Log;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//public class ItemLost extends AbstractTelegram {
//
//    public static void validate(InputStream inputStream, Log log, String logPrefix) throws IOException {
//        byte[] header = new byte[4];
//        readFully(inputStream, header, 0, 4);
//
//        if ((header[0] & 0xFF) != 0x03)
//            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (header[0] & 0xFF));
//
//        int totalLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
//        if (totalLength < 25)
//            throw new IOException("Total telegram length too short: expected at least 25 bytes but got " + totalLength);
//
//        byte[] cotp = new byte[3];
//        readFully(inputStream, cotp, 0, 3);
//        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80)
//            throw new IOException("Invalid COTP header. Expected [0x02, 0xF0, 0x80]");
//
//        byte[] payload = new byte[18];
//        readFully(inputStream, payload, 0, 18);
//
//        int telegramType = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
//        int telegramLength = ((payload[2] & 0xFF) << 8) | (payload[3] & 0xFF);
//        int subsystemId = ((payload[4] & 0xFF) << 8) | (payload[5] & 0xFF);
//        int subsystemComponent = ((payload[6] & 0xFF) << 8) | (payload[7] & 0xFF);
//        int globalId = ((payload[8] & 0xFF) << 24) | ((payload[9] & 0xFF) << 16)
//                     | ((payload[10] & 0xFF) << 8) | (payload[11] & 0xFF);
//        int plcIndex = ((payload[12] & 0xFF) << 8) | (payload[13] & 0xFF);
//        int location = ((payload[14] & 0xFF) << 8) | (payload[15] & 0xFF);
//        
//        log.info(logPrefix, "Item Lost telegram: " + bytesToHex(buffer));
//        log.info(logPrefix, String.format(
//                "Received ITEM LOST telegram: TT=%d, LL=%d, SS=%d, CC=%d, GID=%d, PLCIdx=%d, Dest=%d, AltDest=%d",
//                telegramType, telegramLength, subsystemId, subsystemComponent,
//                globalId, plcIndex, location
//        ));
//
//        if (telegramType != 40)
//            throw new IOException("Unexpected telegram type. Expected 46 (ITEM LOST) but got " + telegramType);
//    }
//
//    @Override
//    public int getTelegramType() {
//        return 46; // TT = 46 ItemLost
//    }
//
//    @Override
//    public int getTelegramLength() {
//        return 8; // 8 words = 16 bytes
//    }
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
//}





package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.InputStream;

public class ItemLost extends AbstractTelegram {

    /** No-args constructor */
    public ItemLost() {
    }

    /** Validate telegram received from PLC */
    public void validate(InputStream inputStream, Log log, String logPrefix) throws IOException {
        // === Step 1: Read TPKT header (4 bytes) ===
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);
        if ((tpkt[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (tpkt[0] & 0xFF));

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);
        if (totalLength < 25)
            throw new IOException("Total telegram length too short: " + totalLength);

        log.info(logPrefix, "📦 Incoming Item Lost telegram total length: " + totalLength + " bytes");

        // === Step 2: Read remaining data ===
        byte[] buffer = new byte[totalLength - 4]; // subtract TPKT header
        readFully(inputStream, buffer, 0, buffer.length);

        // === Step 3: Validate COTP header (3 bytes) ===
        if ((buffer[0] & 0xFF) != 0x02 || (buffer[1] & 0xFF) != 0xF0 || (buffer[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header");

        // === Step 4: Extract headers ===
        int channelId = ((buffer[3] & 0xFF) << 8) | (buffer[4] & 0xFF);
        int version = ((buffer[5] & 0xFF) << 8) | (buffer[6] & 0xFF);
        int sequenceNumber = ((buffer[7] & 0xFF) << 24) | ((buffer[8] & 0xFF) << 16)
                           | ((buffer[9] & 0xFF) << 8) | (buffer[10] & 0xFF);

        int telegramType = ((buffer[11] & 0xFF) << 8) | (buffer[12] & 0xFF);
        int telegramLength = ((buffer[13] & 0xFF) << 8) | (buffer[14] & 0xFF);
        int subsystemId = ((buffer[15] & 0xFF) << 8) | (buffer[16] & 0xFF);
        int subsystemComponent = ((buffer[17] & 0xFF) << 8) | (buffer[18] & 0xFF);
        int globalId = ((buffer[19] & 0xFF) << 24) | ((buffer[20] & 0xFF) << 16)
                   | ((buffer[21] & 0xFF) << 8) | (buffer[22] & 0xFF);
        int plcIndex = ((buffer[23] & 0xFF) << 8) | (buffer[24] & 0xFF);
        int location = ((buffer[25] & 0xFF) << 8) | (buffer[26] & 0xFF);
        int reason = ((buffer[27] & 0xFF) << 8) | (buffer[28] & 0xFF);
        log.info(logPrefix, "📥 Received Telegram: " + bytesToHex(buffer));
        log.info(logPrefix, String.format(
            "Header Info -> ChannelID=%d | Version=%d | SeqNo=%d | Type=%d | Length=%d words | SubsystemID=%d | Component=%d | GlobalID=%d | PLC Index=%d | Location=%d |reason=%d",
            channelId, version, sequenceNumber, telegramType, telegramLength,
            subsystemId, subsystemComponent, globalId, plcIndex, location ,reason
        ));

        // === Step 6: Check telegram type ===
        if (telegramType != 46)
            throw new IOException("Unexpected telegram type. Expected 40 (ITEM ENTER) but got " + telegramType);

        log.info(logPrefix, "✅ ITEM Lost telegram received successfully");
    }

    @Override
    public int getTelegramType() {
        return 40; // TT = 40 decimal
    }

    @Override
    public int getTelegramLength() {
        return 9; // 9 words = 18 bytes payload
    }

    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1) throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }
}

