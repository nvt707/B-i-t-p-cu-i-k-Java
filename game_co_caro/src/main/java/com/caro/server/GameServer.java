package com.caro.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
    
    public static HashMap<Integer, Socket[]> rooms = new HashMap<>();
    public static int roomIdCounter = 1;

    public static List<DataOutputStream> lobbyClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(8888);
        System.out.println("Server khởi động tại port 8888. Hỗ trợ hệ thống Sảnh Chờ!");

        while (true) {
            Socket client = server.accept();
            System.out.println("Có người vào sảnh chờ!");
            new LobbyHandler(client).start();
        }
    }

    public static void broadcastRooms() {
        StringBuilder sb = new StringBuilder("ROOMS_LIST");
        for (Map.Entry<Integer, Socket[]> entry : rooms.entrySet()) {
            Socket[] p = entry.getValue();
            if (p[0] != null && !p[0].isClosed() && p[1] == null) {
                sb.append(",Phòng ").append(entry.getKey()).append(" (1/2)");
            }
        }

        String message = sb.toString();
        for (DataOutputStream out : lobbyClients) {
            try {
                out.writeUTF(message);
            } catch (IOException ignored) {}
        }
    }

    static class LobbyHandler extends Thread {
        private Socket socket;
        public LobbyHandler(Socket socket) { this.socket = socket; }

        public void run() {
            DataOutputStream out = null;
            try {
                DataInputStream in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                lobbyClients.add(out);

                while (true) {
                    String msg = in.readUTF();

                    if (msg.equals("GET_ROOMS")) {
                        broadcastRooms(); 

                    } else if (msg.equals("CREATE_ROOM")) {
                        int id = roomIdCounter++;
                        Socket[] players = { socket, null }; 
                        rooms.put(id, players);
                        out.writeUTF("ROOM_CREATED," + id);

                        lobbyClients.remove(out);

                        broadcastRooms();
                        break;

                    } else if (msg.startsWith("JOIN_ROOM")) {
                        String roomIdStr = msg.split(",")[1].trim();
                        roomIdStr = roomIdStr.replaceAll("[^0-9]", ""); 
                        int id = Integer.parseInt(roomIdStr);

                        Socket[] players = rooms.get(id);

                        if (players != null && players[1] == null) {
                            players[1] = socket;

                            lobbyClients.remove(out); 

                            rooms.remove(id);

                            broadcastRooms();

                            new GameSession(players[0], players[1], id).start();
                            break;
                        } else {
                            out.writeUTF("JOIN_FAILED");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Một người chơi đã thoát khỏi sảnh.");
                cleanupDeadRooms(); 
            } finally {
                if (out != null) lobbyClients.remove(out);
                broadcastRooms();
            }
        }

        private void cleanupDeadRooms() {
            Iterator<Map.Entry<Integer, Socket[]>> it = rooms.entrySet().iterator();
            boolean hasDeadRoom = false;
            while (it.hasNext()) {
                Map.Entry<Integer, Socket[]> entry = it.next();
                Socket[] p = entry.getValue();
                if (p[0] == null || p[0].isClosed() || p[0] == socket) {
                    it.remove();
                    hasDeadRoom = true;
                }
            }
            if (hasDeadRoom) broadcastRooms();
        }
    }
}
