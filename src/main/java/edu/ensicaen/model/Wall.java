package edu.ensicaen.model;

import java.io.Serializable;

public class Wall implements Serializable {
    public int x;
    public int y;
    public int dir;
    public int penalty;
    public static final int UP = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;

    public Wall() {

    }

    public Wall(int aX, int aY, int aDirection, int aPenalty) {
        x = aX;
        y = aY;
        dir = aDirection;
        penalty = aPenalty;
    }

    public Wall(int aX, int aY, int aDirection) {
        x = aX;
        y = aY;
        dir = aDirection;
        penalty = 0;
    }

    public boolean equals(Object obj) {
        Wall w = (Wall) obj;
        return (x == w.x && y == w.y && dir == w.dir);
    }
}
