package cfg;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * The control flow graph of a C source file: which lines can run directly after which. Create
 * one with {@link ControlFlowGraphBuilder}.
 *
 * @author sikde
 */
public final class ControlFlowGraph {

    private final Node root;
    private final List<String> lines;
    private final int[][] adjacency = new int[GraphData.MAX_NODES][GraphData.MAX_NODES];

    /**
     * Creates a graph from linked nodes and records every edge reachable from the root.
     *
     * @param root the node the graph starts from
     * @param lines the source lines; each line's index is its node number
     * @throws ArrayIndexOutOfBoundsException if a reachable node's number is
     *         {@link GraphData#MAX_NODES} or more
     */
    ControlFlowGraph(Node root, List<String> lines) {
        this.root = root;
        this.lines = lines;
        fillAdjacency(root, new boolean[lines.size()]);
    }

    /**
     * Records every edge reachable from {@code node} in the adjacency matrix, using a depth-first
     * traversal.
     *
     * @param node the node to start from
     * @param visited which node numbers the traversal has already reached
     */
    private void fillAdjacency(Node node, boolean[] visited) {
        visited[node.getNumber()] = true;

        for (Node child : node.getChildren()) {
            if (!visited[child.getNumber()]) {
                fillAdjacency(child, visited);
            }
        }
        for (Node child : node.getChildren()) {
            adjacency[node.getNumber()][child.getNumber()] = 1;
        }
    }

    // ***************************************************************************
    // Reading the graph's shape
    // ***************************************************************************

    /**
     * Returns the number of lines in the source file. Node numbers range from 0 to one less than
     * this.
     *
     * @return the source file's line count
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * Returns the source line of a node.
     *
     * @param number the node's number
     * @return the source line, exactly as read
     * @throws IndexOutOfBoundsException if no line has that number
     */
    public String getStatement(int number) {
        return lines.get(number);
    }

    /**
     * Tells whether the graph has an edge between two nodes.
     *
     * @param from number of the node the edge leaves
     * @param to number of the node the edge enters
     * @return true if the edge exists
     */
    public boolean hasEdge(int from, int to) {
        return adjacency[from][to] == 1;
    }

    /**
     * Counts the edges between nodes for the source lines.
     *
     * @return the number of edges
     */
    public int countEdges() {
        int edgeCount = 0;
        for (int from = 0; from < getLineCount(); from++) {
            for (int to = 0; to < getLineCount(); to++) {
                if (hasEdge(from, to)) {
                    edgeCount++;
                }
            }
        }
        return edgeCount;
    }

    // ***************************************************************************
    // Measuring distances from the root
    // ***************************************************************************

    /**
     * Finds each node's distance from the root with a breadth-first traversal. The root is on
     * level 1.
     *
     * @return each node's level, indexed by node number; {@link GraphData#UNREACHABLE_LEVEL} for
     *         nodes the root cannot reach
     */
    public int[] computeLevels() {
        int[] levels = new int[GraphData.MAX_NODES];
        Arrays.fill(levels, GraphData.UNREACHABLE_LEVEL);
        levels[root.getNumber()] = 1;

        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(root.getNumber());
        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int next = 0; next < GraphData.MAX_NODES; next++) {
                if (hasEdge(current, next) && levels[next] > levels[current] + 1) {
                    levels[next] = levels[current] + 1;
                    queue.add(next);
                }
            }
        }
        return levels;
    }
}
