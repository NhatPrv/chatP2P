package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public void saveMessage(String sender, String receiver, String message) {
        String sql = "INSERT INTO messages (sender, receiver, message) VALUES (?, ?, ?)";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getConversation(String user1, String user2) {
        List<String> conversation = new ArrayList<>();
        String sql = "SELECT sender, message, sent_at FROM messages " +
                "WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?) ORDER BY sent_at ASC";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String msg = rs.getString("message");
                Timestamp time = rs.getTimestamp("sent_at");
                conversation.add("[" + time + "] " + sender + ": " + msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conversation;
    }
}
