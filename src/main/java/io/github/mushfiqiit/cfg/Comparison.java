package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.Operand.Constant;
import io.github.mushfiqiit.cfg.Operand.Variable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A condition that can be evaluated: two operands, each a variable or an integer, compared with
 * an {@link Operator}. Create one with {@link Condition#toComparison()}.
 *
 * @param left the left operand
 * @param operator the comparison operator
 * @param right the right operand
 */
record Comparison(Operand left, Operator operator, Operand right) {

    // ***************************************************************************
    // Creating comparisons
    // ***************************************************************************

    /**
     * Creates a comparison if both operands are simple enough to evaluate.
     *
     * @param left the left operand's text
     * @param operator the operator
     * @param right the right operand's text
     * @return the comparison, or an empty value if an operand is not a variable name or an
     *         integer that fits in an {@code int}
     */
    static Optional<Comparison> of(String left, Operator operator, String right) {
        return Operand.parse(left).flatMap(leftOperand ->
                Operand.parse(right).map(rightOperand -> new Comparison(leftOperand, operator, rightOperand)));
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
        return operands().filter(Variable.class::isInstance).map(operand -> ((Variable) operand).name()).toList();
    }

    /**
     * Lists the integers the comparison uses.
     *
     * @return the integers, left before right
     */
    List<Integer> constants() {
        return operands().filter(Constant.class::isInstance).map(operand -> ((Constant) operand).value()).toList();
    }

    /**
     * Evaluates the comparison.
     *
     * @param values the value of every variable it uses
     * @return whether the comparison holds
     * @throws IllegalArgumentException if a variable the comparison uses has no value
     */
    boolean holds(Map<String, Integer> values) {
        return operator.test(left.valueIn(values), right.valueIn(values));
    }

    /**
     * Returns both operands.
     *
     * @return the left operand, then the right one
     */
    private Stream<Operand> operands() {
        return Stream.of(left, right);
    }
}
