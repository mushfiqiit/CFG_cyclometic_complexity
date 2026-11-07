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

    /**
     * Creates a node showing its number, which follows the mouse while dragged.
     *
     * @param x horizontal position in the scene
     * @param y vertical position in the scene
     * @param nodeText label shown after a double-click
     * @param nodeNumber label shown at first and after a second double-click
     */
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

    /**
     * Tells whether a click was a double-click with the primary mouse button.
     *
     * @param event the click to check
     * @return true for a primary-button click with a click count of exactly 2
     */
    private static boolean isPrimaryDoubleClick(MouseEvent event) {
        return event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2;
    }

    /** Shows the text if the number is showing, and the number otherwise. */
    private void toggleLabel() {
        setText(getText().equals(nodeNumber) ? nodeText : nodeNumber);
    }
}
