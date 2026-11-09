package io.github.mushfiqiit.cfg;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A condition found in an {@code if}, {@code else if} or {@code while} statement. Each kind of
 * condition is its own type: a comparison of two operands such as {@code k > 0}, a negated operand
 * such as {@code !done}, a single operand such as {@code flag}, or a condition too complex to
 * read, such as {@code (a + b) > 3} or {@code a > b && b > 0}, which keeps only its text. An
 * operand is a variable name or an integer.
 */
sealed interface Condition
        permits Condition.Compare, Condition.Negation, Condition.Truthy, Condition.Unsolvable {

    // ***************************************************************************
    // The kinds of condition
    // ***************************************************************************

    /**
     * A comparison of two operands, such as {@code a <= b}.
     *
     * @param text the condition as written in the source
     * @param left the left operand, without whitespace
     * @param sign the comparison operator
     * @param right the right operand, without whitespace
     */
    record Compare(String text, String left, Operator sign, String right) implements Condition {

        /**
         * Returns the operator's symbol.
         *
         * @return the comparison operator's symbol, such as {@code "<="}
         */
        @Override
        public Optional<String> operator() {
            return Optional.of(sign.symbol());
        }

        /**
         * Returns both operands.
         *
         * @return the left and right operand, in that order
         */
        @Override
        public List<String> operands() {
            return List.of(left, right);
        }

        /**
         * Turns this comparison into one that can be evaluated.
         *
         * @return the comparison, or an empty value if an operand is an integer too large for an
         *         {@code int}
         */
        @Override
        public Optional<Comparison> toComparison() {
            return Comparison.of(left, sign, right);
        }
    }

    /**
     * A negated operand, such as {@code !done}, which holds when the operand is 0.
     *
     * @param text the condition as written in the source
     * @param operand the negated operand, without whitespace
     */
    record Negation(String text, String operand) implements Condition {

        /**
         * Returns the operator.
         *
         * @return {@code "!"}
         */
        @Override
        public Optional<String> operator() {
            return Optional.of("!");
        }

        /**
         * Returns the operand.
         *
         * @return a list holding only the negated operand
         */
        @Override
        public List<String> operands() {
            return List.of(operand);
        }

        /**
         * Reads {@code !a} as {@code a == 0}.
         *
         * @return the comparison, or an empty value if the operand is an integer too large for an
         *         {@code int}
         */
        @Override
        public Optional<Comparison> toComparison() {
            return Comparison.of(operand, Operator.EQUAL, "0");
        }
    }

    /**
     * A single operand, such as {@code flag}, which holds when the operand is not 0.
     *
     * @param text the condition as written in the source
     * @param operand the operand, without whitespace
     */
    record Truthy(String text, String operand) implements Condition {

        /**
         * Returns the operator.
         *
         * @return an empty value, because a single operand has no operator
         */
        @Override
        public Optional<String> operator() {
            return Optional.empty();
        }

        /**
         * Returns the operand.
         *
         * @return a list holding only the operand
         */
        @Override
        public List<String> operands() {
            return List.of(operand);
        }

        /**
         * Reads {@code a} as {@code a != 0}.
         *
         * @return the comparison, or an empty value if the operand is an integer too large for an
         *         {@code int}
         */
        @Override
        public Optional<Comparison> toComparison() {
            return Comparison.of(operand, Operator.NOT_EQUAL, "0");
        }
    }

    /**
     * A condition too complex to read, such as {@code (a + b) > 3}; only its text is kept.
     *
     * @param text the condition as written in the source
     */
    record Unsolvable(String text) implements Condition {

        /**
         * Returns the operator.
         *
         * @return an empty value, because the condition is not read into parts
         */
        @Override
        public Optional<String> operator() {
            return Optional.empty();
        }

        /**
         * Returns the operands.
         *
         * @return an empty list, because the condition is not read into parts
         */
        @Override
        public List<String> operands() {
            return List.of();
        }

        /**
         * Returns no comparison, because the condition cannot be solved.
         *
         * @return an empty value
         */
        @Override
        public Optional<Comparison> toComparison() {
            return Optional.empty();
        }
    }

    // ***************************************************************************
    // Reading a condition from a source line
    // ***************************************************************************

    /**
     * Reads the condition between the parentheses of a line.
     *
     * @param line an {@code if}, {@code else if} or {@code while} line
     * @return a {@link Compare}, {@link Negation} or {@link Truthy} if the condition has one of
     *         those forms, otherwise an {@link Unsolvable}
     */
    static Condition from(String line) {
        return Reader.read(line);
    }

    /** Reads conditions from source lines, keeping its patterns out of the interface. */
    final class Reader {

        /** A variable name or an integer. */
        private static final String OPERAND = CTokens.IDENTIFIER.pattern() + "|" + CTokens.INTEGER.pattern();
        private static final Pattern COMPARISON = Pattern.compile(
                "(" + OPERAND + ")\\s*(" + Operator.SYMBOL.pattern() + ")\\s*(" + OPERAND + ")");
        private static final Pattern NEGATION = Pattern.compile("!\\s*(" + OPERAND + ")");
        private static final Pattern SINGLE_OPERAND = Pattern.compile(OPERAND);

        /** Prevents instantiation; this class only holds the reading logic. */
        private Reader() {
        }

        /**
         * Reads the condition between the parentheses of a line. The condition keeps its text as
         * written; the operands are read from a copy where each minus sign is joined to its
         * number, so {@code a < - 5} has the operands {@code a} and {@code -5}.
         *
         * @param line an {@code if}, {@code else if} or {@code while} line
         * @return the condition, of the kind its form matches
         */
        private static Condition read(String line) {
            String text = textBetweenParentheses(line);
            String joined = CTokens.joinMinusSigns(text);
            Matcher comparison = COMPARISON.matcher(joined);
            if (comparison.matches()) {
                Operator sign = Operator.fromSymbol(comparison.group(2)).orElseThrow();
                return new Compare(text, comparison.group(1), sign, comparison.group(3));
            }
            Matcher negation = NEGATION.matcher(joined);
            if (negation.matches()) {
                return new Negation(text, negation.group(1));
            }
            if (SINGLE_OPERAND.matcher(joined).matches()) {
                return new Truthy(text, joined);
            }
            return new Unsolvable(text);
        }

        /**
         * Extracts the condition as written: the text between the first {@code (} and the last
         * {@code )}, or to the end of the line if there is no {@code )}.
         *
         * @param line an {@code if}, {@code else if} or {@code while} line
         * @return the condition text without surrounding whitespace
         */
        private static String textBetweenParentheses(String line) {
            int open = line.indexOf('(');
            int close = line.lastIndexOf(')');
            String text = close > open ? line.substring(open + 1, close) : line.substring(open + 1);
            return text.strip();
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
    String text();

    /**
     * Returns the operator shown for this condition.
     *
     * @return the comparison operator, such as {@code ">"}, or {@code "!"} for a negation; an
     *         empty value for a single operand or a condition too complex to read
     */
    Optional<String> operator();

    /**
     * Returns the operands, such as {@code "k"} and {@code "0"} for {@code k > 0}.
     *
     * @return the operands' text in the order they appear, as a read-only list; empty for a
     *         condition too complex to read
     */
    List<String> operands();

    /**
     * Turns this condition into a comparison that can be evaluated. A single operand {@code a} is
     * read as {@code a != 0}, and {@code !a} as {@code a == 0}.
     *
     * @return the comparison, or an empty value if the condition is too complex, such as
     *         {@code (a + b) > 3} or {@code a > b && b > 0}
     */
    Optional<Comparison> toComparison();
}
