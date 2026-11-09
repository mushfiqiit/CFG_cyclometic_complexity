package io.github.mushfiqiit.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Entry point of the program. Reads the command line, loads the C source file, asks which
 * analysis to run, and runs it; each step is done by its own class.
 *
 * <p>Usage: {@code CFG_cyclometic_complexity [source-file]}. See {@link CommandLine#USAGE}.
 */
public final class Main {

    /** Exit status when the program finished normally. */
    static final int EXIT_SUCCESS = 0;

    /** Exit status when the source file cannot be read or analyzed, or no analysis was chosen. */
    static final int EXIT_INPUT_ERROR = 1;

    /** Exit status when the command-line arguments are invalid. */
    static final int EXIT_USAGE_ERROR = 2;

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
     * Runs the program: reads the command line, loads and checks the source file, asks which
     * analysis to run, and runs it.
     *
     * @param args the command-line arguments
     * @param in where to read the menu choice from
     * @param out where to print the reports
     * @param err where to print error messages
     * @param showGraph what to do with the graph after the complexity report
     * @return {@link #EXIT_SUCCESS}, {@link #EXIT_INPUT_ERROR} or {@link #EXIT_USAGE_ERROR}
     */
    static int run(String[] args, InputStream in, PrintStream out, PrintStream err,
            Consumer<ControlFlowGraph> showGraph) {
        CommandLine command = CommandLine.parse(args);
        if (command instanceof CommandLine.ShowHelp) {
            out.println(CommandLine.USAGE);
            return EXIT_SUCCESS;
        }
        if (command instanceof CommandLine.Invalid invalid) {
            err.println(invalid.message());
            err.println(CommandLine.USAGE);
            return EXIT_USAGE_ERROR;
        }

        out.println("Control Flow Graph Analyzer");
        Optional<ParsedSource> loaded = SourceLoader.load(((CommandLine.Analyze) command).sourceFile(), out, err);
        if (loaded.isEmpty()) {
            return EXIT_INPUT_ERROR;
        }
        ParsedSource source = loaded.orElseThrow();

        Optional<Menu.Analysis> choice;
        try {
            choice = Menu.ask(in, out);
        } catch (IOException e) {
            err.println("Error: cannot read the menu choice: " + e.getMessage());
            return EXIT_INPUT_ERROR;
        }
        if (choice.isEmpty()) {
            err.println("Error: no analysis was chosen.");
            return EXIT_INPUT_ERROR;
        }
        switch (choice.orElseThrow()) {
            case COMPLEXITY -> reportComplexity(ComplexityAnalysis.of(source), out, showGraph);
            case TEST_CASES -> new TestCaseReport(out).print(VariableConditionAnalysis.of(source));
        }
        return EXIT_SUCCESS;
    }

    /**
     * Prints the option 1 report, then hands the graph to {@code showGraph}. The program passes
     * {@link GraphViewer#show(ControlFlowGraph)}, which opens a window and blocks until it is
     * closed.
     *
     * @param analysis the finished analysis
     * @param out where to print the report
     * @param showGraph what to do with the graph once the report is printed
     */
    private static void reportComplexity(ComplexityAnalysis analysis, PrintStream out,
            Consumer<ControlFlowGraph> showGraph) {
        new GraphPrinter(out).printReport(analysis);
        out.println();
        out.println("Opening the graph window. Close it to exit.");
        showGraph.accept(analysis.graph());
    }
}
