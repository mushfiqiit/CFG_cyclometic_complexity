package io.github.mushfiqiit.cfg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.Set;

/**
 * The control flow graph of a C source file: which lines can run directly after which. Each node
 * is a source line, numbered from 0 by its position in the file. Create one with
 * {@link #of(ParsedSource)}.
 *
 * <p>The graph holds only the lines the entry line can reach. It is built once and does not change.
 *
 * <p>The methods that look up a line by its node number, {@link #getStatement(int)},
 * {@link #successorsOf(int)}, {@link #levelOf(int)} and {@link #hasEdge(int, int)} (for its
 * {@code from} line), throw {@link IndexOutOfBoundsException} if no line has that number.
 */
public final class ControlFlowGraph {

    private final List<String> lines;
    private final int entry;
    private final List<List<Integer>> successors;
    private final int[] levels;
    private final List<Integer> reachableNodes;
    private final int edgeCount;

    /**
     * Creates a graph from linked lines, keeping every edge reachable from the entry line and each
     * line's distance from it.
     *
     * @param lines the source lines; each line's index is its node number
     * @param entry the node the graph starts from
     * @param edges the lines each line leads to, one set per line
     */
    ControlFlowGraph(List<String> lines, int entry, List<? extends Set<Integer>> edges) {
        this.lines = List.copyOf(lines);
        this.entry = entry;
        this.successors = collectSuccessors(entry, edges);
        this.levels = measureLevels(entry);

        List<Integer> reached = new ArrayList<>();
        int edgeTotal = 0;
        for (int node = 0; node < lines.size(); node++) {
            if (levels[node] > 0) {
                reached.add(node);
            }
            edgeTotal += successors.get(node).size();
        }
        this.reachableNodes = Collections.unmodifiableList(reached);
        this.edgeCount = edgeTotal;
    }

    /**
     * Walks the edges from the entry line and keeps each reached line's successors, without
     * duplicates and in order of node number.
     *
     * @param entry the node to start from
     * @param edges the lines each line leads to, one set per line
     * @return one read-only list of successors per line; empty for lines the entry cannot reach
     */
    private static List<List<Integer>> collectSuccessors(int entry, List<? extends Set<Integer>> edges) {
        boolean[] visited = new boolean[edges.size()];
        ArrayDeque<Integer> pending = new ArrayDeque<>(List.of(entry));
        visited[entry] = true;
        while (!pending.isEmpty()) {
            for (int next : edges.get(pending.pop())) {
                if (!visited[next]) {
                    visited[next] = true;
                    pending.push(next);
                }
            }
        }
        List<List<Integer>> result = new ArrayList<>(edges.size());
        for (int line = 0; line < edges.size(); line++) {
            result.add(visited[line] ? edges.get(line).stream().sorted().toList() : List.of());
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Builds the control flow graph of a source file, starting from the function's header.
     *
     * @param source the source to build the graph of
     * @return the graph
     * @throws IllegalArgumentException if the source has no code
     */
    static ControlFlowGraph of(ParsedSource source) {
        return ControlFlowGraphBuilder.build(source);
    }

    /**
     * Finds each node's distance from the entry line with a breadth-first traversal. The entry line
     * is on level 1.
     *
     * @param entry number of the entry node
     * @return each line's level, or 0 for lines the entry line cannot reach
     */
    private int[] measureLevels(int entry) {
        int[] result = new int[lines.size()];
        result[entry] = 1;
        Queue<Integer> queue = new ArrayDeque<>(List.of(entry));
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
     * Returns the nodes in the graph: the lines the entry line can reach, including the entry
     * line itself.
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
     *         line with no successors or one the entry line cannot reach
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
     * Counts the nodes in the graph: the lines the entry line can reach, including the entry
     * line itself.
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
     * Returns a line's distance from the entry line: 1 for the entry line, 2 for the lines right
     * after it, and so on.
     *
     * @param node the line's node number
     * @return the level, or an empty value if the entry line cannot reach the line
     */
    public OptionalInt levelOf(int node) {
        return levels[node] > 0 ? OptionalInt.of(levels[node]) : OptionalInt.empty();
    }
}
