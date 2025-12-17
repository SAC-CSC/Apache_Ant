// Base                 : Conveyor Sortaion Controller
// Class                : PlcConfigurationLoader Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : code module 
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version



package platform.core.config;

import platform.core.log.FileLogger;
import platform.core.log.Log;

public class PlcConfigurationLoader {
    private final String connectionName;
    private final String plcIp;
    private final int plcPort;
    private final String logPrefix;
    private final Log logger;
    private final String tsap ;
    private final String remoteTsap ;

    public PlcConfigurationLoader(String configFilePath) {
        try {
            IniConfigLoader config = new IniConfigLoader(configFilePath);

            this.connectionName = config.getGlobal("ConnectionName");
            this.plcIp = config.get("ConnectionHandler", "PeerAddress");
            this.plcPort = config.getInt("ConnectionHandler", "PeerPort");
            this.tsap = config.get("ProtocolHandler", "TSAP");  // placeholder will be resolved
            this.remoteTsap = config.get("ProtocolHandler", "RemoteTSAP"); 
            this.logPrefix = connectionName + ".io.RFC1006TSAPHandler";
            this.logger = new FileLogger(connectionName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load PLC configuration from file: " + configFilePath, e);
        }
    }

    public String getPlcIp() {
        return plcIp;
    }

    public int getPlcPort() {
        return plcPort;
    }

    public Log getLogger() {
        return logger;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public String getConnectionName() {
        return connectionName;
    }
    public String gettsap() {
    	return tsap ;
    }
    public String getremoteTsap() {
    	return remoteTsap ;
    }
}
