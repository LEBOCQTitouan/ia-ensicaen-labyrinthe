package edu.ensicaen.model;

import java.awt.Point;

/**
 * A state for the Q-Learning algorithm.
 */
public class State extends Point {

    public State(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /*
     * Used for debugging only...
     */
    public void printState() {
        System.out.print("State=<" + x + "," + y + ">");
    }

    /*
     * Copies the newState object in to this current state.
     */
    public void copy(State newState) {
        x = newState.x;
        y = newState.y;
    }

    @Override
    public boolean equals(Object Obj) {
        State st = (State) Obj;
        return (x == st.x && y == st.y);
    }

}
