package io.github.mushfiqiit.cfg;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A comparison operator, with its C symbol and how it compares two integers. This is the only
 * place the supported operators are listed.
 */
enum Operator {

    /** {@code <} */
    LESS("<") {
        @Override
        boolean test(int left, int right) {
            return left < right;
        }
    },

    /** {@code <=} */
    LESS_OR_EQUAL("<=") {
        @Override
        boolean test(int left, int right) {
            return left <= right;
        }
    },

    /** {@code >} */
    GREATER(">") {
        @Override
        boolean test(int left, int right) {
            return left > right;
        }
    },

    /** {@code >=} */
    GREATER_OR_EQUAL(">=") {
        @Override
        boolean test(int left, int right) {
            return left >= right;
        }
    },

    /** {@code ==} */
    EQUAL("==") {
        @Override
        boolean test(int left, int right) {
            return left == right;
        }
    },

    /** {@code !=} */
    NOT_EQUAL("!=") {
        @Override
        boolean test(int left, int right) {
            return left != right;
        }
    };

    /**
     * Matches any operator's symbol. Longer symbols come first, so {@code <=} is never read as
     * {@code <} followed by {@code =}.
     */
    static final Pattern SYMBOL = Pattern.compile(Arrays.stream(values())
            .map(Operator::symbol)
            .sorted(Comparator.comparingInt(String::length).reversed())
            .map(Pattern::quote)
            .collect(Collectors.joining("|")));

    private final String symbol;

    /**
     * Creates an operator.
     *
     * @param symbol how the operator is written in C
     */
    Operator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Finds the operator written with a symbol.
     *
     * @param symbol the symbol, such as {@code "<="}
     * @return the operator, or an empty value if no operator has that symbol
     */
    static Optional<Operator> fromSymbol(String symbol) {
        return Arrays.stream(values()).filter(operator -> operator.symbol.equals(symbol)).findFirst();
    }

    /**
     * Returns how the operator is written in C.
     *
     * @return the symbol, such as {@code "<="}
     */
    String symbol() {
        return symbol;
    }

    /**
     * Compares two integers with this operator.
     *
     * @param left the left operand's value
     * @param right the right operand's value
     * @return whether {@code left} and {@code right} satisfy the operator
     */
    abstract boolean test(int left, int right);
}
