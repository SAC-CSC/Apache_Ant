// =================================================================================
// Base                 : Conveyor Sortation Controller
// Class                : AirlineCodeTableComplete
// Programmer           : Giresh
// Release Date         : 2025-11-11
// Revision Number      : 1.0
// Description          : Code module to parse AIRLINE CODE TABLE COMPLETE telegrams
//                        received from PLC indicating successful or failed download.
// =================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// ---------------------------------------------------------------------------------
//01.00    2025.11.11    Giresh        Initial Version - Structured to match ItemExit format
// =================================================================================

package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.InputStream;

public class FallBackTagTableComplete extends AbstractTelegram {

    /** Telegram Constants */
    private static final int TELEGRAM_TYPE = 160;     // Type = 161 (Airline Code Table Complete)
    private static final int TELEGRAM_LENGTH = 5;     // 5 words = 10 bytes (typical small telegram)
    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAILED = 2;
    private static final int MAX_RETRY = 3;

    /** Telegram Fields */
    private int channelId;
    private int version;
    private int sequenceNumber;
    private int subsystemId;
    private int entryCount;
    private int status;

    /** Retry counter */
    private static int retryCount = 0;

    /** Default Constructor */
    public FallBackTagTableComplete() {
    }

    /** Validate telegram received from PLC */
    public void validate(InputStream inputStream, Log log, String logPrefix) throws IOException {
        // === Step 1: Read TPKT Header (4 bytes) ===
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);
        if ((tpkt[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (tpkt[0] & 0xFF));

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);
        if (totalLength < 20)
            throw new IOException("Invalid telegram length: " + totalLength);

        log.info(logPrefix, "📦 Incoming FallBackTag  Table Complete telegram total length: " + totalLength + " bytes");

        // === Step 2: Read COTP Header (3 bytes) ===
        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);
        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header. Expected [0x02, 0xF0, 0x80]");

        // === Step 3: Read Remaining Telegram Payload ===
        byte[] buffer = new byte[totalLength - 7];
        readFully(inputStream, buffer, 0, buffer.length);

        // === Step 4: Extract Telegram Fields ===
        channelId = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
        version = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        sequenceNumber = ((buffer[4] & 0xFF) << 24) | ((buffer[5] & 0xFF) << 16)
                       | ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
        int telegramType = ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);
        int telegramLength = ((buffer[10] & 0xFF) << 8) | (buffer[11] & 0xFF);

        subsystemId = ((buffer[12] & 0xFF) << 8) | (buffer[13] & 0xFF);
        entryCount = ((buffer[14] & 0xFF) << 8) | (buffer[15] & 0xFF);
        status = ((buffer[16] & 0xFF) << 8) | (buffer[17] & 0xFF);

        // === Step 5: Log Raw and Parsed Data ===
        log.info(logPrefix, "📥 FallBackTag TABLE COMPLETE Raw Telegram: " + bytesToHex(buffer));
        log.info(logPrefix, String.format(
            "Received FallBackTag TABLE COMPLETE -> ChannelID=%d | Version=%d | SeqNo=%d | Type=%d | Length=%d | SubsystemID=%d | EntryCount=%d | Status=%s",
            channelId, version, sequenceNumber, telegramType, telegramLength,
            subsystemId, entryCount, (status == STATUS_SUCCESS ? "Success" : "Failed")
        ));

        // === Step 6: Validate Telegram Type ===
        if (telegramType != TELEGRAM_TYPE)
            throw new IOException("Unexpected telegram type. Expected " + TELEGRAM_TYPE + " but got " + telegramType);

        log.info(logPrefix, "✅ FallBackTag TABLE COMPLETE telegram parsed successfully");

        // === Step 7: Handle Telegram Status ===
        handleStatus(log, logPrefix);
    }

    /** Handle success/failure of table completion */
    private void handleStatus(Log log, String logPrefix) {
        if (status == STATUS_SUCCESS) {
            retryCount = 0;
            log.info(logPrefix, "✅ FallBackTag Table Download completed successfully");
        } else if (status == STATUS_FAILED) {
            retryCount++;
            log.warn(logPrefix, "⚠️ FallBackTag Table Download failed | Retry " + retryCount + "/" + MAX_RETRY);
            if (retryCount <= MAX_RETRY) {
                triggerRetry(log, logPrefix);
            } else {
                triggerAlarmToScada(log, logPrefix);
            }
        } else {
            log.error(logPrefix, "❌ Invalid status code received: " + status);
        }
    }

    /** Retry logic for failed download */
    private void triggerRetry(Log log, String logPrefix) {
        log.info(logPrefix, "🔁 Retrying Airline Code Table download (Attempt "
                + retryCount + "/" + MAX_RETRY + ")");
        // TODO: Implement telegram resend logic
    }

    /** Trigger SCADA alarm after 3 failed retries */
    private void triggerAlarmToScada(Log log, String logPrefix) {
        log.error(logPrefix, "🚨 Max retries reached. Triggering alarm to SCADA.");
        // TODO: Implement alarm telegram/event here
    }

    @Override
    public int getTelegramType() {
        return TELEGRAM_TYPE;
    }

    @Override
    public int getTelegramLength() {
        return TELEGRAM_LENGTH;
    }

    /** Utility to read bytes safely */
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
