package com.xray.backend.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@Service
public class XRayTcpService {

    private static final int CONNECTION_TIMEOUT_MS = 2000;

    public boolean checkConnection(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String sendCommand(String ip, int port, String command) {
        // Implementation for sending command and receiving response
        // For now, simulating a response
        if (checkConnection(ip, port)) {
            return "ACK: " + command;
        } else {
            throw new RuntimeException("Device unreachable at " + ip + ":" + port);
        }
    }
}
