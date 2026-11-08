package cfg;

import static cfg.TestSupport.build;
import static cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
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
        assertEquals(OptionalInt.of(1), graph.levelOf(1));
        assertEquals(OptionalInt.of(2), graph.levelOf(2));
        assertEquals(OptionalInt.of(3), graph.levelOf(3));
        assertEquals(OptionalInt.of(4), graph.levelOf(15));
    }

    @Test
    void linesTheRootCannotReachHaveNoLevelAndNoSuccessors() {
        assertEquals(OptionalInt.empty(), graph.levelOf(0));
        assertTrue(graph.successorsOf(0).isEmpty());
        assertFalse(graph.reachableNodes().contains(0));
    }

    @Test
    void reachableNodesAreListedInOrder() {
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16), graph.reachableNodes());
    }

    @Test
    void successorsAreListedInOrderWithoutDuplicates() {
        assertEquals(List.of(4, 15), graph.successorsOf(3));
        assertEquals(List.of(5, 7), graph.successorsOf(4));
        assertEquals(List.of(), graph.successorsOf(16));
    }

    @Test
    void queriesCannotChangeTheGraph() {
        assertThrows(UnsupportedOperationException.class, () -> graph.reachableNodes().add(0));
        assertThrows(UnsupportedOperationException.class, () -> graph.successorsOf(3).add(0));
    }
}
