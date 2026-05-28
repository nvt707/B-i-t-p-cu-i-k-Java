package com.caro.core;

public class Board {
    private int size;
    private char[][] grid;

    public Board(int size) { // Truyền 3 hoặc 5
        this.size = size;
        this.grid = new char[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                grid[i][j] = '-';
    }

    public boolean move(int x, int y, char symbol) {
        if (x >= 0 && x < size && y >= 0 && y < size && grid[x][y] == '-') {
            grid[x][y] = symbol;
            return true;
        }
        return false;
    }

    // Giản lược hàm checkWin để code ngắn gọn
    public boolean checkWin(char symbol) {
        // Logic kiểm tra hàng ngang, dọc, chéo tùy theo size (3 hoặc 5)
        return false;
    }

    // Lấy giá trị của một ô
    public char getCell(int x, int y) {
        return grid[x][y];
    }
    // Hoàn tác nước đi (dành cho Bot thử nghiệm trong đầu)
    public void undoMove(int x, int y) {
        grid[x][y] = '-';
    }
}