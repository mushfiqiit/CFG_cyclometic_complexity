package cfg;

import javafx.scene.shape.Line;

/**
 * A line that stays attached to two graph nodes as they move.
 *
 * @author Asus
 */
class Edge extends Line {

    Edge(GraphicNode startNode, GraphicNode endNode) {
        startXProperty().bind(startNode.layoutXProperty());
        startYProperty().bind(startNode.layoutYProperty());
        endXProperty().bind(endNode.layoutXProperty());
        endYProperty().bind(endNode.layoutYProperty());
    }
}
