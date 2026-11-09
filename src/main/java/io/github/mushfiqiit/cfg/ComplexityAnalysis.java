package io.github.mushfiqiit.cfg;

/**
 * The result of the cyclomatic complexity option: the control flow graph and the decision points,
 * which give the complexity in two independent ways. Printing is left to {@link GraphPrinter},
 * so the result can be shown or checked in any other way too.
 *
 * @param graph the function's control flow graph
 * @param decisions the decision points counted in the source
 */
record ComplexityAnalysis(ControlFlowGraph graph, DecisionCounts decisions) {

    /**
     * Builds the graph and counts the decision points of a source file.
     *
     * @param source the source to analyze
     * @return the analysis
     */
    static ComplexityAnalysis of(ParsedSource source) {
        ControlFlowGraph graph = ControlFlowGraph.of(source);
        return new ComplexityAnalysis(graph, ComplexityCalculator.countDecisions(source));
    }

    /**
     * Returns the complexity worked out from the graph.
     *
     * @return {@code M = E - N + 2}
     */
    int fromGraph() {
        return graph.cyclomaticComplexity();
    }

    /**
     * Returns the complexity worked out from the decision points.
     *
     * @return {@code M = D + 1}
     */
    int fromDecisions() {
        return decisions.complexity();
    }

    /**
     * Tells whether both ways give the same complexity, as they do for well-structured code.
     *
     * @return {@code true} if the graph and the decision points agree
     */
    boolean resultsAgree() {
        return fromGraph() == fromDecisions();
    }
}
