// Base                 : Conveyor Sortaion Controller
// Class                : FileLogger Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : code module 
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version


package platform.core.log;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * FileLogger that logs into logs/<logFileName>
 * and extends base Log class to override debug/info/error methods.
 */
public class FileLogger extends Log {

    private File logFile;
   // private final File logDir = new File("logs");
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private LocalDate currentLogDate;
    private final File logDir = new File(System.getProperty("user.home"), "CSC_SAC_logs");


    public FileLogger(String logFileNamePrefix) {
        try {
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                System.out.println("Logs directory created: " + created + " at " + logDir.getAbsolutePath());
            } else {
                System.out.println("Logs directory already exists at " + logDir.getAbsolutePath());
            }

            currentLogDate = LocalDate.now();
            logFile = new File(logDir, logFileNamePrefix + "_" + currentLogDate.format(fileDateFormatter) + ".log");

            if (!logFile.exists()) {
                boolean createdFile = logFile.createNewFile();
                System.out.println("Log file created: " + createdFile + " at " + logFile.getAbsolutePath());
            } else {
                System.out.println("Log file already exists at " + logFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize log file", e);
        }
    }


    private void rotateLogFileIfNeeded(String logFileNamePrefix) {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentLogDate)) {
            currentLogDate = today;
            logFile = new File(logDir, logFileNamePrefix + "_" + currentLogDate.format(fileDateFormatter) + ".log");
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void write(String level, String prefix, String message) {
        rotateLogFileIfNeeded("application"); // Replace with your base filename if needed

        String timestamp = LocalDateTime.now().format(dtf);
        String entry = timestamp + " " + prefix + " [" + level + "] " + message;

        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(entry + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace(); // fallback to console
        }
    }

    @Override
    public void debug(String prefix, String message) {
        write("DEBUG", prefix, message);
    }

    @Override
    public void info(String prefix, String message) {
        write("INFO", prefix, message);
    }

    @Override
    public void error(String prefix, String message) {
        write("ERROR", prefix, message);
    }

    @Override
    public void error(String prefix, String message, Throwable t) {
        write("ERROR", prefix, message);
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            t.printStackTrace(pw);
        } catch (IOException e) {
            e.printStackTrace(); // fallback
        }
    }

    public void log(LogLevel level, String prefix, String message) {
        write(level.name(), prefix, message);
    }
}
