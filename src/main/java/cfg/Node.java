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

    public Node(int number, String statement) {
        this.number = number;
        this.statement = statement;
    }

    public int getNumber() {
        return number;
    }

    public String getStatement() {
        return statement;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void addChild(Node child) {
        children.add(child);
    }
}
