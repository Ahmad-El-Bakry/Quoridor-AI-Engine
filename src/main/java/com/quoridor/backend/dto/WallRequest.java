package com.quoridor.backend.dto;

public record WallRequest(int playerId, int anchorX, int anchorY, String orientation) {}