package com.quoridor.backend.service;

import com.quoridor.backend.model.Board;
import com.quoridor.backend.model.Coordinate;
import com.quoridor.backend.model.Player;
import com.quoridor.backend.model.Wall;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

@Service
public class AiService {

    private final Random random = new Random();

    public void takeTurn(Board board, int difficultyLevel) {
        if (difficultyLevel == 1) makeEasyMove(board);
        else if (difficultyLevel == 2) makeMediumMove(board);
        else if (difficultyLevel == 3) makeHardMove(board);
        else makeOracleMove(board); 
    }

    // --- LEVEL 1: EASY (Stays dumb and random) ---
    private void makeEasyMove(Board board) {
        Player ai = board.getCurrentPlayerId() == 1 ? board.getPlayer1() : board.getPlayer2();
        if (ai.getWallsRemaining() > 0 && random.nextInt(4) == 0) {
            if (placeRandomWall(board, ai)) return; 
        }
        makeRandomMove(board, ai);
    }

    // --- LEVEL 2: MEDIUM (The Basic Sprinter) ---
    private void makeMediumMove(Board board) {
        Player ai = board.getCurrentPlayerId() == 1 ? board.getPlayer1() : board.getPlayer2();
        Player human = board.getCurrentPlayerId() == 1 ? board.getPlayer2() : board.getPlayer1();

        // THE OVERRIDE: If completely out of walls, just sprint!
        if (ai.getWallsRemaining() <= 0) {
            executeSprintMove(board, ai);
            return;
        }

        if (random.nextInt(4) == 0) {
            if (placeTargetedWall(board, ai, human)) return;
        }
        
        executeSprintMove(board, ai);
    }

    // --- LEVEL 3: HARD (Greedy Tactician) ---
    private void makeHardMove(Board board) {
        Player ai = board.getCurrentPlayerId() == 1 ? board.getPlayer1() : board.getPlayer2();
        Player human = board.getCurrentPlayerId() == 1 ? board.getPlayer2() : board.getPlayer1();

        // THE OVERRIDE: If out of walls, bypass all heavy math and sprint instantly. (No panic, fast performance)
        if (ai.getWallsRemaining() <= 0) {
            executeSprintMove(board, ai);
            return;
        }

        int currentAiPath = getShortestPathLength(board, ai);
        int currentHumanPath = getShortestPathLength(board, human);

        double bestScore = Double.NEGATIVE_INFINITY;
        Coordinate bestMove = null;
        Wall bestWall = null;

        // 1. Evaluate Pawn Moves
        for (Coordinate move : getValidPawnMoves(board, ai)) {
            Coordinate oldPos = ai.getPosition();
            ai.setPosition(move); 
            int aiPath = getShortestPathLength(board, ai);
            int humanPath = getShortestPathLength(board, human);
            ai.setPosition(oldPos); 
            
            if (aiPath == -1 || humanPath == -1) continue;
            double score = (humanPath - aiPath) * 10.0; 
            
            if (score > bestScore) {
                bestScore = score; bestMove = move; bestWall = null;
            }
        }

        // 2. Evaluate Walls
        for (Wall testWall : getSmartWalls(board, ai, human)) {
            board.getActiveWalls().add(testWall); 
            int humanPath = getShortestPathLength(board, human);
            int aiPath = getShortestPathLength(board, ai);
            board.getActiveWalls().remove(board.getActiveWalls().size() - 1); 

            if (aiPath == -1 || humanPath == -1) continue;
            
            // Wall Penalty: AI loses points for wasting a wall if it doesn't significantly help
            double score = ((humanPath - aiPath) * 10.0) 
                         + ((humanPath - currentHumanPath) * (currentHumanPath <= 4 ? 60.0 : 20.0)) 
                         - ((aiPath - currentAiPath) * 15.0) 
                         - 5.0; // Flat penalty for consuming a wall

            if (score > bestScore) {
                bestScore = score; bestWall = testWall; bestMove = null;
            }
        }

        if (bestWall != null) board.placeWall(ai.getId(), bestWall);
        else if (bestMove != null) board.movePlayer(ai.getId(), bestMove);
        else executeSprintMove(board, ai); 
    }

