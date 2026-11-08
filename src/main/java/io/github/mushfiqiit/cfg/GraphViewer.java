package io.github.mushfiqiit.cfg;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Draws a {@link ControlFlowGraph} in a window, one row per level. */
public class GraphViewer extends Application {

    private static final int WINDOW_WIDTH = 1500;
    private static final int WINDOW_HEIGHT = 700;
    private static final int COLUMN_SPACING = 120;
    private static final int ROW_SPACING = 40;

    /**
     * The graph to draw. JavaFX creates the viewer itself through its no-argument constructor, so
     * {@link #show(ControlFlowGraph)} hands the graph over here before launching.
     */
    private static volatile ControlFlowGraph graphToShow;

    // ***************************************************************************
    // Opening the window
    // ***************************************************************************

    /**
     * Creates the viewer. JavaFX calls this when {@link #show(ControlFlowGraph)} launches the
     * window.
     */
    public GraphViewer() {
    }

    /**
     * Opens a window showing the graph and blocks until it is closed. Can only be called once per
     * program run, because JavaFX can only be launched once.
     *
     * @param graph the graph to draw
     * @throws NullPointerException if {@code graph} is null
     */
    public static void show(ControlFlowGraph graph) {
        graphToShow = Objects.requireNonNull(graph, "graph");
        launch();
    }

    /**
     * Builds the scene from the graph passed to {@link #show(ControlFlowGraph)} and shows it.
     * Called by JavaFX.
     *
     * @param stage the window to show the graph in
     */
    @Override
    public void start(Stage stage) {
        Group root = new Group();
        initGraph(root);

        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        stage.show();
    }

    // ***************************************************************************
    // Building the scene from the graph
    // ***************************************************************************

    /**
     * Adds the graph's nodes and edges to the scene.
     *
     * @param root the group that holds the scene's contents
     */
    private void initGraph(Group root) {
        ControlFlowGraph graph = graphToShow;
        Map<Integer, GraphicNode> nodes = addNodes(root, graph);
        addEdges(root, graph, nodes);
    }

    /**
     * Adds a node for each line in the graph. Each level is drawn as a row, and nodes on the same
     * level are placed left to right in order of node number. Each node is labelled with its
     * 1-based line number, and double-clicking it shows its source line.
     *
     * @param root the group that holds the scene's contents
     * @param graph the graph to draw
     * @return the nodes added, keyed by node number
     */
    private Map<Integer, GraphicNode> addNodes(Group root, ControlFlowGraph graph) {
        Map<Integer, GraphicNode> nodes = new HashMap<>();
        Map<Integer, Integer> nodesPerLevel = new HashMap<>();

        for (int number : graph.reachableNodes()) {
            int level = graph.levelOf(number).orElseThrow();
            int column = nodesPerLevel.merge(level, 1, Integer::sum) - 1;
            GraphicNode node = GraphicNode.of(COLUMN_SPACING * (column + 1), level * ROW_SPACING,
                    number + 1, graph.getStatement(number).strip());
            nodes.put(number, node);
            root.getChildren().add(node);
        }
        return nodes;
    }

    /**
     * Adds a line for each edge in the graph, behind all the nodes.
     *
     * @param root the group that holds the scene's contents
     * @param graph the graph to draw
     * @param nodes the nodes to connect, keyed by node number
     */
    private void addEdges(Group root, ControlFlowGraph graph, Map<Integer, GraphicNode> nodes) {
        for (int from : graph.reachableNodes()) {
            for (int to : graph.successorsOf(from)) {
                Edge edge = Edge.of(nodes.get(from), nodes.get(to));
                root.getChildren().add(0, edge); // Index 0 keeps edges behind the nodes.
            }
        }
    }
}
