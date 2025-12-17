// Base                 : Conveyor Sortaion Controller
// Class                : AbstractTelegram Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : Communication module 
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version


// Abstract base class for all telegrams
package platform.io.telegraph;

public abstract class AbstractTelegram {

    // Every telegram should define its type
    public abstract int getTelegramType();

    // Every telegram should define its length
    public abstract int getTelegramLength();

    // Optional: Parse incoming telegrams (not used here but useful for future)
    public AbstractTelegram parse(byte[] data) throws Exception {
        throw new UnsupportedOperationException("Parsing not implemented for this telegram.");
    }

    // Optional: Convert telegram to raw byte array
    public byte[] toByteArray() {
        throw new UnsupportedOperationException("toByteArray not implemented.");
    }

    // Utility method to convert byte array to hex string
    protected String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}
