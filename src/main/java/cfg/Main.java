package cfg;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/** Entry point: reads the C source file and runs the analysis the user selects. */
public final class Main {

    private static final String SOURCE_FILE = "test.txt";
    private static final int GENERATE_TEST_CASE_OPTION = 2;

    private Main() {
    }

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
