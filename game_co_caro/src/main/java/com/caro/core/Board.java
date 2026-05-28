package com.caro.core;

public class Board {
    private int size;
    private char[][] grid;

    public Board(int size) { 
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

    public boolean checkWin(char symbol) {
        return false;
    }

    public char getCell(int x, int y) {
        return grid[x][y];
    }
    
    public void undoMove(int x, int y) {
        grid[x][y] = '-';
    }
}
