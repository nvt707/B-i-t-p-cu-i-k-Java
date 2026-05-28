package com.caro.client;

import com.caro.core.Board;
import com.caro.core.BotPlayer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class GameClient {
    
    private JFrame gameFrame;
    private JButton[][] buttons;
    private JLabel statusLabel;

    private JFrame lobbyFrame;
    private DefaultListModel<String> roomListModel;

    private int size;
    private boolean isOnline;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String mySymbol = "X";
    private boolean myTurn = true;
    private boolean isGameOver = false;

    private Board board;
    private BotPlayer bot;

    public GameClient() {
        String[] modes = {"Chơi với Máy (AI)", "Chơi Online (Người vs Người)"};
        int modeChoice = JOptionPane.showOptionDialog(null, "Chọn chế độ chơi:", "Menu Game",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, modes, modes[0]);
        if (modeChoice == -1) System.exit(0);
        isOnline = (modeChoice == 1);

        String[] sizes = {"3x3", "5x5", "10x10", "15x15", "20x20"};
        int sizeChoice = JOptionPane.showOptionDialog(null, "Chọn độ khó (kích thước):", "Menu Game",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, sizes, sizes[0]);
        if (sizeChoice == -1) System.exit(0);

        int[] actualSizes = {3, 5, 10, 15, 20};
        size = actualSizes[sizeChoice];

        if (isOnline) {
            connectAndShowLobby();
        } else {
            initBoardUI();
            board = new Board(size);
            bot = new BotPlayer();
            statusLabel.setText("Bạn là X. Tới lượt bạn!");
        }
    }

    
    private void connectAndShowLobby() {
        try {
            String serverIp = JOptionPane.showInputDialog(null,
                    "Nhập IP của Server (để nguyên 'localhost' nếu Server chạy trên máy này):",
                    "localhost");
            if (serverIp == null || serverIp.trim().isEmpty()) System.exit(0);

            socket = new Socket(serverIp, 8888);
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            lobbyFrame = new JFrame("Sảnh Chờ Caro Online");
            lobbyFrame.setSize(350, 400);
            lobbyFrame.setLayout(new BorderLayout());
            lobbyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            roomListModel = new DefaultListModel<>();
            JList<String> roomList = new JList<>(roomListModel);
            roomList.setFont(new Font("Arial", Font.BOLD, 14));
            lobbyFrame.add(new JScrollPane(roomList), BorderLayout.CENTER);
            JPanel btnPanel = new JPanel();
            JButton btnRefresh = new JButton("Làm mới");
            JButton btnCreate  = new JButton("Tạo phòng");
            JButton btnJoin    = new JButton("Vào phòng");

            btnRefresh.addActionListener(e -> sendMsg("GET_ROOMS"));
            btnCreate.addActionListener(e -> sendMsg("CREATE_ROOM"));
            btnJoin.addActionListener(e -> {
                String selected = roomList.getSelectedValue();
                if (selected != null) {
                    String roomId = selected.split(" ")[1];
                    sendMsg("JOIN_ROOM," + roomId);
                } else {
                    JOptionPane.showMessageDialog(lobbyFrame, "Vui lòng chọn 1 phòng!");
                }
            });

            btnPanel.add(btnRefresh);
            btnPanel.add(btnCreate);
            btnPanel.add(btnJoin);
            lobbyFrame.add(btnPanel, BorderLayout.SOUTH);
            lobbyFrame.setLocationRelativeTo(null);
            lobbyFrame.setVisible(true);

            new Thread(this::listenToServer).start();
            sendMsg("GET_ROOMS");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Không thể kết nối đến Server! Vui lòng kiểm tra lại IP.");
            System.exit(0);
        }
    }

    private void sendMsg(String msg) {
        try { out.writeUTF(msg); } catch (IOException ignored) {}
    }

    private void listenToServer() {
        try {
            while (true) {
                String msg = in.readUTF();
                SwingUtilities.invokeLater(() -> processServerMessage(msg));
            }
        } catch (IOException e) {
            if (!isGameOver && gameFrame != null)
                SwingUtilities.invokeLater(() -> statusLabel.setText("Mất kết nối Server!"));
        }
    }

    private void processServerMessage(String msg) {
        System.out.println("Nhận từ Server: " + msg);

        String[] parts = msg.split(",");
        if (parts.length < 1) return;

        String command = parts[0].trim();

        if (command.equals("ROOMS_LIST")) {
            SwingUtilities.invokeLater(() -> {
                roomListModel.clear();
                for (int i = 1; i < parts.length; i++) {
                    String roomInfo = parts[i].trim();
                    if (!roomInfo.isEmpty() && !roomInfo.equalsIgnoreCase("ROOMS_LIST")) {
                        roomListModel.addElement(roomInfo);
                    }
                }
            });
        }
            
        else if (command.equals("ROOM_CREATED")) {
            SwingUtilities.invokeLater(() -> {
                roomListModel.clear();
                roomListModel.addElement("Đã tạo phòng " + parts[1].trim() + ". Đang chờ đối thủ...");
            });
        }
            
        else if (command.equals("START")) {
            mySymbol = parts[1].trim();
            myTurn = mySymbol.equals("X");

            SwingUtilities.invokeLater(() -> {
                if (lobbyFrame != null) lobbyFrame.setVisible(false);
                initBoardUI();
                statusLabel.setText(myTurn ? "Trận đấu bắt đầu! Lượt của bạn." : "Chờ đối thủ đánh...");
            });
        }
            
        else if (command.equals("MOVE")) {
            try {
                int r = Integer.parseInt(parts[parts.length - 2].trim());
                int c = Integer.parseInt(parts[parts.length - 1].trim());

                String opponentSymbol = mySymbol.equals("X") ? "O" : "X";

                SwingUtilities.invokeLater(() -> {
                    buttons[r][c].setText(opponentSymbol);
                    buttons[r][c].setForeground(opponentSymbol.equals("X") ? Color.RED : Color.BLUE);

                    if (!checkGameState(opponentSymbol)) {
                        myTurn = true;
                        statusLabel.setText("Đến lượt bạn (" + mySymbol + ")!");
                    }
                });
            } catch (Exception e) {
                System.err.println("Lỗi phân tích tọa độ: " + e.getMessage());
            }
        }
    }

    
    private void initBoardUI() {
        gameFrame = new JFrame("Cờ Caro " + (isOnline ? "Online" : "vs AI") + " - " + size + "x" + size);

        int cellSize = 80, fontSize = 40;
        if (size == 10) { cellSize = 50; fontSize = 24; }
        else if (size == 15) { cellSize = 40; fontSize = 18; }
        else if (size == 20) { cellSize = 35; fontSize = 14; }

        gameFrame.setSize(size * cellSize, size * cellSize + 60);
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setLayout(new BorderLayout());

        statusLabel = new JLabel("Khởi tạo...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gameFrame.add(statusLabel, BorderLayout.NORTH);

        JPanel boardPanel = new JPanel(new GridLayout(size, size));
        buttons = new JButton[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, fontSize));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setMargin(new Insets(0, 0, 0, 0));
                final int row = i, col = j;
                buttons[i][j].addActionListener(e -> handleMove(row, col));
                boardPanel.add(buttons[i][j]);
            }
        }
        gameFrame.add(boardPanel, BorderLayout.CENTER);
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setVisible(true);
    }

    private void handleMove(int row, int col) {
        if (!myTurn || isGameOver || !buttons[row][col].getText().equals("")) return;

        buttons[row][col].setText(mySymbol);
        buttons[row][col].setForeground(mySymbol.equals("X") ? Color.RED : Color.BLUE);
        myTurn = false;

        if (isOnline) {
            
            sendMsg("MOVE," + row + "," + col);
            statusLabel.setText("Đang chờ đối thủ...");
            checkGameState(mySymbol);
        } else {
            
            if (checkGameState(mySymbol)) return;
            board.move(row, col, 'X');
            statusLabel.setText("Máy đang suy nghĩ...");

            new Thread(() -> {
                try { Thread.sleep(600); } catch (Exception ignored) {}
                int[] botMove = bot.getMove(board, size);
                SwingUtilities.invokeLater(() -> {
                    buttons[botMove[0]][botMove[1]].setText("O");
                    buttons[botMove[0]][botMove[1]].setForeground(Color.BLUE);
                    if (!checkGameState("O")) {
                        myTurn = true;
                        statusLabel.setText("Tới lượt bạn!");
                    }
                });
            }).start();
        }
    }

    private boolean checkGameState(String symbol) {
        if (checkWin(symbol)) { endGame(symbol + " ĐÃ CHIẾN THẮNG!"); return true; }
        if (isBoardFull())    { endGame("VÁN CỜ HÒA!");               return true; }
        return false;
    }

   
    private boolean checkWin(String symbol) {
        int winReq = (size == 3) ? 3 : 5;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (!buttons[i][j].getText().equals(symbol)) continue;
                if (j <= size - winReq && checkDir(i, j, 0,  1, symbol, winReq)) return true;
                if (i <= size - winReq && checkDir(i, j, 1,  0, symbol, winReq)) return true;
                if (i <= size - winReq && j <= size - winReq && checkDir(i, j, 1,  1, symbol, winReq)) return true;
                if (i <= size - winReq && j >= winReq - 1   && checkDir(i, j, 1, -1, symbol, winReq)) return true;
            }
        }
        return false;
    }

    private boolean checkDir(int r, int c, int dr, int dc, String symbol, int winReq) {
        for (int k = 0; k < winReq; k++)
            if (!buttons[r + k*dr][c + k*dc].getText().equals(symbol)) return false;
        return true;
    }

    private boolean isBoardFull() {
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if (buttons[i][j].getText().equals("")) return false;
        return true;
    }

    
    private void endGame(String message) {
        isGameOver = true;
        myTurn = false;
        statusLabel.setText(message);
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                buttons[i][j].setEnabled(false);

        int choice = JOptionPane.showConfirmDialog(gameFrame,
                message + "\nBạn có muốn chơi ván mới không?",
                "Kết thúc Game", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            if (gameFrame  != null) gameFrame.dispose();
            if (lobbyFrame != null) lobbyFrame.dispose();
            if (socket != null && !socket.isClosed())
                try { socket.close(); } catch (IOException ignored) {}
            new GameClient();
        } else {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        new GameClient();
    }
}
