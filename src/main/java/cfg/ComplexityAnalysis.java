package cfg;

import java.util.List;

/**
 * Runs the cyclomatic complexity option: builds the control flow graph, prints it and both
 * complexity results, then shows the graph in a window.
 */
public final class ComplexityAnalysis {

    /** Prevents instantiation; this class only has static methods. */
    private ComplexityAnalysis() {
    }

    /**
     * Runs the analysis on a source file, then opens the graph window and blocks until it is
     * closed.
     *
     * <p>Files longer than {@link GraphData#MAX_NODES} lines make this method throw a runtime
     * exception.
     *
     * @param lines the source lines to analyze
     */
    public static void run(List<String> lines) {
        ConsoleText.printTitle("Cyclomatic Complexity");
        ControlFlowGraphBuilder builder = ControlFlowGraphBuilder.from(lines);
        GraphPrinter.printEntryPoint(builder.root());

        ControlFlowGraph graph = builder.build();
        GraphPrinter.printGraph(graph);
        GraphPrinter.printComplexity(graph, ComplexityCalculator.countDecisions(lines));

        System.out.println();
        System.out.println("Opening the graph window. Close it to exit.");
        GraphViewer.show(graph);
    }
}
