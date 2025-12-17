package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.OutputStream;

public class AirlineCodeTableStartTlg extends AbstractTelegram {

    private final int subsystemId;
    private final int channelId;
    private final int version;
    private final int sequenceNumber;

    public AirlineCodeTableStartTlg(int subsystemId, int channelId, int version, int sequenceNumber) {
        this.subsystemId = subsystemId;
        this.channelId = channelId;
        this.version = version;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public int getTelegramType() {
        return 157;
    }

    @Override
    public int getTelegramLength() {
        return 3; // 3 words = 6 bytes payload
    }

    public byte[] buildTelegram() {
        // === Step 1: Payload ===
        byte[] payload = new byte[6];
        int i = 0;
        payload[i++] = (byte) (getTelegramType() >> 8);
        payload[i++] = (byte) (getTelegramType() & 0xFF);
        payload[i++] = (byte) (getTelegramLength() >> 8);
        payload[i++] = (byte) (getTelegramLength() & 0xFF);
        payload[i++] = (byte) (subsystemId >> 8);
        payload[i]   = (byte) (subsystemId & 0xFF);

        // === Step 2: Channel Header (8 bytes) ===
        byte[] channelFields = new byte[8];
        int cnt = 0;
        channelFields[cnt++] = (byte) (channelId >> 8);
        channelFields[cnt++] = (byte) (channelId & 0xFF);
        channelFields[cnt++] = (byte) (version >> 8);
        channelFields[cnt++] = (byte) (version & 0xFF);
        channelFields[cnt++] = (byte) (sequenceNumber >> 24);
        channelFields[cnt++] = (byte) (sequenceNumber >> 16);
        channelFields[cnt++] = (byte) (sequenceNumber >> 8);
        channelFields[cnt++] = (byte) (sequenceNumber & 0xFF);

        // === Step 3: TPKT + COTP Header (7 bytes) ===
        int totalLength = payload.length + channelFields.length + 7;
        byte[] header = new byte[7];
        cnt = 0;
        header[cnt++] = 0x03;             // TPKT Version
        header[cnt++] = 0x00;             // Reserved
        header[cnt++] = (byte) ((totalLength >> 8) & 0xFF);
        header[cnt++] = (byte) (totalLength & 0xFF);
        header[cnt++] = 0x02;             // COTP DT PDU
        header[cnt++] = (byte) 0xF0;      // EOT + flags
        header[cnt++] = (byte) 0x80;      // Flags

        // === Step 4: Combine all ===
        byte[] fullTelegram = new byte[header.length + channelFields.length + payload.length];
        cnt = 0;
        for (byte b : header) fullTelegram[cnt++] = b;
        for (byte b : channelFields) fullTelegram[cnt++] = b;
        for (byte b : payload) fullTelegram[cnt++] = b;

        return fullTelegram;
    }

    public void send(OutputStream outputStream, Log log, String logPrefix) throws IOException {
        byte[] telegram = buildTelegram();
        log.info(logPrefix, "Sending AIRLINE CODE TABLE START telegram: " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();
    }

    protected  String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}
