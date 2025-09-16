package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientGUI extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnLogout, btnChat;
    private JList<String> listOnline;
    private DefaultListModel<String> onlineModel;
    private Socket serverSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String currentUser;
    private PeerServer peerServer;
    private int peerPort;
    private final Map<String, PeerInfo> peerInfoMap = new ConcurrentHashMap<>();
    private final Map<String, ChatWindow> chatWindows = new ConcurrentHashMap<>();
    private volatile boolean intentionalDisconnect = false;

    public ClientGUI() {
        setTitle("Client - P2P Chat");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel panelLogin = new JPanel(new GridLayout(3, 2));
        panelLogin.setBorder(BorderFactory.createTitledBorder("Đăng nhập"));

        panelLogin.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        panelLogin.add(txtUsername);

        panelLogin.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        panelLogin.add(txtPassword);

        btnLogin = new JButton("Login");
        btnLogout = new JButton("Logout");
        btnLogout.setEnabled(false);

        panelLogin.add(btnLogin);
        panelLogin.add(btnLogout);

        add(panelLogin, BorderLayout.NORTH);

        onlineModel = new DefaultListModel<>();
        listOnline = new JList<>(onlineModel);
        add(new JScrollPane(listOnline), BorderLayout.CENTER);

        btnChat = new JButton("Chat riêng");
        btnChat.setEnabled(false);
        add(btnChat, BorderLayout.SOUTH);

        btnLogin.addActionListener(e -> login());
        btnLogout.addActionListener(e -> logout());
        btnChat.addActionListener(e -> startChat());
    }

    private void login() {
        if (currentUser != null) {
            return;
        }

        stopPeerServer();
        peerPort = 6000 + (int) (Math.random() * 1000);
        peerServer = new PeerServer(peerPort, this::handleIncomingMessage);
        new Thread(peerServer, "PeerServer-" + peerPort).start();
        System.out.println("PeerServer lắng nghe ở port " + peerPort);

        try {
            serverSocket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            out = new PrintWriter(serverSocket.getOutputStream(), true);

            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            intentionalDisconnect = false;
            out.println("LOGIN:" + username + ":" + password + ":" + peerPort);

            String response = in.readLine();
            if ("LOGIN_OK".equals(response)) {
                currentUser = username;
                JOptionPane.showMessageDialog(this, "Login thành công!");
                btnLogin.setEnabled(false);
                btnLogout.setEnabled(true);
                btnChat.setEnabled(true);
                new Thread(this::listenServer, "ServerListener").start();
            } else {
                JOptionPane.showMessageDialog(this, "Sai username/password!");
                intentionalDisconnect = true;
                stopPeerServer();
                closeServerConnection();
            }
        } catch (IOException ex) {
            stopPeerServer();
            closeServerConnection();
            JOptionPane.showMessageDialog(this, "Không thể kết nối server!");
        }
    }

    private void logout() {
        if (currentUser == null && out == null) {
            return;
        }

        intentionalDisconnect = true;
        if (out != null && currentUser != null) {
            out.println("LOGOUT:" + currentUser);
        }

        closeServerConnection();
        stopPeerServer();
        peerInfoMap.clear();

        for (ChatWindow window : new ArrayList<>(chatWindows.values())) {
            window.dispose();
        }
        chatWindows.clear();

        onlineModel.clear();
        btnLogin.setEnabled(true);
        btnLogout.setEnabled(false);
        btnChat.setEnabled(false);
        currentUser = null;
        intentionalDisconnect = false;
    }

    private void listenServer() {
        try {
            String line;
            while (true) {
                BufferedReader reader = in;
                if (reader == null) {
                    break;
                }
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("ONLINE_LIST:")) {
                    processOnlineList(line.substring(12));
                }
            }
        } catch (IOException e) {
            handleServerDisconnect();
            return;
        }

        handleServerDisconnect();
    }

    private void processOnlineList(String payload) {
        Map<String, PeerInfo> updated = new HashMap<>();
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
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (!username.isEmpty()) {
                    updated.put(username, new PeerInfo(username, host, port));
                }
            }
        }

        peerInfoMap.clear();
        peerInfoMap.putAll(updated);

        SwingUtilities.invokeLater(() -> {
            onlineModel.clear();
            for (String user : updated.keySet()) {
                if (!user.equals(currentUser)) {
                    onlineModel.addElement(user);
                }
            }
        });
    }

    private void handleIncomingMessage(String sender, String message, String hostAddress) {
        if (sender == null || sender.isEmpty()) {
            sender = hostAddress != null ? hostAddress : "Unknown";
        }
        final String fromUser = sender;
        final String content = message;

        SwingUtilities.invokeLater(() -> {
            ChatWindow window = chatWindows.get(fromUser);
            if (window == null) {
                PeerInfo info = peerInfoMap.get(fromUser);
                if (info == null) {
                    JOptionPane.showMessageDialog(this, fromUser + ": " + content);
                    return;
                }
                window = openChatWindow(info);
            }
            if (window != null) {
                window.appendIncomingMessage(fromUser, content);
            }
        });
    }

    private ChatWindow openChatWindow(PeerInfo info) {
        if (info == null) {
            return null;
        }

        ChatWindow existing = chatWindows.get(info.getUsername());
        if (existing != null) {
            existing.toFront();
            existing.requestFocus();
            return existing;
        }

        ChatWindow window = new ChatWindow(currentUser, info.getUsername(), info.getHost(), info.getPort());
        chatWindows.put(info.getUsername(), window);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                chatWindows.remove(info.getUsername());
            }
        });
        window.setVisible(true);
        return window;
    }

    private void handleServerDisconnect() {
        closeServerConnection();
        if (!intentionalDisconnect) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Mất kết nối server."));
        }
        intentionalDisconnect = false;
    }

    private void stopPeerServer() {
        if (peerServer != null) {
            peerServer.stop();
            peerServer = null;
        }
    }

    private void closeServerConnection() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        serverSocket = null;
        in = null;
        out = null;
    }

    private void startChat() {
        String selected = listOnline.getSelectedValue();
        if (selected == null) {
            return;
        }

        PeerInfo info = peerInfoMap.get(selected);
        if (info == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin peer của " + selected);
            return;
        }

        openChatWindow(info);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
