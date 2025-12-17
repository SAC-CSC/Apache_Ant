// Base                 : Conveyor Sortaion Controller
// Class                : RFC1006TSAPHandler Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : Communication module 
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version


package rfc;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.ByteArrayOutputStream;
import platform.core.log.Log;
import platform.core.log.LogLevel;
import conveyorinterface.frontend.TelegramSenderUI;
import conveyorinterface.plctelegrams.ConnectedTlg;
import conveyorinterface.plctelegrams.ReadyTlg;
import conveyorinterface.plctelegrams.TelegramDispatcher;


public class RFC1006TSAPHandler {

    private String plcIp;
    private int port;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private Log log;
    private String logPrefix;
    protected String _TSAP ;
    protected String _externalTSAP ;

    static final int HEADSIZE = 4;
    static final byte TPKT_VERSION = (byte) 0x03;
    static final byte TPKT_RESERVE = (byte) 0x00;
    static final byte TPDU_CR = (byte) 0xE0;

    protected int _connectionId = 1;
    protected int _externalConnectionId = 0;

    static final byte TPDU_SIZE_CMD = (byte) 0xC0;
    static final byte TPDU_CALLING_CMD = (byte) 0xC1;
    static final byte TPDU_CALLED_CMD = (byte) 0xC2;

    protected byte _tpduSize = (byte) 0x0b;
    protected int _tpduSizeInBytes;
    byte[] _sendBuf;

    static final int SubsystemID = 1;
    static final int BypassMode = 0;
    static final int LLC_Mode = 0;
    static final int channelId = 7;
    static final int version = 1;
    private volatile long lastSentTime = System.currentTimeMillis();
    private volatile long lastReceivedTime = System.currentTimeMillis();
    private final long transmitTimeout = 5000;
    private final long receiveTimeout = 15000;
    private Thread keepAliveMonitorThread;
    private boolean monitorRunning = false;
    protected int sequenceNo = 0;
    private boolean connected = false;

    public RFC1006TSAPHandler(String plcIp, int port, Log log, String logPrefix , String tsap , String remoteTsap) {
        this.plcIp = plcIp;
        this.port = port;
        this.log = log;
        this.logPrefix = logPrefix;
        this._TSAP = tsap ;
        this._externalTSAP = remoteTsap;
    }

    public RFC1006TSAPHandler() {
		// TODO Auto-generated constructor stub
	}

	public void connect() {
        while (true) {
            try {
                log.info(logPrefix, "Attempting to connect to PLC at " + plcIp + ":" + port);
                socket = new Socket(plcIp, port);
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                log.info(logPrefix, "Connection to PLC successful.");
                initializeConnectionSequence();
                this.connected = true;
                break;

            } catch (IOException e) {
                log.error(logPrefix, "Connection failed: " + e.getMessage());
                waitBeforeRetry();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(logPrefix, "Connection thread interrupted. Exiting...");
                return;
            }
        }
    }

    private void initializeConnectionSequence() throws IOException, InterruptedException {
        sendConnectRequest();
        log.info(logPrefix, "Sending Connection Request (CR) TSAP=" + _TSAP + ", externalTSAP=" + _externalTSAP);
        Thread.sleep(100);

        validateConnectResponse();
        log.info(logPrefix, "Received Connection Confirm (CC)");
        Thread.sleep(100);

        new ConnectedTlg(channelId, version, 0x01, SubsystemID)
            .send(outputStream, log, logPrefix);
        lastSentTime = System.currentTimeMillis();
        Thread.sleep(100);

        ReadyTlg readyTelegram = new ReadyTlg(channelId, version, 0x01, SubsystemID, BypassMode, LLC_Mode);
        readyTelegram.send(outputStream, log, logPrefix);
        sequenceNo = readyTelegram.validate(inputStream, log, logPrefix);
        lastSentTime = System.currentTimeMillis();

        Thread.sleep(1000);
        sendAckTelegram(sequenceNo);
        if (waitForInitialResponse()) {
            log.info(logPrefix, "KeepAlive response received. Entering KeepAlive-only mode.");
            TelegramDispatcher telegramDispatcher = new TelegramDispatcher(inputStream, outputStream, log, logPrefix, this::scheduleReconnect);

            telegramDispatcher.startDispatchLoop(); // handles all telegrams
            telegramDispatcher.startTriggerServer();
            TelegramSenderUI ui = new TelegramSenderUI(telegramDispatcher);
            ui.showUI();
          
        } else {
            log.warn(logPrefix, "No KeepAlive response. Starting full telegram dispatch mode.");
              TelegramDispatcher telegramDispatcher =new TelegramDispatcher(inputStream, outputStream, log, logPrefix, this::scheduleReconnect);
              telegramDispatcher.startDispatchLoop(); // handles all telegrams
              // === Step 4: Launch frontend UI ===
              telegramDispatcher.startTriggerServer();
              TelegramSenderUI ui = new TelegramSenderUI(telegramDispatcher);
              ui.showUI();
        }
    }

