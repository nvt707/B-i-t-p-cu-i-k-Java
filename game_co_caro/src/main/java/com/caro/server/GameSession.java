package com.caro.server;

import java.io.*;
import java.net.Socket;

public class GameSession extends Thread {
    private Socket p1, p2;
    private int roomId;

    public GameSession(Socket p1, Socket p2, int roomId) {
        this.p1 = p1;
        this.p2 = p2;
        this.roomId = roomId;
    }

    public void run() {
        try {
            DataInputStream  in1  = new DataInputStream(p1.getInputStream());
            DataOutputStream out1 = new DataOutputStream(p1.getOutputStream());
            DataInputStream  in2  = new DataInputStream(p2.getInputStream());
            DataOutputStream out2 = new DataOutputStream(p2.getOutputStream());

            // Gửi lệnh bắt đầu – P1 là X đi trước, P2 là O
            out1.writeUTF("START,X");
            out2.writeUTF("START,O");

            // Chuyển tiếp nước đi giữa 2 người chơi
            while (true) {
                // ✅ FIX: client đã gửi "MOVE,row,col" nên forward nguyên,
                //         không thêm "MOVE," nữa để tránh "MOVE,MOVE,row,col"
                String moveP1 = in1.readUTF(); // nhận "MOVE,row,col" từ P1
                out2.writeUTF(moveP1);         // forward sang P2

                String moveP2 = in2.readUTF(); // nhận "MOVE,row,col" từ P2
                out1.writeUTF(moveP2);         // forward sang P1
            }
        } catch (IOException e) {
            System.out.println("Phòng " + roomId + ": Người chơi đã thoát.");
        } finally {
            // Dọn phòng khỏi danh sách khi game kết thúc
            GameServer.rooms.remove(roomId);
            try { p1.close(); } catch (IOException ignored) {}
            try { p2.close(); } catch (IOException ignored) {}
        }
    }
}