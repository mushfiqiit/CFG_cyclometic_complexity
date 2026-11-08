package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.programPath;
import static io.github.mushfiqiit.cfg.TestSupport.runProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mushfiqiit.cfg.TestSupport.ProgramRun;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
    void optionOnePrintsTheReportAndThenShowsTheGraph() {
        ProgramRun run = runProgram("1\n", programPath("sample.c").toString());

        assertEquals(Main.EXIT_SUCCESS, run.status());
        assertTrue(run.output().contains("Cyclomatic Complexity\n=====================\nEntry point: line 2: int main(){\n"));
        assertTrue(run.output().contains("M = E - N + 2 = 19 - 16 + 2 = 5"));
        assertTrue(run.output().endsWith("Opening the graph window. Close it to exit.\n"));
        assertEquals(16, run.shownGraph().countNodes());
        assertEquals("", run.errors());
    }

    @Test
    void optionTwoShowsNoGraph() {
        assertNull(runProgram("2\n", programPath("sample.c").toString()).shownGraph());
    }

    @Test
    void errorsGoToTheErrorStreamOnly() {
        ProgramRun run = runProgram("", folder.resolve("nope.c").toString());

        assertFalse(run.output().contains("Error"));
        assertTrue(run.errors().startsWith("Error: cannot open source file: "));
        assertNull(run.shownGraph());
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
    void failureToReadTheMenuAnswerIsReportedWithItsCause() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("input closed");
            }
        };

        int status = Main.run(new String[] {programPath("sample.c").toString()}, broken,
                new PrintStream(output, true, StandardCharsets.UTF_8), new PrintStream(errors, true, StandardCharsets.UTF_8),
                graph -> { });

        assertEquals(Main.EXIT_INPUT_ERROR, status);
        assertEquals("Error: cannot read the menu choice: input closed", errors.toString(StandardCharsets.UTF_8).strip());
    }

    @Test
    void fileThatIsNotUtf8IsReportedInsteadOfLookingEmpty() throws IOException {
        Path latin1 = folder.resolve("latin1.c");
        Files.write(latin1, "int main(void) {\n    printf(\"caf\u00e9\");\n}\n".getBytes(StandardCharsets.ISO_8859_1));

        ProgramRun run = runProgram("2\n", latin1.toString());

        assertEquals(Main.EXIT_INPUT_ERROR, run.status());
        assertEquals("Error: cannot read source file: " + latin1 + " is not valid UTF-8 text\n", run.errors());
    }

    @Test
    void folderIsReportedWithoutAStackTrace() {
        ProgramRun run = runProgram("2\n", folder.toString());

        assertEquals(Main.EXIT_INPUT_ERROR, run.status());
        assertTrue(run.errors().startsWith("Error: cannot open source file: " + folder + " ("), run.errors());
        assertFalse(run.errors().contains("Exception"));
    }

    @Test
    void numbersTooLargeForAnIntAreReportedInsteadOfCrashing() throws IOException {
        String path = file("big.c",
                "int main(void) {",
                "    int big = 99999999999;",
                "    int a;",
                "    if (a > big) {",
                "    }",
                "    if (a > 99999999999) {",
                "    }",
                "}");

        ProgramRun run = runProgram("2\n", path);

        assertEquals(Main.EXIT_SUCCESS, run.status(), run.errors());
        assertTrue(run.output().contains("  big       computed in the code\n"), run.output());
        assertTrue(run.output().contains("no values: depends on big, which is computed in the code"));
        assertTrue(run.output().contains("no values: the condition is too complex to solve"));
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
