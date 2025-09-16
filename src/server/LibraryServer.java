package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class LibraryServer {
    private static final int PORT = 5000;
    private static Map<String, ClientHandler> onlineClients = new HashMap<>();

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
        private Socket socket;
        private String username;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                while (true) {
                    String line = in.readLine();
                    if (line == null) break;

                    String[] parts = line.split(":", 3);
                    switch (parts[0]) {
                        case "LOGIN":
                            username = parts[1];
                            String password = parts[2];
                            UserDAO dao = new UserDAO();
                            if (dao.checkLogin(username, password)) {
                                onlineClients.put(username, this);
                                out.println("LOGIN_OK");
                                broadcastOnlineList();
                            } else {
                                out.println("LOGIN_FAIL");
                            }
                            break;
                        case "LOGOUT":
                            onlineClients.remove(username);
                            broadcastOnlineList();
                            socket.close();
                            return;
                    }
                }
            } catch (IOException e) {
                onlineClients.remove(username);
            }
        }
    }

    private static void broadcastOnlineList() {
        String list = String.join(",", onlineClients.keySet());
        for (ClientHandler c : onlineClients.values()) {
            try {
                new PrintWriter(c.socket.getOutputStream(), true)
                    .println("ONLINE_LIST:" + list);
            } catch (IOException ignored) {}
        }
    }
}
