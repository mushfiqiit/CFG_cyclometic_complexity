package cfg;

import java.util.ArrayList;
import java.util.List;

/** Extracts the declared variables and the branch conditions from C source lines. */
public class TestCaseGenerationParser {

    private final List<String> lines;
    private final List<Condition> conditions = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();

    /**
     * Creates a parser for a source file. Nothing is parsed until {@link #start()} runs.
     *
     * @param lines the source lines to parse
     */
    public TestCaseGenerationParser(List<String> lines) {
        this.lines = lines;
    }

    // ***************************************************************************
    // Running the parser
    // ***************************************************************************

    /**
     * Parses every line, printing each {@code int} declaration as it is found, then prints each
     * condition followed by every declared variable.
     *
     * @throws IndexOutOfBoundsException if a condition has no operands, such as {@code if()}
     */
    public void start() {
        for (String line : lines) {
            if (isConditional(line)) {
                conditions.add(parseCondition(line));
            } else if (SyntaxIdentifier.isInt(line)) {
                System.out.println(line);
                parseDeclaration(line);
            }
        }
        printResults();
    }

    // ***************************************************************************
    // Parsing conditions
    // ***************************************************************************

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

    /**
     * Extracts the operands and comparison operator from the text after the first {@code (}.
     *
     * @param line an {@code if}, {@code else if} or {@code while} line
     * @return the parsed condition
     */
    private static Condition parseCondition(String line) {
        Condition condition = new Condition();
        StringBuilder variable = new StringBuilder();
        StringBuilder comparison = new StringBuilder();
        boolean insideParentheses = false;

        for (char character : line.toCharArray()) {
            if (insideParentheses && !isConditionDelimiter(character)) {
                if (SyntaxIdentifier.isComparisonSign(character)) {
                    comparison.append(character);
                    addPendingVariable(condition, variable);
                } else if (character != ' ') {
                    variable.append(character);
                    applyPendingComparison(condition, comparison);
                }
            }
            if (character == '(') {
                insideParentheses = true;
            }
        }
        addPendingVariable(condition, variable);
        return condition;
    }

    /**
     * Tells whether a character is skipped when reading a condition.
     *
     * @param character the character to check
     * @return true for {@code )}, <code>{</code> and <code>}</code>
     */
    private static boolean isConditionDelimiter(char character) {
        return character == ')' || character == '{' || character == '}';
    }

    /**
     * Adds the operand read so far to the condition, then clears it. Does nothing if it is empty.
     *
     * @param condition the condition being parsed
     * @param variable the characters of the operand read so far
     */
    private static void addPendingVariable(Condition condition, StringBuilder variable) {
        if (variable.length() > 0) {
            condition.addVariable(new Variable(variable.toString()));
            variable.setLength(0);
        }
    }

    /**
     * Sets the operator read so far on the condition, then clears it. Does nothing if it is empty.
     *
     * @param condition the condition being parsed
     * @param comparison the characters of the operator read so far
     */
    private static void applyPendingComparison(Condition condition, StringBuilder comparison) {
        if (comparison.length() > 0) {
            condition.setComparisonSign(comparison.toString());
            comparison.setLength(0);
        }
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
                    variables.add(new Variable(name));
                }
                token.setLength(0);
            } else if (character != ';') {
                token.append(character);
            }
        }

        // Unlike the tokens above, the last one is kept even when it is empty.
        String lastName = token.toString();
        if (!lastName.equals("int")) {
            variables.add(new Variable(lastName));
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
