package com.quoridor.backend.controller;

import com.quoridor.backend.dto.MoveRequest;
import com.quoridor.backend.dto.WallRequest;
import com.quoridor.backend.model.Board;
import com.quoridor.backend.model.Coordinate;
import com.quoridor.backend.model.Player;
import com.quoridor.backend.model.Wall;
import com.quoridor.backend.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*") 
public class GameController {

    private Board gameBoard = new Board();
    private final AiService aiService = new AiService();
    
    private int aiDifficulty = 4; 
    private boolean isAiEnabled = true; 

    // --- EVENT SOURCING ARCHITECTURE ---
    private List<GameAction> actionHistory = new ArrayList<>();
    private List<GameAction> redoStack = new ArrayList<>();

    private static class GameAction {
        boolean isWall;
        int playerId;
        int x;
        int y;
        String orientation;

        // Constructor for Pawn Move
        public GameAction(int playerId, int x, int y) {
            this.isWall = false; this.playerId = playerId; this.x = x; this.y = y;
        }
        // Constructor for Wall Placement
        public GameAction(int playerId, int x, int y, String orientation) {
            this.isWall = true; this.playerId = playerId; this.x = x; this.y = y; this.orientation = orientation;
        }
    }

    private void rebuildBoard() {
        gameBoard = new Board();
        for (GameAction action : actionHistory) {
            if (action.isWall) {
                gameBoard.placeWall(action.playerId, new Wall(new Coordinate(action.x, action.y), Wall.Orientation.valueOf(action.orientation)));
            } else {
                gameBoard.movePlayer(action.playerId, new Coordinate(action.x, action.y));
            }
        }
    }
    // -----------------------------------

    @GetMapping("/state")
    public ResponseEntity<Board> getGameState() {
        return ResponseEntity.ok(gameBoard);
    }

    @GetMapping("/valid-moves")
    public ResponseEntity<List<Coordinate>> getValidMoves(@RequestParam int playerId) {
        Player player = (playerId == 1) ? gameBoard.getPlayer1() : gameBoard.getPlayer2();
        List<Coordinate> validMoves = new ArrayList<>();
        Coordinate pos = player.getPosition();
        
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                Coordinate target = new Coordinate(pos.getX() + dx, pos.getY() + dy);
                if (gameBoard.isValidPawnMove(player, target)) {
                    validMoves.add(target);
                }
            }
        }
        return ResponseEntity.ok(validMoves);
    }

    @PostMapping("/settings")
    public ResponseEntity<String> updateSettings(@RequestParam boolean aiEnabled, @RequestParam int difficulty) {
        this.isAiEnabled = aiEnabled;
        this.aiDifficulty = difficulty;
        return ResponseEntity.ok("Settings updated");
    }

    @PostMapping("/move")
    public ResponseEntity<String> movePawn(@RequestBody MoveRequest request) {
        Coordinate target = new Coordinate(request.targetX(), request.targetY());
        boolean success = gameBoard.movePlayer(request.playerId(), target);
        
        if (success) {
            actionHistory.add(new GameAction(request.playerId(), request.targetX(), request.targetY()));
            redoStack.clear();
            
            // THE FIX: Explicitly tell the frontend exactly who won!
            if (gameBoard.getPlayer1().hasWon()) return ResponseEntity.ok("Player 1 Wins!");
            if (gameBoard.getPlayer2().hasWon()) return ResponseEntity.ok("Player 2 Wins!");
            
            return ResponseEntity.ok("Move successful");
        }
        return ResponseEntity.badRequest().body("Invalid move");
    }

    @PostMapping("/wall")
    public ResponseEntity<String> placeWall(@RequestBody WallRequest request) {
        Player player = (request.playerId() == 1) ? gameBoard.getPlayer1() : gameBoard.getPlayer2();
        if (player.getWallsRemaining() <= 0) return ResponseEntity.badRequest().body("Out of walls!");

        Wall.Orientation orientation;
        try { orientation = Wall.Orientation.valueOf(request.orientation().toUpperCase()); } 
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body("Invalid Orientation"); }

        boolean success = gameBoard.placeWall(request.playerId(), new Wall(new Coordinate(request.anchorX(), request.anchorY()), orientation));

        if (success) {
            actionHistory.add(new GameAction(request.playerId(), request.anchorX(), request.anchorY(), orientation.name()));
            redoStack.clear();
            return ResponseEntity.ok("Wall placed successfully");
        }
        return ResponseEntity.badRequest().body("Invalid: This wall traps a player!");
    }

    @PostMapping("/ai-turn")
    public ResponseEntity<String> takeAiTurn() {
        if (isAiEnabled && gameBoard.getCurrentPlayerId() == 2) {
            
            // Diffing tracking variables
            Coordinate oldPos = gameBoard.getPlayer2().getPosition();
            int oldWalls = gameBoard.getPlayer2().getWallsRemaining();
            
            aiService.takeTurn(gameBoard, aiDifficulty);
            
            int newWalls = gameBoard.getPlayer2().getWallsRemaining();
            Coordinate newPos = gameBoard.getPlayer2().getPosition();
            
            // Record the AI's action via Diffing
            if (newWalls < oldWalls) {
                Wall w = gameBoard.getActiveWalls().get(gameBoard.getActiveWalls().size() - 1);
                actionHistory.add(new GameAction(2, w.getAnchor().getX(), w.getAnchor().getY(), w.getOrientation().name()));
            } else {
                actionHistory.add(new GameAction(2, newPos.getX(), newPos.getY()));
            }
            redoStack.clear();

            if (gameBoard.getPlayer2().hasWon()) return ResponseEntity.ok("AI Wins!");
            return ResponseEntity.ok("AI moved successfully");
        }
        return ResponseEntity.badRequest().body("Not the AI's turn.");
    }

    @PostMapping("/undo")
    public ResponseEntity<String> undo() {
        if (actionHistory.isEmpty()) return ResponseEntity.badRequest().body("Nothing to undo!");
        
        GameAction last = actionHistory.remove(actionHistory.size() - 1);
        redoStack.add(last);
        
        // Smart PvE Pop: If we played against AI and just undid the AI's move, we must undo the human's move too
        if (isAiEnabled && last.playerId == 2 && !actionHistory.isEmpty()) {
            GameAction humanLast = actionHistory.remove(actionHistory.size() - 1);
            redoStack.add(humanLast);
        }
        
        rebuildBoard();
        return ResponseEntity.ok("Undo successful");
    }

    @PostMapping("/redo")
    public ResponseEntity<String> redo() {
        if (redoStack.isEmpty()) return ResponseEntity.badRequest().body("Nothing to redo!");
        
        GameAction next = redoStack.remove(redoStack.size() - 1);
        actionHistory.add(next);
        
        // Smart PvE Push: If we fast-forwarded a human move, fast-forward the AI move too
        if (isAiEnabled && next.playerId == 1 && !redoStack.isEmpty() && redoStack.get(redoStack.size() - 1).playerId == 2) {
            GameAction aiNext = redoStack.remove(redoStack.size() - 1);
            actionHistory.add(aiNext);
        }
        
        rebuildBoard();
        return ResponseEntity.ok("Redo successful");
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetGame() {
        actionHistory.clear();
        redoStack.clear();
        gameBoard = new Board();
        return ResponseEntity.ok("Game reset successfully");
    }
}