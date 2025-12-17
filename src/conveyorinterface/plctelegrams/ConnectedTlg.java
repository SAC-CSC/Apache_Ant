// Base                 : Conveyor Sortaion Controller
// Class                : ConnectedTlg Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : code module 
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version

package conveyorinterface.plctelegrams;

import java.io.OutputStream;
import java.io.IOException;

import platform.io.telegraph.AbstractTelegram;

public class ConnectedTlg extends AbstractTelegram {

    private final int channelId;
    private final int version;
    private final int sequenceNumber;
    private final int subsystemId;


    public ConnectedTlg(int channelId, int version, int sequenceNumber, int subsystemId ) {
        this.channelId = channelId;
        this.version = version;
        this.sequenceNumber = sequenceNumber;
        this.subsystemId = subsystemId;

    }

    public byte[] buildTelegram() {
        // === Payload (Connected Telegram) ===
        byte[] payload = new byte[] {
            (byte)(channelId >> 8), (byte)(channelId & 0xFF),
            (byte)(version >> 8), (byte)(version & 0xFF),
            (byte)(sequenceNumber >> 24), (byte)(sequenceNumber >> 16),
            (byte)(sequenceNumber >> 8), (byte)(sequenceNumber & 0xFF),
            (byte)(getTelegramType() >> 8), (byte)(getTelegramType() & 0xFF),
            (byte)(getTelegramLength() >> 8), (byte)(getTelegramLength() & 0xFF),
            (byte)(subsystemId >> 8), (byte)(subsystemId & 0xFF),
        };

        // === RFC1006 Header (TPKT + COTP) ===
        int totalLength = payload.length + 7;
        byte[] header = new byte[] {
            0x03, 0x00,
            (byte)(totalLength >> 8), (byte)(totalLength & 0xFF), // TPKT
            0x02, (byte)0xF0, (byte)0x80                          // COTP
        };

        // === Combine header + payload ===
        byte[] fullTelegram = new byte[header.length + payload.length];
        System.arraycopy(header, 0, fullTelegram, 0, header.length);
        System.arraycopy(payload, 0, fullTelegram, header.length, payload.length);

        return fullTelegram;
    }

    public void send(OutputStream outputStream, platform.core.log.Log log, String logPrefix) throws IOException {
        byte[] telegram = buildTelegram();
        log.info(logPrefix, "Sending CONNECTED telegram: " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();  
        log.info(logPrefix, "CONNECTED telegram (TT=02) sent from CSC to PLC");
    }

    @Override
    public int getTelegramType() {
        return 0x0002; // CONNECTED
    }

    @Override
    public int getTelegramLength() {
        return 3; // Just the length field, for sample purpose
    }

    protected String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}
