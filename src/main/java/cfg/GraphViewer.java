package cfg;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Draws the graph saved by {@link GraphFileWriter} in a window, one row per level. */
public class GraphViewer extends Application {

    private static final int WINDOW_WIDTH = 1500;
    private static final int WINDOW_HEIGHT = 700;
    private static final int COLUMN_SPACING = 120;
    private static final int ROW_SPACING = 40;

    // ***************************************************************************
    // Opening the window
    // ***************************************************************************

    /** Creates the viewer. JavaFX calls this when {@link #show()} launches the window. */
    public GraphViewer() {
    }

    /**
     * Opens the graph window and blocks until it is closed. Can only be called once per program
     * run.
     */
    public static void show() {
        launch();
    }

    /**
     * Builds the scene from the saved graph files and shows it. Called by JavaFX.
     *
     * @param stage the window to show the graph in
     * @throws FileNotFoundException if either graph file is missing
     */
    @Override
    public void start(Stage stage) throws FileNotFoundException {
        Group root = new Group();
        initGraph(root);

        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        stage.show();
    }

    // ***************************************************************************
    // Building the scene from the saved graph files
    // ***************************************************************************

    /**
     * Adds the graph's nodes and edges to the scene.
     *
     * @param root the group that holds the scene's contents
     * @throws FileNotFoundException if either graph file is missing
     */
    private void initGraph(Group root) throws FileNotFoundException {
        // Each node's text is the line of the edges file at its number, not its source statement.
        List<String> nodeTexts = SourceCodeReader.readLines(GraphData.EDGES_FILE);
        Map<Integer, GraphicNode> nodes = addNodes(root, nodeTexts);
        addEdges(root, nodes);
    }

    /**
     * Adds a node for each reachable entry in the levels file. Each level is drawn as a row, and
     * nodes on the same level are placed left to right.
     *
     * @param root the group that holds the scene's contents
     * @param nodeTexts the labels shown on double-click, indexed by node number
     * @return the nodes added, keyed by node number
     * @throws FileNotFoundException if the levels file is missing
     */
    private Map<Integer, GraphicNode> addNodes(Group root, List<String> nodeTexts) throws FileNotFoundException {
        Map<Integer, GraphicNode> nodes = new HashMap<>();
        int[] nodesPerLevel = new int[GraphData.MAX_NODES];

        try (Scanner levels = new Scanner(new File(GraphData.LEVELS_FILE))) {
            levels.nextInt(); // Skips the node count at the top of the file.

            while (levels.hasNextInt()) {
                int number = levels.nextInt();
                int level = levels.nextInt();
                if (level >= GraphData.UNREACHABLE_LEVEL) {
                    continue;
                }

                int column = nodesPerLevel[level]++;
                GraphicNode node = GraphicNode.of(COLUMN_SPACING * (column + 1), level * ROW_SPACING,
                        number, nodeTexts.get(number));
                nodes.put(number, node);
                root.getChildren().add(node);
            }
        }
        return nodes;
    }

    /**
     * Adds a line for each entry in the edges file, behind all the nodes.
     *
     * @param root the group that holds the scene's contents
     * @param nodes the nodes to connect, keyed by node number
     * @throws FileNotFoundException if the edges file is missing
     */
    private void addEdges(Group root, Map<Integer, GraphicNode> nodes) throws FileNotFoundException {
        try (Scanner edges = new Scanner(new File(GraphData.EDGES_FILE))) {
            while (edges.hasNextInt()) {
                int source = edges.nextInt();
                int destination = edges.nextInt();

                Edge edge = Edge.of(nodes.get(source), nodes.get(destination));
                root.getChildren().add(0, edge); // Index 0 keeps edges behind the nodes.
            }
        }
    }
}
