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
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Draws the graph saved by {@link ControlFlowGraph} in a window, one row per level. */
public class GraphViewer extends Application {

    private static final int WINDOW_WIDTH = 1500;
    private static final int WINDOW_HEIGHT = 700;
    private static final int COLUMN_SPACING = 120;
    private static final int ROW_SPACING = 40;
    private static final int EDGE_WIDTH = 7;

    // ***************************************************************************
    // Opening the window
    // ***************************************************************************

    /** Opens the window and blocks until it is closed. */
    public static void show() {
        launch();
    }

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

    private void initGraph(Group root) throws FileNotFoundException {
        // Each node's text is the line of the edges file at its number, not its source statement.
        List<String> nodeTexts = SourceCodeReader.readLines(GraphData.EDGES_FILE);
        Map<Integer, GraphicNode> nodes = addNodes(root, nodeTexts);
        addEdges(root, nodes);
    }

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
                GraphicNode node = new GraphicNode(COLUMN_SPACING * (column + 1), level * ROW_SPACING,
                        nodeTexts.get(number), Integer.toString(number));
                nodes.put(number, node);
                root.getChildren().add(node);
            }
        }
        return nodes;
    }

    private void addEdges(Group root, Map<Integer, GraphicNode> nodes) throws FileNotFoundException {
        try (Scanner edges = new Scanner(new File(GraphData.EDGES_FILE))) {
            while (edges.hasNextInt()) {
                int source = edges.nextInt();
                int destination = edges.nextInt();

                Edge edge = new Edge(nodes.get(source), nodes.get(destination));
                edge.setStroke(Color.BLANCHEDALMOND);
                edge.setStrokeWidth(EDGE_WIDTH);
                root.getChildren().add(0, edge); // Index 0 keeps edges behind the nodes.
            }
        }
    }
}
