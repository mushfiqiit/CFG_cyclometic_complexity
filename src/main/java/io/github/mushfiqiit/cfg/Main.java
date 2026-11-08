package io.github.mushfiqiit.cfg;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point of the program. Reads a C source file, asks the user which analysis to run, and
 * runs it.
 *
 * <p>Usage: {@code CFG_cyclometic_complexity [source-file]}. Without an argument, the program
 * reads {@code test.txt} from the working directory.
 */
public final class Main {

    private static final String DEFAULT_SOURCE_FILE = "test.txt";
    /** Menu option that lists the test case data: variables and conditions. */
    private static final int VARIABLE_CONDITION_OPTION = 2;

    private static final int EXIT_UNREADABLE_FILE = 1;
    private static final int EXIT_USAGE_ERROR = 2;
    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage: CFG_cyclometic_complexity [source-file]",
            "",
            "Builds the control flow graph of a C source file or lists its variables and conditions.",
            "  source-file  path of the C file to analyze (default: " + DEFAULT_SOURCE_FILE + ")",
            "  -h, --help   show this message and exit");

    /** Prevents instantiation; this class only holds the entry point. */
    private Main() {
    }

    // ***************************************************************************
    // Running the program
    // ***************************************************************************

    /**
     * Reads the source file named on the command line, or {@code test.txt} if none is given, then
     * reads a menu option from standard input. Option 2 lists variables and conditions; any other number
     * builds the control flow graph.
     *
     * <p>Exits with status 2 for more than one argument or an unknown option, and with status 1 if
     * the source file cannot be opened.
     *
     * @param args at most one argument: the path of the C source file, or {@code -h} /
     *        {@code --help}
     * @throws java.util.InputMismatchException if the option entered is not a whole number
     */
    public static void main(String[] args) {
        if (isHelpRequest(args)) {
            System.out.println(USAGE);
            return;
        }
        if (args.length > 1) {
            exitWithError("Error: expected at most one source file, but got " + args.length + " arguments.",
                    EXIT_USAGE_ERROR, true);
        }
        if (args.length == 1 && isOption(args[0])) {
            exitWithError("Error: unknown option: " + args[0], EXIT_USAGE_ERROR, true);
        }
        String sourceFile = args.length == 1 ? args[0] : DEFAULT_SOURCE_FILE;

        System.out.println("Control Flow Graph Analyzer");
        List<String> lines = readSourceFile(sourceFile);
        System.out.println("Source file: " + sourceFile + " (" + lines.size() + " lines)");

        printMenu();
        int option = new Scanner(System.in).nextInt();

        if (option == VARIABLE_CONDITION_OPTION) {
            VariableConditionAnalysis.run(lines);
        } else {
            ComplexityAnalysis.run(lines);
        }
    }

    // ***************************************************************************
    // Handling the command line
    // ***************************************************************************

    /** Prints the analysis menu and the prompt for the user's choice. */
    private static void printMenu() {
        System.out.println();
        System.out.println("Choose an analysis:");
        System.out.println("  1. Cyclomatic complexity and control flow graph");
        System.out.println("  2. Test case data (variables and conditions)");
        System.out.print("Enter 1 or 2: ");
        System.out.flush();
    }

    /**
     * Tells whether the only argument asks for the usage message.
     *
     * @param args the command-line arguments
     * @return true if the arguments are exactly {@code -h} or {@code --help}
     */
    private static boolean isHelpRequest(String[] args) {
        return args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"));
    }

    /**
     * Tells whether an argument looks like an option rather than a file path.
     *
     * @param argument a command-line argument
     * @return true if the argument starts with {@code -}
     */
    private static boolean isOption(String argument) {
        return argument.startsWith("-");
    }

    /**
     * Reads the source file, or exits with an error message if it cannot be opened.
     *
     * @param sourceFile path of the C source file
     * @return the file's lines
     */
    private static List<String> readSourceFile(String sourceFile) {
        try {
            return SourceCodeReader.readLines(sourceFile);
        } catch (FileNotFoundException e) {
            exitWithError("Error: cannot open source file: " + e.getMessage(), EXIT_UNREADABLE_FILE, false);
            throw new AssertionError("unreachable");
        }
    }

    /**
     * Prints an error message to standard error and exits.
     *
     * @param message the error to print
     * @param status the exit status
     * @param showUsage whether to print the usage message after the error
     */
    private static void exitWithError(String message, int status, boolean showUsage) {
        System.err.println(message);
        if (showUsage) {
            System.err.println(USAGE);
        }
        System.exit(status);
    }
}
