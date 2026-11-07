package cfg;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point of the program. Reads the C source file, asks the user which analysis to run, and
 * runs it.
 */
public final class Main {

    private static final String SOURCE_FILE = "test.txt";
    private static final int GENERATE_TEST_CASE_OPTION = 2;

    /** Prevents instantiation; this class only holds the entry point. */
    private Main() {
    }

    /**
     * Reads {@code test.txt} from the working directory, then reads a menu option from standard
     * input. Option 2 generates test case data; any other number builds the control flow graph.
     *
     * @param args command-line arguments, which are ignored
     * @throws IOException if the source file cannot be read or the graph files cannot be written
     * @throws java.util.InputMismatchException if the option entered is not a whole number
     */
    public static void main(String[] args) throws IOException {
        System.out.println("Control Flow Graph\n\n");
        List<String> lines = SourceCodeReader.readLines(SOURCE_FILE);

        System.out.println("Enter option: 1.Cyclomatic Complexity 2. Generate Testcase");
        int option = new Scanner(System.in).nextInt();

        if (option == GENERATE_TEST_CASE_OPTION) {
            new TestCaseGenerationParser(lines).start();
        } else {
            new ControlFlowGraph(lines).analyze();
        }
    }
}
