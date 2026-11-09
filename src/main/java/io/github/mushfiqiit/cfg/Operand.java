package io.github.mushfiqiit.cfg;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/** One side of a {@link Comparison}: a variable or an integer constant, read once from its text. */
sealed interface Operand permits Operand.Variable, Operand.Constant {

    /**
     * A variable, such as {@code count}.
     *
     * @param name the variable's name
     */
    record Variable(String name) implements Operand {

        /**
         * Returns the variable's value.
         *
         * @param values the value of every variable
         * @return the value of this variable
         * @throws IllegalArgumentException if {@code values} has no value for this variable
         */
        @Override
        public int valueIn(Map<String, Integer> values) {
            Integer value = values.get(name);
            if (value == null) {
                throw new IllegalArgumentException("no value for variable " + name);
            }
            return value;
        }
    }

    /**
     * An integer constant, such as {@code -5}.
     *
     * @param value the constant's value
     */
    record Constant(int value) implements Operand {

        /**
         * Returns the constant's value, which does not depend on the variables.
         *
         * @param values ignored
         * @return the constant's value
         */
        @Override
        public int valueIn(Map<String, Integer> values) {
            return value;
        }
    }

    /**
     * Reads an operand from its text.
     *
     * @param text a variable name or an integer, without whitespace
     * @return the operand, or an empty value if the text is neither a variable name nor an integer
     *         that fits in an {@code int}
     */
    static Optional<Operand> parse(String text) {
        if (CTokens.isIdentifier(text)) {
            return Optional.of(new Variable(text));
        }
        OptionalInt value = CTokens.parseInteger(text);
        return value.isPresent() ? Optional.of(new Constant(value.getAsInt())) : Optional.empty();
    }

    /**
     * Returns the operand's value.
     *
     * @param values the value of every variable
     * @return the constant itself, or the variable's value
     * @throws IllegalArgumentException if the operand is a variable with no value in {@code values}
     */
    int valueIn(Map<String, Integer> values);
}
