package com.caro.core;

public class BotPlayer {

    // Bảng điểm lượng giá (Heuristic). Chuỗi càng dài điểm càng cao đột biến.
    // Điểm Tấn công (O) luôn cao hơn Phòng thủ (X) một chút để máy ưu tiên dứt điểm nếu có cơ hội.
    private int[] attackScores = {0, 3, 24, 243, 2197, 19773};
    private int[] defendScores = {0, 1, 9,  81,  729,  6561};

    public int[] getMove(Board board, int size) {
        int bestX = -1;
        int bestY = -1;
        long maxScore = -1;

        // Quét toàn bộ bàn cờ để chấm điểm từng ô trống
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board.getCell(i, j) == '-') { // Chỉ xét ô chưa ai đánh
                    long score = evaluateMove(board, size, i, j);

                    if (score > maxScore) {
                        maxScore = score;
                        bestX = i;
                        bestY = j;
                    }
                }
            }
        }

        // Nếu bàn cờ hoàn toàn trống (lượt đầu tiên), máy đánh vào giữa bàn cờ
        if (maxScore == 0) {
            bestX = size / 2;
            bestY = size / 2;
        }

        board.move(bestX, bestY, 'O');
        return new int[]{bestX, bestY};
    }

    // Hàm chấm điểm tổng hợp cho 1 ô cụ thể
    private long evaluateMove(Board board, int size, int row, int col) {
        long totalScore = 0;

        // 4 hướng: Ngang, Dọc, Chéo xuôi (\), Chéo ngược (/)
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

        for (int[] dir : directions) {
            int dx = dir[0];
            int dy = dir[1];

            // Đếm số quân liên tiếp của Máy (O) và Người (X) xung quanh ô này
            int attackCount = countConsecutivePieces(board, size, row, col, dx, dy, 'O');
            int defendCount = countConsecutivePieces(board, size, row, col, dx, dy, 'X');

            // Giới hạn index mảng (tối đa là 5) để không bị lỗi out of bounds
            attackCount = Math.min(attackCount, 5);
            defendCount = Math.min(defendCount, 5);

            // Cộng dồn điểm
            totalScore += attackScores[attackCount];
            totalScore += defendScores[defendCount];
        }
        return totalScore;
    }

    // Đếm số quân giống nhau nằm liên tiếp kề với ô đang xét
    private int countConsecutivePieces(Board board, int size, int row, int col, int dx, int dy, char symbol) {
        int count = 0;

        // Duyệt về phía trước
        for (int i = 1; i <= 4; i++) {
            int r = row + i * dx;
            int c = col + i * dy;
            if (r < 0 || r >= size || c < 0 || c >= size) break; // Ra khỏi bàn cờ
            if (board.getCell(r, c) == symbol) count++;
            else break; // Dừng lại ngay khi bị ngắt quãng (gặp ô trống hoặc địch)
        }

        // Duyệt về phía sau
        for (int i = 1; i <= 4; i++) {
            int r = row - i * dx;
            int c = col - i * dy;
            if (r < 0 || r >= size || c < 0 || c >= size) break;
            if (board.getCell(r, c) == symbol) count++;
            else break; // Dừng lại ngay khi bị ngắt quãng
        }

        return count;
    }
}