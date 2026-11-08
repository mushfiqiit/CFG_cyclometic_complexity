package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Checks what each combination of command-line arguments asks for. */
class CommandLineTest {

    @Test
    void helpOptionsAskForHelp() {
        assertEquals(new CommandLine.ShowHelp(), CommandLine.parse(new String[] {"-h"}));
        assertEquals(new CommandLine.ShowHelp(), CommandLine.parse(new String[] {"--help"}));
    }

    @Test
    void noArgumentAnalyzesTheDefaultFile() {
        assertEquals(new CommandLine.Analyze("test.txt"), CommandLine.parse(new String[0]));
    }

    @Test
    void oneFileArgumentAnalyzesThatFile() {
        assertEquals(new CommandLine.Analyze("my code/program.c"), CommandLine.parse(new String[] {"my code/program.c"}));
    }

    @Test
    void unknownOptionsAndExtraArgumentsAreInvalid() {
        assertEquals(new CommandLine.Invalid("Error: unknown option: --verbose"),
                CommandLine.parse(new String[] {"--verbose"}));
        assertEquals(new CommandLine.Invalid("Error: expected at most one source file, but got 2 arguments."),
                CommandLine.parse(new String[] {"a.c", "b.c"}));
        assertEquals(new CommandLine.Invalid("Error: expected at most one source file, but got 2 arguments."),
                CommandLine.parse(new String[] {"--help", "a.c"}));
    }
}
