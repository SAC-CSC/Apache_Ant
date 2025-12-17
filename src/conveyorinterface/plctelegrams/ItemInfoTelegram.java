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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * ITEM INFO Telegram (TT=165) 
 * Direction: CSC -> PLC
 */
public class ItemInfoTelegram {

    private static final int TELEGRAM_TYPE = 165;
    private static final int TELEGRAM_LENGTH = 12;

    private final int subsystemId;          // SS
    private final int plcIndex;             // XX
    private final String iata;              // AAAAAAAAAA (10 bytes)
    private final ScreeningLevel screeningLevel; // JJ
    private final ScreeningResult screeningResult; // RR
    private final CustomsResult customsResult;     // SS

    public ItemInfoTelegram(int subsystemId,
                            int plcIndex,
                            String iata,
                            ScreeningLevel screeningLevel,
                            ScreeningResult screeningResult,
                            CustomsResult customsResult) {
        this.subsystemId = subsystemId;
        this.plcIndex = plcIndex;
        this.iata = iata;
        this.screeningLevel = screeningLevel;
        this.screeningResult = screeningResult;
        this.customsResult = customsResult;
    }

    // Getters
    public int getSubsystemId() { return subsystemId; }
    public int getPlcIndex() { return plcIndex; }
    public String getIata() { return iata; }
    public ScreeningLevel getScreeningLevel() { return screeningLevel; }
    public ScreeningResult getScreeningResult() { return screeningResult; }
    public CustomsResult getCustomsResult() { return customsResult; }

    /**
     * Serialize telegram into byte array
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 10 + 3); 
        // TT + LL + SS + XX + IATA + JJ + RR + Customs
        buffer.put((byte) TELEGRAM_TYPE);
        buffer.put((byte) TELEGRAM_LENGTH);
        buffer.put((byte) subsystemId);
        buffer.put((byte) plcIndex);

        byte[] iataBytes = iata.getBytes(StandardCharsets.US_ASCII);
        byte[] paddedIata = new byte[10];
        System.arraycopy(iataBytes, 0, paddedIata, 0,
                Math.min(iataBytes.length, paddedIata.length));
        buffer.put(paddedIata);

        buffer.put((byte) screeningLevel.getCode());
        buffer.put((byte) screeningResult.getCode());
        buffer.put((byte) customsResult.getCode());

        return buffer.array();
    }

    /**
     * Deserialize telegram from byte array
     */
    public static Optional<ItemInfoTelegram> fromBytes(byte[] data) {
        if (data == null || data.length < 18) {
            return Optional.empty();
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int tt = buffer.get() & 0xFF;
        int ll = buffer.get() & 0xFF;

        if (tt != TELEGRAM_TYPE || ll != TELEGRAM_LENGTH) {
            return Optional.empty();
        }

        int subsystemId = buffer.get() & 0xFF;
        int plcIndex = buffer.get() & 0xFF;

        byte[] iataBytes = new byte[10];
        buffer.get(iataBytes);
        String iata = new String(iataBytes, StandardCharsets.US_ASCII).trim();

        ScreeningLevel screeningLevel = ScreeningLevel.fromCode(buffer.get() & 0xFF);
        ScreeningResult screeningResult = ScreeningResult.fromCode(buffer.get() & 0xFF);
        CustomsResult customsResult = CustomsResult.fromCode(buffer.get() & 0xFF);

        return Optional.of(new ItemInfoTelegram(
                subsystemId, plcIndex, iata, screeningLevel, screeningResult, customsResult
        ));
    }

    @Override
    public String toString() {
        return "ItemInfoTelegram{" +
                "subsystemId=" + subsystemId +
                ", plcIndex=" + plcIndex +
                ", iata='" + iata + '\'' +
                ", screeningLevel=" + screeningLevel +
                ", screeningResult=" + screeningResult +
                ", customsResult=" + customsResult +
                '}';
    }

    // ================== ENUMS ==================

    public enum ScreeningLevel {
        UNDEFINED(0),
        LEVEL_1(11), LEVEL_1A(12), LEVEL_1B(13),
        LEVEL_2(21), LEVEL_2A(22), LEVEL_2B(23),
        LEVEL_3(31), LEVEL_3A(32), LEVEL_3B(33),
        LEVEL_4(41), LEVEL_4A(42), LEVEL_4B(43),
        LEVEL_5(51), LEVEL_5A(52), LEVEL_5B(53);

        private final int code;
        ScreeningLevel(int code) { this.code = code; }
        public int getCode() { return code; }

        public static ScreeningLevel fromCode(int code) {
            for (ScreeningLevel s : values()) {
                if (s.code == code) return s;
            }
            return UNDEFINED;
        }
    }

    public enum ScreeningResult {
        UNDEFINED(0),
        MACHINE_ALARM(1), MACHINE_CLEAR(2), ERROR_UNKNOWN(3), TIMEOUT(5),
        OBVIOUS_THREAT(9),

        LEVEL2A_ALARM(11), LEVEL2A_CLEAR(12), LEVEL2A_ERROR(13), LEVEL2A_TIMEOUT(15),
        LEVEL2B_ALARM(21), LEVEL2B_CLEAR(22), LEVEL2B_ERROR(23), LEVEL2B_TIMEOUT(25),

        L3_ETD_ALARM(91), L3_ETD_CLEAR(92), L3_ETD_ERROR(93), L3_ETD_TIMEOUT(95),
        L3_ETD_RECONCILE(96), L3_ETD_TCU(97),

        UNKNOWN(-1);

        private final int code;
        ScreeningResult(int code) { this.code = code; }
        public int getCode() { return code; }

        public static ScreeningResult fromCode(int code) {
            for (ScreeningResult s : values()) {
                if (s.code == code) return s;
            }
            return UNKNOWN;
        }
    }

    public enum CustomsResult {
        UNDEFINED(0),
        ALARM(31), CLEAR(32), ERROR(33), TIMEOUT(35),
        L3_ALARM(91), L3_CLEAR(92), L3_ERROR(93), L3_TIMEOUT(95),
        UNKNOWN(-1);

        private final int code;
        CustomsResult(int code) { this.code = code; }
        public int getCode() { return code; }

        public static CustomsResult fromCode(int code) {
            for (CustomsResult s : values()) {
                if (s.code == code) return s;
            }
            return UNKNOWN;
        }
    }
}
