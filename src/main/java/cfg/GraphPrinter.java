package cfg;

/** Prints a {@link ControlFlowGraph} and its graph-based complexity to standard output. */
final class GraphPrinter {

    /** Prevents instantiation; this class only has static methods. */
    private GraphPrinter() {
    }

    // ***************************************************************************
    // Printing the root
    // ***************************************************************************

    /**
     * Prints the root node's number and source line.
     *
     * @param root the graph's root node
     */
    static void printRoot(Node root) {
        System.out.println("\n\n\nroot node no: " + root.getNumber() + "\n"
                + "root node statement: " + root.getStatement());
    }

    // ***************************************************************************
    // Printing the graph
    // ***************************************************************************

    /**
     * Prints the adjacency list, the adjacency matrix and the graph-based complexity.
     *
     * @param graph the graph to print
     */
    static void printGraph(ControlFlowGraph graph) {
        printAdjacencyList(graph);
        printAdjacencyMatrix(graph);
        printGraphComplexity(graph);
    }

    /**
     * Prints each node's number followed by the numbers of the nodes it leads to.
     *
     * @param graph the graph to print
     */
    private static void printAdjacencyList(ControlFlowGraph graph) {
        System.out.println("\nAdjacency List:");
        for (int from = 0; from < graph.getLineCount(); from++) {
            System.out.print("\t" + from + "  ->   ");
            for (int to = 0; to < graph.getLineCount(); to++) {
                if (graph.hasEdge(from, to)) {
                    System.out.print(to + " ");
                }
            }
            System.out.println();
        }
    }

    /**
     * Prints the adjacency matrix, one row per node, with 1 marking an edge and 0 its absence.
     *
     * @param graph the graph to print
     */
    private static void printAdjacencyMatrix(ControlFlowGraph graph) {
        System.out.println("\nAdjacency Matrix:");
        for (int from = 0; from < graph.getLineCount(); from++) {
            System.out.print("\t" + from + "\t");
            for (int to = 0; to < graph.getLineCount(); to++) {
                System.out.print((graph.hasEdge(from, to) ? 1 : 0) + " ");
            }
            System.out.println();
        }
    }

    /**
     * Prints the node and edge counts and the complexity calculated from them as
     * edges - nodes + 2. The node count is the number of lines minus one.
     *
     * <p>The "Edges" and "Verteces" labels are swapped: each shows the other's count.
     *
     * @param graph the graph to measure
     */
    private static void printGraphComplexity(ControlFlowGraph graph) {
        int nodeCount = graph.getLineCount() - 1;
        int edgeCount = graph.countEdges();

        System.out.println("Edges " + nodeCount);
        System.out.println("Verteces " + edgeCount);
        System.out.println("Cyclometic complexity using cfg is " + (edgeCount - nodeCount + 2));
    }
}