    // --- LEVEL 4: THE ORACLE (Lightweight Monte Carlo) ---
    private void makeOracleMove(Board board) {
        Player ai = board.getCurrentPlayerId() == 1 ? board.getPlayer1() : board.getPlayer2();
        Player human = board.getCurrentPlayerId() == 1 ? board.getPlayer2() : board.getPlayer1();

        // THE OVERRIDE: Zero Panic & Ultimate Speed. If out of walls, skip all 500 simulations and sprint.
        if (ai.getWallsRemaining() <= 0) {
            executeSprintMove(board, ai);
            return;
        }

        // Optimized settings for smooth browser performance
        int simulationsPerMove = 20; 
        int rolloutDepth = 6; 

        double bestScore = Double.NEGATIVE_INFINITY;
        Coordinate bestMove = null;
        Wall bestWall = null;

        Coordinate sprintMove = findNextStepInShortestPath(board, ai);

        // 1. Rollouts on Pawn Moves
        for (Coordinate move : getValidPawnMoves(board, ai)) {
            Coordinate oldPos = ai.getPosition();
            ai.setPosition(move);

            double score = 0;
            for (int i = 0; i < simulationsPerMove; i++) {
                score += simulateRollout(board, ai, human, rolloutDepth, false);
            }
            ai.setPosition(oldPos);

            // Tie-Breaker: Heavily favor the absolute shortest path to prevent zig-zagging
            if (sprintMove != null && move.getX() == sprintMove.getX() && move.getY() == sprintMove.getY()) {
                score += (simulationsPerMove * 5.0); 
            }

            if (score > bestScore) {
                bestScore = score; bestMove = move; bestWall = null;
            }
        }

        // 2. Rollouts on Smart Walls
        for (Wall testWall : getSmartWalls(board, ai, human)) {
            board.getActiveWalls().add(testWall);
            ai.setWallsRemaining(ai.getWallsRemaining() - 1);

            if (getShortestPathLength(board, ai) != -1 && getShortestPathLength(board, human) != -1) {
                double score = 0;
                for (int i = 0; i < simulationsPerMove; i++) {
                    score += simulateRollout(board, ai, human, rolloutDepth, false);
                }
                // Minor penalty for wall usage to hoard them naturally
                score -= (simulationsPerMove * 2.0);

                if (score > bestScore) {
                    bestScore = score; bestWall = testWall; bestMove = null;
                }
            }

            board.getActiveWalls().remove(board.getActiveWalls().size() - 1);
            ai.setWallsRemaining(ai.getWallsRemaining() + 1);
        }

        if (bestWall != null) board.placeWall(ai.getId(), bestWall);
        else if (bestMove != null) board.movePlayer(ai.getId(), bestMove);
        else executeSprintMove(board, ai); 
    }

    private double simulateRollout(Board board, Player ai, Player human, int depth, boolean isHumanTurn) {
        int aiPath = getShortestPathLength(board, ai);
        int humanPath = getShortestPathLength(board, human);

        if (aiPath == 0) return 1000.0 + (depth * 10.0); 
        if (humanPath == 0) return -1000.0 - (depth * 10.0) - (aiPath * 5.0); 
        if (depth == 0) return ((humanPath - aiPath) * 10.0); 

        Player active = isHumanTurn ? human : ai;
        List<Coordinate> moves = getValidPawnMoves(board, active);
        if (moves.isEmpty()) return isHumanTurn ? 1000.0 : -1000.0;

        Coordinate chosenMove;
        if (random.nextInt(10) < 7) {
            Coordinate bestStep = findNextStepInShortestPath(board, active);
            chosenMove = bestStep != null ? bestStep : moves.get(random.nextInt(moves.size()));
        } else {
            chosenMove = moves.get(random.nextInt(moves.size()));
        }

        Coordinate oldPos = active.getPosition();
        active.setPosition(chosenMove);

        double result = simulateRollout(board, ai, human, depth - 1, !isHumanTurn);

        active.setPosition(oldPos); 
        return result;
    }

