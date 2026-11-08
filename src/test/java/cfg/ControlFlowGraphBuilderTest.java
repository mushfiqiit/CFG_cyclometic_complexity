package cfg;

import static cfg.TestSupport.build;
import static cfg.TestSupport.edgeSet;
import static cfg.TestSupport.edges;
import static cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks how source lines are linked into a control flow graph. Edges use 1-based lines. */
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
    void blankLineInThePreambleIsSkipped() {
        ControlFlowGraphBuilder builder = assertDoesNotThrow(() -> ControlFlowGraphBuilder.from(program("typical.c")));

        assertEquals("int main(void) {", builder.root().getStatement());
    }

    @Test
    void blankLinesInsideAFunctionAreSkipped() {
        ControlFlowGraph graph = build(List.of(
                "\tint main(){",
                "    int a;",
                "",
                "    return 0;",
                "",
                "}"));

        assertEquals(edgeSet("1->2", "2->4", "4->6"), edges(graph));
    }

    @Test
    void sampleProgramHasTheExpectedEdges() {
        ControlFlowGraph graph = build(program("sample.c"));

        assertEquals(edgeSet(
                "2->3", "3->4",
                "4->5", "4->16",
                "5->6", "6->7", "7->14", "5->8",
                "8->9", "9->10", "10->14", "8->11",
                "11->12", "12->13", "13->14", "11->14",
                "14->15", "15->4",
                "16->17"), edges(graph));
    }

    @Test
    void elseBranchStartsAtTheIfAndBothBranchesLeadToTheNextStatement() {
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

        assertEquals(edgeSet("1->2", "2->3", "3->4", "4->8", "2->5", "5->6", "6->7", "7->8", "8->9"),
                edges(graph));
    }

    @Test
    void ifWithoutElseAlsoLeadsToTheNextStatement() {
        ControlFlowGraph graph = build(List.of(
                "\tint main(){",
                "    if(b<2){",
                "        b=1;",
                "    }",
                "    return 0;",
                "}"));

        assertEquals(edgeSet("1->2", "2->3", "3->4", "4->5", "2->5", "5->6"), edges(graph));
    }

    @Test
    void statementAfterIfElseIsReachedFromEveryBranch() {
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

        assertEquals(edgeSet("1->2", "2->3", "3->4", "4->8", "2->5", "5->6", "6->7", "7->8",
                "8->9", "9->10", "10->11", "8->11"), edges(graph));
    }

    @Test
    void eachElseIfHangsOffTheConditionBeforeIt() {
        ControlFlowGraph graph = build(List.of(
                "\tint main(){",
                "    if(a){",
                "    }",
                "    else if(b){",
                "    }",
                "    else{",
                "    }",
                "}"));

        assertEquals(edgeSet("1->2", "2->3", "2->4", "4->5", "4->6", "6->7", "3->8", "5->8", "7->8"),
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

    @Test
    void wordStartingWithElseIsAnOrdinaryStatement() {
        ControlFlowGraph graph = build(List.of(
                "\tint main(){",
                "    if(a){",
                "        a=1;",
                "    }",
                "    elsewhere=1;",
                "}"));

        assertEquals(edgeSet("1->2", "2->3", "3->4", "4->5", "2->5", "5->6"), edges(graph));
    }

    @Test
    void fileThatEndsBeforeTheBlockClosesDoesNotCrash() {
        ControlFlowGraph graph = build(List.of("\tint main(){", "    int a;", "    a=1;"));

        assertEquals(edgeSet("1->2", "2->3"), edges(graph));
    }

    @Test
    void buildCanOnlyBeCalledOnce() {
        ControlFlowGraphBuilder builder = ControlFlowGraphBuilder.from(program("sample.c"));
        builder.build();

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void longProgramsAreSupported() {
        List<String> lines = new ArrayList<>(List.of("\tint main(){"));
        for (int i = 0; i < 500; i++) {
            lines.add("    if(a>" + i + "){");
            lines.add("        a=" + i + ";");
            lines.add("    }");
        }
        lines.add("}");

        ControlFlowGraph graph = build(lines);

        assertEquals(lines.size(), graph.countNodes());
        assertEquals(501, graph.countEdges() - graph.countNodes() + 2);
    }
}
