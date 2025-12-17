// =================================================================================
// Base                 : Conveyor Sortation Controller
// Class                : AirlineCodeTableEndTlg
// Programmer           : Giresh
// Release Date         : 2025-11-10
// Revision Number      : 1.1
// Description          : Telegram module for AIRLINE CODE TABLE END with Entry Count
// =================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// ---------------------------------------------------------------------------------
// 01.00    2025.06.19   Giresh        Initial Version
// 01.01    2025.11.10   Giresh        Added Channel ID, Version, Sequence Number, Entry Count
// =================================================================================

package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.OutputStream;

public class AirlineCodeTableEndTlg extends AbstractTelegram {

    private final int subsystemId;
    private final int channelId;
    private final int version;
    private final int sequenceNumber;
    private final int entryCount;   // ✅ newly added field

    public AirlineCodeTableEndTlg(int subsystemId, int channelId, int version, int sequenceNumber, int entryCount) {
        this.subsystemId = subsystemId;
        this.channelId = channelId;
        this.version = version;
        this.sequenceNumber = sequenceNumber;
        this.entryCount = entryCount;
    }

    @Override
    public int getTelegramType() {
        return 159; // Airline Code Table End telegram type
    }

    @Override
    public int getTelegramLength() {
        return 4; // updated for additional entryCount field (words)
    }

    public byte[] buildTelegram() {
        // === Step 1: Payload (now includes entry count) ===
        byte[] payload = new byte[8];
        int i = 0;

        payload[i++] = (byte) (getTelegramType() >> 8);
        payload[i++] = (byte) (getTelegramType() & 0xFF);
        payload[i++] = (byte) (getTelegramLength() >> 8);
        payload[i++] = (byte) (getTelegramLength() & 0xFF);
        payload[i++] = (byte) (subsystemId >> 8);
        payload[i++] = (byte) (subsystemId & 0xFF);
        payload[i++] = (byte) (entryCount >> 8);   // ✅ Entry count high byte
        payload[i]   = (byte) (entryCount & 0xFF); // ✅ Entry count low byte

        // === Step 2: Channel + Version + Sequence (8 bytes) ===
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

        // === Step 3: ISO-on-TCP Header (7 bytes) ===
        int totalLength = headerLength() + channelFields.length + payload.length;
        byte[] header = new byte[7];
        int h = 0;
        header[h++] = 0x03;  // TPKT Version
        header[h++] = 0x00;  // Reserved
        header[h++] = (byte) ((totalLength >> 8) & 0xFF);
        header[h++] = (byte) (totalLength & 0xFF);
        header[h++] = 0x02;  // COTP PDU Type
        header[h++] = (byte) 0xF0;
        header[h++] = (byte) 0x80;

        // === Step 4: Combine All ===
        byte[] fullTelegram = new byte[header.length + channelFields.length + payload.length];
        int pos = 0;
        for (byte b : header) fullTelegram[pos++] = b;
        for (byte b : channelFields) fullTelegram[pos++] = b;
        for (byte b : payload) fullTelegram[pos++] = b;

        return fullTelegram;
    }

    private int headerLength() {
        return 7;
    }

    
    public void send(OutputStream outputStream, Log log, String logPrefix) throws IOException {
        byte[] telegram = buildTelegram();
        log.info(logPrefix, String.format(
                "Sending AIRLINE CODE TABLE END telegram [Type=%d, Channel=%d, Version=%d, Seq=%d, EntryCount=%d]: %s",
                getTelegramType(), channelId, version, sequenceNumber, entryCount, bytesToHex(telegram)
        ));
        outputStream.write(telegram);
        outputStream.flush();
    }

    protected String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}
