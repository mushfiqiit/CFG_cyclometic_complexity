package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks which layouts are accepted and how each unsupported layout is reported. */
class SourceFormatCheckerTest {

    private static List<String> problems(String... lines) {
        return SourceFormatChecker.check(ParsedSource.of(CommentStripper.strip(List.of(lines)))).stream()
                .map(SourceFormatChecker.Problem::toString).toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sample.c", "typical.c", "two-preamble-lines.c"})
    void testProgramsAreSupported(String name) {
        assertEquals(List.of(), SourceFormatChecker.check(ParsedSource.of(CommentStripper.strip(program(name)))));
    }

    @Test
    void initializerListsAndBracesInStringsAreOrdinaryStatements() {
        assertEquals(List.of(), problems(
                "int main(void) {",
                "    int a[3] = {1, 2, 3};",
                "    printf(\"{ and }\");",
                "    char c = '}';",
                "}"));
    }

    // ***************************************************************************
    // Problems within a line
    // ***************************************************************************

    @Test
    void reportsBlocksOnOneLine() {
        assertEquals(List.of("line 2: put each } on a line of its own, and start the next block on a new line,"
                + " as in:  }  then  else {"), problems(
                "int main(void) {",
                "    if (a) { b = 1; } else { b = 2; }",
                "}"));
    }

    @Test
    void reportsCloseBraceSharedWithElse() {
        assertTrue(problems(
                "int main(void) {",
                "    if (a) {",
                "        b = 1;",
                "    } else {",
                "        b = 2;",
                "    }",
                "}").get(0).startsWith("line 4: put each } on a line of its own"));
    }

    @Test
    void reportsControlStatementsWithoutBraces() {
        assertEquals(List.of(
                "line 2: end this line with {; an if, else, for or while without braces is not supported",
                "line 4: end this line with {; an if, else, for or while without braces is not supported"), problems(
                "int main(void) {",
                "    if (x)",
                "        y = 1;",
                "    while (k-- > 0);",
                "}"));
    }

    @Test
    void reportsAnOpeningBraceOnItsOwnLine() {
        assertEquals(List.of("line 3: put { at the end of the if, else, for or while line above, not on a line of its own"),
                problems(
                "int main(void) {",
                "    while (k > 0)",
                "    {",
                "        k--;",
                "    }",
                "}").subList(1, 2));
    }

    @Test
    void reportsUnsupportedBlocksAndExtraFunctions() {
        assertEquals(List.of(
                "line 2: only if, else if, else, for and while blocks are supported, not do, switch or others",
                "line 4: only if, else if, else, for and while blocks are supported, not do, switch or others",
                "line 7: only one function can be analyzed; put each function in a file of its own"), problems(
                "int main(void) {",
                "    switch (k) {",
                "    }",
                "    do {",
                "    }",
                "}",
                "int helper(void) {",
                "}"));
    }

    // ***************************************************************************
    // Problems with the overall structure
    // ***************************************************************************

    @Test
    void reportsAFileWithoutCode() {
        assertEquals(List.of("line 2: there is no code to analyze; the file needs a function such as \"int main(void) {\""),
                problems("#include <stdio.h>", "// nothing else"));
        assertEquals(List.of("line 1: there is no code to analyze; the file needs a function such as \"int main(void) {\""),
                problems());
    }

    @ParameterizedTest
    @ValueSource(strings = {"int main(void) {", "int main() {", "static int count_items(int *items) {", "char *name(void) {"})
    void acceptsCommonFunctionHeaders(String header) {
        assertEquals(List.of(), problems(header, "    return 0;", "}"));
    }

    @Test
    void reportsCodeBeforeTheFunction() {
        assertTrue(problems("int counter = 0;", "int main(void) {", "}").get(0)
                .startsWith("line 1: expected the function to analyze"));
    }

    @Test
    void reportsElseWithoutIf() {
        assertEquals(List.of("line 5: this else has no matching if; it must come right after the } that closes an if or else if"),
                problems(
                "int main(void) {",
                "    while (k > 0) {",
                "        k--;",
                "    }",
                "    else {",
                "    }",
                "}"));
    }

    /** Braces match from the inside out, so the last } closes the for loop and the function stays open. */
    @Test
    void reportsABlockThatIsNeverClosed() {
        assertEquals(List.of("line 1: this block is never closed; add a } on its own line"), problems(
                "int main(void) {",
                "    for (int i = 0; i < 3; i++) {",
                "        a = i;",
                "}"));
    }

    @Test
    void reportsCodeAfterTheFunction() {
        assertEquals(List.of("line 4: only one function can be analyzed, and this line comes after the function"
                + " that ends on line 2"), problems(
                "int main(void) {",
                "}",
                "",
                "x = 1;"));
    }
}
