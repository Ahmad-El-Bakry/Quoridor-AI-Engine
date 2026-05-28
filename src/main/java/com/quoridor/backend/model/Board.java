package com.quoridor.backend.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Board {

    private Player player1;
    private Player player2;
    private List<Wall> activeWalls;
    private int currentPlayerId;

    public Board() {
        // Standard Quoridor Setup: 9x9 grid (Coordinates 0-8)
        // Player 1 starts at top (4,0) and needs to reach bottom row (8)
        this.player1 = new Player(1, new Coordinate(4, 0), 8);
        
        // Player 2 starts at bottom (4,8) and needs to reach top row (0)
        this.player2 = new Player(2, new Coordinate(4, 8), 0);
        
        this.activeWalls = new ArrayList<>();
        this.currentPlayerId = 1; // Player 1 always starts
    }

    // --- GETTERS & SETTERS ---
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public List<Wall> getActiveWalls() { return activeWalls; }
    public int getCurrentPlayerId() { return currentPlayerId; }
    
    public Player getCurrentPlayer() {
        return (currentPlayerId == 1) ? player1 : player2;
    }

    // --- CORE GAME ACTIONS ---
    public boolean movePlayer(int playerId, Coordinate target) {
        if (playerId != currentPlayerId) return false;
        Player player = (playerId == 1) ? player1 : player2;

        if (isValidPawnMove(player, target)) {
            player.setPosition(target);
            switchTurn();
            return true;
        }
        return false;
    }

    public boolean placeWall(int playerId, Wall wall) {
        if (playerId != currentPlayerId) return false;
        Player player = (playerId == 1) ? player1 : player2;

        if (player.getWallsRemaining() > 0 && isValidWallPlacement(player, wall)) {
            activeWalls.add(wall);
            player.setWallsRemaining(player.getWallsRemaining() - 1);
            switchTurn();
            return true;
        }
        return false;
    }

    private void switchTurn() {
        currentPlayerId = (currentPlayerId == 1) ? 2 : 1;
    }

    // --- PAWN MOVEMENT PHYSICS ---
    public boolean isValidPawnMove(Player player, Coordinate target) {
        int px = player.getPosition().getX();
        int py = player.getPosition().getY();
        int tx = target.getX();
        int ty = target.getY();

        // Prevent moving off the board
        if (tx < 0 || tx > 8 || ty < 0 || ty > 8) return false;

        Player opponent = (player.getId() == 1) ? player2 : player1;
        int ox = opponent.getPosition().getX();
        int oy = opponent.getPosition().getY();

        // Rule 1: Cannot step directly on the opponent
        if (tx == ox && ty == oy) return false;

        int dx = tx - px;
        int dy = ty - py;

        // Rule 2: Normal Orthogonal Move (1 step)
        if (Math.abs(dx) + Math.abs(dy) == 1) {
            return !hasWallBetween(px, py, tx, ty);
        }

        // Rule 3: Straight Jump Over Opponent (2 steps)
        if ((Math.abs(dx) == 2 && dy == 0) || (Math.abs(dy) == 2 && dx == 0)) {
            int midX = px + (dx / 2);
            int midY = py + (dy / 2);
            
            // The opponent MUST be in the middle square to allow a straight jump
            if (midX == ox && midY == oy) {
                return !hasWallBetween(px, py, midX, midY) && !hasWallBetween(midX, midY, tx, ty);
            }
            return false;
        }

        // Rule 4: The Strict Diagonal Jump (1 step X, 1 step Y)
        if (Math.abs(dx) == 1 && Math.abs(dy) == 1) {
            
            // Scenario A: Opponent is horizontally adjacent
            if (ox == px + dx && oy == py) {
                // Is the straight path behind the opponent blocked by a wall or the edge of the board?
                boolean blockedStraight = (ox + dx < 0 || ox + dx > 8) || hasWallBetween(ox, oy, ox + dx, oy);
                
                if (blockedStraight) {
                    // Make sure no walls block the diagonal slip
                    return !hasWallBetween(px, py, ox, oy) && !hasWallBetween(ox, oy, tx, ty);
                }
            }
            
            // Scenario B: Opponent is vertically adjacent
            if (ox == px && oy == py + dy) {
                // Is the straight path behind the opponent blocked by a wall or the edge of the board?
                boolean blockedStraight = (oy + dy < 0 || oy + dy > 8) || hasWallBetween(ox, oy, ox, oy + dy);
                
                if (blockedStraight) {
                    // Make sure no walls block the diagonal slip
                    return !hasWallBetween(px, py, ox, oy) && !hasWallBetween(ox, oy, tx, ty);
                }
            }
        }

        return false;
    }

    // --- WALL PLACEMENT PHYSICS ---
    public boolean isValidWallPlacement(Player player, Wall wall) {
        int wx = wall.getAnchor().getX();
        int wy = wall.getAnchor().getY();

        // 1. Bounds Check (Wall anchors are on an 8x8 grid between the 9x9 spaces)
        if (wx < 0 || wx > 7 || wy < 0 || wy > 7) return false;

        // 2. Collision Check (Does it overlap another wall?)
        for (Wall w : activeWalls) {
            if (w.getOrientation() == wall.getOrientation()) {
                if (wall.getOrientation() == Wall.Orientation.HORIZONTAL) {
                    if (w.getAnchor().getY() == wy && Math.abs(w.getAnchor().getX() - wx) <= 1) return false;
                } else {
                    if (w.getAnchor().getX() == wx && Math.abs(w.getAnchor().getY() - wy) <= 1) return false;
                }
            } else {
                // Crossing walls
                if (w.getAnchor().getX() == wx && w.getAnchor().getY() == wy) return false;
            }
        }

        // 3. The "No Trapping" Rule (A wall cannot completely block a player's path to the goal)
        // We temporarily add the wall, test both players, and then remove it.
        activeWalls.add(wall);
        boolean p1CanWin = canReachGoal(player1);
        boolean p2CanWin = canReachGoal(player2);
        activeWalls.remove(activeWalls.size() - 1);

        return p1CanWin && p2CanWin;
    }

    // --- UTILITY: WALL COLLISION CHECKER ---
    private boolean hasWallBetween(int x1, int y1, int x2, int y2) {
        // Moving Vertically
        if (x1 == x2) {
            int minY = Math.min(y1, y2);
            for (Wall w : activeWalls) { 
                if (w.getOrientation() == Wall.Orientation.HORIZONTAL) {
                    if (w.getAnchor().getY() == minY && (w.getAnchor().getX() == x1 || w.getAnchor().getX() == x1 - 1)) {
                        return true;
                    }
                }
            }
        }
        // Moving Horizontally
        else if (y1 == y2) {
            int minX = Math.min(x1, x2);
            for (Wall w : activeWalls) {
                if (w.getOrientation() == Wall.Orientation.VERTICAL) {
                    if (w.getAnchor().getX() == minX && (w.getAnchor().getY() == y1 || w.getAnchor().getY() == y1 - 1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // --- UTILITY: PATHFINDING (BFS) ---
    // Used exclusively to guarantee that placing a wall doesn't break the game
    private boolean canReachGoal(Player player) {
        boolean[][] visited = new boolean[9][9];
        Queue<Coordinate> queue = new LinkedList<>();
        
        Coordinate start = player.getPosition();
        queue.add(start);
        visited[start.getX()][start.getY()] = true;

        // Orthogonal directions only for basic pathfinding checks
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            Coordinate curr = queue.poll();
            
            // Did we reach the target row?
            if (curr.getY() == player.getGoalRow()) return true;

            for (int[] d : dirs) {
                int nx = curr.getX() + d[0];
                int ny = curr.getY() + d[1];
                
                // Bounds check
                if (nx >= 0 && nx <= 8 && ny >= 0 && ny <= 8) {
                    // Check if a wall blocks this specific step, and if we haven't visited it yet
                    if (!visited[nx][ny] && !hasWallBetween(curr.getX(), curr.getY(), nx, ny)) {
                        visited[nx][ny] = true;
                        queue.add(new Coordinate(nx, ny));
                    }
                }
            }
        }
        return false; // Completely trapped!
    }
}