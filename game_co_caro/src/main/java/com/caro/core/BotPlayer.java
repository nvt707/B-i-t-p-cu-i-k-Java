package com.caro.core;

public class BotPlayer {

   
    private int[] attackScores = {0, 3, 24, 243, 2197, 19773};
    private int[] defendScores = {0, 1, 9,  81,  729,  6561};

    public int[] getMove(Board board, int size) {
        int bestX = -1;
        int bestY = -1;
        long maxScore = -1;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board.getCell(i, j) == '-') { 
                    long score = evaluateMove(board, size, i, j);

                    if (score > maxScore) {
                        maxScore = score;
                        bestX = i;
                        bestY = j;
                    }
                }
            }
        }

        if (maxScore == 0) {
            bestX = size / 2;
            bestY = size / 2;
        }

        board.move(bestX, bestY, 'O');
        return new int[]{bestX, bestY};
    }

    private long evaluateMove(Board board, int size, int row, int col) {
        long totalScore = 0;

        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

        for (int[] dir : directions) {
            int dx = dir[0];
            int dy = dir[1];

            int attackCount = countConsecutivePieces(board, size, row, col, dx, dy, 'O');
            int defendCount = countConsecutivePieces(board, size, row, col, dx, dy, 'X');

            attackCount = Math.min(attackCount, 5);
            defendCount = Math.min(defendCount, 5);

            totalScore += attackScores[attackCount];
            totalScore += defendScores[defendCount];
        }
        return totalScore;
    }

    private int countConsecutivePieces(Board board, int size, int row, int col, int dx, int dy, char symbol) {
        int count = 0;

        for (int i = 1; i <= 4; i++) {
            int r = row + i * dx;
            int c = col + i * dy;
            if (r < 0 || r >= size || c < 0 || c >= size) break; 
            if (board.getCell(r, c) == symbol) count++;
            else break;
        }

        for (int i = 1; i <= 4; i++) {
            int r = row - i * dx;
            int c = col - i * dy;
            if (r < 0 || r >= size || c < 0 || c >= size) break;
            if (board.getCell(r, c) == symbol) count++;
            else break; 
        }

        return count;
    }
}
