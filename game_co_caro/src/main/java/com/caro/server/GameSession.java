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

            out1.writeUTF("START,X");
            out2.writeUTF("START,O");

            while (true) {
                
                String moveP1 = in1.readUTF(); 
                out2.writeUTF(moveP1);        

                String moveP2 = in2.readUTF();
                out1.writeUTF(moveP2);         
            }
        } catch (IOException e) {
            System.out.println("Phòng " + roomId + ": Người chơi đã thoát.");
        } finally {
            GameServer.rooms.remove(roomId);
            try { p1.close(); } catch (IOException ignored) {}
            try { p2.close(); } catch (IOException ignored) {}
        }
    }
}
