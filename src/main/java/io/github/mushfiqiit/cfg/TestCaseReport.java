package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.TestCaseGenerator.ConditionTooComplex;
import io.github.mushfiqiit.cfg.TestCaseGenerator.Contradiction;
import io.github.mushfiqiit.cfg.TestCaseGenerator.DependsOnComputedVariable;
import io.github.mushfiqiit.cfg.TestCaseGenerator.EarlierConditionTooComplex;
import io.github.mushfiqiit.cfg.TestCaseGenerator.Found;
import io.github.mushfiqiit.cfg.TestCaseGenerator.NoValues;
import io.github.mushfiqiit.cfg.TestCaseGenerator.Reason;
import io.github.mushfiqiit.cfg.TestCaseGenerator.TestCase;
import io.github.mushfiqiit.cfg.TestCaseGenerator.TooManyCombinations;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Prints a {@link VariableConditionAnalysis} as the option 2 report. */
final class TestCaseReport {

    private final PrintStream out;

    /**
     * Creates a report that writes to the given stream.
     *
     * @param out where to print
     */
    TestCaseReport(PrintStream out) {
        this.out = out;
    }

    // ***************************************************************************
    // Printing the report
    // ***************************************************************************

    /**
     * Prints the variables, the conditions to cover and the generated test cases.
     *
     * @param analysis the analysis to print
     */
    void print(VariableConditionAnalysis analysis) {
        ConsoleText.printTitle(out, "Test Case Data");
        printVariables(analysis);
        printConditions(analysis.conditions());
        printTestCases(analysis);
    }

    /**
     * Prints each variable a test case uses and where its value comes from.
     *
     * @param analysis the analysis
     */
    private void printVariables(VariableConditionAnalysis analysis) {
        ConsoleText.printSection(out, "Variables");
        if (analysis.variables().isEmpty() && analysis.undeclaredInputs().isEmpty()) {
            out.println("  No int variables were declared.");
            return;
        }
        ConsoleTable table = new ConsoleTable("Variable", "Value");
        analysis.variables().forEach(variable -> table.addRow(variable.name(), variable.describeSource()));
        analysis.undeclaredInputs().forEach(name -> table.addRow(name, "input (used but not declared)"));
        table.print(out);
    }

    /**
     * Prints each condition with its operator and operands.
     *
     * @param conditions the conditions
     */
    private void printConditions(List<ConditionSite> conditions) {
        ConsoleText.printSection(out, "Conditions to cover");
        if (conditions.isEmpty()) {
            out.println("  No if, else if or while conditions were found.");
            return;
        }
        ConsoleTable table = new ConsoleTable("#", "Line", "Condition", "Operator", "Operands").alignRight(0, 1);
        for (int i = 0; i < conditions.size(); i++) {
            Condition condition = conditions.get(i).condition();
            table.addRow(i + 1, conditions.get(i).lineNumber(), orDash(condition.text()),
                    condition.operator().orElse("-"), orDash(String.join(", ", condition.operands())));
        }
        table.print(out);
    }

    /**
     * Prints one test case per outcome of each condition, with the inputs that produce it.
     *
     * @param analysis the analysis
     */
    private void printTestCases(VariableConditionAnalysis analysis) {
        ConsoleText.printSection(out, "Test cases");
        if (analysis.conditions().isEmpty()) {
            out.println("  No test cases: there are no conditions to cover.");
            return;
        }
        out.println("  Each condition is made true in one test case and false in another, using inputs");
        out.println("  that reach it. Inputs are the values at the start of the function; assignments");
        out.println("  made before a condition is checked are not taken into account.");
        out.println();

        List<TestCase> cases = analysis.testCases();
        ConsoleTable table = new ConsoleTable("#", "Covers", "Outcome", "Inputs").alignRight(0);
        for (int i = 0; i < cases.size(); i++) {
            TestCase testCase = cases.get(i);
            table.addRow(i + 1, testCase.site().describe(), testCase.outcome(), describe(testCase.result()));
        }
        table.print(out);

        out.println();
        out.println("  Values found for " + analysis.coveredCount() + " of " + cases.size() + " condition outcomes.");
    }

    // ***************************************************************************
    // Wording
    // ***************************************************************************

    /**
     * Describes what was found for a test case, such as {@code "a=1, b=0"}.
     *
     * @param result the values found, or the reason there are none
     * @return the inputs, {@code "none needed"} if there are no input variables, or
     *         {@code "no values: "} followed by the reason
     */
    static String describe(TestCaseGenerator.Result result) {
        if (result instanceof NoValues noValues) {
            return "no values: " + describe(noValues.reason());
        }
        Map<String, Integer> inputs = ((Found) result).inputs();
        if (inputs.isEmpty()) {
            return "none needed";
        }
        return inputs.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    /**
     * Explains why no values were found, in words.
     *
     * @param reason the reason
     * @return the explanation, such as {@code "the condition is too complex to solve"}
     */
    static String describe(Reason reason) {
        if (reason instanceof ConditionTooComplex) {
            return "the condition is too complex to solve";
        }
        if (reason instanceof EarlierConditionTooComplex earlier) {
            return "the condition on line " + earlier.lineNumber() + ", which leads here, is too complex to solve";
        }
        if (reason instanceof DependsOnComputedVariable computed) {
            return "depends on " + computed.variable() + ", which is computed in the code";
        }
        if (reason instanceof Contradiction) {
            return "this outcome contradicts a condition on the way here";
        }
        if (reason instanceof TooManyCombinations) {
            return "too many variables to search; no values were tried";
        }
        throw new AssertionError("unknown reason: " + reason);
    }

    /**
     * Replaces an empty value with {@code -}.
     *
     * @param value the value to show
     * @return the value, or {@code -} if it is empty
     */
    private static String orDash(String value) {
        return value.isEmpty() ? "-" : value;
    }
}
