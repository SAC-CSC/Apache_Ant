// =================================================================================
// Base                 : Conveyor Sortation Controller
// Class                : ItemTransfer Class
// Programmer           : Giresh
// Release Date         : 2025-07-23
// Revision Number      : 1.0
// Description          : Code module to construct and parse ITEM TRANSFER telegrams
// =================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// ---------------------------------------------------------------------------------
//01.00    2025.07.23    Giresh        Initial Version
// =================================================================================

package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.InputStream;

public class ItemTransfer extends AbstractTelegram {

    /** No-args constructor */
    public ItemTransfer() {
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

        log.info(logPrefix, "📦 Incoming Item Transfer telegram total length: " + totalLength + " bytes");

        // === Step 2: Read remaining data ===
        byte[] buffer = new byte[totalLength - 4];
        readFully(inputStream, buffer, 0, buffer.length);

        // === Step 3: Validate COTP header ===
        if ((buffer[0] & 0xFF) != 0x02 || (buffer[1] & 0xFF) != 0xF0 || (buffer[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header");

        // === Step 4: Extract header information ===
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
        int event = ((buffer[25] & 0xFF) << 8) | (buffer[26] & 0xFF);
        int location = ((buffer[27] & 0xFF) << 8) | (buffer[28] & 0xFF);
        int handshake = ((buffer[29] & 0xFF) << 8) | (buffer[30] & 0xFF);

        // === Step 5: Interpret event and handshake ===
        String eventString = (event == 0) ? "Enter" : (event == 1) ? "Leave" : "Unknown";
        String handshakeString = (handshake >= 1 && handshake <= 7)
                ? "Valid Handshake No"
                : (handshake == 8) ? "Stray Item" : "Unknown";

        // === Step 6: Log details ===
        log.info(logPrefix, "📥 Item Transfer Raw Telegram: " + bytesToHex(buffer));
        log.info(logPrefix, String.format(
            "Received ITEM TRANSFER telegram -> ChannelID=%d | Version=%d | SeqNo=%d | Type=%d | Length=%d | SS=%d | CC=%d | GID=%d | PLCIdx=%d | Event=%s | Location=%d | Handshake=%s",
            channelId, version, sequenceNumber, telegramType, telegramLength,
            subsystemId, subsystemComponent, globalId, plcIndex, eventString, location, handshakeString
        ));

        // === Step 7: Validate telegram type ===
        if (telegramType != 50)
            throw new IOException("Unexpected telegram type. Expected 50 (ITEM TRANSFER) but got " + telegramType);

        log.info(logPrefix, "✅ ITEM TRANSFER telegram received successfully");
    }

    @Override
    public int getTelegramType() {
        return 50; // TT = 50 decimal
    }

    @Override
    public int getTelegramLength() {
        return 10; // 10 words = 20 bytes payload
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
