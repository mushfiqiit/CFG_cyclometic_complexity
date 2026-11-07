package cfg;

import java.util.ArrayList;
import java.util.List;

/** A comparison found in an {@code if}, {@code else if} or {@code while} statement. */
public class Condition {

    private final List<Variable> variables = new ArrayList<>();
    private String comparisonSign;

    /** Creates a condition with no operands and no comparison sign. Use {@link #from(String)}. */
    private Condition() {
    }

    // ***************************************************************************
    // Parsing a condition from a source line
    // ***************************************************************************

    /**
     * Parses the operands and comparison operator from the text after the first {@code (} of a
     * line.
     *
     * @param line an {@code if}, {@code else if} or {@code while} line
     * @return the parsed condition, with no comparison sign if none was found
     */
    public static Condition from(String line) {
        Condition condition = new Condition();
        StringBuilder variable = new StringBuilder();
        StringBuilder comparison = new StringBuilder();
        boolean insideParentheses = false;

        for (char character : line.toCharArray()) {
            if (insideParentheses && !isConditionDelimiter(character)) {
                if (SyntaxIdentifier.isComparisonSign(character)) {
                    comparison.append(character);
                    condition.addPendingVariable(variable);
                } else if (character != ' ') {
                    variable.append(character);
                    condition.applyPendingComparison(comparison);
                }
            }
            if (character == '(') {
                insideParentheses = true;
            }
        }
        condition.addPendingVariable(variable);
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
     * Adds the operand read so far to this condition, then clears it. Does nothing if it is empty.
     *
     * @param variable the characters of the operand read so far
     */
    private void addPendingVariable(StringBuilder variable) {
        if (variable.length() > 0) {
            variables.add(Variable.from(variable.toString()));
            variable.setLength(0);
        }
    }

    /**
     * Sets the operator read so far on this condition, then clears it. Does nothing if it is empty.
     *
     * @param comparison the characters of the operator read so far
     */
    private void applyPendingComparison(StringBuilder comparison) {
        if (comparison.length() > 0) {
            comparisonSign = comparison.toString();
            comparison.setLength(0);
        }
    }

    // ***************************************************************************
    // Reading the condition
    // ***************************************************************************

    /**
     * Returns the comparison operator.
     *
     * @return the operator, such as {@code ">"} or {@code "=="}, or null if none was found
     */
    public String getComparisonSign() {
        return comparisonSign;
    }

    /**
     * Prints each operand on its own line, then the first operand's name again.
     *
     * @throws IndexOutOfBoundsException if the condition has no operands
     */
    public void printVariables() {
        variables.forEach(System.out::println);
        System.out.println(variables.get(0).name());
    }
}
