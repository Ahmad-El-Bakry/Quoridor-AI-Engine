package com.quoridor.backend.dto;

public record MoveRequest(int playerId, int targetX, int targetY) {}