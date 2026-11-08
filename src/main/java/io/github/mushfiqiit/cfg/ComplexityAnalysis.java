package io.github.mushfiqiit.cfg;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs the cyclomatic complexity option: builds the control flow graph, prints it and both
 * complexity results, then shows the graph.
 */
public final class ComplexityAnalysis {

    /** Prevents instantiation; this class only has static methods. */
    private ComplexityAnalysis() {
    }

    /**
     * Runs the analysis on a source file, prints the report, then hands the graph to
     * {@code showGraph}. The program passes {@link GraphViewer#show(ControlFlowGraph)}, which
     * opens a window and blocks until it is closed.
     *
     * @param lines the source lines to analyze, with comments removed
     * @param out where to print the report
     * @param showGraph what to do with the finished graph
     */
    public static void run(List<String> lines, PrintStream out, Consumer<ControlFlowGraph> showGraph) {
        GraphPrinter printer = new GraphPrinter(out);
        ConsoleText.printTitle(out, "Cyclomatic Complexity");
        ControlFlowGraphBuilder builder = ControlFlowGraphBuilder.from(lines);
        printer.printEntryPoint(builder.root());

        ControlFlowGraph graph = builder.build();
        printer.printGraph(graph);
        printer.printComplexity(graph, ComplexityCalculator.countDecisions(lines));

        out.println();
        out.println("Opening the graph window. Close it to exit.");
        showGraph.accept(graph);
    }
}
