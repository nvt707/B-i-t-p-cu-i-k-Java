package com.caro.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
    // Lưu trữ danh sách phòng: ID Phòng -> Mảng 2 Socket
    public static HashMap<Integer, Socket[]> rooms = new HashMap<>();
    public static int roomIdCounter = 1;

    // THÊM 1: Lưu danh sách người đang đứng ở Sảnh chờ để Auto-Broadcast
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

    // THÊM 2: Hàm gửi danh sách phòng mới nhất cho TẤT CẢ mọi người trong sảnh
    public static void broadcastRooms() {
        StringBuilder sb = new StringBuilder("ROOMS_LIST");
        for (Map.Entry<Integer, Socket[]> entry : rooms.entrySet()) {
            Socket[] p = entry.getValue();
            // Chỉ hiển thị các phòng chưa đủ 2 người và chủ phòng vẫn kết nối
            if (p[0] != null && !p[0].isClosed() && p[1] == null) {
                // Định dạng tên: "Phòng 1 (1/2)" (Khớp với logic tách chuỗi của Client)
                sb.append(",Phòng ").append(entry.getKey()).append(" (1/2)");
            }
        }

        String message = sb.toString();
        // Gửi thông báo đến toàn bộ Client đang online ở Sảnh
        for (DataOutputStream out : lobbyClients) {
            try {
                out.writeUTF(message);
            } catch (IOException ignored) {}
        }
    }

    // Luồng xử lý từng người chơi ở Sảnh Chờ
    static class LobbyHandler extends Thread {
        private Socket socket;
        public LobbyHandler(Socket socket) { this.socket = socket; }

        public void run() {
            DataOutputStream out = null;
            try {
                DataInputStream in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                // Khi vào game, thêm họ vào danh sách nhận thông báo Sảnh
                lobbyClients.add(out);

                while (true) {
                    String msg = in.readUTF();

                    if (msg.equals("GET_ROOMS")) {
                        broadcastRooms(); // Gửi danh sách ngay lập tức

                    } else if (msg.equals("CREATE_ROOM")) {
                        int id = roomIdCounter++;
                        Socket[] players = { socket, null }; // Chủ phòng là P1
                        rooms.put(id, players);
                        out.writeUTF("ROOM_CREATED," + id);

                        // Chủ phòng chuyển sang trạng thái chờ, rút khỏi Sảnh
                        lobbyClients.remove(out);

                        // THÊM 3: Thông báo cho TẤT CẢ mọi người là có phòng mới
                        broadcastRooms();
                        break;

                    } else if (msg.startsWith("JOIN_ROOM")) {
                        // Tách chuỗi để lấy đúng ID phòng, đề phòng client gửi chữ dư thừa
                        String roomIdStr = msg.split(",")[1].trim();
                        roomIdStr = roomIdStr.replaceAll("[^0-9]", ""); // Xóa chữ, giữ lại số
                        int id = Integer.parseInt(roomIdStr);

                        Socket[] players = rooms.get(id);

                        if (players != null && players[1] == null) {
                            players[1] = socket;

                            lobbyClients.remove(out); // Thoát danh sách sảnh để vào Game

                            // THÊM 4: QUAN TRỌNG - Xóa phòng khỏi danh sách vì đã đủ 2/2 người
                            rooms.remove(id);

                            // Báo cho mọi người là phòng này đã hết chỗ
                            broadcastRooms();

                            // Bắt đầu luồng Game (Bạn tự có file GameSession rồi)
                            new GameSession(players[0], players[1], id).start();
                            break;
                        } else {
                            out.writeUTF("JOIN_FAILED");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Một người chơi đã thoát khỏi sảnh.");
                cleanupDeadRooms(); // Xóa phòng nếu họ vừa tạo xong tắt app luôn
            } finally {
                // Nếu thoát hẳn game thì xóa tên khỏi Sảnh và báo cho mọi người
                if (out != null) lobbyClients.remove(out);
                broadcastRooms();
            }
        }

        // THÊM 5: Tự động dọn rác (Phòng trống)
        private void cleanupDeadRooms() {
            Iterator<Map.Entry<Integer, Socket[]>> it = rooms.entrySet().iterator();
            boolean hasDeadRoom = false;
            while (it.hasNext()) {
                Map.Entry<Integer, Socket[]> entry = it.next();
                Socket[] p = entry.getValue();
                // Nếu chủ phòng bị mất kết nối hoặc là người vừa thoát
                if (p[0] == null || p[0].isClosed() || p[0] == socket) {
                    it.remove();
                    hasDeadRoom = true;
                }
            }
            // Nếu có phòng rác vừa bị xóa, báo lại cho sảnh để nó biến mất
            if (hasDeadRoom) broadcastRooms();
        }
    }
}