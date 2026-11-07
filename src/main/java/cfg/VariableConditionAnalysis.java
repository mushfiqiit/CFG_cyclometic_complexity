package cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs the test case data option: lists the {@code int} variables declared in C source lines, each
 * with a test value of 0, and the conditions of their {@code if}, {@code else if} and
 * {@code while} statements.
 */
public final class VariableConditionAnalysis {

    /** A condition and the 1-based number of the line it was found on. */
    private record FoundCondition(int lineNumber, Condition condition) {
    }

    private final List<FoundCondition> conditions = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();

    /** Creates an empty analysis. Use {@link #run(List)} to analyze a source file. */
    private VariableConditionAnalysis() {
    }

    // ***************************************************************************
    // Running the analysis
    // ***************************************************************************

    /**
     * Analyzes a source file and prints the test input values and the conditions to cover.
     *
     * @param lines the source lines to analyze
     */
    public static void run(List<String> lines) {
        VariableConditionAnalysis analysis = new VariableConditionAnalysis();
        analysis.collect(lines);
        analysis.printResults();
    }

    /**
     * Collects the conditions and declared variables from every line.
     *
     * @param lines the source lines to scan
     */
    private void collect(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (isConditional(line)) {
                conditions.add(new FoundCondition(index + 1, Condition.from(line)));
            } else if (SyntaxIdentifier.isInt(line)) {
                parseDeclaration(line);
            }
        }
    }

    /**
     * Tells whether a line holds a condition to parse.
     *
     * @param line the source line to check
     * @return true for {@code if}, {@code else if} and {@code while} lines; {@code for} lines are
     *         not included
     */
    private static boolean isConditional(String line) {
        return SyntaxIdentifier.isIf(line) || SyntaxIdentifier.isElseIf(line) || SyntaxIdentifier.isWhile(line);
    }

    // ***************************************************************************
    // Parsing declarations
    // ***************************************************************************

    /**
     * Adds each variable name in an {@code int} declaration to the variable list. Names are
     * separated by commas or spaces, and {@code ;} is ignored.
     *
     * @param line an {@code int} declaration line
     */
    private void parseDeclaration(String line) {
        StringBuilder token = new StringBuilder();

        for (char character : line.toCharArray()) {
            if (character == ',' || character == ' ') {
                addVariable(token.toString());
                token.setLength(0);
            } else if (character != ';') {
                token.append(character);
            }
        }
        addVariable(token.toString());
    }

    /**
     * Adds a declared name to the variable list, skipping the {@code int} keyword and empty names.
     *
     * @param name a token from a declaration line
     */
    private void addVariable(String name) {
        if (!name.equals("int") && !name.isEmpty()) {
            variables.add(Variable.from(name));
        }
    }

    // ***************************************************************************
    // Printing results
    // ***************************************************************************

    /** Prints the test input values, then the conditions to cover. */
    private void printResults() {
        ConsoleText.printTitle("Test Case Data");

        ConsoleText.printSection("Test input values");
        if (variables.isEmpty()) {
            System.out.println("  No int variables were declared.");
        } else {
            ConsoleTable table = new ConsoleTable("Variable", "Value").alignRight(1);
            variables.forEach(variable -> table.addRow(variable.name(), variable.value()));
            table.print();
        }

        ConsoleText.printSection("Conditions to cover");
        if (conditions.isEmpty()) {
            System.out.println("  No if, else if or while conditions were found.");
        } else {
            ConsoleTable table = new ConsoleTable("#", "Line", "Condition", "Operator", "Operands")
                    .alignRight(0, 1);
            for (int i = 0; i < conditions.size(); i++) {
                FoundCondition found = conditions.get(i);
                Condition condition = found.condition();
                table.addRow(i + 1, found.lineNumber(), orDash(condition.getText()),
                        orDash(condition.getComparisonSign()), operandNames(condition));
            }
            table.print();
        }
    }

    /**
     * Lists a condition's operand names separated by commas.
     *
     * @param condition the condition to describe
     * @return the operand names, or {@code -} if there are none
     */
    private static String operandNames(Condition condition) {
        String names = condition.getOperands().stream().map(Variable::name).collect(Collectors.joining(", "));
        return orDash(names);
    }

    /**
     * Replaces a missing or empty value with {@code -}.
     *
     * @param value the value to show
     * @return the value, or {@code -} if it is null or empty
     */
    private static String orDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }
}
