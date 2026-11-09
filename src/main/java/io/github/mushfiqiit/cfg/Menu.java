package io.github.mushfiqiit.cfg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/** Asks the user which analysis to run. */
final class Menu {

    /** The analyses the user can choose, in menu order. */
    enum Analysis {
        /** Option 1: the control flow graph and its cyclomatic complexity. */
        COMPLEXITY("1", "Cyclomatic complexity and control flow graph"),
        /** Option 2: the variables, conditions and generated test cases. */
        TEST_CASES("2", "Test case data (variables and conditions)");

        private final String key;
        private final String description;

        /**
         * Creates a menu choice.
         *
         * @param key what the user types to choose it
         * @param description how the menu describes it
         */
        Analysis(String key, String description) {
            this.key = key;
            this.description = description;
        }
    }

    private static final String PROMPT = "Enter 1 or 2: ";
    private static final String RETRY = "Please enter 1 or 2.";

    /** Prevents instantiation; this class only has static methods. */
    private Menu() {
    }

    // ***************************************************************************
    // Asking for a choice
    // ***************************************************************************

    /**
     * Shows the menu, then asks for a choice until the answer is one of the menu keys.
     *
     * @param in where to read the answers from, as UTF-8 text; the menu keys are plain digits,
     *        which are the same in every common console encoding
     * @param out where to print the menu and prompts
     * @return the chosen analysis, or an empty value if the input ends before a valid answer
     * @throws IOException if the answers cannot be read
     */
    static Optional<Analysis> ask(InputStream in, PrintStream out) throws IOException {
        printChoices(out);
        BufferedReader input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        while (true) {
            out.print(PROMPT);
            out.flush();
            String answer = input.readLine();
            if (answer == null) {
                out.println();
                return Optional.empty();
            }
            Optional<Analysis> choice = analysisFor(answer.strip());
            if (choice.isPresent()) {
                return choice;
            }
            out.println(RETRY);
        }
    }

    /**
     * Prints the list of analyses.
     *
     * @param out where to print
     */
    private static void printChoices(PrintStream out) {
        out.println();
        out.println("Choose an analysis:");
        for (Analysis analysis : Analysis.values()) {
            out.println("  " + analysis.key + ". " + analysis.description);
        }
    }

    /**
     * Finds the analysis an answer chooses.
     *
     * @param answer the user's answer, without surrounding whitespace
     * @return the analysis whose key it is, or an empty value
     */
    private static Optional<Analysis> analysisFor(String answer) {
        return Arrays.stream(Analysis.values()).filter(analysis -> analysis.key.equals(answer)).findFirst();
    }
}