    private boolean waitForInitialResponse() {
        try {
            for (int retries = 0; retries < 3; retries++) {
                KeepAliveTelegram();
                log.info(logPrefix, "Waiting for KeepAlive response (" + (retries + 1) + "/3)...");

                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 5000) {
                    if (inputStream.available() >= 4) {
                        byte[] header = new byte[4];
                        readFully(inputStream, header, 0, 4);

                        int length = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
                        byte[] data = new byte[length - 4];
                        readFully(inputStream, data, 0, data.length);

                        if ((data[0] & 0xFF) == 0x02 && (data[1] & 0xFF) == 0xF0 && (data[2] & 0xFF) == 0x80) {
                            log.info(logPrefix, "Valid KeepAlive response received.");
                            lastReceivedTime = System.currentTimeMillis();
                            return true;
                        }
                    }
                    Thread.sleep(100);
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error(logPrefix, "Initial KeepAlive wait failed: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        return false;
    }

    private void startKeepAliveMonitor() {
        monitorRunning = true;

        keepAliveMonitorThread = new Thread(() -> {
            log.info(logPrefix, "KeepAlive monitor thread started.");

            while (monitorRunning) {
                try {
                    Thread.sleep(1000);
                    long now = System.currentTimeMillis();

                    if ((now - lastReceivedTime) > 15000) {
                        log.error(logPrefix, "PLC unresponsive for 15s. Triggering disconnect.");
                        break;
                    }

                    if ((now - lastSentTime) >= 5000) {
                        log.debug(logPrefix, "Sending KeepAlive...");
                        KeepAliveTelegram();
                        lastSentTime = System.currentTimeMillis();
                    }

                    if (inputStream.available() >= 4) {
                        byte[] header = new byte[4];
                        readFully(inputStream, header, 0, 4);

                        int length = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
                        byte[] data = new byte[length - 4];
                        readFully(inputStream, data, 0, data.length);

                        if ((data[0] & 0xFF) == 0x02 && (data[1] & 0xFF) == 0xF0 && (data[2] & 0xFF) == 0x80) {
                            lastReceivedTime = System.currentTimeMillis();
                            log.debug(logPrefix, "KeepAlive acknowledged.");
                        }
                    }

                } catch (Exception e) {
                    log.error(logPrefix, "KeepAlive monitor error: " + e.getMessage());
                    break;
                }
            }

            try {
                disconnect();
            } catch (IOException e) {
                log.error(logPrefix, "Error during disconnect: " + e.getMessage());
            }
            scheduleReconnect();
        }, "KeepAliveMonitor");

        keepAliveMonitorThread.setDaemon(true);
        keepAliveMonitorThread.start();
    }

    private void scheduleReconnect() {
        new Thread(() -> {
            waitBeforeRetry();
            connect();
        }, "PLC-Reconnect").start();
    }

    private void waitBeforeRetry() {
        try {
            log.info(logPrefix, "Waiting 20 seconds before retrying connection...");
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(logPrefix, "Reconnect wait interrupted.");
        }
    }

    public void disconnect() throws IOException {
        log.info(logPrefix, "Disconnecting from PLC...");
        monitorRunning = false;

        if (keepAliveMonitorThread != null && keepAliveMonitorThread.isAlive()) {
            keepAliveMonitorThread.interrupt();
        }

        if (inputStream != null) inputStream.close();
        if (outputStream != null) outputStream.close();
        if (socket != null && !socket.isClosed()) socket.close();
        this.connected = false;
    }

    // this function is used to send the connection request to the PLC...
    protected void sendConnectRequest() throws IOException {
        int cnt = 0;
        int tpduLen = 13 + _TSAP.length() + _externalTSAP.length();
        int totalLen = HEADSIZE + 1 + tpduLen;
        _sendBuf = new byte[totalLen];

        _sendBuf[cnt++] = TPKT_VERSION;
        _sendBuf[cnt++] = TPKT_RESERVE;
        _sendBuf[cnt++] = (byte) ((totalLen >> 8) & 0xff);
        _sendBuf[cnt++] = (byte) (totalLen & 0xff);
        _sendBuf[cnt++] = (byte) (tpduLen & 0xff);
        _sendBuf[cnt++] = TPDU_CR;

        int id = _externalConnectionId;
        _sendBuf[cnt++] = (byte) ((id >> 8) & 0xff);
        _sendBuf[cnt++] = (byte) (id & 0xff);

        id = _connectionId;
        _sendBuf[cnt++] = (byte) ((id >> 8) & 0xff);
        _sendBuf[cnt++] = (byte) (id & 0xff);

        _sendBuf[cnt++] = (byte) 0;
        _sendBuf[cnt++] = TPDU_SIZE_CMD;
        _sendBuf[cnt++] = (byte) 0x01;
        _sendBuf[cnt++] = _tpduSize;

        String vaParm = _TSAP;
        _sendBuf[cnt++] = TPDU_CALLING_CMD;
        _sendBuf[cnt++] = (byte) vaParm.length();
        for (int i = 0; i < vaParm.length(); i++) _sendBuf[cnt++] = (byte) vaParm.charAt(i);

        vaParm = _externalTSAP;
        _sendBuf[cnt++] = TPDU_CALLED_CMD;
        _sendBuf[cnt++] = (byte) vaParm.length();
        for (int i = 0; i < vaParm.length(); i++) _sendBuf[cnt++] = (byte) vaParm.charAt(i);

        outputStream.write(_sendBuf, 0, cnt);
        outputStream.flush();   
        log.debug(logPrefix, "CR telegram sent");
        log.info(logPrefix, "Sending CR telegram telegram: " + bytesToHex(_sendBuf));
        lastSentTime = System.currentTimeMillis();
    }

    // this function is used to validate the confirm request come from the PLC...
    private void validateConnectResponse() throws IOException {
        byte[] header = new byte[4];
        readFully(inputStream, header, 0, 4);

        if ((header[0] & 0xFF) != 0x03) {
            throw new IOException("Invalid TPKT version: expected 0x03 but found " + (header[0] & 0xFF));
        }

        int totalLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
        int cotpLength = totalLength - 4;

        if (cotpLength < 7) {
            throw new IOException("COTP segment too short, expected at least 7 bytes but got " + cotpLength);
        }

        byte[] cotp = new byte[cotpLength];
        readFully(inputStream, cotp, 0, cotpLength);

        // Basic validation of Connect Confirm PDU
        if ((cotp[0] & 0xFF) != (cotpLength - 1)) {
            throw new IOException("Incorrect COTP length indicator (LI): expected " + (cotpLength - 1) + " but got " + (cotp[0] & 0xFF));
        }

        if ((cotp[1] & 0xFF) != 0xD0) {
            throw new IOException("Invalid Connect Confirm (CC) PDU type: Expected 0xD0, found 0x" + Integer.toHexString(cotp[1] & 0xFF));
        }

        log.info(logPrefix, "Connection Confirm (CC) received successfully");
        log.info(logPrefix, "Receiving CC telegram: " + bytesToHex(header) + " " + bytesToHex(cotp));

        // Start parsing parameters after header (LI=0, PDUType=1, DST=2-3, SRC=4-5, Class=6) => Start at idx = 7
        int idx = 7;

        while (idx + 1 < cotp.length) {
            int paramType = cotp[idx++] & 0xFF;
            int paramLength = cotp[idx++] & 0xFF;

            if (idx + paramLength > cotp.length) {
                throw new IOException("Malformed parameter: length exceeds COTP payload");
            }

            switch (paramType) {
                case 0xC0: // TPDU Size
                    if (paramLength != 1) {
                        throw new IOException("Invalid TPDU Size parameter length (expected 1, got " + paramLength + ")");
                    }
                    _tpduSize = (byte) (cotp[idx] & 0xFF);  // e.g., 0x0A
                    _tpduSizeInBytes = (int) Math.pow(2, _tpduSize);
                    _sendBuf = new byte[_tpduSizeInBytes + 4];  // +4 for TPKT header
                    log.info(logPrefix, "TPDU size set to " + _tpduSizeInBytes + " bytes (2^" + _tpduSize + ")");
                    break;

                case 0xC1: // Calling TSAP
                case 0xC2: // Called TSAP
                    // Optionally you can store or log TSAP values
                    log.debug(logPrefix, "TSAP (Type 0x" + Integer.toHexString(paramType) + "): " +
                    		bytesToHex(cotp, idx, paramLength));
                    break;

                default:
                    log.info(logPrefix, "Unknown parameter in CC telegram: Type=0x" + String.format("%02X", paramType));
                    break;
            }

            idx += paramLength;
        }

        lastReceivedTime = System.currentTimeMillis();
    }
    private static String bytesToHex(byte[] arr, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            sb.append(String.format("%02X ", arr[i]));
        }
        return sb.toString().trim();
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public boolean isConnectionAlive() {
        return this.connected;
    }
    
    public void sendAckTelegram(int sequenceNumber) throws IOException {
        // === Channel Header Parameters ===
        int channelId = 7;             // 0x0007 (assigned communication channel)
        int protocolVersion = 1;       // 0x0001
        int internalSeqNum = sequenceNumber; // Sequence used internally

        // === Build Channel Header (8 bytes) ===
        byte[] channelHeader = new byte[] {
            (byte)(channelId >> 8), (byte)(channelId & 0xFF),
            (byte)(protocolVersion >> 8), (byte)(protocolVersion & 0xFF),
            (byte)(internalSeqNum >> 24), (byte)(internalSeqNum >> 16),
            (byte)(internalSeqNum >> 8), (byte)(internalSeqNum)
        };

        // === ACK Payload (6 bytes) ===
        byte[] ackPayload = new byte[] {
            0x00, (byte) 0x66,                      // TT = 102
            0x00, 0x04,                             // LL = 4
            (byte) ((sequenceNumber >> 24) & 0xFF), // Q1
            (byte) ((sequenceNumber >> 16) & 0xFF), // Q2
            (byte) ((sequenceNumber >> 8) & 0xFF),  // Q3
            (byte) (sequenceNumber & 0xFF)          // Q4
        };

        // === Total Payload (Channel Header + ACK Payload) ===
        byte[] fullPayload = new byte[channelHeader.length + ackPayload.length];
        System.arraycopy(channelHeader, 0, fullPayload, 0, channelHeader.length);
        System.arraycopy(ackPayload, 0, fullPayload, channelHeader.length, ackPayload.length);

        // === Add TPKT (RFC1006) + COTP Headers ===
        int totalLength = fullPayload.length + 7; // 4 bytes TPKT + 3 bytes COTP
        byte[] telegram = new byte[totalLength];

        // === TPKT Header ===
        telegram[0] = 0x03; // TPKT version
        telegram[1] = 0x00;
        telegram[2] = (byte)((totalLength >> 8) & 0xFF);
        telegram[3] = (byte)(totalLength & 0xFF);

        // === COTP Header ===
        telegram[4] = 0x02;      // LI = 2
        telegram[5] = (byte) 0xF0; // COTP PDU Type = Data
        telegram[6] = (byte) 0x80; // EOT = 1

        // === Copy Payload (Channel Header + ACK Payload) ===
        System.arraycopy(fullPayload, 0, telegram, 7, fullPayload.length);

        // === Send to PLC ===
        log.info(logPrefix, "Sending ACK (TT=102) telegram: " + bytesToHex(telegram));
        outputStream.write(telegram);
        outputStream.flush();
        lastSentTime = System.currentTimeMillis();
        log.info(logPrefix, "ACK telegram sent from CSC to PLC");
    }


  
    public void KeepAliveTelegram() throws IOException {
        byte[] KeepAliveTelegram = new byte[] {
            TPKT_VERSION, TPKT_RESERVE, 0x00, 0x08, 0x02, (byte) 0xf0, (byte) 0x80, 0x00
        };
        outputStream.write(KeepAliveTelegram);
        outputStream.flush();
        lastSentTime = System.currentTimeMillis();
        log.info(logPrefix, "KeepAlive telegram sent");
       // === Send to PLC ===
        lastSentTime = System.currentTimeMillis();
        log.info(logPrefix, "Sending KeepAlive  telegram: " + bytesToHex(KeepAliveTelegram));
    }

    public boolean validateKeepAliveResponse() {
        try {
            byte[] response = new byte[8];
            readFully(inputStream, response, 0, response.length);

            // TPKT Version check (should be 0x03)
            if ((response[0] & 0xFF) != 0x03) {
                log.error(logPrefix, "Invalid TPKT version in KeepAlive response");
                return false;
            }

            // COTP PDU check (should be F0 80)
            if ((response[5] & 0xFF) != 0xF0 || (response[6] & 0xFF) != 0x80) {
                log.error(logPrefix, "Invalid COTP PDU in KeepAlive response");
                return false;
            }

            lastReceivedTime = System.currentTimeMillis(); // 🟢 valid telegram received
            log.info(logPrefix, "KeepAlive response received successfully");
            log.info(logPrefix, "Received KeepAlive telegram: " + bytesToHex(response));
            return true;
            

        } catch (IOException e) {
            log.error(logPrefix, "Error reading KeepAlive response: " + e.getMessage());
            return false;
        }
    }


    public byte[] receiveTelegram() throws IOException {
        byte[] header = new byte[4];
        readFully(inputStream, header, 0, 4);

        if ((header[0] & 0xFF) != 0x03) throw new IOException("Invalid TPKT version");

        int totalLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
        byte[] payload = new byte[totalLength - 4];
        readFully(inputStream, payload, 0, payload.length);
        lastReceivedTime = System.currentTimeMillis();
        log.debug(logPrefix, "Telegram received from PLC");
        return payload;
    }



  
    private void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = in.read(buffer, offset, length);
            if (read == -1) throw new IOException("Connection closed unexpectedly");
            offset += read;
            length -= read;
        }
    }

    private String getTSAPString(byte[] tsap) {
        StringBuilder sb = new StringBuilder();
        for (byte b : tsap) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
