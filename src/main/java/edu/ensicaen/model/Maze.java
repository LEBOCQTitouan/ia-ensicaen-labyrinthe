package edu.ensicaen.model;

import edu.ensicaen.model.agent.Action;
import edu.ensicaen.util.Utility;

import java.awt.Point;
import java.io.Serializable;
import java.util.Vector;
import java.util.logging.Level;

public class Maze implements Serializable {
    public int height;
    public int width;
    public Vector<State> goals;
    public Vector<State> starts;
    public Vector<Wall> walls;

    public Maze(int width, int height) {
        this.width = width;
        this.height = height;

        walls = new Vector<>();
        goals = new Vector<>();
        starts = new Vector<>();
    }

    /**
     * Adds a starting state to the maze.
     */
    public void addStart(State st) {
        starts.add(st);
    }

    /**
     * Adds the goal state to the maze. if a goal already exists at that
     * position then the goal is removed from that location. this enables
     * goal addition and deletion using the GUI
     */
    public void addGoal(State st) {
        // do not add goals if they lie outside the maze dimensions
        if (st.x < 0 || st.y < 0 || st.x >= width || st.y >= height)
            return;

        if (!goals.contains(st))
            goals.add(st);
        else goals.remove(st);
    }

    /**
     * Determines if the transition from current state to next state is a
     * valid transition. A transition is invalid when there is a wall between
     * the current state and next state OR the next state lies outside the
     * maze boundary.
     */
    public boolean isValidTransition(State curr, State st) {
        Wall possibleWall = new Wall(curr.x, curr.y, getDirection(curr, st));
        return !walls.contains(possibleWall);
    }

    /*
     * Returns the direction (UP=0, RIGHT=1, DOWN=2, LEFT=3) in which state
     * 'st' lies with respect to the state 'curr'.
     */
    public int getDirection(State curr, State st) {
        switch (curr.x - st.x) {
            case -1:
                return Wall.RIGHT;
            case 1:
                return Wall.LEFT;
            default:
                switch (curr.y - st.y) {
                    case 1:
                        return Wall.DOWN;
                    default:
                        return Wall.UP;
                }
        }
    }

    /**
     * Returns all possible valid successors of the state 'currState'.
     */
    public Vector<State> getValidSuccessors(State currState) {
        Vector<State> successors = new Vector<>();
        for (int i = 0; i < Action.NUM_ACTIONS; i++) {
            State newState = Action.performAction(currState, i);
            if (isValidTransition(currState, newState))
                successors.add(newState);
        }
        return successors;
    }

    /**
     * Returns all the successors (valid successors as well as invalid
     * successors) of the state 'currState'
     */
    public Vector<State> getSuccessors(State currState) {
        Vector<State> successors = new Vector<>();

        for (int i = 0; i < Action.NUM_ACTIONS; i++) {
            State newState = Action.performAction(currState, i);
            successors.add(newState);
        }
        return successors;
    }

    /**
     * Adds a wall to the maze
     */
    public void addWall(Wall newWall) {
        // do not add walls if they lie outside the maze dimensions
        if (newWall.x < 0 || newWall.y < 0 || newWall.x >= width || newWall.y >= height)
            return;

        if (!isWallPresent(newWall)) {
            walls.add(newWall);
        } else {
            // remove the wall at that location
            walls.remove(newWall);
        }
    }

    /**
     * Returns the reward which will result if there is an attempt to make a
     * transition from state 'curr' to state 'st'. returns penalty if the
     * transition is invalid else returns zero.
     */
    public double getReward(State curr, State st) {
        Wall possibleWall = new Wall(curr.x, curr.y, getDirection(curr, st));
        int index = walls.indexOf(possibleWall);
        if (index != -1) {
            Wall w = walls.get(index);
            return w.penalty;
        }
        return 0;
    }

    /**
     * Returns true if a wall is present at the specified location else
     * returns false.
     */
    public boolean isWallPresent(Wall newWall) {
        boolean wallPresent = false;
        Wall w;
        for (Wall wall : walls) {
            w = wall;
            if (w.x == newWall.x && w.y == newWall.y && w.dir == newWall.dir) {
                wallPresent = true;
                break;
            }
        }
        return wallPresent;
    }

    /**
     * Only for debugging purposes...
     * Displays the maze status on the console.
     */
    public void display() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point currentPoint = new Point(y, x);
                if (!walls.contains(currentPoint))
                    System.out.print("0 ");
                else
                    System.out.print("1 ");
            }
            System.out.println();
        }
    }


    /**
     * Only for debugging purposes...
     * Prints on the console all the walls that are present in the maze
     */
    public void printWalls() {
        Wall w;
        if (walls.isEmpty())
            Utility.LOGGER.log(Level.INFO, "No walls yet");

        for (Wall wall : walls) {
            w = wall;
            Utility.LOGGER.log(Level.INFO,
                    "Wall at " + w.x + " " + w.y + " " + w.dir + " "
                    + w.penalty);
        }
    }

    /**
     * Only for debugging purposes...
     * Prints on the console all the goals that are present in the maze
     */
    public void printGoals() {
        State st;
        if (goals.isEmpty())
            Utility.LOGGER.log(Level.INFO, "No goals yet");
        for (State goal : goals) {
            st = goal;
            Utility.LOGGER.log(Level.INFO, "Goal at " + st.x + " " + st.y);
        }
    }
}