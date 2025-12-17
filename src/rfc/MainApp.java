// Base                 : Conveyor Sortation Controller
// Class                : Main Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : Communication module with multi-PLC handling
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version with multi-threaded PLC

package rfc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class MainApp {

    public static void main(String[] args) {
        System.out.println("✅ CSC_SAC MainApp starting...");
   
        // Get working directory dynamically
        String baseDir = System.getProperty("user.dir");
        System.out.println("Working Directory: " + baseDir);
        String[] configPaths = {
        		baseDir + "/config/ConveyorPlcChannel_03.config",
                baseDir + "/config/ConveyorPlcChannel_04.config",
        	};

        ExecutorService executor = Executors.newFixedThreadPool(configPaths.length);

        for (String configPath : configPaths) {
            PlcCommunicationTask task = new PlcCommunicationTask(configPath);
            executor.submit(task);
        }

        // Shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down all PLC handlers...");
            executor.shutdownNow();
        }));

        // 🟡 Prevent the main thread from exiting immediately
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
