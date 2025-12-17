package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FallbackTagEntryTlg extends AbstractTelegram {

    private final int subsystemId;
    private final int channelId;
    private final int version;
    private final int sequenceNumber;
    private final String fallbackTag; // 10 ASCII chars
    private final int destination;
    private final int screeningLevel; // RR code
    private final int numAltDest; // 0..3
    private final int alt1;
    private final int alt2;
    private final int alt3;

    public FallbackTagEntryTlg(int subsystemId, int channelId, int version, int sequenceNumber,
                               String fallbackTag, int destination, int screeningLevel,
                               int numAltDest, int alt1, int alt2, int alt3) {
        this.subsystemId = subsystemId;
        this.channelId = channelId;
        this.version = version;
        this.sequenceNumber = sequenceNumber;
        this.fallbackTag = fallbackTag;
        this.destination = destination;
        this.screeningLevel = screeningLevel;
        this.numAltDest = numAltDest;
        this.alt1 = alt1;
        this.alt2 = alt2;
        this.alt3 = alt3;
    }

    @Override
    public int getTelegramType() {
        return 155; // Fallback Tag Entry
    }

    @Override
    public int getTelegramLength() {
        return 14; // 14 words = 28 bytes payload
    }

    public byte[] buildTelegram() {
        byte[] payload = new byte[28];
        int i = 0;

        // TT + LL + Subsystem ID
        payload[i++] = (byte) (getTelegramType() >> 8);
        payload[i++] = (byte) (getTelegramType() & 0xFF);
        payload[i++] = (byte) (getTelegramLength() >> 8);
        payload[i++] = (byte) (getTelegramLength() & 0xFF);
        payload[i++] = (byte) (subsystemId >> 8);
        payload[i++] = (byte) (subsystemId & 0xFF);

        // Fallback Tag - 10 ASCII chars, split into bytes
        byte[] tagBytes = new byte[10];
        byte[] inputBytes = fallbackTag.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(inputBytes, 0, tagBytes, 0, Math.min(10, inputBytes.length));
        System.arraycopy(tagBytes, 0, payload, i, 10);
        i += 10;

        // Destination
        payload[i++] = (byte) (destination >> 8);
        payload[i++] = (byte) (destination & 0xFF);

        // Required Screening Level
        payload[i++] = (byte) (screeningLevel >> 8);
        payload[i++] = (byte) (screeningLevel & 0xFF);

        // Number of Alternative Destinations
        payload[i++] = (byte) (numAltDest >> 8);
        payload[i++] = (byte) (numAltDest & 0xFF);

        // Alternative Destinations (2 bytes each)
        payload[i++] = (byte) (alt1 >> 8);
        payload[i++] = (byte) (alt1 & 0xFF);
        payload[i++] = (byte) (alt2 >> 8);
        payload[i++] = (byte) (alt2 & 0xFF);
        payload[i++] = (byte) (alt3 >> 8);
        payload[i++] = (byte) (alt3 & 0xFF);

        // --- TPKT + COTP Header ---
        int totalLength = payload.length + 7 + 8; // 7 header + 8 channel fields
        byte[] header = new byte[]{
                0x03, 0x00,
                (byte) (totalLength >> 8), (byte) (totalLength & 0xFF),
                0x02, (byte) 0xF0, (byte) 0x80
        };

        // --- Channel Fields ---
        byte[] channelFields = new byte[8];
        channelFields[0] = (byte) ((channelId >> 8) & 0xFF);
        channelFields[1] = (byte) (channelId & 0xFF);
        channelFields[2] = (byte) ((version >> 8) & 0xFF);
        channelFields[3] = (byte) (version & 0xFF);
        channelFields[4] = (byte) ((sequenceNumber >> 24) & 0xFF);
        channelFields[5] = (byte) ((sequenceNumber >> 16) & 0xFF);
        channelFields[6] = (byte) ((sequenceNumber >> 8) & 0xFF);
        channelFields[7] = (byte) (sequenceNumber & 0xFF);

        // --- Combine all ---
        byte[] telegram = new byte[header.length + channelFields.length + payload.length];
        int cnt = 0;
        for (byte b : header) telegram[cnt++] = b;
        for (byte b : channelFields) telegram[cnt++] = b;
        for (byte b : payload) telegram[cnt++] = b;

        return telegram;
    }

    public void send(OutputStream outputStream, Log log, String logPrefix) throws IOException {
        byte[] telegram = buildTelegram();
        log.info(logPrefix, "Sending FALLBACK TAG ENTRY telegram: " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();
    }
}
