package cfg;

import java.util.ArrayList;
import java.util.List;

/** A comparison found in an {@code if}, {@code else if} or {@code while} statement. */
public class Condition {

    private final List<Variable> variables = new ArrayList<>();
    private String comparisonSign;

    /** Creates a condition with no variables and no comparison sign. */
    public Condition() {
    }

    /**
     * Adds an operand of the comparison.
     *
     * @param variable the operand, in the order it appears in the condition
     */
    public void addVariable(Variable variable) {
        variables.add(variable);
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
     * Sets the comparison operator.
     *
     * @param comparisonSign the operator, such as {@code ">"} or {@code "=="}
     */
    public void setComparisonSign(String comparisonSign) {
        this.comparisonSign = comparisonSign;
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
