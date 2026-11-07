package cfg;

import java.util.ArrayList;
import java.util.List;

/**
 * A control flow graph node: one source line and the nodes that can run after it.
 *
 * @author sikde
 */
public class Node {

    private final int number;
    private final String statement;
    private final List<Node> children = new ArrayList<>();

    /**
     * Creates a node with no children.
     *
     * @param number index of the source line, counting from 0
     * @param statement text of the source line
     */
    public Node(int number, String statement) {
        this.number = number;
        this.statement = statement;
    }

    /**
     * Returns the index of this node's source line.
     *
     * @return the line index, counting from 0
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the text of this node's source line.
     *
     * @return the source line, exactly as read
     */
    public String getStatement() {
        return statement;
    }

    /**
     * Returns the nodes that can run directly after this one.
     *
     * @return the children in the order they were added; changes to the list affect this node
     */
    public List<Node> getChildren() {
        return children;
    }

    /**
     * Adds a node that can run directly after this one.
     *
     * @param child the node to add
     */
    public void addChild(Node child) {
        children.add(child);
    }
}
