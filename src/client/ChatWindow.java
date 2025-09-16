package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import server.MessageDAO; // import DAO

public class ChatWindow extends JFrame {
    private JTextArea chatArea;
    private JTextField txtMessage;
    private PrintWriter pw;
    private Socket socket;
    private String user;
    private String target;
    private MessageDAO messageDAO;

    public ChatWindow(String user, String target, String peerIP, int peerPort) {
        this.user = user;
        this.target = target;
        this.messageDAO = new MessageDAO();

        setTitle("Chat với " + target);
        setSize(400, 300);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        txtMessage = new JTextField();
        add(txtMessage, BorderLayout.SOUTH);

        loadHistory();

        try {
            socket = new Socket(peerIP, peerPort);  // <- dùng IP & port thật của peer
            pw = new PrintWriter(socket.getOutputStream(), true);
            chatArea.append("Bắt đầu chat với " + target + "...\n");
        } catch (IOException e) {
            chatArea.append("Không thể kết nối tới " + target + " tại " + peerIP + ":" + peerPort + "\n");
        }

        txtMessage.addActionListener(e -> {
            String msg = txtMessage.getText();
            if (!msg.isEmpty() && pw != null) {
                pw.println(user + ": " + msg);
                chatArea.append("Tôi: " + msg + "\n");
                messageDAO.saveMessage(user, target, msg);
                txtMessage.setText("");
            }
        });
    }

    private void loadHistory() {
        List<String> history = messageDAO.getConversation(user, target);
        for (String line : history) {
            chatArea.append(line + "\n");
        }
    }
}
