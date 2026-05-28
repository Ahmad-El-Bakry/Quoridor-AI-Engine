package com.quoridor.backend.model;

public class Player {
    private int id;
    private Coordinate position;
    private int goalRow;
    private int wallsRemaining;

    public Player(int id, Coordinate startPosition, int goalRow) {
        this.id = id;
        this.position = startPosition;
        this.goalRow = goalRow;
        this.wallsRemaining = 10; // Every player starts with 10 walls
    }

    // --- GETTERS ---
    public int getId() { 
        return id; 
    }
    
    public Coordinate getPosition() { 
        return position; 
    }
    
    public int getGoalRow() { 
        return goalRow; 
    }
    
    public int getWallsRemaining() { 
        return wallsRemaining; 
    }

    // --- SETTERS ---
    public void setPosition(Coordinate position) { 
        this.position = position; 
    }
    
    public void setWallsRemaining(int wallsRemaining) { 
        this.wallsRemaining = wallsRemaining; 
    }

    // --- HELPERS ---
    // Restored method for Board.java to use during normal gameplay
    public void useWall() {
        this.wallsRemaining--;
    }

    public boolean hasWon() {
        return position.getY() == goalRow;
    }
}