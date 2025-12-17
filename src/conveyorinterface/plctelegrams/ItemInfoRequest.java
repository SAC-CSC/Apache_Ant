package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ItemInfoRequest extends AbstractTelegram {

    private static final int REQUEST_TYPE = 164;
    private static final int INFO_TYPE    = 165;
    private static final int INFO_LENGTH  = 15; // 15 words = 30 bytes

    /** ==================== Parsing ITEM INFO REQUEST (TT=164) ==================== */
    public static class ItemInfoRequestData {
        public int channelId;
        public int version;
        public int sequenceNumber;
        public int subsystemId;
        public int plcIndex;
        public int location;
        public String iataCode;
    }

    public ItemInfoRequestData parseRequest(InputStream inputStream, Log log, String logPrefix) throws IOException {
        byte[] tpkt = new byte[4];
        readFully(inputStream, tpkt, 0, 4);

        int totalLength = ((tpkt[2] & 0xFF) << 8) | (tpkt[3] & 0xFF);

        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);

        byte[] buffer = new byte[totalLength - 7];
        readFully(inputStream, buffer, 0, buffer.length);

        ItemInfoRequestData requestData = new ItemInfoRequestData();
        requestData.channelId      = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
        requestData.version        = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        requestData.sequenceNumber = ((buffer[4] & 0xFF) << 24) | ((buffer[5] & 0xFF) << 16)
                                  | ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
        int telegramType           = ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);
        int telegramLength         = ((buffer[10] & 0xFF) << 8) | (buffer[11] & 0xFF);
        requestData.subsystemId    = ((buffer[12] & 0xFF) << 8) | (buffer[13] & 0xFF);
        requestData.plcIndex       = ((buffer[14] & 0xFF) << 8) | (buffer[15] & 0xFF);
        requestData.location       = ((buffer[16] & 0xFF) << 8) | (buffer[17] & 0xFF);

        int iataStart = 18;
        int iataLength = 10;
        requestData.iataCode = "";
        if (iataStart < buffer.length) {
            int actualLength = Math.min(iataLength, buffer.length - iataStart);
            byte[] iataBytes = new byte[actualLength];
            System.arraycopy(buffer, iataStart, iataBytes, 0, actualLength);
            requestData.iataCode = new String(iataBytes, StandardCharsets.US_ASCII).trim();
        }
        log.info(logPrefix, "📥 ITEM INFO REQUEST Telegram: " + bytesToHex(buffer));
        // ===== Decode log for incoming request =====
        log.info(logPrefix, "📥 ITEM INFO REQUEST (TT=164) Received: " +
                "ChannelID=" + requestData.channelId + ", " +
                "Version=" + requestData.version + ", " +
                "SequenceNumber=" + requestData.sequenceNumber + ", " +
                "SubsystemID=" + requestData.subsystemId + ", " +
                "PLCIndex=" + requestData.plcIndex + ", " +
                "Location=" + requestData.location + ", " +
                "IATACode=" + requestData.iataCode + ", " +
                "TelegramType=" + telegramType + ", " +
                "TelegramLength=" + telegramLength);


        return requestData;
    }

    /** ==================== Build and Send ITEM INFO (TT=165) ==================== */
    public static byte[] build_ItemInfoTelegram(
            int channelId, int version, int sequenceNumber,
            int subsystemId, int plcIndex, String iataCode,
            int screeningLevel, int screeningResult, int customResult,
            int minScreeningLevel, int customsRequired, int ebsStatus) {

        int cnt = 0;
        byte[] payload = new byte[INFO_LENGTH * 2]; // 30 bytes
        payload[cnt++] = 0x00; payload[cnt++] = (byte)INFO_TYPE; // TT=165
        payload[cnt++] = 0x00; payload[cnt++] = (byte)INFO_LENGTH; // LL=15

        payload[cnt++] = (byte)((subsystemId >> 8) & 0xFF);
        payload[cnt++] = (byte)(subsystemId & 0xFF);
        payload[cnt++] = (byte)((plcIndex >> 8) & 0xFF);
        payload[cnt++] = (byte)(plcIndex & 0xFF);

        byte[] iataBytes = padIATA(iataCode);
        System.arraycopy(iataBytes, 0, payload, cnt, 10); cnt += 10;

        payload[cnt++] = 0; payload[cnt++] = (byte)(screeningLevel & 0xFF);
        payload[cnt++] = 0; payload[cnt++] = (byte)(screeningResult & 0xFF);
        payload[cnt++] = 0; payload[cnt++] = (byte)(customResult & 0xFF);
        payload[cnt++] = 0; payload[cnt++] = (byte)(minScreeningLevel & 0xFF);
        payload[cnt++] = 0; payload[cnt++] = (byte)(customsRequired & 0xFF);
        payload[cnt++] = 0; payload[cnt++] = (byte)(ebsStatus & 0xFF);

        // TPKT + COTP Header
        int totalLength = payload.length + 7 + 8;
        byte[] header = new byte[7];
        cnt = 0;
        header[cnt++] = 0x03; header[cnt++] = 0x00;
        header[cnt++] = (byte)((totalLength >> 8) & 0xFF);
        header[cnt++] = (byte)(totalLength & 0xFF);
        header[cnt++] = 0x02; header[cnt++] = (byte)0xF0; header[cnt++] = (byte)0x80;

        // Channel fields
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

        // Combine all
        byte[] telegram = new byte[header.length + channelFields.length + payload.length];
        cnt = 0;
        for (byte b : header) telegram[cnt++] = b;
        for (byte b : channelFields) telegram[cnt++] = b;
        for (byte b : payload) telegram[cnt++] = b;

        return telegram;
    }

    public void sendItemInfo(OutputStream outputStream, Log log, String logPrefix,
                             int channelId, int version, int sequenceNumber,
                             int subsystemId, int plcIndex, String iataCode,
                             int screeningLevel, int screeningResult, int customResult,
                             int minScreeningLevel, int customsRequired, int ebsStatus) throws IOException {

        byte[] telegram = build_ItemInfoTelegram(
                channelId, version, sequenceNumber, subsystemId, plcIndex, iataCode,
                screeningLevel, screeningResult, customResult, minScreeningLevel, customsRequired, ebsStatus
        );

        outputStream.write(telegram);
        outputStream.flush();
        log.info(logPrefix, "📥 ITEM INFO Telegram: " + bytesToHex(telegram));
        // ===== Decode log for outgoing telegram =====
        log.info(logPrefix, "📥 ITEM INFO Telegram (TT=165) Sent: " +
                "ChannelID=" + channelId + ", " +
                "Version=" + version + ", " +
                "SequenceNumber=" + sequenceNumber + ", " +
                "SubsystemID=" + subsystemId + ", " +
                "PLCIndex=" + plcIndex + ", " +
                "IATACode=" + iataCode + ", " +
                "ScreeningLevel=" + screeningLevel + ", " +
                "ScreeningResult=" + screeningResult + ", " +
                "CustomResult=" + customResult + ", " +
                "MinScreeningLevel=" + minScreeningLevel + ", " +
                "CustomsRequired=" + customsRequired + ", " +
                "EBSStatus=" + ebsStatus);

    }

    /** ==================== Auto-respond to request ==================== */
    public void respondToRequest(InputStream inputStream, OutputStream outputStream, Log log, String logPrefix,
                                 int screeningLevel, int screeningResult, int customResult,
                                 int minScreeningLevel, int customsRequired, int ebsStatus) throws IOException {

        ItemInfoRequestData requestData = parseRequest(inputStream, log, logPrefix);

        sendItemInfo(outputStream, log, logPrefix,
                requestData.channelId, requestData.version, requestData.sequenceNumber,
                requestData.subsystemId, requestData.plcIndex, requestData.iataCode,
                screeningLevel, screeningResult, customResult,
                minScreeningLevel, customsRequired, ebsStatus);

        log.info(logPrefix, "✅ Responded to ITEM INFO REQUEST with decoded telegram data");
    }

    /** ==================== Utilities ==================== */
    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1) throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }

    private static byte[] padIATA(String iata) {
        byte[] result = new byte[10];
        if (iata != null) {
            byte[] src = iata.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, result, 0, Math.min(src.length, 10));
        }
        return result;
    }



    @Override
    public int getTelegramType() { return INFO_TYPE; }

    @Override
    public int getTelegramLength() { return INFO_LENGTH; }
}
