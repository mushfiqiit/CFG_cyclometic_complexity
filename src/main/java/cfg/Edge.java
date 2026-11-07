package cfg;

import javafx.scene.shape.Line;

/**
 * A line that stays attached to two graph nodes as they move.
 *
 * @author Asus
 */
class Edge extends Line {

    /**
     * Creates a line from one node to another and binds its ends to the nodes' positions.
     *
     * @param startNode the node the edge leaves
     * @param endNode the node the edge enters
     * @throws NullPointerException if either node is null
     */
    Edge(GraphicNode startNode, GraphicNode endNode) {
        startXProperty().bind(startNode.layoutXProperty());
        startYProperty().bind(startNode.layoutYProperty());
        endXProperty().bind(endNode.layoutXProperty());
        endYProperty().bind(endNode.layoutYProperty());
    }
}
