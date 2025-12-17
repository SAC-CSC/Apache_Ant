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

import platform.core.log.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class FallbackTagTableSender {

    public static void sendFallbackTable(OutputStream out, Log log, String logPrefix,
                                         int subsystemId, List<FallbackTagEntryTlg> entries) throws IOException {

        // 1️⃣ Start
   //     new FallbackTagTableStartTlg(subsystemId).send(out, log, logPrefix);

        // 2️⃣ Send Entries (TT=155)
        for (FallbackTagEntryTlg entry : entries) {
            entry.send(out, log, logPrefix);
            try { Thread.sleep(50); } catch (InterruptedException ignored) {} // small delay
        }

        // 3️⃣ End (TT=156)
//        new FallbackTagTableEndTlg(subsystemId, entries.size()).send(out, log, logPrefix);

        log.info(logPrefix, "Fallback Tag Table download complete. Total entries sent: " + entries.size());
    }
}
