// =================================================================================
// Base                 : Conveyor Sortation Controller
// Class                : KeySwitch Class
// Programmer           : Giresh
// Release Date         : 2025-11-06
// Revision Number      : 1.2
// Description          : Code module to parse KEY SWITCH (112) telegram from PLC
// =================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// ---------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version
//01.01    2025.11.06    Giresh         Implemented Key Switch (112) PLC => CSC logic
//01.02    2025.11.06    Giresh         Refactored with channelId, version, seqNo parsing
// =================================================================================

package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handles the Key Switch (112) Telegram from PLC => CSC
 * 
 * Format (PLC → CSC):
 * - Channel ID (2 bytes)
 * - Version (2 bytes)
 * - Sequence Number (4 bytes)
 * - TT : Telegram Type = 112
 * - LL : Telegram Length = 06
 * - SS : Subsystem ID (PLC System)
 * - CC : Subsystem Component (Optional)
 * - KK : Key Status (0=Off, 1=On)
 * - DD : Location (1=ByPass Mode, 2=LLC Mode)
 */
public class KeySwitch extends AbstractTelegram {

    /** Default Constructor */
    public KeySwitch() {}

    /** Validate telegram received from PLC */
    public void validate(InputStream inputStream, Log log, String logPrefix) throws IOException {

        // === Step 1: Read TPKT header (4 bytes) ===
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);
        if ((tpkt[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (tpkt[0] & 0xFF));

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);
        if (totalLength < 25)
            throw new IOException("Invalid total telegram length: " + totalLength);

        log.info(logPrefix, "📦 Incoming KEY SWITCH (112) telegram total length: " + totalLength + " bytes");

        // === Step 2: Read COTP header (3 bytes) ===
        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);
        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header. Expected [0x02, 0xF0, 0x80]");

        // === Step 3: Read remaining telegram payload ===
        byte[] buffer = new byte[totalLength - 7];
        readFully(inputStream, buffer, 0, buffer.length);

        // === Step 4: Extract common telegram header fields ===
        int channelId       = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
        int version         = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        int sequenceNumber  = ((buffer[4] & 0xFF) << 24) | ((buffer[5] & 0xFF) << 16)
                            | ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
        int telegramType    = ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);
        int telegramLength  = ((buffer[10] & 0xFF) << 8) | (buffer[11] & 0xFF);
        int subsystemId     = ((buffer[12] & 0xFF) << 8) | (buffer[13] & 0xFF);
        int component       = ((buffer[14] & 0xFF) << 8) | (buffer[15] & 0xFF);
        int keyStatus       = ((buffer[16] & 0xFF) << 8) | (buffer[17] & 0xFF);
        int location        = ((buffer[18] & 0xFF) << 8) | (buffer[19] & 0xFF);

        // === Step 5: Validate telegram type ===
        if (telegramType != 112)
            throw new IOException("Unexpected telegram type. Expected 112 (KEY SWITCH) but got " + telegramType);

        // === Step 6: Interpret field values ===
        String keyState = (keyStatus == 0) ? "OFF" : (keyStatus == 1) ? "ON" : "UNKNOWN";
        String mode = switch (location) {
            case 1 -> "ByPass Mode";
            case 2 -> "LLC Mode";
            default -> "Unknown Mode";
        };

        // === Step 7: Log received telegram ===
        log.info(logPrefix, "📥 KEY SWITCH (112) Raw Telegram: " + bytesToHex(buffer));
        log.info(logPrefix, String.format(
                "Received KEY SWITCH telegram -> ChannelID=%d | Version=%d | SeqNo=%d | Type=%d | Len=%d | SS=%d | CC=%d | KeyStatus=%s | Location=%s",
                channelId, version, sequenceNumber, telegramType, telegramLength,
                subsystemId, component, keyState, mode
        ));

        // === Step 8: Validate telegram length ===
        if (telegramLength != 6)
            throw new IOException("Unexpected telegram length. Expected 6 but got " + telegramLength);

        log.info(logPrefix, "✅ KEY SWITCH (112) telegram parsed successfully");
    }

    @Override
    public int getTelegramType() {
        return 112;
    }

    @Override
    public int getTelegramLength() {
        return 6;
    }

    /** Utility: Read fixed number of bytes */
    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1)
                throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }
}
