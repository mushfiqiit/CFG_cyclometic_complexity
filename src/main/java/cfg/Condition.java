package cfg;

import java.util.ArrayList;
import java.util.List;

/** A comparison found in an {@code if}, {@code else if} or {@code while} statement. */
public class Condition {

    private final List<Variable> variables = new ArrayList<>();
    private String comparisonSign;

    public void addVariable(Variable variable) {
        variables.add(variable);
    }

    public String getComparisonSign() {
        return comparisonSign;
    }

    public void setComparisonSign(String comparisonSign) {
        this.comparisonSign = comparisonSign;
    }

    /** Prints each variable, then the first variable's name. Throws if there are no variables. */
    public void printVariables() {
        variables.forEach(System.out::println);
        System.out.println(variables.get(0).name());
    }
}
