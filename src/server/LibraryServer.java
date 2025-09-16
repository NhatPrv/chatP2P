package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LibraryServer {
    private static final int PORT = 5000;
    private static final Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started at port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;
        private int peerPort;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    String line = in.readLine();
                    if (line == null) break;

                    String[] parts = line.split(":", 4);
                    switch (parts[0]) {
                        case "LOGIN":
                            handleLogin(parts);
                            break;
                        case "LOGOUT":
                            out.println("LOGOUT_OK");
                            return;
                        default:
                            out.println("UNKNOWN_COMMAND");
                    }
                }
            } catch (IOException e) {
                // client disconnected unexpectedly
            } finally {
                cleanup();
            }
        }

        private void handleLogin(String[] parts) {
            if (parts.length < 4) {
                out.println("LOGIN_FAIL");
                return;
            }

            String attemptedUser = parts[1];
            String password = parts[2];
            int peerPortCandidate;
            try {
                peerPortCandidate = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                out.println("LOGIN_FAIL");
                return;
            }
            if (peerPortCandidate <= 0 || peerPortCandidate > 65535) {
                out.println("LOGIN_FAIL");
                return;
            }

            UserDAO dao = new UserDAO();
            if (dao.checkLogin(attemptedUser, password)) {
                username = attemptedUser;
                peerPort = peerPortCandidate;

                ClientHandler existing = onlineClients.put(username, this);
                if (existing != null && existing != this) {
                    existing.closeSilently();
                }

                out.println("LOGIN_OK");
                broadcastOnlineList();
            } else {
                out.println("LOGIN_FAIL");
            }
        }

        private void cleanup() {
            if (username != null) {
                if (onlineClients.remove(username, this)) {
                    broadcastOnlineList();
                }
            }
            closeSilently();
        }

        private void closeSilently() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private String getPeerInfo() {
            if (username == null) {
                return "";
            }
            String host = socket.getInetAddress().getHostAddress();
            return username + "|" + host + "|" + peerPort;
        }
    }

    private static void broadcastOnlineList() {
        String list = onlineClients.values().stream()
                .map(ClientHandler::getPeerInfo)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(";"));

        for (ClientHandler client : onlineClients.values()) {
            client.sendMessage("ONLINE_LIST:" + list);
        }
    }
}
