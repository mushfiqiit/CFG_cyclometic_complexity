package cfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the variables and conditions option: lists the {@code int} variables declared in C source
 * lines, each with a test value of 0, and the conditions of their {@code if}, {@code else if} and
 * {@code while} statements.
 */
public final class VariableConditionAnalysis {

    private final List<Condition> conditions = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();

    /** Creates an empty analysis. Use {@link #run(List)} to analyze a source file. */
    private VariableConditionAnalysis() {
    }

    // ***************************************************************************
    // Running the analysis
    // ***************************************************************************

    /**
     * Analyzes a source file, printing each {@code int} declaration as it is found, then prints
     * each condition followed by every declared variable.
     *
     * @param lines the source lines to analyze
     * @throws IndexOutOfBoundsException if a condition has no operands, such as {@code if()}
     */
    public static void run(List<String> lines) {
        VariableConditionAnalysis analysis = new VariableConditionAnalysis();
        analysis.collect(lines);
        analysis.printResults();
    }

    /**
     * Collects the conditions and declared variables from every line, printing each {@code int}
     * declaration as it is found.
     *
     * @param lines the source lines to scan
     */
    private void collect(List<String> lines) {
        for (String line : lines) {
            if (isConditional(line)) {
                conditions.add(Condition.from(line));
            } else if (SyntaxIdentifier.isInt(line)) {
                System.out.println(line);
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
                String name = token.toString();
                if (!name.equals("int") && !name.isEmpty()) {
                    variables.add(Variable.from(name));
                }
                token.setLength(0);
            } else if (character != ';') {
                token.append(character);
            }
        }

        // Unlike the tokens above, the last one is kept even when it is empty.
        String lastName = token.toString();
        if (!lastName.equals("int")) {
            variables.add(Variable.from(lastName));
        }
    }

    // ***************************************************************************
    // Printing results
    // ***************************************************************************

    /**
     * Prints each condition's operator and operands, then every declared variable.
     *
     * @throws IndexOutOfBoundsException if a condition has no operands
     */
    private void printResults() {
        for (Condition condition : conditions) {
            System.out.println(condition.getComparisonSign());
            condition.printVariables();
        }
        variables.forEach(System.out::println);
    }
}
