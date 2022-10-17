package cfg;

import java.util.ArrayList;

public class ConditionChecking {
    public ArrayList<Variable> variables=new ArrayList<>();
    public String comparisonSign;
    public void ConditionChecking() {

    }

    public void addVariable(Variable variableToAdd) {
        variables.add(variableToAdd);
    }

    public void setComparisonSign(String ComparisonSign) {
        this.comparisonSign=ComparisonSign;
    }

    public void printVariables() {
        for(Variable variable:variables){
            System.out.println(variable.variableName+" "+variable.value);
        }
        System.out.println(variables.get(0).variableName);
    }
}
