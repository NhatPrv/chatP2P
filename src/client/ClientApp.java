package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ClientApp {
    private static volatile List<String> onlineUsers = new ArrayList<>();
    private static final Map<String, PeerInfo> peerInfoMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PeerServer peerServer = null;
        Socket server = null;
        try {
            int peerPort = 6000 + (int) (Math.random() * 1000);
            peerServer = new PeerServer(peerPort, (sender, message, host) ->
                    System.out.println("\n[Tin nhắn] " + sender + ": " + message));
            new Thread(peerServer, "PeerServer-CLI").start();
            System.out.println("PeerServer lắng nghe ở port " + peerPort);

            server = new Socket("localhost", 5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
            PrintWriter out = new PrintWriter(server.getOutputStream(), true);

            System.out.print("Username: ");
            String user = sc.nextLine();
            System.out.print("Password: ");
            String pass = sc.nextLine();

            out.println("LOGIN:" + user + ":" + pass + ":" + peerPort);
            String response = in.readLine();
            if (!"LOGIN_OK".equals(response)) {
                System.out.println("Sai tài khoản hoặc mật khẩu!");
                return;
            }

            BufferedReader reader = in;
            Thread listener = new Thread(() -> listenForUpdates(reader, user));
            listener.setDaemon(true);
            listener.start();

            menuLoop(sc, out, server, user);
        } catch (IOException e) {
            System.out.println("Không thể kết nối server: " + e.getMessage());
        } finally {
            if (peerServer != null) {
                peerServer.stop();
            }
            if (server != null && !server.isClosed()) {
                try {
                    server.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void menuLoop(Scanner sc, PrintWriter out, Socket server, String user) {
        while (true) {
            System.out.println("\n1. Xem danh sách online");
            System.out.println("2. Chat với ai đó");
            System.out.println("3. Logout");
            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    System.out.println("Online: " + new ArrayList<>(onlineUsers));
                    break;
                case "2":
                    System.out.print("Nhập tên người muốn chat: ");
                    String target = sc.nextLine();
                    PeerInfo info = peerInfoMap.get(target);
                    if (info == null) {
                        System.out.println("Người dùng không khả dụng.");
                        break;
                    }
                    ChatPeer.startChat(user, target, info.getHost(), info.getPort());
                    break;
                case "3":
                    if (out != null) {
                        out.println("LOGOUT:" + user);
                    }
                    if (server != null && !server.isClosed()) {
                        try {
                            server.close();
                        } catch (IOException ignored) {
                        }
                    }
                    return;
                default:
                    System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private static void listenForUpdates(BufferedReader in, String currentUser) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("ONLINE_LIST:")) {
                    updatePeerInfo(line.substring(12), currentUser);
                }
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối server");
        }
    }

    private static void updatePeerInfo(String payload, String currentUser) {
        Map<String, PeerInfo> updated = new ConcurrentHashMap<>();
        if (payload != null && !payload.isEmpty()) {
            String[] entries = payload.split(";");
            for (String entry : entries) {
                if (entry == null || entry.isEmpty()) {
                    continue;
                }
                String[] parts = entry.split("\\|");
                if (parts.length < 3) {
                    continue;
                }
                String username = parts[0].trim();
                String host = parts[1].trim();
                int port;
                try {
                    port = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (!username.isEmpty()) {
                    updated.put(username, new PeerInfo(username, host, port));
                }
            }
        }

        peerInfoMap.clear();
        peerInfoMap.putAll(updated);
        List<String> display = new ArrayList<>();
        for (String name : updated.keySet()) {
            if (!name.equals(currentUser)) {
                display.add(name);
            }
        }
        onlineUsers = display;
        System.out.println("\n[Danh sách online] " + display);
    }
}