    // --- OPTIMIZATION ENGINE ---
    private List<Wall> getSmartWalls(Board board, Player actor, Player opponent) {
        List<Wall> smartWalls = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                boolean nearAi = Math.abs(x - actor.getPosition().getX()) <= 2 && Math.abs(y - actor.getPosition().getY()) <= 2;
                boolean nearHuman = Math.abs(x - opponent.getPosition().getX()) <= 2 && Math.abs(y - opponent.getPosition().getY()) <= 2;
                if (nearAi || nearHuman) {
                    for (Wall.Orientation o : Wall.Orientation.values()) {
                        Wall w = new Wall(new Coordinate(x, y), o);
                        if (board.isValidWallPlacement(actor, w)) smartWalls.add(w);
                    }
                }
            }
        }
        return smartWalls;
    }

    // --- AI UTILITIES & PATHFINDING (BFS) ---

    // THE FIX: Perfect Vision! Scans a full 5x5 grid so the AI sees all straight and diagonal jumps.
    private List<Coordinate> getValidPawnMoves(Board board, Player player) {
        List<Coordinate> valid = new ArrayList<>();
        Coordinate curr = player.getPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx == 0 && dy == 0) continue; 
                Coordinate target = new Coordinate(curr.getX() + dx, curr.getY() + dy);
                if (board.isValidPawnMove(player, target)) {
                    valid.add(target);
                }
            }
        }
        return valid;
    }

    private void executeSprintMove(Board board, Player ai) {
        Coordinate bestMove = findNextStepInShortestPath(board, ai);
        if (bestMove != null) {
            board.movePlayer(ai.getId(), bestMove);
        } else {
            makeRandomMove(board, ai);
        }
    }

    private boolean placeRandomWall(Board board, Player ai) {
        for (int i=0; i<20; i++) {
            Wall w = new Wall(new Coordinate(random.nextInt(8), random.nextInt(8)), random.nextBoolean() ? Wall.Orientation.HORIZONTAL : Wall.Orientation.VERTICAL);
            if (board.isValidWallPlacement(ai, w)) { board.placeWall(ai.getId(), w); return true; }
        }
        return false;
    }

    private boolean placeTargetedWall(Board board, Player ai, Player human) {
        Coordinate next = findNextStepInShortestPath(board, human);
        if (next == null) return false;
        Coordinate curr = human.getPosition();
        int minX = Math.min(curr.getX(), next.getX()); int minY = Math.min(curr.getY(), next.getY());
        Wall.Orientation o = (curr.getX() == next.getX()) ? Wall.Orientation.HORIZONTAL : Wall.Orientation.VERTICAL;
        int[][] anchors = (o == Wall.Orientation.HORIZONTAL) ? new int[][]{{minX, minY}, {minX - 1, minY}} : new int[][]{{minX, minY}, {minX, minY - 1}};
        for (int[] a : anchors) {
            if (a[0]>=0 && a[0]<=7 && a[1]>=0 && a[1]<=7) {
                Wall w = new Wall(new Coordinate(a[0], a[1]), o);
                if (board.isValidWallPlacement(ai, w)) { board.placeWall(ai.getId(), w); return true; }
            }
        }
        return placeRandomWall(board, ai);
    }

    private void makeRandomMove(Board board, Player ai) {
        List<Coordinate> moves = getValidPawnMoves(board, ai);
        if (!moves.isEmpty()) board.movePlayer(ai.getId(), moves.get(random.nextInt(moves.size())));
    }

    private int getShortestPathLength(Board board, Player player) {
        boolean[][] visited = new boolean[9][9];
        Queue<int[]> queue = new LinkedList<>(); 
        Coordinate start = player.getPosition();
        queue.add(new int[]{start.getX(), start.getY(), 0}); 
        visited[start.getX()][start.getY()] = true;
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            if (curr[1] == player.getGoalRow()) return curr[2];
            for (Coordinate next : getValidPawnMoves(board, new Player(player.getId(), new Coordinate(curr[0], curr[1]), player.getGoalRow()))) {
                if (!visited[next.getX()][next.getY()]) {
                    visited[next.getX()][next.getY()] = true;
                    queue.add(new int[]{next.getX(), next.getY(), curr[2] + 1});
                }
            }
        }
        return -1; 
    }

    private Coordinate findNextStepInShortestPath(Board board, Player player) {
        boolean[][] visited = new boolean[9][9];
        Coordinate[][] parent = new Coordinate[9][9]; 
        Queue<Coordinate> queue = new LinkedList<>();
        Coordinate start = player.getPosition();
        queue.add(start);
        visited[start.getX()][start.getY()] = true;
        Coordinate goal = null;
        while (!queue.isEmpty()) {
            Coordinate curr = queue.poll();
            if (curr.getY() == player.getGoalRow()) { goal = curr; break; }
            for (Coordinate next : getValidPawnMoves(board, new Player(player.getId(), curr, player.getGoalRow()))) {
                if (!visited[next.getX()][next.getY()]) {
                    visited[next.getX()][next.getY()] = true;
                    parent[next.getX()][next.getY()] = curr;
                    queue.add(next);
                }
            }
        }
        if (goal != null) {
            Coordinate step = goal;
            while (parent[step.getX()][step.getY()] != null && !parent[step.getX()][step.getY()].equals(start)) step = parent[step.getX()][step.getY()];
            return step;
        }
        return null;
    }
}