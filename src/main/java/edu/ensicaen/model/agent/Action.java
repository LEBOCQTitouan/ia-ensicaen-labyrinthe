package edu.ensicaen.model.agent;

import edu.ensicaen.model.State;
import edu.ensicaen.util.Utility;

import java.util.logging.Level;

/**
 * An action to take within the maze.
 */
public class Action {
    public static final int NUM_ACTIONS = 4;
    public static final int UP = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;

    public Action() {}

    /**
     * Performs the specified action (UP, RIGHT, DOWN, LEFT) on the given state
     * and returns the resulting new state.
     */
    public static State performAction(State state, int action) {
        State newState;
        switch (action) {
            case UP:
                newState = new State(state.x, state.y + 1);
                break;
            case RIGHT:
                newState = new State(state.x + 1, state.y);
                break;
            case DOWN:
                newState = new State(state.x, state.y - 1);
                break;
            case LEFT:
                newState = new State(state.x - 1, state.y);
                break;
            default:
                newState = state;
                break;
        }
        return newState;
    }

    /**
     * Performs the specified action (UP, RIGHT, DOWN, LEFT) on the given state
     * and returns the resulting new state. The pjog parameter has to be set
     * to 1 (equiprobability of actions).
     */
    public static State performAction(State state, int action, double PJOG) {
        double rand = Math.random();
        State newState;
        double randomActionProbability = 1 / (Action.NUM_ACTIONS);
        int choosenAction = action;

        for (int i = 0; i < Action.NUM_ACTIONS; i++) {
            if (rand < (i + 1) * randomActionProbability) {
                choosenAction = i;
                break;
            }
        }
        Utility.LOGGER.log(Level.INFO, "Action to be taken: {0}", action);
        Utility.LOGGER.log(Level.INFO, "Rand: {0}", rand);
        Utility.LOGGER.log(Level.INFO, "Taken: {0}", choosenAction);

        newState = performAction(state, choosenAction);
        return newState;
    }
}
