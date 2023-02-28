package edu.ensicaen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.ensicaen.model.Maze;
import edu.ensicaen.model.agent.QLearningEngine;
import edu.ensicaen.model.State;
import edu.ensicaen.model.Wall;
import edu.ensicaen.util.Utility;
import edu.ensicaen.view.GraphicsUtil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JFrame;
import javax.swing.BorderFactory;
import javax.swing.DebugGraphics;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

public class Main extends JFrame implements ActionListener {
    public static final Color SADDLEBROWN = new Color(0.5450981f, 0.2705882f, 0.07450981f);
    public static final Color SANDYBROWN = new Color(0.9568627f, 0.643137276f, 0.3764706f);
    public static final Color MEDIUMORCHID = new Color(0.7294118f, 0.33333334f, 0.827451f);
    public static final Color YELLOWGREEN = new Color(0.6039216f, 0.8039216f, 0.1960784f);
    public static final Color ORANGERED = new Color(1.0f, 0.27058825f, 0.0f);
    public static final Color OLIVEDRAB = new Color(0.41960785f, 0.5568628f, 0.1372549f);
	
    private JPanel jPanel;
    private JSeparator jSeparator;
    private JCheckBox jDecayLRCheckBox;
    private JLabel jStatusLabel;
    private JTextField jDelayTextField;
    private JTextField jCyclesTextField;
    private JTextField jLearnRateTextField;
    private JTextField jEpsilonTextField;

    private Maze theMaze = null;
    private QLearningEngine ql;

    private boolean showValue = true;
    private boolean animate = true;

    DecimalFormat df = new DecimalFormat("0.0");

    private String mazeStatus = "Load Maze First...";
    private String algorithmStatus = "";

    public static void main(String[] args) {
        Main app = new Main();
        app.setVisible(true);
    }

    public Main() {
        super("Labyrinth: Q-learning simulation");
        initGUI();
    }

