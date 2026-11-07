package cfg;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * A line that stays attached to two graph nodes as they move.
 *
 * @author Asus
 */
class Edge extends Line {

    private static final Color EDGE_COLOR = Color.BLANCHEDALMOND;
    private static final int EDGE_WIDTH = 7;

    /**
     * Creates an unstyled line from one node to another and binds its ends to the nodes'
     * positions. Use {@link #of(GraphicNode, GraphicNode)} to create edges.
     *
     * @param startNode the node the edge leaves
     * @param endNode the node the edge enters
     * @throws NullPointerException if either node is null
     */
    private Edge(GraphicNode startNode, GraphicNode endNode) {
        startXProperty().bind(startNode.layoutXProperty());
        startYProperty().bind(startNode.layoutYProperty());
        endXProperty().bind(endNode.layoutXProperty());
        endYProperty().bind(endNode.layoutYProperty());
    }

    /**
     * Creates a styled edge from one node to another that follows both nodes as they move.
     *
     * @param startNode the node the edge leaves
     * @param endNode the node the edge enters
     * @return the new edge
     * @throws NullPointerException if either node is null
     */
    static Edge of(GraphicNode startNode, GraphicNode endNode) {
        Edge edge = new Edge(startNode, endNode);
        edge.setStroke(EDGE_COLOR);
        edge.setStrokeWidth(EDGE_WIDTH);
        return edge;
    }
}
