// =================================================================================
// Base                 : Conveyor Sortation Controller
// Class                : ItemEnter Class
// Programmer           : Giresh
// Release Date         : 2025-11-06
// Revision Number      : 1.2
// Description          : Code module to parse ITEM ENTER (40) telegram from PLC => CSC
// =================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// ---------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version
//01.01    2025.11.06    Giresh         Implemented ITEM ENTER (40) PLC => CSC logic
//01.02    2025.11.06    Giresh         Refactored with ChannelId, Version, SeqNo, field extraction
// =================================================================================

package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handles the ITEM ENTER (40) Telegram from PLC => Host (CSC)
 *
 * <pre>
 * Direction: PLC → Host
 * 
 * Format:
 *   TT : 40  = Telegram Type
 *   LL : 09  = Telegram Length
 *   SS : Subsystem ID (PLC System)
 *   CC : Subsystem Component (Optional)
 *   GGGG : Global ID (Item unique ID)
 *   XX : PLC Index
 *   LL : Location (Entry point identification)
 *   DD : Destination (Optional)
 * 
 * Usage:
 * - Sent when an item enters from an untracked area.
 * - Used for logging only (not for reporting).
 * </pre>
 */
public class ItemEnter extends AbstractTelegram {

    /** Default Constructor */
    public ItemEnter() {}

    /** Validate telegram received from PLC */
  
    public void validate(InputStream inputStream, Log log, String logPrefix) throws IOException {

        // === Step 1: Read TPKT header (4 bytes) ===
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);

        if ((tpkt[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (tpkt[0] & 0xFF));

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);
        if (totalLength < 30)
            throw new IOException("Invalid total telegram length: " + totalLength);

        log.info(logPrefix, "📦 Incoming ITEM ENTER (40) telegram total length: " + totalLength + " bytes");

        // === Step 2: Read COTP header (3 bytes) ===
        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);

        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header: Expected [0x02, 0xF0, 0x80]");

        // === Step 3: Read remaining telegram payload ===
        byte[] buffer = new byte[totalLength - 7]; // minus TPKT(4) + COTP(3)
        readFully(inputStream, buffer, 0, buffer.length);

        // === Step 4: Extract common header fields ===
        int channelId       = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
        int version         = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        int sequenceNumber  = ((buffer[4] & 0xFF) << 24) | ((buffer[5] & 0xFF) << 16)
                            | ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
        int telegramType    = ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);
        int telegramLength  = ((buffer[10] & 0xFF) << 8) | (buffer[11] & 0xFF);
        int subsystemId     = ((buffer[12] & 0xFF) << 8) | (buffer[13] & 0xFF);
        int component       = ((buffer[14] & 0xFF) << 8) | (buffer[15] & 0xFF);
        int globalId = ((buffer[16] & 0xFF) << 24) | ((buffer[17] & 0xFF) << 16)
                          | ((buffer[18] & 0xFF) << 8) | (buffer[19] & 0xFF);
        int plcIndex        = ((buffer[20] & 0xFF) << 8) | (buffer[21] & 0xFF);
        int location        = ((buffer[22] & 0xFF) << 8) | (buffer[23] & 0xFF);
        int destination     = ((buffer[24] & 0xFF) << 8) | (buffer[25] & 0xFF); // optional field

        // === Step 5: Validate telegram type ===
        if (telegramType != 40)
            throw new IOException("Unexpected telegram type. Expected 40 (ITEM ENTER) but got " + telegramType);

        // === Step 6: Validate telegram length ===
        if (telegramLength != 9)
            throw new IOException("Invalid telegram length: expected 9 but got " + telegramLength);

        // === Step 7: Log raw data and parsed fields ===
        log.info(logPrefix, "📥 ITEM ENTER (40) Raw Telegram: " + bytesToHex(buffer));
        log.info(logPrefix, String.format(
                "Received ITEM ENTER telegram -> ChannelID=%d | Version=%d | SeqNo=%d | Type=%d | Len=%d | SS=%d | CC=%d | GlobalID=%d | PLCIndex=%d | Location=%d | Destination=%d",
                channelId, version, sequenceNumber, telegramType, telegramLength,
                subsystemId, component, globalId, plcIndex, location, destination
        ));

        // === Step 8: Final Confirmation ===
        log.info(logPrefix, "✅ ITEM ENTER (40) telegram parsed successfully");
    }

    @Override
    public int getTelegramType() {
        return 40;
    }

    @Override
    public int getTelegramLength() {
        return 9;
    }

    /** Utility: Read a fixed number of bytes from stream */
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
