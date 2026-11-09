package io.github.mushfiqiit.cfg;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A condition that can be evaluated: two operands, each a variable or an integer, compared with
 * {@code <}, {@code <=}, {@code >}, {@code >=}, {@code ==} or {@code !=}. Create one with
 * {@link Condition#toComparison()}.
 *
 * @param left a variable name or an integer
 * @param operator one of {@code < <= > >= == !=}
 * @param right a variable name or an integer
 */
record Comparison(String left, String operator, String right) {

    private static final Pattern VARIABLE = Pattern.compile("[A-Za-z_]\\w*");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    private static final Set<String> OPERATORS = Set.of("<", "<=", ">", ">=", "==", "!=");

    // ***************************************************************************
    // Creating comparisons
    // ***************************************************************************

    /**
     * Creates a comparison if its parts are simple enough to evaluate.
     *
     * @param left the left operand's text
     * @param operator the operator's text
     * @param right the right operand's text
     * @return the comparison, or an empty value if an operand is not a variable or an integer
     *         that fits in an {@code int}, or the operator is not supported
     */
    static Optional<Comparison> of(String left, String operator, String right) {
        boolean simple = OPERATORS.contains(operator) && isOperand(left) && isOperand(right);
        return simple ? Optional.of(new Comparison(left, operator, right)) : Optional.empty();
    }

    /**
     * Tells whether text is a variable name.
     *
     * @param operand the text to check
     * @return true for a name such as {@code k} or {@code count_2}
     */
    static boolean isVariable(String operand) {
        return VARIABLE.matcher(operand).matches();
    }

    /**
     * Tells whether text is an integer that fits in an {@code int}.
     *
     * @param operand the text to check
     * @return true for a number such as {@code 0} or {@code -5}
     */
    static boolean isInteger(String operand) {
        return parseInteger(operand).isPresent();
    }

    /**
     * Reads an integer that fits in an {@code int}. Larger numbers are not read, so they never
     * cause an exception.
     *
     * @param text the text to read
     * @return the integer, or an empty value if the text is not a whole number or does not fit
     *         in an {@code int}
     */
    static OptionalInt parseInteger(String text) {
        if (!INTEGER.matcher(text).matches()) {
            return OptionalInt.empty();
        }
        BigInteger value = new BigInteger(text);
        return value.bitLength() < Integer.SIZE ? OptionalInt.of(value.intValue()) : OptionalInt.empty();
    }

    /**
     * Tells whether text can be an operand.
     *
     * @param operand the text to check
     * @return true for a variable name or an integer
     */
    private static boolean isOperand(String operand) {
        return isVariable(operand) || isInteger(operand);
    }

    // ***************************************************************************
    // Reading and evaluating comparisons
    // ***************************************************************************

    /**
     * Lists the variables the comparison uses.
     *
     * @return the variable names, left before right
     */
    List<String> variables() {
        List<String> variables = new ArrayList<>();
        for (String operand : List.of(left, right)) {
            if (isVariable(operand)) {
                variables.add(operand);
            }
        }
        return variables;
    }

    /**
     * Lists the integers the comparison uses.
     *
     * @return the integers, left before right
     */
    List<Integer> constants() {
        List<Integer> constants = new ArrayList<>();
        for (String operand : List.of(left, right)) {
            parseInteger(operand).ifPresent(constants::add);
        }
        return constants;
    }

    /**
     * Evaluates the comparison.
     *
     * @param values the value of every variable it uses
     * @return whether the comparison holds
     * @throws IllegalArgumentException if a variable the comparison uses has no value
     */
    boolean holds(Map<String, Integer> values) {
        for (String variable : variables()) {
            if (values.get(variable) == null) {
                throw new IllegalArgumentException("no value for variable " + variable);
            }
        }
        int a = valueOf(left, values);
        int b = valueOf(right, values);
        return switch (operator) {
            case "<" -> a < b;
            case "<=" -> a <= b;
            case ">" -> a > b;
            case ">=" -> a >= b;
            case "==" -> a == b;
            default -> a != b;
        };
    }

    /**
     * Returns an operand's value.
     *
     * @param operand a variable name or an integer
     * @param values the value of every variable
     * @return the integer itself, or the variable's value
     */
    private static int valueOf(String operand, Map<String, Integer> values) {
        OptionalInt integer = parseInteger(operand);
        return integer.isPresent() ? integer.getAsInt() : values.get(operand);
    }
}
