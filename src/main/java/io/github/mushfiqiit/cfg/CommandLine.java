package io.github.mushfiqiit.cfg;

/**
 * What the command-line arguments ask for: the usage message, an analysis of a source file, or
 * nothing valid. Create one with {@link #parse(String[])}.
 */
sealed interface CommandLine permits CommandLine.ShowHelp, CommandLine.Analyze, CommandLine.Invalid {

    /** The file analyzed when no source file is given. */
    String DEFAULT_SOURCE_FILE = "test.txt";

    /** The usage message, with the exit statuses. */
    String USAGE = String.join(System.lineSeparator(),
            "Usage: CFG_cyclometic_complexity [source-file]",
            "",
            "Builds the control flow graph of a C source file or lists its variables and conditions.",
            "  source-file  path of the C file to analyze (default: " + DEFAULT_SOURCE_FILE + ")",
            "  -h, --help   show this message and exit",
            "",
            "Exit status: 0 on success, 1 if the file cannot be read or analyzed or no analysis",
            "is chosen, 2 if the arguments are invalid.");

    /** The arguments ask for the usage message. */
    record ShowHelp() implements CommandLine {
    }

    /**
     * The arguments ask for a source file to be analyzed.
     *
     * @param sourceFile the file's path, as given, or {@link #DEFAULT_SOURCE_FILE}
     */
    record Analyze(String sourceFile) implements CommandLine {
    }

    /**
     * The arguments are not valid.
     *
     * @param message what is wrong, starting with {@code "Error: "}
     */
    record Invalid(String message) implements CommandLine {
    }

    // ***************************************************************************
    // Reading the arguments
    // ***************************************************************************

    /**
     * Reads the command-line arguments. Exactly {@code -h} or {@code --help} asks for help; no
     * argument analyzes {@link #DEFAULT_SOURCE_FILE}; one argument that does not start with
     * {@code -} names the file to analyze. Anything else is invalid.
     *
     * @param args the command-line arguments
     * @return what the arguments ask for
     */
    static CommandLine parse(String[] args) {
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            return new ShowHelp();
        }
        if (args.length > 1) {
            return new Invalid("Error: expected at most one source file, but got " + args.length + " arguments.");
        }
        if (args.length == 1 && args[0].startsWith("-")) {
            return new Invalid("Error: unknown option: " + args[0]);
        }
        return new Analyze(args.length == 1 ? args[0] : DEFAULT_SOURCE_FILE);
    }
}
