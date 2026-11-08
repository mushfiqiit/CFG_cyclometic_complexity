package io.github.mushfiqiit.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** A comparison found in an {@code if}, {@code else if} or {@code while} statement. */
public class Condition {

    private final String text;
    private final List<String> operands = new ArrayList<>();
    private String comparisonSign;

    /**
     * Creates a condition with no operands and no comparison sign. Use {@link #from(String)}.
     *
     * @param text the condition as written in the source
     */
    private Condition(String text) {
        this.text = text;
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
        Condition condition = new Condition(conditionText(line));
        StringBuilder operand = new StringBuilder();
        StringBuilder comparison = new StringBuilder();
        boolean insideParentheses = false;

        for (char character : line.toCharArray()) {
            if (insideParentheses && !isConditionDelimiter(character)) {
                if (SyntaxIdentifier.isComparisonSign(character)) {
                    comparison.append(character);
                    condition.addPendingOperand(operand);
                } else if (character != ' ') {
                    operand.append(character);
                    condition.applyPendingComparison(comparison);
                }
            }
            if (character == '(') {
                insideParentheses = true;
            }
        }
        condition.addPendingOperand(operand);
        return condition;
    }

    /**
     * Extracts the condition as written: the text between the first {@code (} and the last
     * {@code )}, or to the end of the line if there is no {@code )}.
     *
     * @param line an {@code if}, {@code else if} or {@code while} line
     * @return the condition text without surrounding whitespace
     */
    private static String conditionText(String line) {
        int open = line.indexOf('(');
        int close = line.lastIndexOf(')');
        String text = close > open ? line.substring(open + 1, close) : line.substring(open + 1);
        return text.strip();
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
     * @param operand the characters of the operand read so far
     */
    private void addPendingOperand(StringBuilder operand) {
        if (operand.length() > 0) {
            operands.add(operand.toString());
            operand.setLength(0);
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
     * Returns the condition as written in the source, such as {@code "a > b"}.
     *
     * @return the text between the parentheses
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the comparison operator.
     *
     * @return the operator, such as {@code ">"} or {@code "=="}, or null if none was found
     */
    public String getComparisonSign() {
        return comparisonSign;
    }

    /**
     * Turns this condition into a comparison that can be evaluated. A single operand {@code a} is
     * read as {@code a != 0}, and {@code !a} as {@code a == 0}.
     *
     * @return the comparison, or an empty value if the condition is too complex, such as
     *         {@code (a + b) > 3} or {@code a > b && b > 0}
     */
    Optional<Comparison> toComparison() {
        if (operands.size() == 1 && comparisonSign == null) {
            return Comparison.of(operands.get(0), "!=", "0");
        }
        if (operands.size() == 1 && comparisonSign.equals("!")) {
            return Comparison.of(operands.get(0), "==", "0");
        }
        if (operands.size() == 2 && comparisonSign != null) {
            return Comparison.of(operands.get(0), comparisonSign, operands.get(1));
        }
        return Optional.empty();
    }

    /**
     * Returns the operands of the comparison, such as {@code "k"} and {@code "0"} for
     * {@code k > 0}.
     *
     * @return the operands' text in the order they appear, as a read-only list
     */
    public List<String> getOperands() {
        return Collections.unmodifiableList(operands);
    }
}
