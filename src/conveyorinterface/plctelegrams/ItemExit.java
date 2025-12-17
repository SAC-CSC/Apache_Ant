// =================================================================================
// Base                 : Conveyor Sortation Controller
// Class                : ItemExit Class
// Programmer           : Giresh
// Release Date         : 2025-07-23
// Revision Number      : 1.0
// Description          : Code module to construct and parse ITEM EXIT telegrams
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

public class ItemExit extends AbstractTelegram {

    /** Default Constructor */
    public ItemExit() {
    }

    /** Validate telegram received from PLC */
    public  void validate(InputStream inputStream, Log log, String logPrefix) throws IOException {
        // === Step 1: Read TPKT header (4 bytes) ===
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);
        if ((tpkt[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (tpkt[0] & 0xFF));

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);
        if (totalLength < 25)
            throw new IOException("Total telegram length too short: " + totalLength);

        log.info(logPrefix, "📦 Incoming Item Exit telegram total length: " + totalLength + " bytes");

        // === Step 2: Read COTP header (3 bytes) ===
        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);
        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header. Expected [0x02, 0xF0, 0x80]");

        // === Step 3: Read remaining telegram payload ===
        byte[] buffer = new byte[totalLength - 7];
        readFully(inputStream, buffer, 0, buffer.length);

        // === Step 4: Extract telegram fields ===
        int channelId = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
        int version = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        int sequenceNumber = ((buffer[4] & 0xFF) << 24) | ((buffer[5] & 0xFF) << 16)
                           | ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
        int telegramType = ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);
        int telegramLength = ((buffer[10] & 0xFF) << 8) | (buffer[11] & 0xFF);
        int subsystemId = ((buffer[12] & 0xFF) << 8) | (buffer[13] & 0xFF);
        int subsystemComponent = ((buffer[14] & 0xFF) << 8) | (buffer[15] & 0xFF);
        int globalId = ((buffer[16] & 0xFF) << 24) | ((buffer[17] & 0xFF) << 16)
                     | ((buffer[18] & 0xFF) << 8) | (buffer[19] & 0xFF);
        int plcIndex = ((buffer[20] & 0xFF) << 8) | (buffer[21] & 0xFF);
        int location = ((buffer[22] & 0xFF) << 8) | (buffer[23] & 0xFF);

        // === Step 5: Log received data ===
        log.info(logPrefix, "📥 ITEM EXIT Raw Telegram: " + bytesToHex(buffer));
        log.info(logPrefix, String.format(
            "Received ITEM EXIT telegram -> ChannelID=%d | Version=%d | SeqNo=%d | Type=%d | Length=%d | SS=%d | CC=%d | GID=%d | PLCIdx=%d | Location=%d",
            channelId, version, sequenceNumber, telegramType, telegramLength,
            subsystemId, subsystemComponent, globalId, plcIndex, location
        ));

        // === Step 6: Validate telegram type ===
        if (telegramType != 51)
            throw new IOException("Unexpected telegram type. Expected 51 (ITEM EXIT) but got " + telegramType);

        log.info(logPrefix, "✅ ITEM EXIT telegram parsed successfully");
    }

    @Override
    public int getTelegramType() {
        return 51; // TT = 51 ItemExit
    }

    @Override
    public int getTelegramLength() {
        return 8; // 8 words = 16 bytes payload
    }

    /** Utility function to read bytes safely */
    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1) throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }
}
