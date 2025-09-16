package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.ServerSocket;

public class ServerFrame extends JFrame {
    private JTextArea logArea;
    private JButton btnStart, btnStop;
    private Thread serverThread;
    private ServerSocket serverSocket;

    public ServerFrame() {
        setTitle("Library Server");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel panelButtons = new JPanel();
        btnStart = new JButton("Start Server");
        btnStop = new JButton("Stop Server");
        btnStop.setEnabled(false);

        panelButtons.add(btnStart);
        panelButtons.add(btnStop);
        add(panelButtons, BorderLayout.SOUTH);

        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
    }

    private void startServer() {
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        log("Starting server...");

        serverThread = new Thread(() -> {
            try {
                LibraryServer.main(null);  // Gọi server chạy trong thread
            } catch (IOException e) {
                log("Error: " + e.getMessage());
            }
        });
        serverThread.start();
        log("Server is running.");
    }

    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (serverThread != null && serverThread.isAlive()) {
                serverThread.interrupt();
            }
            log("Server stopped.");
        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        }
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerFrame().setVisible(true));
    }
}
