package io.github.mushfiqiit.cfg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The control flow graph of a C source file: which lines can run directly after which. Each node
 * is a source line, numbered from 0 by its position in the file. Create one with
 * {@link ControlFlowGraphBuilder}.
 *
 * <p>The graph holds only the lines the root can reach. It is built once and does not change.
 *
 * <p>The methods that look up a line by its node number, {@link #getStatement(int)},
 * {@link #successorsOf(int)}, {@link #levelOf(int)} and {@link #hasEdge(int, int)} (for its
 * {@code from} line), throw {@link IndexOutOfBoundsException} if no line has that number.
 *
 * @author sikde
 */
public final class ControlFlowGraph {

    private final List<String> lines;
    private final int entry;
    private final List<List<Integer>> successors;
    private final int[] levels;
    private final List<Integer> reachableNodes;
    private final int edgeCount;

    /**
     * Creates a graph from linked nodes, recording every edge reachable from the root and each
     * node's distance from it.
     *
     * @param root the node the graph starts from
     * @param lines the source lines; each line's index is its node number
     */
    ControlFlowGraph(Node root, List<String> lines) {
        this.lines = List.copyOf(lines);
        this.entry = root.getNumber();
        this.successors = collectSuccessors(root, lines.size());
        this.levels = measureLevels(root.getNumber());

        List<Integer> reached = new ArrayList<>();
        int edges = 0;
        for (int node = 0; node < lines.size(); node++) {
            if (levels[node] > 0) {
                reached.add(node);
            }
            edges += successors.get(node).size();
        }
        this.reachableNodes = Collections.unmodifiableList(reached);
        this.edgeCount = edges;
    }

    /**
     * Walks the linked nodes from the root and records each node's successors, without
     * duplicates and in order of node number.
     *
     * @param root the node to start from
     * @param lineCount number of lines in the source file
     * @return one read-only list of successors per line; empty for lines the root cannot reach
     */
    private static List<List<Integer>> collectSuccessors(Node root, int lineCount) {
        List<SortedSet<Integer>> found = new ArrayList<>(lineCount);
        for (int line = 0; line < lineCount; line++) {
            found.add(new TreeSet<>());
        }
        boolean[] visited = new boolean[lineCount];
        ArrayDeque<Node> pending = new ArrayDeque<>(List.of(root));
        visited[root.getNumber()] = true;
        while (!pending.isEmpty()) {
            Node node = pending.pop();
            for (Node child : node.getChildren()) {
                found.get(node.getNumber()).add(child.getNumber());
                if (!visited[child.getNumber()]) {
                    visited[child.getNumber()] = true;
                    pending.push(child);
                }
            }
        }
        List<List<Integer>> result = new ArrayList<>(lineCount);
        for (SortedSet<Integer> nodeSuccessors : found) {
            result.add(List.copyOf(nodeSuccessors));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Finds each node's distance from the root with a breadth-first traversal. The root is on
     * level 1.
     *
     * @param root number of the root node
     * @return each line's level, or 0 for lines the root cannot reach
     */
    private int[] measureLevels(int root) {
        int[] result = new int[lines.size()];
        result[root] = 1;
        Queue<Integer> queue = new ArrayDeque<>(List.of(root));
        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int next : successors.get(current)) {
                if (result[next] == 0) {
                    result[next] = result[current] + 1;
                    queue.add(next);
                }
            }
        }
        return result;
    }

    // ***************************************************************************
    // Reading the source lines
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
     */
    public String getStatement(int number) {
        return lines.get(number);
    }

    // ***************************************************************************
    // Reading the graph's shape
    // ***************************************************************************

    /**
     * Returns the nodes in the graph: the lines the root can reach, including the root itself.
     *
     * @return the node numbers in increasing order, as a read-only list
     */
    public List<Integer> reachableNodes() {
        return reachableNodes;
    }

    /**
     * Returns the lines that can run directly after a line.
     *
     * @param node the line's node number
     * @return the successors' node numbers in increasing order, as a read-only list; empty for a
     *         line with no successors or one the root cannot reach
     */
    public List<Integer> successorsOf(int node) {
        return successors.get(node);
    }

    /**
     * Tells whether the graph has an edge between two lines.
     *
     * @param from number of the node the edge leaves
     * @param to number of the node the edge enters
     * @return true if the edge exists
     */
    public boolean hasEdge(int from, int to) {
        return successors.get(from).contains(to);
    }

    /**
     * Returns the line the graph starts from: the function's header.
     *
     * @return the entry line's node number
     */
    public int entry() {
        return entry;
    }

    /**
     * Counts the nodes in the graph: the lines the root can reach, including the root itself.
     *
     * @return the number of nodes
     */
    public int countNodes() {
        return reachableNodes.size();
    }

    /**
     * Counts the edges in the graph.
     *
     * @return the number of edges
     */
    public int countEdges() {
        return edgeCount;
    }

    /**
     * Works out the cyclomatic complexity from the graph's shape, as {@code M = E - N + 2}.
     *
     * @return the number of edges minus the number of nodes, plus 2
     */
    public int cyclomaticComplexity() {
        return countEdges() - countNodes() + 2;
    }

    /**
     * Returns a line's distance from the root: 1 for the root, 2 for the lines right after it, and
     * so on.
     *
     * @param node the line's node number
     * @return the level, or an empty value if the root cannot reach the line
     */
    public OptionalInt levelOf(int node) {
        return levels[node] > 0 ? OptionalInt.of(levels[node]) : OptionalInt.empty();
    }
}
