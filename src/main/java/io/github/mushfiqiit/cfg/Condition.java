package io.github.mushfiqiit.cfg;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A condition found in an {@code if}, {@code else if} or {@code while} statement. Simple
 * conditions, such as {@code k > 0}, {@code !done} or {@code flag}, are read into an operator and
 * operands; anything else, such as {@code (a + b) > 3} or {@code a > b && b > 0}, keeps only its
 * text.
 */
public final class Condition {

    /** A variable name or an integer; a minus sign may be followed by spaces, as in {@code - 5}. */
    private static final String OPERAND = "[A-Za-z_]\\w*|-?\\s*\\d+";
    private static final Pattern COMPARISON =
            Pattern.compile("(" + OPERAND + ")\\s*(<=|>=|==|!=|<|>)\\s*(" + OPERAND + ")");
    private static final Pattern NEGATION = Pattern.compile("!\\s*(" + OPERAND + ")");
    private static final Pattern SINGLE_OPERAND = Pattern.compile(OPERAND);

    private final String text;
    private final String comparisonSign;
    private final List<String> operands;

    /**
     * Creates a condition. Use {@link #from(String)}.
     *
     * @param text the condition as written in the source
     * @param comparisonSign the operator, or null if there is none
     * @param operands the operands in the order they appear
     */
    private Condition(String text, String comparisonSign, List<String> operands) {
        this.text = text;
        this.comparisonSign = comparisonSign;
        this.operands = List.copyOf(operands);
    }

    // ***************************************************************************
    // Reading a condition from a source line
    // ***************************************************************************

    /**
     * Reads the condition between the parentheses of a line. A comparison of two operands, a
     * negated operand ({@code !a}) or a single operand ({@code a}) is read into its parts, where
     * an operand is a variable name or an integer; any other condition keeps only its text.
     *
     * @param line an {@code if}, {@code else if} or {@code while} line
     * @return the condition
     */
    public static Condition from(String line) {
        String text = conditionText(line);
        Matcher comparison = COMPARISON.matcher(text);
        if (comparison.matches()) {
            return new Condition(text, comparison.group(2),
                    List.of(compact(comparison.group(1)), compact(comparison.group(3))));
        }
        Matcher negation = NEGATION.matcher(text);
        if (negation.matches()) {
            return new Condition(text, "!", List.of(compact(negation.group(1))));
        }
        if (SINGLE_OPERAND.matcher(text).matches()) {
            return new Condition(text, null, List.of(compact(text)));
        }
        return new Condition(text, null, List.of());
    }

    /**
     * Removes the spaces an operand may contain, so {@code - 5} becomes {@code -5}.
     *
     * @param operand the operand as written
     * @return the operand without whitespace
     */
    private static String compact(String operand) {
        return operand.replaceAll("\\s", "");
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

    // ***************************************************************************
    // Reading the condition
    // ***************************************************************************

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
        if (operands.size() == 2) {
            return Comparison.of(operands.get(0), comparisonSign, operands.get(1));
        }
        return Optional.empty();
    }

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
     * @return the operator, such as {@code ">"}, {@code "=="} or {@code "!"}, or null for a single
     *         operand or a condition too complex to read
     */
    public String getComparisonSign() {
        return comparisonSign;
    }

    /**
     * Returns the operands of the comparison, such as {@code "k"} and {@code "0"} for
     * {@code k > 0}.
     *
     * @return the operands' text in the order they appear, as a read-only list; empty for a
     *         condition too complex to read
     */
    public List<String> getOperands() {
        return operands;
    }
}
