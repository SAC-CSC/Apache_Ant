package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AirlineCodeEntryTlg extends AbstractTelegram {

    private final int subsystemId;
    private final int channelId;
    private final int version;
    private final int sequenceNumber;
    private final String airlineCode; // 3 ASCII chars
    private final int destination;
    private final int numAltDest; // 0..3
    private final int alt1;
    private final int alt2;
    private final int alt3;

    public AirlineCodeEntryTlg(int subsystemId, int channelId, int version, int sequenceNumber,
                               String airlineCode, int destination, int numAltDest,
                               int alt1, int alt2, int alt3) {
        this.subsystemId = subsystemId;
        this.channelId = channelId;
        this.version = version;
        this.sequenceNumber = sequenceNumber;
        this.airlineCode = airlineCode;
        this.destination = destination;
        this.numAltDest = numAltDest;
        this.alt1 = alt1;
        this.alt2 = alt2;
        this.alt3 = alt3;
    }

    @Override
    public int getTelegramType() {
        return 158;
    }

    @Override
    public int getTelegramLength() {
        return 10; // 10 words = 20 bytes payload
    }

    public byte[] buildTelegram() {
        // --- Payload (TT + LL + fields) ---
        byte[] codeBytes = new byte[4];
        byte[] inputCode = airlineCode.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(inputCode, 0, codeBytes, 0, Math.min(4, inputCode.length));

        byte[] payload = new byte[22];
        int i = 0;

        payload[i++] = (byte) (getTelegramType() >> 8);
        payload[i++] = (byte) (getTelegramType() & 0xFF);
        payload[i++] = (byte) (getTelegramLength() >> 8);
        payload[i++] = (byte) (getTelegramLength() & 0xFF);
        payload[i++] = (byte) (subsystemId >> 8);
        payload[i++] = (byte) (subsystemId & 0xFF);

        System.arraycopy(codeBytes, 0, payload, i, 4);
        i += 4;

        payload[i++] = (byte) (destination >> 8);
        payload[i++] = (byte) (destination & 0xFF);
       
        payload[i++] = (byte) (numAltDest >> 8);
        payload[i++] = (byte) (numAltDest & 0xFF);

        payload[i++] = (byte) (alt1 >> 8);
        payload[i++] = (byte) (alt1 & 0xFF);
        payload[i++] = (byte) (alt2 >> 8);
        payload[i++] = (byte) (alt2 & 0xFF);
        payload[i++] = (byte) (alt3 >> 8);
        payload[i] = (byte) (alt3 & 0xFF);

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

        // --- Combine header + channel fields + payload ---
        byte[] telegram = new byte[header.length + channelFields.length + payload.length];
        int cnt = 0;
        for (byte b : header) telegram[cnt++] = b;
        for (byte b : channelFields) telegram[cnt++] = b;
        for (byte b : payload) telegram[cnt++] = b;

        return telegram;
    }

    public void send(OutputStream outputStream, Log log, String logPrefix) throws IOException {
        byte[] telegram = buildTelegram();
        log.info(logPrefix, "Sending AIRLINE CODE ENTRY telegram: " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();
    }
}
