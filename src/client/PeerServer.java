package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class PeerServer implements Runnable {
    private final int port;
    private final MessageHandler handler;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public PeerServer(int port, MessageHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handleClient(socket)).start();
                } catch (SocketException e) {
                    if (running) {
                        System.out.println("PeerServer error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("PeerServer stopped: " + e.getMessage());
            }
        } finally {
            closeServerSocket();
        }
    }

    public void stop() {
        running = false;
        closeServerSocket();
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (handler != null) {
                    int idx = line.indexOf(':');
                    String sender = idx >= 0 ? line.substring(0, idx).trim() : "";
                    String message = idx >= 0 ? line.substring(idx + 1).trim() : line;
                    handler.onIncomingMessage(sender, message, socket.getInetAddress().getHostAddress());
                }
            }
        } catch (IOException ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public interface MessageHandler {
        void onIncomingMessage(String sender, String message, String hostAddress);
    }
}
