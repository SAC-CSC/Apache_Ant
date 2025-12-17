// Base                 : Conveyor Sortation Controller
// Class                : ItemTlg Class
// Programmer           : Giresh
// Release Date         : 2025-07-23
// Revision Number      : 1.0
// Description          : Code module to construct and parse ITEM telegrams
// =================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// ---------------------------------------------------------------------------------
//01.00    2025.07.23    Giresh        Initial Version

package conveyorinterface.plctelegrams;

import platform.io.telegraph.AbstractTelegram;
import platform.core.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ItemTlg extends AbstractTelegram {

    private final int globalId;
    private final int plcIndex;
    private final int destination;
    private final int altDestination;
    private final int subsystemId;
    private final int subsystemComponent;

    public ItemTlg(int globalId, int plcIndex, int destination, int altDestination,
                   int subsystemId, int subsystemComponent) {
        this.globalId = globalId;
        this.plcIndex = plcIndex;
        this.destination = destination;
        this.altDestination = altDestination;
        this.subsystemId = subsystemId;
        this.subsystemComponent = subsystemComponent;
    }

    public byte[] buildTelegram() {
        byte[] payload = new byte[] {
                (byte)(getTelegramType() >> 8), (byte)(getTelegramType() & 0xFF), // TT
                (byte)(getTelegramLength() >> 8), (byte)(getTelegramLength() & 0xFF), // LL
                (byte)(subsystemId >> 8), (byte)(subsystemId & 0xFF), // SS
                (byte)(subsystemComponent >> 8), (byte)(subsystemComponent & 0xFF), // CC
                (byte)(globalId >> 24), (byte)(globalId >> 16), (byte)(globalId >> 8), (byte)(globalId), // GGGG
                (byte)(plcIndex >> 8), (byte)(plcIndex & 0xFF), // XX
                (byte)(destination >> 8), (byte)(destination & 0xFF), // DD
                (byte)(altDestination >> 8), (byte)(altDestination & 0xFF) // EE
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
        log.info(logPrefix, "Sending ITEM Destination telegram: " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();
    }

    public static void validate(InputStream inputStream, Log log, String logPrefix) throws IOException {
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

        int telegramType = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
        int telegramLength = ((payload[2] & 0xFF) << 8) | (payload[3] & 0xFF);
        int subsystemId = ((payload[4] & 0xFF) << 8) | (payload[5] & 0xFF);
        int subsystemComponent = ((payload[6] & 0xFF) << 8) | (payload[7] & 0xFF);
        int globalId = ((payload[8] & 0xFF) << 24) | ((payload[9] & 0xFF) << 16)
                     | ((payload[10] & 0xFF) << 8) | (payload[11] & 0xFF);
        int plcIndex = ((payload[12] & 0xFF) << 8) | (payload[13] & 0xFF);
        int destination = ((payload[14] & 0xFF) << 8) | (payload[15] & 0xFF);
        int altDestination = ((payload[16] & 0xFF) << 8) | (payload[17] & 0xFF);

        log.info(logPrefix, String.format(
                "Received ITEM Destination telegram: TT=%d, LL=%d, SS=%d, CC=%d, GID=%d, PLCIdx=%d, Dest=%d, AltDest=%d",
                telegramType, telegramLength, subsystemId, subsystemComponent,
                globalId, plcIndex, destination, altDestination
        ));

        if (telegramType != 42)
            throw new IOException("Unexpected telegram type. Expected 42 (ITEM) but got " + telegramType);
    }

    @Override
    public int getTelegramType() {
        return 42; // TT = 42
    }

    @Override
    public int getTelegramLength() {
        return 9; // 9 words = 18 bytes
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
