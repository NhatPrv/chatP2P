package client;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientApp {
    private static List<String> onlineUsers = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        Socket server = new Socket("localhost", 5000);
        BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
        PrintWriter out = new PrintWriter(server.getOutputStream(), true);

        Scanner sc = new Scanner(System.in);
        System.out.print("Username: ");
        String user = sc.nextLine();
        System.out.print("Password: ");
        String pass = sc.nextLine();

        out.println("LOGIN:" + user + ":" + pass);
        String response = in.readLine();
        if (!"LOGIN_OK".equals(response)) {
            System.out.println("Sai tài khoản hoặc mật khẩu!");
            return;
        }

        // Thread lắng nghe danh sách online
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("ONLINE_LIST:")) {
                        onlineUsers = Arrays.asList(line.substring(12).split(","));
                        System.out.println("\n[Danh sách online] " + onlineUsers);
                    }
                }
            } catch (IOException e) {
                System.out.println("Mất kết nối server");
            }
        }).start();

        // Menu chat
        while (true) {
            System.out.println("\n1. Xem danh sách online");
            System.out.println("2. Chat với ai đó");
            System.out.println("3. Logout");
            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    System.out.println("Online: " + onlineUsers);
                    break;
                case "2":
                    System.out.print("Nhập tên người muốn chat: ");
                    String target = sc.nextLine();
                    ChatPeer.startChat(user, target);
                    break;
                case "3":
                    out.println("LOGOUT:" + user);
                    server.close();
                    return;
            }
        }
    }
}
