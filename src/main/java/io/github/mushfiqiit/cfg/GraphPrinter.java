package io.github.mushfiqiit.cfg;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Prints a {@link ControlFlowGraph} and its complexity to standard output. Lines are shown with
 * the 1-based numbers an editor uses, so node {@code n} is printed as line {@code n + 1}.
 */
final class GraphPrinter {

    private static final int MAX_STATEMENT_LENGTH = 40;

    /** Prevents instantiation; this class only has static methods. */
    private GraphPrinter() {
    }

    // ***************************************************************************
    // Printing the entry point
    // ***************************************************************************

    /**
     * Prints the line the graph starts from.
     *
     * @param root the graph's root node
     */
    static void printEntryPoint(Node root) {
        System.out.println("Entry point: line " + lineNumber(root.getNumber()) + ": "
                + root.getStatement().strip());
    }

    // ***************************************************************************
    // Printing the graph
    // ***************************************************************************

    /**
     * Prints each line the entry point can reach, with the lines that can run right after it, then
     * the same edges as an adjacency matrix.
     *
     * @param graph the graph to print
     */
    static void printGraph(ControlFlowGraph graph) {
        List<Integer> nodes = graph.reachableNodes();
        printSuccessorTable(graph, nodes);
        printAdjacencyMatrix(graph, nodes);
    }

    /**
     * Prints one row per reachable line: its number, its statement and the lines it leads to.
     *
     * @param graph the graph to print
     * @param nodes the reachable node numbers, in order
     */
    private static void printSuccessorTable(ControlFlowGraph graph, List<Integer> nodes) {
        ConsoleText.printSection("Control flow graph");
        System.out.println("  Each row is a source line, followed by the lines that can run right after it.");
        System.out.println();

        ConsoleTable table = new ConsoleTable("Line", "Statement", "Next lines").alignRight(0);
        for (int node : nodes) {
            String statement = ConsoleText.shorten(graph.getStatement(node).strip(), MAX_STATEMENT_LENGTH);
            table.addRow(lineNumber(node), statement, successors(graph, node));
        }
        table.print();
    }

    /**
     * Prints the adjacency matrix of the reachable lines, with {@code 1} where the row's line can
     * run right before the column's line and {@code .} elsewhere.
     *
     * @param graph the graph to print
     * @param nodes the reachable node numbers, in order
     */
    private static void printAdjacencyMatrix(ControlFlowGraph graph, List<Integer> nodes) {
        ConsoleText.printSection("Adjacency matrix");
        System.out.println("  1 means the row's line can run right before the column's line.");
        System.out.println();

        String[] headers = new String[nodes.size() + 1];
        headers[0] = "Line";
        for (int column = 0; column < nodes.size(); column++) {
            headers[column + 1] = String.valueOf(lineNumber(nodes.get(column)));
        }
        ConsoleTable table = new ConsoleTable(headers);
        for (int column = 0; column < headers.length; column++) {
            table.alignRight(column);
        }
        for (int from : nodes) {
            Object[] cells = new Object[headers.length];
            cells[0] = lineNumber(from);
            for (int column = 0; column < nodes.size(); column++) {
                cells[column + 1] = graph.hasEdge(from, nodes.get(column)) ? "1" : ".";
            }
            table.addRow(cells);
        }
        table.print();
    }

    // ***************************************************************************
    // Printing the complexity
    // ***************************************************************************

    /**
     * Prints the cyclomatic complexity worked out both ways: from the graph as edges - nodes + 2,
     * and from the decision points as decisions + 1. For well-structured code the two agree.
     *
     * @param graph the graph to measure
     * @param decisions the decision points counted in the source
     */
    static void printComplexity(ControlFlowGraph graph, DecisionCounts decisions) {
        int nodeCount = graph.countNodes();
        int edgeCount = graph.countEdges();
        int fromGraph = edgeCount - nodeCount + 2;

        ConsoleText.printSection("Cyclomatic complexity");
        System.out.printf("  From the graph:            M = E - N + 2 = %d - %d + 2 = %d%n",
                edgeCount, nodeCount, fromGraph);
        System.out.printf("  From the decision points:  M = D + 1 = %d + 1 = %d%n",
                decisions.total(), decisions.complexity());
        System.out.println("  Decision points: " + String.join(", ",
                ConsoleText.count(decisions.ifs(), "if", "ifs"),
                ConsoleText.count(decisions.elseIfs(), "else if", "else ifs"),
                ConsoleText.count(decisions.loops(), "loop", "loops")));
    }

    // ***************************************************************************
    // Helpers
    // ***************************************************************************

    /**
     * Lists the line numbers that can run right after a node.
     *
     * @param graph the graph to inspect
     * @param node the node's number
     * @return the line numbers separated by commas, or {@code (end)} if there are none
     */
    private static String successors(ControlFlowGraph graph, int node) {
        List<Integer> next = graph.successorsOf(node);
        return next.isEmpty() ? "(end)"
                : next.stream().map(to -> String.valueOf(lineNumber(to))).collect(Collectors.joining(", "));
    }

    /**
     * Converts a node number to the 1-based line number shown to the user.
     *
     * @param node the node's number, counting from 0
     * @return the line number, counting from 1
     */
    private static int lineNumber(int node) {
        return node + 1;
    }
}
