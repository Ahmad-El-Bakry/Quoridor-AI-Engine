package com.quoridor.backend.model;

public class Wall {
    public enum Orientation { HORIZONTAL, VERTICAL }
    
    private Coordinate anchor; 
    private Orientation orientation;

    public Wall(Coordinate anchor, Orientation orientation) {
        this.anchor = anchor;
        this.orientation = orientation;
    }

    public Coordinate getAnchor() { return anchor; }
    public Orientation getOrientation() { return orientation; }
}