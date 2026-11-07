package cfg;

import java.io.IOException;
import java.util.List;

/**
 * Runs the cyclomatic complexity option: builds the control flow graph, saves and prints it,
 * prints both complexity results, then shows the graph in a window.
 */
public final class ComplexityAnalysis {

    /** Prevents instantiation; this class only has static methods. */
    private ComplexityAnalysis() {
    }

    /**
     * Runs the analysis on a source file, then opens the graph window and blocks until it is
     * closed.
     *
     * <p>Blank lines and files longer than {@link GraphData#MAX_NODES} lines make this method
     * throw a runtime exception.
     *
     * @param lines the source lines to analyze
     * @throws IOException if either graph file cannot be written
     */
    public static void run(List<String> lines) throws IOException {
        ControlFlowGraphBuilder builder = ControlFlowGraphBuilder.from(lines);
        GraphPrinter.printRoot(builder.root());

        ControlFlowGraph graph = builder.build();
        GraphFileWriter.saveLevels(graph);
        GraphPrinter.printGraph(graph);
        GraphFileWriter.saveEdges(graph);

        System.out.println("Cyclometic complexity is " + ComplexityCalculator.calculate(lines));
        GraphViewer.show();
    }
}
