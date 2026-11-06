package cfg;

public class Variable {
    public String variableName;
    public int value;
    public Variable(String VariableName, int Value) {
        this.variableName=VariableName;
        this.value=Value;
    }

    public Variable(String VariableName) {
        this.variableName=VariableName;
        this.value=0;
    }
}