    private void initGUI() {
        try {
            setSize(800, 600);
            setExtendedState(MAXIMIZED_BOTH);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            setMainPanel();
            addLoadMazeButton("LoadMaze...", 25, 58, 110, 28);
            addLabel("Epsilon", 20, 126, 85, 21);
            addJEpsilonTextField();
            addLabel("Precision", 20, 147, 85, 21);
            addPrecisionTextField("0.001", 110, 146, 55, 20);
            addLabel("Learning Rate", 20, 168, 85, 21);
            addJLearnRateTextField();
            addJDecayLRCheckBox();
            addInitializeButton("Initialize", 25, 223, 110, 28);
            addUpdateButton("Update", 25, 258, 110, 28);
            addStepButton("Step", 25, 314, 110, 28);
            addEpisodeButton("Episode", 25, 350, 110, 28);
            addJCyclesTextField();
            addCyclesButton("Cycles", 85, 386, 70, 28);
            addLabel("Delay (ms)", 20, 430, 85, 21);
            addJDelayTextField();
            addJValuesCheckBox();
            addJAnimateCheckBox();
            addSeparator();
            addJStatusLabel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent evt) {
        String command = evt.getActionCommand();
        Object source = evt.getSource();

        if (command.equals("LoadMaze...")) {
            String path = getClass().getResource("/simplemaze.json").getPath();
            loadMaze(path);
        } else if (command.equals("Initialize") && theMaze != null) {
            initializeMazeValues();
        } else if (command.equals("Update") && ql != null) {
            updateQLProperties();
        } else if (command.equals("Step")) {
            step();
        } else if (command.equals("Episode")) {
            episodes();
        } else if (command.equals("Cycles")) {
            cycles();
        } 
        if (source instanceof JTextField) {
            repaint();
        } else if (source instanceof JCheckBox) {
            JCheckBox jcb = (JCheckBox) evt.getSource();

            if (jcb.getText().equals("Show Values"))
                showValue = jcb.isSelected();
            if (jcb.getText().equals("Animate"))
                animate = jcb.isSelected();

            repaint();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        this.myUpdate(g);
    }

    public void myUpdate(Graphics g) {
        jSeparator.setSize(4, jPanel.getBounds().height - 2);
        jStatusLabel.setText(mazeStatus + algorithmStatus);
        showGrid(getGraphics());
    }

    public int squareSize = 60;
    int X = 192;
    int Y = 100;
    Color goldColor = new Color(220, 100, 55);

    public void showGrid(Graphics g) {
        squareSize = 60;

        if (ql != null) {
            drawValues(g);
        }
        if (theMaze != null) {
            drawMaze(g);
            drawGoal(g);
            drawWalls(g);
        }
        if (ql != null)
            drawCurrPosition(g, ql.getCurrentState());
    }

// PACKAGE METHODS

    void drawMaze(Graphics g) {
        g.setColor(Color.black);
        for (int counter = 0; counter <= theMaze.width; counter++) {
            g.drawLine(X + squareSize * counter, Y, X + squareSize * counter, Y + squareSize * theMaze.height);
        }
        for (int counter = 0; counter <= theMaze.height; counter++) {
            g.drawLine(X, Y + squareSize * counter, X + squareSize * theMaze.width, Y + squareSize * counter);
        }
    }

    void drawGoal(Graphics g) {
        Vector<State> goals = theMaze.goals;
        for (State goal : goals) {
            GraphicsUtil.fillRect(g, X + goal.x * squareSize + 1,
                    Y + (theMaze.height - 1 - goal.y) * squareSize + 1, squareSize - 2, squareSize - 2, goldColor);
        }
    }

    void drawWalls(Graphics g) {
        int aX; // start point of the wall
        int aY;
        int bX; // end point of the wall
        int bY;

        for (int i = 0; i < theMaze.walls.size(); i++) {
            Wall w = theMaze.walls.get(i);
            int nodeX = w.x;
            int nodeY = w.y;
            switch (w.dir) {
                case Wall.UP:
                    aX = nodeX * squareSize;
                    bX = (nodeX + 1) * squareSize;
                    aY = (theMaze.height - nodeY - 1) * squareSize;
                    bY = (theMaze.height - nodeY - 1) * squareSize;
                    GraphicsUtil.drawLine(g, X + aX, Y + aY, X + bX, Y + bY, 5);
                    break;
                case Wall.DOWN:
                    aX = nodeX * squareSize;
                    bX = (nodeX + 1) * squareSize;
                    aY = (theMaze.height - nodeY) * squareSize;
                    bY = (theMaze.height - nodeY) * squareSize;
                    GraphicsUtil.drawLine(g, X + aX, Y + aY, X + bX, Y + bY, 5);
                    break;
                case Wall.RIGHT:
                    aX = (nodeX + 1) * squareSize;
                    bX = (nodeX + 1) * squareSize;
                    aY = (theMaze.height - nodeY - 1) * squareSize;
                    bY = (theMaze.height - nodeY) * squareSize;
                    GraphicsUtil.drawLine(g, X + aX, Y + aY, X + bX, Y + bY, 5);
                    break;
                case Wall.LEFT:
                    aX = (nodeX) * squareSize;
                    bX = (nodeX) * squareSize;
                    aY = (theMaze.height - nodeY) * squareSize;
                    bY = (theMaze.height - nodeY - 1) * squareSize;
                    Utility.LOGGER.log(Level.INFO, "Left wall ax, ay, " +
                            "bx, by = " + aX + "," + aY + "," + bX +
                            "," + bY);
                    GraphicsUtil.drawLine(g, X + aX, Y + aY, X + bX, Y + bY, 5);
                    break;
                default:
                    break;
            }
        }
    }

    void drawCurrPosition(Graphics g, State s) {
        int centreX = squareSize * s.x + squareSize / 2;
        int centreY = squareSize * (theMaze.height - 1 - s.y) + squareSize / 2;
        int radius = squareSize / 5;
        Color c = SANDYBROWN;
        if (!ql.isBestAction)
            c = Color.YELLOW;

        GraphicsUtil.fillCircle(g, X + centreX, Y + centreY, radius, c);
        GraphicsUtil.drawCircle(g, X + centreX, Y + centreY, radius, 1, Color.black);

        GraphicsUtil.drawCircle(g, X + centreX - radius / 3, Y + centreY - radius / 3, radius / 6, 1, Color.black);
        GraphicsUtil.drawCircle(g, X + centreX - radius / 3, Y + centreY - radius / 3, 1, 1, Color.black);

        GraphicsUtil.drawCircle(g, X + centreX + radius / 3, Y + centreY - radius / 3, radius / 6, 1, Color.black);
        GraphicsUtil.drawCircle(g, X + centreX + radius / 3, Y + centreY - radius / 3, 1, 1, Color.black);

        if (ql.receivedPenalty) {
            GraphicsUtil.drawArc(g, X + centreX - radius / 2,
                    Y + centreY + radius / 3, radius, 2 * radius / 3, 0, 180,
                    SADDLEBROWN);
        } else {
            GraphicsUtil.drawArc(g, X + centreX - radius / 2, Y + centreY,
                    radius, 2 * radius / 3, 0, -180, SADDLEBROWN);
        }
    }

    void drawValues(Graphics g) {
        double[][] values = ql.getStateValues();
        int max = 1 + (int) Math.ceil(ql.getMax());

        double[][][] qsa = ql.getqValues();

        for (int xval = 0; xval < theMaze.width; xval++) {
            for (int y = 0; y < theMaze.height; y++) {
                int yval = theMaze.height - 1 - y;
                if (values[xval][y] >= 0) {
                    int red = 155 - Math.min((int) (255.0 * (values[xval][y]) / max), 155);
                    int green = 155 - Math.min((int) (255.0 * (values[xval][y]) / max), 155);
                    int b = 255 - Math.min((int) (255.0 * (values[xval][y]) / max), 220);

                    g.setColor(new Color(red, green, b));
                } else
                    g.setColor(goldColor);

                g.fillRect(X + xval * squareSize + 1, Y + yval * squareSize + 1, squareSize - 1, squareSize - 1);
            }
        }
        if (!showValue)
            return;

        g.setColor(Color.WHITE);
        for (int i = 0; i < qsa.length; i++) {
            for (int j = 0; j < qsa[i].length; j++) {
                g.drawString(df.format(qsa[i][j][0]), X + (i * squareSize) + (squareSize / 2) - 5, Y + ((theMaze.height - j) * squareSize) - squareSize + 15);
                g.drawString(df.format(qsa[i][j][1]), X + (i * squareSize) + squareSize - 20, Y + ((theMaze.height - j) * squareSize) - (squareSize / 2));
                g.drawString(df.format(qsa[i][j][2]), X + (i * squareSize) + (squareSize / 2) - 5, Y + ((theMaze.height - j) * squareSize) - 5);
                g.drawString(df.format(qsa[i][j][3]), X + (i * squareSize) + 5, Y + ((theMaze.height - j) * squareSize) - (squareSize / 2));
            }
        }

    }

// PRIVATE METHODS

    private void setMainPanel() {
        jPanel = new JPanel();
        this.getContentPane().add(jPanel, BorderLayout.CENTER);
        jPanel.setLayout(null);
        jPanel.setBackground(Color.WHITE);
        jPanel.setPreferredSize(new Dimension(200, 600));
    }

    private void addLoadMazeButton(String title, int x, int y, int width,
                               int height) {
        JButton jbutton = new JButton();
        jbutton.setText(title);
        jbutton.setBounds(x, y, width, height);
        jbutton.setActionCommand(title);
        jbutton.setToolTipText("Load a maze to try out algorithm.");
        jbutton.addActionListener(this);
        jPanel.add(jbutton);
    }

    private void addInitializeButton(String title, int x, int y, int width,
                                   int height) {
        JButton jbutton = new JButton();
        jbutton.setText(title);
        jbutton.setBounds(x, y, width, height);
        jbutton.setActionCommand(title);
        jbutton.setToolTipText("Initialize the algorithm with given parameters. You can reset algorithm by clicking this button.");
        jbutton.addActionListener(this);
        jPanel.add(jbutton);
    }

    private void addUpdateButton(String title, int x, int y, int width,
                                     int height) {
        JButton jbutton = new JButton();
        jbutton.setText(title);
        jbutton.setBounds(x, y, width, height);
        jbutton.setActionCommand(title);
        jbutton.setToolTipText("Enable changes in algorithm parameters without initializing algorithm.");
        jbutton.addActionListener(this);
        jPanel.add(jbutton);
    }

    private void addStepButton(String title, int x, int y, int width,
                                 int height) {
        JButton jbutton = new JButton();
        jbutton.setText(title);
        jbutton.setBounds(x, y, width, height);
        jbutton.setActionCommand(title);
        jbutton.setToolTipText("The agent makes a transition from one state to the other.");
        jbutton.addActionListener(this);
        jPanel.add(jbutton);
    }

    private void addEpisodeButton(String title, int x, int y, int width,
                               int height) {
        JButton jbutton = new JButton();
        jbutton.setText(title);
        jbutton.setBounds(x, y, width, height);
        jbutton.setActionCommand(title);
        jbutton.setToolTipText("The agent executes an entiere episode (i.e. keeps transitionning from one state to the other until it reaches the goal state. After that, it starts from the start state again.");
        jbutton.addActionListener(this);
        jPanel.add(jbutton);
    }

    private void addCyclesButton(String title, int x, int y, int width,
                                  int height) {
        JButton jbutton = new JButton();
        jbutton.setText(title);
        jbutton.setBounds(x, y, width, height);
        jbutton.setActionCommand(title);
        jbutton.setToolTipText("Executes the specified number of episodes at once. User need not keep clicking the episode button every time instead he can specify the number of episodes to be executed.");
        jbutton.addActionListener(this);
        jPanel.add(jbutton);
    }

    private void addJStatusLabel() {
        jStatusLabel = new JLabel();
        jStatusLabel.setText("Load Maze First... | Q-learning");
        jStatusLabel.setBounds(182, 14, 670, 34);
        jStatusLabel.setBackground(new Color(250, 250, 250));
        jStatusLabel.setBorder(BorderFactory.createTitledBorder(null,
                "", TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Ubuntu", Font.PLAIN, 12),
                new Color(20, 20, 22)));
        jStatusLabel.setFont(new Font("Ubuntu", Font.PLAIN,
                12));
        jStatusLabel.setDebugGraphicsOptions(DebugGraphics.NONE_OPTION);
        jStatusLabel.setOpaque(true);
        jStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        jStatusLabel.setPreferredSize(new Dimension(684, 35));
        jPanel.add(jStatusLabel);
    }

    private void addSeparator() {
        jSeparator = new JSeparator();
        jPanel.add(jSeparator);
        jSeparator.setBounds(170, 2, 4, 400);
        jSeparator.setBorder(BorderFactory.createTitledBorder(
                null,
                "",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                new Font("Ubuntu", Font.BOLD, 11),
                new Color(0, 0, 0)));
    }

    private void addJAnimateCheckBox() {
        JCheckBox jAnimateCheckBox = new JCheckBox();
        jAnimateCheckBox.setText("Animate");
        jAnimateCheckBox.setBounds(25, 593, 85, 17);
        jAnimateCheckBox.setOpaque(false);
        jAnimateCheckBox.setSelected(true);
        jAnimateCheckBox.setToolTipText("Turn off the animation and let the simulation run in the background.\n This works best when you initialize the algorithm with the desired\n parameters, uncheck the \"Animate\" checkbox and specify the number of\n episodes to be executed in the \"cycles\" field, then let the simulation\n run in the background and observe the state of the environment after\n those many episodes.");
        jAnimateCheckBox.addActionListener(this);
        jPanel.add(jAnimateCheckBox);
    }

    private void addJValuesCheckBox() {
        JCheckBox jValuesCheckBox = new JCheckBox();
        jPanel.add(jValuesCheckBox);
        jValuesCheckBox.setText("Show Values");
        jValuesCheckBox.setBounds(25, 553, 110, 17);
        jValuesCheckBox.setOpaque(false);
        jValuesCheckBox.setSelected(true);
        jValuesCheckBox.setToolTipText("When checked, shows current " +
                "value-function for all the states.");
        jValuesCheckBox.addActionListener(this);
    }

    private void addJDelayTextField() {
        jDelayTextField = new JTextField();
        jDelayTextField.setText("30");
        jDelayTextField.setBounds(110, 430, 50, 20);
        jDelayTextField.setHorizontalAlignment(SwingConstants.RIGHT);
        jDelayTextField.setToolTipText("Specify delay in microseconds " +
                "between the two successive steps during the animation.");
        jPanel.add(jDelayTextField);
    }

    private void addJCyclesTextField() {
        jCyclesTextField = new JTextField();
        jCyclesTextField.setText("1000");
        jCyclesTextField.setBounds(25, 387, 55, 26);
        jCyclesTextField.setToolTipText("Number of episodes.");
        jPanel.add(jCyclesTextField);
    }

    private void addJDecayLRCheckBox() {
        jDecayLRCheckBox = new JCheckBox();
        jPanel.add(jDecayLRCheckBox);
        jDecayLRCheckBox.setText("Decaying LR");
        jDecayLRCheckBox.setBounds(25, 195, 110, 17);
        jDecayLRCheckBox.setOpaque(false);
        jDecayLRCheckBox.setSelected(true);
        jDecayLRCheckBox.setToolTipText("Check to enable the decaying " +
                "learning rate scheme. Formula for decay is: decay = 1000 / " +
                "(1000 + number of episodes).");
        jDecayLRCheckBox.addActionListener(this);
    }

    private void addJLearnRateTextField() {
        jLearnRateTextField = new JTextField();
        jPanel.add(jLearnRateTextField);
        jLearnRateTextField.setText("0.7");
        jLearnRateTextField.setBounds(110, 167, 55, 20);
        jLearnRateTextField.setToolTipText("Learning rate for the Q-Learning " +
                "algorithm: can have values between 0 and 1.");
        jLearnRateTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    private void addJEpsilonTextField() {
        jEpsilonTextField = new JTextField();
        jEpsilonTextField.setText("0.1");
        jEpsilonTextField.setBounds(110, 125, 55, 20);
        jEpsilonTextField.setHorizontalAlignment(SwingConstants.RIGHT);
        jEpsilonTextField.setToolTipText("Epsilon for the epsilon greedy " +
                "policy for better exploration. E.g. if epsilon = 0.1, the " +
                "agent takes the best action with probability of 0.9 and " +
                "takes any of the other actions with probability of 0.1/(N-1)" +
                ", where N is total number of possible actions in a state.");
        jPanel.add(jEpsilonTextField);
    }

    private void addLabel(String text, int x, int y, int width, int height) {
        JLabel jlabel = new JLabel(text);
        jlabel.setBounds(x, y, width, height);
        jlabel.setHorizontalAlignment(SwingConstants.RIGHT);
        jPanel.add(jlabel);
    }

    private void addPrecisionTextField(String text, int x, int y, int width,
                               int height) {
        JTextField jtextField = new JTextField();
        jtextField.setText(text);
        jtextField.setBounds(x, y, width, height);
        jtextField.setHorizontalAlignment(SwingConstants.RIGHT);
        jtextField.setToolTipText("Precision used for checking wether " +
                "algorithm has converged.");
        jPanel.add(jtextField);
    }

    private void updateQLProperties() {
        ql.setProperty(QLearningEngine.Properties.Epsilon, jEpsilonTextField.getText());
        ql.setProperty(QLearningEngine.Properties.LearningRate, jLearnRateTextField.getText());
        if (jDecayLRCheckBox.isSelected())
            ql.setProperty(QLearningEngine.Properties.DecayingLR, "true");
        else
            ql.setProperty(QLearningEngine.Properties.DecayingLR, "false");
    }

    private void cycles() {
        Utility.LOGGER.log(Level.INFO, "Cycles");
        int delay = Integer.parseInt(jDelayTextField.getText());
        int numCycles = Integer.parseInt(jCyclesTextField.getText());
        if (ql != null) {
            for (int i = 0; i < numCycles; i++) {
                while (!ql.step()) {
                    if (animate) {
                        Utility.delay(delay);
                        myUpdate(getGraphics());
                    }
                }
            }
            repaint();
        }
    }

    private void episodes() {
        Utility.LOGGER.log(Level.INFO,"Episode");
        int delay = Integer.parseInt(jDelayTextField.getText());
        if (ql != null) {
            while (!ql.step()) {
                if (animate) {
                    Utility.delay(delay);
                    myUpdate(getGraphics());
                }
            }
            repaint();
        }
    }

    private void step() {
        Utility.LOGGER.log(Level.INFO,"step");
        if (ql != null && ql.step()) {
                jStatusLabel.setText("Goal Reached");
        }
        repaint();
    }

    private void initializeMazeValues() {
        Utility.LOGGER.log(Level.INFO,"Q-Learning");
        double learningRate = Double.parseDouble((jLearnRateTextField.getText()));
        double epsilon = Double.parseDouble((jEpsilonTextField.getText()));
        boolean decayingLR = jDecayLRCheckBox.isSelected();
        ql = new QLearningEngine(theMaze, learningRate, epsilon, decayingLR);
        algorithmStatus = " Q-Learning ";
        repaint();
    }

    private void loadMaze(String pathname) {
        Utility.LOGGER.log(Level.INFO, "Loading maze from JSON file");
        Gson gson = new Gson();
        Reader reader;

        try {
            reader = new BufferedReader(new FileReader(pathname));
            theMaze = gson.fromJson(reader, Maze.class);
            reader.close();
        } catch (IOException e) {
            Utility.LOGGER.log(Level.SEVERE, e.getMessage());
            throw new RuntimeException(e);
        }
        mazeStatus = " [ " + theMaze.width + " X " + theMaze.height + " Maze, Wall Penalty:" + (theMaze.walls.get(0)).penalty + "] | ";
        algorithmStatus = " Q-Learning --> Click Initialize";
        ql = null;
        repaint();
    }

    private void saveMaze(String pathname) {
        Utility.LOGGER.log(Level.INFO, "Saving maze to JSON");
        Gson gson;
        Writer writer;

        try {
            gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(theMaze);
            writer = Files.newBufferedWriter(Paths.get(pathname));
            writer.write(jsonOutput);
            writer.close();
            mazeStatus = " [ " + theMaze.width + " X " + theMaze.height + " Maze, Wall Penalty:" + (theMaze.walls.get(0)).penalty + "] | ";
            algorithmStatus = " Q-Learning --> Click Initialize";
        } catch (Exception e) {
            Utility.LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private void deserializeMaze(String pathname) {
        Utility.LOGGER.log(Level.INFO, "Deserializing maze");
        File file;
        FileInputStream fis;
        GZIPInputStream gzis;
        ObjectInputStream in;

        try {
            file = new File(pathname);
            fis  = new FileInputStream(file);
            gzis = new GZIPInputStream(fis);
            in   = new ObjectInputStream(gzis);
            theMaze = (Maze) in.readObject();
            in.close();
            mazeStatus = " [ " + theMaze.width + " X " + theMaze.height + " Maze, Wall Penalty:" + (theMaze.walls.get(0)).penalty + "] | ";
            algorithmStatus = " Q-Learning --> Click Initialize";
        } catch (Exception e) {
            Utility.LOGGER.log(Level.SEVERE, e.getMessage());
        }
        ql = null;
        repaint();
    }

    public void serializeMaze(String pathname) {
        try {
            Utility.LOGGER.log(Level.INFO,"Serializing maze...");
            FileOutputStream fos = new FileOutputStream(pathname);
            GZIPOutputStream gzos = new GZIPOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(gzos);
            oos.writeObject(theMaze);
            oos.close();
        } catch(Exception e) {
            Utility.LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

}
