package cfg;

import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * A draggable graph node. Double-clicking switches its label between number and text.
 *
 * @author Asus
 */
class GraphicNode extends Button {

    private final String nodeText;
    private final String nodeNumber;

    GraphicNode(double x, double y, String nodeText, String nodeNumber) {
        this.nodeText = nodeText;
        this.nodeNumber = nodeNumber;

        setLayoutX(x);
        setLayoutY(y);
        setText(nodeNumber);

        setOnMouseDragged(event -> {
            setLayoutX(event.getSceneX());
            setLayoutY(event.getSceneY());
        });
        setOnMouseClicked(event -> {
            if (isPrimaryDoubleClick(event)) {
                toggleLabel();
            }
        });
    }

    private static boolean isPrimaryDoubleClick(MouseEvent event) {
        return event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2;
    }

    private void toggleLabel() {
        setText(getText().equals(nodeNumber) ? nodeText : nodeNumber);
    }
}
