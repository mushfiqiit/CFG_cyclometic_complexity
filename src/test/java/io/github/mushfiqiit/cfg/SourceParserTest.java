package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mushfiqiit.cfg.SourceTree.Block;
import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.Function;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Simple;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks the structure the parser reads from source lines. */
class SourceParserTest {

    /** Describes a tree as indented lines, such as "loop 4" or "close 15", using 1-based lines. */
    private static List<String> outline(Function function) {
        List<String> out = new ArrayList<>(List.of("function " + function.header().number()));
        outline(function.body(), "  ", out);
        return out;
    }

    private static void outline(Block block, String indent, List<String> out) {
        for (Statement statement : block.statements()) {
            if (statement instanceof Simple simple) {
                out.add(indent + "statement " + simple.line().number());
            } else if (statement instanceof Loop loop) {
                out.add(indent + "loop " + loop.header().number());
                outline(loop.body(), indent + "  ", out);
            } else if (statement instanceof IfChain chain) {
                out.add(indent + "chain");
                for (Branch branch : chain.branches()) {
                    out.add(indent + "  branch " + branch.header().number());
                    outline(branch.body(), indent + "    ", out);
                }
            }
        }
        out.add(indent + block.closing().map(line -> "close " + line.number()).orElse("(not closed)"));
    }

    private static Function parse(List<String> lines) {
        return SourceParser.parse(lines).orElseThrow();
    }

    @Test
    void sampleProgramStructure() {
        assertEquals(List.of(
                "function 2",
                "  statement 3",
                "  loop 4",
                "    chain",
                "      branch 5",
                "        statement 6",
                "        close 7",
                "      branch 8",
                "        statement 9",
                "        close 10",
                "      branch 11",
                "        statement 12",
                "        close 13",
                "    statement 14",
                "    close 15",
                "  statement 16",
                "  close 17"), outline(parse(program("sample.c"))));
    }

    @Test
    void blankAndPreambleLinesAreSkipped() {
        Function function = parse(program("typical.c"));

        assertEquals(3, function.header().number());
        assertEquals("int main(void) {", function.header().text());
        assertEquals(List.of(
                "function 3",
                "  statement 4",
                "  loop 5",
                "    statement 6",
                "    close 7",
                "  chain",
                "    branch 8",
                "      statement 9",
                "      close 10",
                "  statement 11",
                "  close 12"), outline(function));
    }

    @Test
    void chainEndsAfterElseOrBeforeAnyOtherLine() {
        assertEquals(List.of(
                "function 1",
                "  chain",
                "    branch 2",
                "      close 3",
                "    branch 5",
                "      close 6",
                "  chain",
                "    branch 7",
                "      close 8",
                "  statement 9",
                "  close 10"), outline(parse(List.of(
                "int main(void) {",
                "    if (a) {",
                "    }",
                "",
                "    else {",
                "    }",
                "    else {",
                "    }",
                "    a = 1;",
                "}"))));
    }

    @Test
    void branchesAndLoopsRecordTheirKeyword() {
        Function function = parse(List.of(
                "int main(void) {",
                "    if (a > 0) {",
                "    }",
                "    else if (a < 0) {",
                "    }",
                "    else {",
                "    }",
                "    for (i = 0; i < 3; i++) {",
                "    }",
                "    while (b != 0) {",
                "    }",
                "}"));
        List<Statement> statements = function.body().statements();

        IfChain chain = (IfChain) statements.get(0);
        assertEquals(List.of(Branch.Kind.IF, Branch.Kind.ELSE_IF, Branch.Kind.ELSE),
                chain.branches().stream().map(Branch::kind).toList());
        assertEquals(Loop.Kind.FOR, ((Loop) statements.get(1)).kind());
        assertEquals(Loop.Kind.WHILE, ((Loop) statements.get(2)).kind());
    }

    @Test
    void elseWithoutIfIsRecordedAsElse() {
        Function function = parse(List.of("int main(void) {", "    else {", "    }", "}"));

        IfChain chain = (IfChain) function.body().statements().get(0);
        assertEquals(Branch.Kind.ELSE, chain.branches().get(0).kind());
    }

    @Test
    void allStatementsVisitsNestedBlocksInLineOrder() {
        Function function = parse(List.of(
                "int main(void) {",
                "    a = 1;",
                "    while (a > 0) {",
                "        if (a > 1) {",
                "            a = 2;",
                "        }",
                "        else {",
                "            a = 3;",
                "        }",
                "    }",
                "    b = 4;",
                "}"));

        List<Integer> firstLines = function.body().allStatements()
                .map(statement -> statement instanceof Simple simple ? simple.line().number()
                        : statement.nestedBlocks().get(0).header().number())
                .toList();
        assertEquals(List.of(2, 3, 4, 5, 8, 11), firstLines);
    }

    @Test
    void nestedBlocksAreTheBranchesOrTheLoopItself() {
        Function function = parse(List.of(
                "int main(void) {", "    if (a) {", "    }", "    else {", "    }", "    for (;;) {", "    }", "    a = 1;", "}"));
        List<Statement> statements = function.body().statements();

        assertEquals(((IfChain) statements.get(0)).branches(), statements.get(0).nestedBlocks());
        assertEquals(List.of(statements.get(1)), statements.get(1).nestedBlocks());
        assertTrue(statements.get(2).nestedBlocks().isEmpty());
    }

    @Test
    void fileThatEndsEarlyLeavesBlocksUnclosed() {
        assertEquals(List.of(
                "function 1",
                "  loop 2",
                "    statement 3",
                "    (not closed)",
                "  (not closed)"), outline(parse(List.of("int main(void) {", "    while (k) {", "        k--;"))));
    }

    @Test
    void linesAfterTheFunctionAreNotRead() {
        Function function = parse(List.of("int main(void) {", "}", "x = 1;"));

        assertTrue(function.body().statements().isEmpty());
        assertEquals(2, function.body().closing().orElseThrow().number());
    }

    @Test
    void indentedHashLinesArePreambleForTheParserAndTheChecker() {
        ParsedSource source = TestSupport.parse(List.of("  #include <stdio.h>", "", "int main(void) {", "    a = 1;", "}"));

        assertEquals(3, source.function().header().number());
        assertTrue(SourceFormatChecker.check(source).isEmpty());
        assertEquals(2, ControlFlowGraph.of(source).entry());
    }

    @Test
    void skipLinesStopsAtTheFirstLineNotSkipped() {
        List<String> lines = List.of("", "  ", "a;", "", "b;");

        assertEquals(2, SourceParser.skipLines(lines, 0, String::isBlank));
        assertEquals(2, SourceParser.skipLines(lines, 2, String::isBlank));
        assertEquals(4, SourceParser.skipLines(lines, 3, String::isBlank));
        assertEquals(5, SourceParser.skipLines(lines, 0, line -> true));
    }

    @Test
    void fileWithoutCodeHasNoFunction() {
        assertTrue(ParsedSource.of(List.of("#include <stdio.h>", "", "// comment")).isEmpty());
        assertTrue(SourceParser.parse(List.of()).isEmpty());
    }

    @Test
    void treeCannotBeChanged() {
        Function function = parse(program("sample.c"));

        assertThrows(UnsupportedOperationException.class, () -> function.body().statements().clear());
    }
}
