package cfg;

import static cfg.TestSupport.build;
import static cfg.TestSupport.edgeSet;
import static cfg.TestSupport.edges;
import static cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins down how source lines are linked into a control flow graph. Edges use 1-based lines. */
class ControlFlowGraphBuilderTest {

    @Test
    void rootIsTheFirstLineAfterThePreamble() {
        ControlFlowGraphBuilder builder = ControlFlowGraphBuilder.from(program("sample.c"));

        assertEquals(1, builder.root().getNumber());
        assertEquals("\tint main(){", builder.root().getStatement());
    }

    @Test
    void linesStartingWithHashOrSlashArePreamble() {
        ControlFlowGraphBuilder builder = ControlFlowGraphBuilder.from(program("two-preamble-lines.c"));

        assertEquals(2, builder.root().getNumber());
    }

    @Test
    void sampleProgramHasTheExpectedEdges() {
        ControlFlowGraph graph = build(program("sample.c"));

        assertEquals(edgeSet(
                "2->3", "3->4",
                "4->5", "4->8", "4->11", "4->16",
                "5->6", "6->7", "7->14",
                "8->9", "9->10", "10->14",
                "11->12", "12->13", "13->14",
                "14->15", "15->4",
                "16->17"), edges(graph));
    }

    /**
     * Both branches lead to the statement after the if/else. Known bug, pinned as current
     * behavior: the {@code else} branch is linked from the line before the {@code if} (1->5), not
     * from the {@code if} itself (2->5). Update this test when the bug is fixed.
     */
    @Test
    void bothBranchesOfIfElseLeadToTheNextStatement() {
        ControlFlowGraph graph = build(List.of(
                "\tint main(){",
                "    if(a>b){",
                "        a=1;",
                "    }",
                "    else{",
                "        b=2;",
                "    }",
                "    a=3;",
                "}"));

        assertEquals(edgeSet("1->2", "2->3", "3->4", "4->8", "1->5", "5->6", "6->7", "7->8", "8->9"),
                edges(graph));
    }

    @Test
    void lastLineOfALoopBodyLeadsBackToTheLoop() {
        ControlFlowGraph graph = build(List.of(
                "\tint main(){",
                "    while(k>0){",
                "        k--;",
                "    }",
                "    return 0;",
                "}"));

        assertEquals(edgeSet("1->2", "2->3", "3->4", "4->2", "2->5", "5->6"), edges(graph));
    }

    /**
     * Known bug, pinned as current behavior: after an if/else, a following {@code if} is linked
     * from the first branch only. Update this test when the bug is fixed.
     */
    @Test
    void ifAfterIfElseIsLinkedFromTheFirstBranchOnly() {
        ControlFlowGraph graph = build(List.of(
                "\tint main(){",
                "    if(a>1){",
                "        a=1;",
                "    }",
                "    else{",
                "        a=2;",
                "    }",
                "    if(b<2){",
                "        b=1;",
                "    }",
                "}"));

        assertEquals(edgeSet("1->2", "2->3", "3->4", "4->8", "1->5", "5->6", "6->7",
                "8->9", "9->10", "10->11"), edges(graph));
    }

    @Test
    void programLongerThanTheNodeLimitThrows() {
        List<String> lines = new ArrayList<>(List.of("\tint main(){"));
        for (int i = 0; i < GraphData.MAX_NODES; i++) {
            lines.add("    a=" + i + ";");
        }
        lines.add("}");

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> build(lines));
    }
}
