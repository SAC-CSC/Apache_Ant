// Base                 : Conveyor Sortaion Controller
// Class                : ReadyTlg Class
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

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReadyTlg extends AbstractTelegram {

    private final int channelId;
    private final int version;
    private final int sequenceNumber;
    private final int subsystemId;
    private final int bypassMode;
    private final int llcMode;

    public ReadyTlg(int channelId, int version, int sequenceNumber, int subsystemId, int bypassMode, int llcMode) {
        this.channelId = channelId;
        this.version = version;
        this.sequenceNumber = sequenceNumber;
        this.subsystemId = subsystemId;
        this.bypassMode = bypassMode;
        this.llcMode = llcMode;
    }

    public byte[] buildTelegram() {
        byte[] payload = new byte[] {
                (byte)(channelId >> 8), (byte)(channelId & 0xFF),
                (byte)(version >> 8), (byte)(version & 0xFF),
                (byte)(sequenceNumber >> 24), (byte)(sequenceNumber >> 16),
                (byte)(sequenceNumber >> 8), (byte)(sequenceNumber),
                (byte)(getTelegramType() >> 8), (byte)(getTelegramType() & 0xFF),
                (byte)(getTelegramLength() >> 8), (byte)(getTelegramLength() & 0xFF),
                (byte)(subsystemId >> 8), (byte)(subsystemId & 0xFF),
                (byte)(bypassMode >> 8), (byte)(bypassMode & 0xFF),
                (byte)(llcMode >> 8), (byte)(llcMode & 0xFF)
        };                                 
                                                                         
        int totalLength = payload.length + 7;
        byte[] header = new byte[] {
                0x03, 0x00,
                (byte)(totalLength >> 8), (byte)(totalLength & 0xFF),
                0x02, (byte)0xF0, (byte)0x80
        };

        byte[] fullTelegram = new byte[header.length + payload.length];
        System.arraycopy(header, 0, fullTelegram, 0, header.length);
        System.arraycopy(payload, 0, fullTelegram, header.length, payload.length);

        return fullTelegram;
    }

    public void send(OutputStream outputStream, Log log, String logPrefix) throws IOException {
        byte[] telegram = buildTelegram();
        log.info(logPrefix, "Sending READY telegram: " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();
    }

    public  int validate(InputStream inputStream, Log log, String logPrefix) throws IOException {
        byte[] header = new byte[4];
        readFully(inputStream, header, 0, 4);

        if ((header[0] & 0xFF) != 0x03)
            throw new IOException("Invalid TPKT version: expected 0x03 but got " + (header[0] & 0xFF));

        int totalLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
        if (totalLength < 25)
            throw new IOException("Total telegram length too short: expected at least 25 bytes but got " + totalLength);

        byte[] cotp = new byte[3];
        readFully(inputStream, cotp, 0, 3);
        if ((cotp[0] & 0xFF) != 0x02 || (cotp[1] & 0xFF) != 0xF0 || (cotp[2] & 0xFF) != 0x80)
            throw new IOException("Invalid COTP header. Expected [0x02, 0xF0, 0x80]");

        byte[] payload = new byte[18];
        readFully(inputStream, payload, 0, 18);

        int channelId = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
        int version = ((payload[2] & 0xFF) << 8) | (payload[3] & 0xFF);
        int sequenceNumber = ((payload[4] & 0xFF) << 24) | ((payload[5] & 0xFF) << 16)
                            | ((payload[6] & 0xFF) << 8) | (payload[7] & 0xFF);
        int telegramType = ((payload[8] & 0xFF) << 8) | (payload[9] & 0xFF);
        int telegramLength = ((payload[10] & 0xFF) << 8) | (payload[11] & 0xFF);
        int subsystemId = ((payload[12] & 0xFF) << 8) | (payload[13] & 0xFF);
        int bypassMode = ((payload[14] & 0xFF) << 8) | (payload[15] & 0xFF);
        int llcMode = ((payload[16] & 0xFF) << 8) | (payload[17] & 0xFF);
        log.info(logPrefix, "Recieve READY telegram: " + bytesToHex(payload));
        log.info(logPrefix, String.format(
                "Received READY telegram: ChannelID=%d, Version=%d, SeqNo=%d, TT=%d, Length=%d words, SubsystemID=%d, Bypass=%d, LLC=%d",
                channelId, version, sequenceNumber, telegramType, telegramLength, subsystemId, bypassMode, llcMode
        ));

        if (telegramType != 5)
            throw new IOException("Unexpected telegram type. Expected 5 (READY) but got " + telegramType);

        if (telegramLength != 5)
            throw new IOException("Unexpected telegram length. Expected 5 words but got " + telegramLength);
        return sequenceNumber ;
    }

    @Override
    public int getTelegramType() {
        return 0x0005; // READY
    }

    @Override
    public int getTelegramLength() {
        return 5; // 5 words = 10 bytes
    }
    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1) throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }
    
}
