package edu.ensicaen.model.agent;

import edu.ensicaen.model.Maze;
import edu.ensicaen.model.State;
import edu.ensicaen.util.Utility;

import java.util.Arrays;
import java.util.logging.Level;

public class QLearningEngine {
    /** The maze on which we perform the algorithm. */
    private final Maze maze;
    /** The Q-table to store Q-values of states and actions. */
    private final double[][][] qValues;
    /** Number of episodes used to explore or act. */
    private int numberOfEpisodes;
    /** The maximum learning rate value given by the UI. */
    private double maxLearningRate;
    /** The learning rate value (depends on maxLearningRate and numEpisodes). */
    private double learningRate;
    /** Epsilon value for the epsilon greedy algorithm. */
    private double epsilon;
    /** The default cost of a path from the current state to another one. */
    private static final int DEFAULTPATHCOST = 1;
    /** Is decaying used? See the GUI class */
    private boolean decayingLR;

    public boolean isBestAction = true;
    public boolean receivedPenalty = false;

    /** The starting state (default: (0,0)) */
    State start;

    /** The current state */
    State currentState;

    /**
     * Used to store current state values for the different shades of blue
     * within the maze (simulation in Main class).
     */
    double[][] stateValues;

    public static class Properties {
        public static int LearningRate = 2;
        public static int Epsilon = 3;
        public static int DecayingLR = 4;
    }

    public QLearningEngine(Maze aMaze, double aLearningRate,
                           double anEpsilon, boolean aDecayingLR) {

        maze = aMaze;
        maxLearningRate = aLearningRate;
        learningRate = maxLearningRate;
        epsilon = anEpsilon;
        decayingLR = aDecayingLR;

        start = new State(0, 0);
        currentState = new State(0, 0);

        stateValues= new double[maze.width][maze.height];
        for (double[] doubles : stateValues) Arrays.fill(doubles, 0);

        qValues = new double[maze.width][maze.height][Action.NUM_ACTIONS];
        for (double[][] doubles : qValues)
            for (double[] aDouble : doubles) Arrays.fill(aDouble, 0);

        numberOfEpisodes = 0;
    }

    public void setProperty(int name, String value) {
        if (name == Properties.Epsilon) {
            epsilon = Double.parseDouble(value);
        } else if (name == Properties.LearningRate) {
            maxLearningRate = Double.parseDouble(value);
        } else if (name == Properties.DecayingLR) {
            decayingLR = Boolean.parseBoolean(value);
        }
    }

    /**
     * Steps over to update the QValue.
     * @return True if a goal has been reached, false otherwise.
     */
    public boolean step() {
        double transitionCost;
        int currentAction;
        State nextState;

        if (hasReachedGoal()) {
            return true;
        }

        // --------------------------------------------------------------------
        // 1) Select action using epsilon greedy exploration policy (random).
        // --------------------------------------------------------------------
        // TO COMPLETE
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // 2) Perform choosen action based on PJOG (noise of environment -
        // here: 1.0).
        // --------------------------------------------------------------------
        // TO COMPLETE
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // 3) If not a valid transition stay in the same state and add penalty.
        // Otherwise, transitionCost is set to pathCost (=1).
        // --------------------------------------------------------------------
        // TO COMPLETE
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // 4) Update the Bellman equation.
        // --------------------------------------------------------------------
        // TO COMPLETE
        // --------------------------------------------------------------------

        return false;
    }

    /**
     * Returns the Q-values.
     */
    public double[][][] getqValues() {
        return qValues;
    }

    /**
     * Returns the state values (minimum values from Q-values).
     */
    public double[][] getStateValues() {
        for (int i = 0; i < maze.width; i++)
            for (int j = 0; j < maze.height; j++)
                stateValues[i][j] = getMinQValues(new State(i, j));

        return stateValues;
    }

    /**
     * Returns the current state.
     */
    public State getCurrentState() {
        return currentState;
    }

    /**
     * Returns the best action with probability (1-pjog) and returns other
     * actions with probability (1-pjog)/numOfActions
     */
    private int chooseAction(State currState, double randNum) {
        int bestAction = getBestAction(qValues[currState.x][currState.y]);
        double d = epsilon / (Action.NUM_ACTIONS);
        int choosenAction = bestAction;

        for (int i = 0; i < Action.NUM_ACTIONS; i++) {
            if (randNum < (i + 1) * d) {
                choosenAction = i;
                break;
            }
        }
        Utility.LOGGER.log(Level.INFO, "Action to be taken: {0}", bestAction);
        Utility.LOGGER.log(Level.INFO, "Rand: {0}", randNum);
        Utility.LOGGER.log(Level.INFO, "Taken: {0}", choosenAction);
        isBestAction = (choosenAction == bestAction);

        return choosenAction;
    }

    /**
     * Returns the best action (the minimum Q-Value) from the possible actions
     * in the current state.
     */
    private int getBestAction(double[] actions) {
        double min = actions[0];
        int bestAction = 0;
        for (int i = 1; i < actions.length; i++) {
            if (min > actions[i]) {
                min = actions[i];
                bestAction = i;
            }
        }
        return bestAction;
    }

    /**
     * Return the minimum value from the Q-values of the given state.
     */
    private double getMinQValues(State st) {
        double min = qValues[st.x][st.y][0];

        for (int i = 0; i < qValues[st.x][st.y].length; i++) {
            if (min > qValues[st.x][st.y][i]) {
                min = qValues[st.x][st.y][i];
            }
        }
        return min;
    }

    /**
     * Returns the maximum value from stateValues or qValues.
     */
    public double getMax() {
        if (null != stateValues) {
            return getMaxFromStateValues();
        } else {
            return getMaxFromQValues();
        }
    }

    private double getMaxFromQValues() {
        double max = qValues[0][0][0];

        for (double[][] qValue : qValues)
            for (double[] doubles : qValue)
                for (double aDouble : doubles)
                    if (max < aDouble)
                        max = aDouble;

        return max;
    }

    private double getMaxFromStateValues() {
        double max = stateValues[0][0];
        for (double[] doubles : stateValues)
            for (double aDouble : doubles)
                if (aDouble > max)
                    max = aDouble;
        return max;
    }

    /**
     * Returns true if a goal has been reached.
     */
    private boolean hasReachedGoal() {
        if (maze.goals.contains(currentState)) {
            currentState.copy(start);
            numberOfEpisodes++;

            if (decayingLR) {
                learningRate = (1000.0 * maxLearningRate) / (1000.0 + numberOfEpisodes);
            } else {
                learningRate = maxLearningRate;
            }

            if (0 == numberOfEpisodes % 1000) {
                Utility.LOGGER.log(Level.INFO,numberOfEpisodes + "," + learningRate);
            }
            return true;
        }
        return false;
    }

}
