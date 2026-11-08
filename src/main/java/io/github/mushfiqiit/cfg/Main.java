package io.github.mushfiqiit.cfg;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Entry point of the program. Reads a C source file, checks that it uses the supported layout,
 * asks the user which analysis to run, and runs it.
 *
 * <p>Usage: {@code CFG_cyclometic_complexity [source-file]}. Without an argument, the program
 * reads {@code test.txt} from the working directory.
 */
public final class Main {

    /** Exit status when the program finished normally. */
    static final int EXIT_SUCCESS = 0;

    /** Exit status when the source file cannot be read or analyzed, or no analysis was chosen. */
    static final int EXIT_INPUT_ERROR = 1;

    /** Exit status when the command-line arguments are invalid. */
    static final int EXIT_USAGE_ERROR = 2;

    private static final String DEFAULT_SOURCE_FILE = "test.txt";
    private static final String COMPLEXITY_OPTION = "1";
    private static final String VARIABLE_CONDITION_OPTION = "2";
    private static final int MAX_PROBLEMS_SHOWN = 10;
    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage: CFG_cyclometic_complexity [source-file]",
            "",
            "Builds the control flow graph of a C source file or lists its variables and conditions.",
            "  source-file  path of the C file to analyze (default: " + DEFAULT_SOURCE_FILE + ")",
            "  -h, --help   show this message and exit",
            "",
            "Exit status: 0 on success, 1 if the file cannot be read or analyzed or no analysis",
            "is chosen, 2 if the arguments are invalid.");

    /** Prevents instantiation; this class only holds the entry point. */
    private Main() {
    }

    // ***************************************************************************
    // Running the program
    // ***************************************************************************

    /**
     * Runs the program on the console, shows the graph in a window, and exits with a non-zero
     * status if it fails. This is the only place that uses the console streams and
     * {@link System#exit(int)}; see {@link #run(String[], InputStream, PrintStream, PrintStream, Consumer)}.
     *
     * @param args at most one argument: the path of the C source file, or {@code -h} /
     *        {@code --help}
     */
    public static void main(String[] args) {
        int status = run(args, System.in, System.out, System.err, GraphViewer::show);
        if (status != EXIT_SUCCESS) {
            System.exit(status);
        }
    }

    /**
     * Reads the source file named on the command line, or {@code test.txt} if none is given, and
     * checks its layout. Then asks which analysis to run, until the answer is 1 or 2, and runs it.
     *
     * @param args at most one argument: the path of the C source file, or {@code -h} /
     *        {@code --help}
     * @param in where to read the menu choice from
     * @param out where to print the reports
     * @param err where to print error messages
     * @param showGraph what to do with the graph after the complexity report
     * @return {@link #EXIT_SUCCESS}, {@link #EXIT_INPUT_ERROR} or {@link #EXIT_USAGE_ERROR}
     */
    static int run(String[] args, InputStream in, PrintStream out, PrintStream err,
            Consumer<ControlFlowGraph> showGraph) {
        if (isHelpRequest(args)) {
            out.println(USAGE);
            return EXIT_SUCCESS;
        }
        if (args.length > 1) {
            return usageError(err, "Error: expected at most one source file, but got " + args.length + " arguments.");
        }
        if (args.length == 1 && isOption(args[0])) {
            return usageError(err, "Error: unknown option: " + args[0]);
        }
        String sourceFile = args.length == 1 ? args[0] : DEFAULT_SOURCE_FILE;

        out.println("Control Flow Graph Analyzer");
        List<String> lines;
        try {
            lines = SourceCodeReader.readLines(sourceFile);
        } catch (FileNotFoundException e) {
            err.println("Error: cannot open source file: " + e.getMessage());
            return EXIT_INPUT_ERROR;
        }
        out.println("Source file: " + sourceFile + " (" + lines.size() + " lines)");

        List<String> code = CommentStripper.strip(lines);
        List<SourceFormatChecker.Problem> problems = SourceFormatChecker.check(code);
        if (!problems.isEmpty()) {
            printFormatProblems(err, sourceFile, problems);
            return EXIT_INPUT_ERROR;
        }

        printMenu(out);
        OptionalInt choice = readChoice(in, out);
        if (choice.isEmpty()) {
            err.println("Error: no analysis was chosen.");
            return EXIT_INPUT_ERROR;
        }
        if (String.valueOf(choice.getAsInt()).equals(VARIABLE_CONDITION_OPTION)) {
            VariableConditionAnalysis.run(code, out);
        } else {
            ComplexityAnalysis.run(code, out, showGraph);
        }
        return EXIT_SUCCESS;
    }

    // ***************************************************************************
    // Asking which analysis to run
    // ***************************************************************************

    /**
     * Prints the analysis menu.
     *
     * @param out where to print
     */
    private static void printMenu(PrintStream out) {
        out.println();
        out.println("Choose an analysis:");
        out.println("  1. Cyclomatic complexity and control flow graph");
        out.println("  2. Test case data (variables and conditions)");
    }

    /**
     * Asks for 1 or 2, asking again after any other answer.
     *
     * @param in where to read the answers from
     * @param out where to print the prompts
     * @return the chosen option, or an empty value if the input ends or cannot be read before a
     *         valid answer
     */
    private static OptionalInt readChoice(InputStream in, PrintStream out) {
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        try {
            while (true) {
                out.print("Enter 1 or 2: ");
                out.flush();
                String answer = input.readLine();
                if (answer == null) {
                    out.println();
                    return OptionalInt.empty();
                }
                answer = answer.strip();
                if (answer.equals(COMPLEXITY_OPTION) || answer.equals(VARIABLE_CONDITION_OPTION)) {
                    return OptionalInt.of(Integer.parseInt(answer));
                }
                out.println("Please enter 1 or 2.");
            }
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }

    // ***************************************************************************
    // Reporting errors
    // ***************************************************************************

    /**
     * Prints why the source file cannot be analyzed, with at most {@value #MAX_PROBLEMS_SHOWN}
     * problems and a summary of the supported layout.
     *
     * @param err where to print
     * @param sourceFile the file's path, as given
     * @param problems the problems found
     */
    private static void printFormatProblems(PrintStream err, String sourceFile,
            List<SourceFormatChecker.Problem> problems) {
        err.println("Error: " + sourceFile + " cannot be analyzed, because it uses a layout this program"
                + " does not support:");
        problems.stream().limit(MAX_PROBLEMS_SHOWN).forEach(problem -> err.println("  " + problem));
        if (problems.size() > MAX_PROBLEMS_SHOWN) {
            err.println("  ... and " + (problems.size() - MAX_PROBLEMS_SHOWN) + " more");
        }
        err.println("Supported layout: one function, one statement per line, { at the end of each if,"
                + " else, for and while line, and each } on a line of its own.");
    }

    /**
     * Prints an error and the usage message.
     *
     * @param err where to print
     * @param message the error to print
     * @return {@link #EXIT_USAGE_ERROR}
     */
    private static int usageError(PrintStream err, String message) {
        err.println(message);
        err.println(USAGE);
        return EXIT_USAGE_ERROR;
    }

    // ***************************************************************************
    // Handling the command line
    // ***************************************************************************

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
}
