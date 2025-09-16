package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

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
        int peerPort = 6000 + (int)(Math.random() * 1000);
    new Thread(new PeerServer(peerPort)).start();
    System.out.println("PeerServer lắng nghe ở port " + peerPort);
        try {
            serverSocket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            out = new PrintWriter(serverSocket.getOutputStream(), true);

            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            out.println("LOGIN:" + username + ":" + password);

            String response = in.readLine();
            if ("LOGIN_OK".equals(response)) {
                currentUser = username;
                JOptionPane.showMessageDialog(this, "Login thành công!");
                btnLogin.setEnabled(false);
                btnLogout.setEnabled(true);
                btnChat.setEnabled(true);
                new Thread(() -> listenServer()).start();
            } else {
                JOptionPane.showMessageDialog(this, "Sai username/password!");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối server!");
        }
    }

    private void logout() {
        if (out != null) {
            out.println("LOGOUT:" + currentUser);
        }
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        onlineModel.clear();
        btnLogin.setEnabled(true);
        btnLogout.setEnabled(false);
        btnChat.setEnabled(false);
    }

    private void listenServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("ONLINE_LIST:")) {
                    String[] users = line.substring(12).split(",");
                    SwingUtilities.invokeLater(() -> {
                        onlineModel.clear();
                        for (String u : users) {
                            if (!u.isEmpty() && !u.equals(currentUser)) {
                                onlineModel.addElement(u);
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Mất kết nối server.");
        }
    }

    private void startChat() {
    String selected = listOnline.getSelectedValue();
    if (selected != null) {
        // Tạm thời test, sau này phải lấy port của peer từ server
        String peerIP = "localhost";
        int peerPort = 6000;
        new ChatWindow(currentUser, selected, peerIP, peerPort).setVisible(true);
    }
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
