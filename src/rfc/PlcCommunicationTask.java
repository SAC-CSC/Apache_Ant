package rfc;

import platform.core.config.PlcConfigurationLoader;
import platform.core.log.Log;

public class PlcCommunicationTask implements Runnable {

	private final String configPath;

	public PlcCommunicationTask(String configPath) {
		this.configPath = configPath;
	}

	@Override
	public void run() {
	    try {
	        System.out.println("Starting PlcCommunicationTask with config: " + configPath);

	        PlcConfigurationLoader loader = new PlcConfigurationLoader(configPath);
	        String plcIp = loader.getPlcIp();
	        int port = loader.getPlcPort();
	        Log log = loader.getLogger();
	        String logPrefix = loader.getLogPrefix();
	        String tsap = loader.gettsap();
	        String remotetsap = loader.getremoteTsap();

	        System.out.println("Config loaded: PLC IP = " + plcIp + ", Port = " + port);

	        RFC1006TSAPHandler handler = new RFC1006TSAPHandler(plcIp, port, log, logPrefix, tsap, remotetsap);

	        while (!Thread.currentThread().isInterrupted()) {
	            try {
	                log.info(logPrefix, "Trying to connect PLC...");
	                handler.connect();
	                log.info(logPrefix, "Connected successfully to PLC...");

	                while (handler.isConnectionAlive() && !Thread.currentThread().isInterrupted()) {
	                    Thread.sleep(1000);
	                }
	            } catch (Exception e) {
	                log.error(logPrefix, "Connection error or unexpected failure", e);
	                try {
	                    Thread.sleep(5000); // Wait before reconnect
	                } catch (InterruptedException ie) {
	                    Thread.currentThread().interrupt();
	                }
	            } finally {
	                try {
	                    handler.disconnect();
	                    log.info(logPrefix, "Disconnected from PLC.");
	                } catch (Exception e) {
	                    log.error(logPrefix, "Error during disconnect", e);
	                }
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Exception in PlcCommunicationTask: ");
	        e.printStackTrace();
	    }
	}
}
