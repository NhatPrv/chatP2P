package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatPeer {
    public static void startChat(String user, String target) {
        try (Socket peer = new Socket("localhost", 6000);
             PrintWriter pw = new PrintWriter(peer.getOutputStream(), true)) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Chat với " + target + " (gõ /exit để thoát)");
            while (true) {
                String msg = sc.nextLine();
                if (msg.equals("/exit")) break;
                pw.println(user + ": " + msg);
            }
        } catch (IOException e) {
            System.out.println("Không thể kết nối peer!");
        }
    }
}
