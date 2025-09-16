package client;

import java.io.*;
import java.net.*;

public class PeerServer implements Runnable {
    private int port;

    public PeerServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("PeerServer stopped: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("[Tin nhắn mới] " + line);
                // Bạn có thể cập nhật UI chat nếu đang mở ChatWindow tương ứng
            }
        } catch (IOException ignored) {}
    }
}
