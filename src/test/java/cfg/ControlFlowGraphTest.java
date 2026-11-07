package cfg;

import static cfg.TestSupport.build;
import static cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins down the questions a finished graph answers: edges, edge count and levels. */
class ControlFlowGraphTest {

    private final ControlFlowGraph graph = build(program("sample.c"));

    @Test
    void reportsLineCountAndStatements() {
        assertEquals(17, graph.getLineCount());
        assertEquals("\t    while(k>0){", graph.getStatement(3));
    }

    @Test
    void hasEdgeUsesZeroBasedNodeNumbers() {
        assertTrue(graph.hasEdge(1, 2));
        assertFalse(graph.hasEdge(2, 1));
    }

    @Test
    void countsEveryEdge() {
        assertEquals(19, graph.countEdges());
    }

    @Test
    void countsOnlyTheLinesTheRootCanReach() {
        assertEquals(16, graph.countNodes());
    }

    @Test
    void levelsAreDistancesFromTheRootStartingAtOne() {
        int[] levels = graph.computeLevels();

        assertEquals(1, levels[1]);
        assertEquals(2, levels[2]);
        assertEquals(3, levels[3]);
        assertEquals(4, levels[15]);
    }

    @Test
    void linesTheRootCannotReachGetTheUnreachableLevel() {
        int[] levels = graph.computeLevels();

        assertEquals(GraphData.UNREACHABLE_LEVEL, levels[0]);
        assertEquals(GraphData.MAX_NODES, levels.length);
    }
}
