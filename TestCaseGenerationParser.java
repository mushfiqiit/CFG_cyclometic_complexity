package cfg;

import java.util.ArrayList;
import java.util.HashMap;

public class TestCaseGenerationParser {
    ArrayList<String> Lines=new ArrayList<>();
    ArrayList<ConditionChecking> conditions=new ArrayList<>();
    ArrayList<Variable> variables=new ArrayList<>();
    SyntaxIdentifier checker=new SyntaxIdentifier();
    HashMap<String, Integer> map= new HashMap<>();
    public TestCaseGenerationParser(ArrayList<String> lines) {
        this.Lines=lines;
    }

    public void start() {
        for(String line:Lines) {
            int bracketFlag=0;
            ConditionChecking temporaryCondition=new ConditionChecking();
            String variable="";
            String comparison="";
            if(checker.isIf(line) || checker.isElseIf(line) || checker.isWhile(line)) {
                for(int i=0;i<line.length();i++) {
                    if (bracketFlag == 1 && line.charAt(i)!=')' && line.charAt(i)!='{' && line.charAt(i)!='}') {
                        if(checker.isComparisonSign(line.charAt(i))) {
                            comparison=comparison+line.charAt(i);
                            if(!variable.isEmpty())
                            {
                                Variable temporary=new Variable(variable, 0);
                                temporaryCondition.addVariable(temporary);
                                variable="";
                            }
                        }
                        else {
                            variable=variable+line.charAt(i);
                            if(!comparison.isEmpty()) {
                                temporaryCondition.setComparisonSign(comparison);
                                comparison="";
                            }
                        }
                    }
                    if (line.charAt(i) == '(') bracketFlag = 1;
                }
                if(!variable.isEmpty())
                {
                    Variable temporary=new Variable(variable, 0);
                    temporaryCondition.addVariable(temporary);
                    variable="";
                }
                conditions.add(temporaryCondition);
            }
            else if(checker.isInt(line)) {
                System.out.println(line);
                String temporary="";
                for(int i=0;i<line.length();i++)
                {
                    if(line.charAt(i)==',' || line.charAt(i)==' ')
                    {
                        Variable tempVariable=new Variable(temporary);
                        if(!temporary.equals("int") && !temporary.isEmpty()) {
                            variables.add(tempVariable);
                            map.put(temporary, 0);
                        }
                        temporary="";
                    }
                    else if(line.charAt(i)!=';')
                    {
                        temporary=temporary+line.charAt(i);
                    }
                }
                Variable tempVariable=new Variable(temporary);
                if(!temporary.equals("int")) variables.add(tempVariable);
                map.put(temporary, 0);
            }
        }
        for(ConditionChecking condition:conditions)
        {
            System.out.println(condition.comparisonSign);
            condition.printVariables();
        }

        for(Variable variable:variables) {
            System.out.println(variable.variableName+" "+variable.value);
        }

    }
}
