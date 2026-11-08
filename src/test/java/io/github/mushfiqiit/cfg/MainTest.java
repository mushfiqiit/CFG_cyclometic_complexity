package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.programPath;
import static io.github.mushfiqiit.cfg.TestSupport.runProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mushfiqiit.cfg.TestSupport.ProgramRun;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the program end to end through {@link Main#run(String[])}, including its error paths. */
class MainTest {

    @TempDir
    Path folder;

    private String file(String name, String... lines) throws IOException {
        Path path = folder.resolve(name);
        Files.writeString(path, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return path.toString();
    }

    // ***************************************************************************
    // Command line
    // ***************************************************************************

    @Test
    void helpPrintsUsageAndExitStatuses() {
        ProgramRun run = runProgram("", "--help");

        assertEquals(Main.EXIT_SUCCESS, run.status());
        assertTrue(run.output().startsWith("Usage: CFG_cyclometic_complexity [source-file]\n"));
        assertTrue(run.output().contains("Exit status: 0 on success"));
    }

    @Test
    void moreThanOneArgumentIsAUsageError() {
        ProgramRun run = runProgram("", "a.c", "b.c");

        assertEquals(Main.EXIT_USAGE_ERROR, run.status());
        assertTrue(run.errors().startsWith("Error: expected at most one source file, but got 2 arguments.\n"));
    }

    @Test
    void unknownOptionIsAUsageError() {
        ProgramRun run = runProgram("", "--verbose");

        assertEquals(Main.EXIT_USAGE_ERROR, run.status());
        assertTrue(run.errors().startsWith("Error: unknown option: --verbose\n"));
    }

    @Test
    void missingFileIsReportedWithoutAStackTrace() {
        ProgramRun run = runProgram("", folder.resolve("nope.c").toString());

        assertEquals(Main.EXIT_INPUT_ERROR, run.status());
        assertTrue(run.errors().startsWith("Error: cannot open source file: "));
        assertTrue(run.errors().contains("nope.c (No such file or directory)"), run.errors());
        assertFalse(run.errors().contains("Exception"));
    }

    // ***************************************************************************
    // Unsupported files
    // ***************************************************************************

    @Test
    void emptyFileIsReportedWithoutAStackTrace() throws IOException {
        Path empty = Files.createFile(folder.resolve("empty.c"));

        ProgramRun run = runProgram("1\n", empty.toString());

        assertEquals(Main.EXIT_INPUT_ERROR, run.status());
        assertTrue(run.errors().contains("line 1: there is no code to analyze"), run.errors());
        assertFalse(run.errors().contains("Exception"));
    }

    @Test
    void unsupportedLayoutIsRejectedBeforeTheMenuWithLineNumbers() throws IOException {
        String path = file("oneline.c",
                "int main(void) {",
                "    if (a) { b = 1; } else { b = 2; }",
                "    return 0;",
                "}");

        ProgramRun run = runProgram("1\n", path);

        assertEquals(Main.EXIT_INPUT_ERROR, run.status());
        assertFalse(run.output().contains("Choose an analysis"));
        assertTrue(run.errors().contains("cannot be analyzed, because it uses a layout this program does not support:\n"
                + "  line 2: put each } on a line of its own"), run.errors());
        assertTrue(run.errors().contains("Supported layout: one function, one statement per line"));
    }

    @Test
    void atMostTenProblemsAreListed() throws IOException {
        String[] lines = new String[14];
        lines[0] = "int main(void) {";
        for (int i = 1; i <= 12; i++) {
            lines[i] = "    if (a)";
        }
        lines[13] = "}";

        ProgramRun run = runProgram("1\n", file("many.c", lines));

        assertEquals(10, run.errors().lines().filter(line -> line.startsWith("  line ")).count());
        assertTrue(run.errors().contains("  ... and 2 more\n"));
    }

    // ***************************************************************************
    // Menu
    // ***************************************************************************

    @Test
    void optionTwoAnalyzesTheFileNamedOnTheCommandLine() {
        String path = programPath("sample.c").toString();

        ProgramRun run = runProgram("2\n", path);

        assertEquals(Main.EXIT_SUCCESS, run.status());
        assertTrue(run.output().startsWith(String.join("\n",
                "Control Flow Graph Analyzer",
                "Source file: " + path + " (17 lines)",
                "",
                "Choose an analysis:",
                "  1. Cyclomatic complexity and control flow graph",
                "  2. Test case data (variables and conditions)",
                "Enter 1 or 2: ",
                "Test Case Data",
                "==============")), run.output());
        assertTrue(run.output().contains("  4    11  a>c        >         a, c\n"));
        assertEquals("", run.errors());
    }

    @Test
    void invalidAnswersAreAskedAgain() {
        ProgramRun run = runProgram("x\n7\n\n 2 \n", programPath("sample.c").toString());

        assertEquals(Main.EXIT_SUCCESS, run.status());
        assertEquals(3, run.output().split("Please enter 1 or 2\\.", -1).length - 1);
        assertTrue(run.output().contains("Test Case Data"));
    }

    @Test
    void endOfInputWithoutAnAnswerIsAnError() {
        ProgramRun run = runProgram("7\n", programPath("sample.c").toString());

        assertEquals(Main.EXIT_INPUT_ERROR, run.status());
        assertEquals("Error: no analysis was chosen.\n", run.errors());
        assertFalse(run.output().contains("Test Case Data"));
    }

    @Test
    void commentsAreIgnored() throws IOException {
        String path = file("comments.c",
                "/* A program",
                "   with { and } in a comment */",
                "#include <stdio.h>",
                "int main(void) {   // entry",
                "    int a = 1; /* one */",
                "    if (a > 0) { // positive",
                "        a--;",
                "    } // end if",
                "    return 0;",
                "}");

        ProgramRun run = runProgram("2\n", path);

        assertEquals(Main.EXIT_SUCCESS, run.status(), run.errors());
        assertTrue(run.output().contains("  1     6  a > 0      >         a, 0\n"), run.output());
    }
}
