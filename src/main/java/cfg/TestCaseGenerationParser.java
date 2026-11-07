package cfg;

import java.util.ArrayList;
import java.util.List;

/** Extracts the declared variables and the branch conditions from C source lines. */
public class TestCaseGenerationParser {

    private final List<String> lines;
    private final List<Condition> conditions = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();

    public TestCaseGenerationParser(List<String> lines) {
        this.lines = lines;
    }

    // ***************************************************************************
    // Running the parser
    // ***************************************************************************

    public void start() {
        for (String line : lines) {
            if (isConditional(line)) {
                conditions.add(parseCondition(line));
            } else if (SyntaxIdentifier.isInt(line)) {
                System.out.println(line);
                parseDeclaration(line);
            }
        }
        printResults();
    }

    // ***************************************************************************
    // Parsing conditions
    // ***************************************************************************

    private static boolean isConditional(String line) {
        return SyntaxIdentifier.isIf(line) || SyntaxIdentifier.isElseIf(line) || SyntaxIdentifier.isWhile(line);
    }

    private static Condition parseCondition(String line) {
        Condition condition = new Condition();
        StringBuilder variable = new StringBuilder();
        StringBuilder comparison = new StringBuilder();
        boolean insideParentheses = false;

        for (char character : line.toCharArray()) {
            if (insideParentheses && !isConditionDelimiter(character)) {
                if (SyntaxIdentifier.isComparisonSign(character)) {
                    comparison.append(character);
                    addPendingVariable(condition, variable);
                } else if (character != ' ') {
                    variable.append(character);
                    applyPendingComparison(condition, comparison);
                }
            }
            if (character == '(') {
                insideParentheses = true;
            }
        }
        addPendingVariable(condition, variable);
        return condition;
    }

    private static boolean isConditionDelimiter(char character) {
        return character == ')' || character == '{' || character == '}';
    }

    private static void addPendingVariable(Condition condition, StringBuilder variable) {
        if (variable.length() > 0) {
            condition.addVariable(new Variable(variable.toString()));
            variable.setLength(0);
        }
    }

    private static void applyPendingComparison(Condition condition, StringBuilder comparison) {
        if (comparison.length() > 0) {
            condition.setComparisonSign(comparison.toString());
            comparison.setLength(0);
        }
    }

    // ***************************************************************************
    // Parsing declarations
    // ***************************************************************************

    private void parseDeclaration(String line) {
        StringBuilder token = new StringBuilder();

        for (char character : line.toCharArray()) {
            if (character == ',' || character == ' ') {
                String name = token.toString();
                if (!name.equals("int") && !name.isEmpty()) {
                    variables.add(new Variable(name));
                }
                token.setLength(0);
            } else if (character != ';') {
                token.append(character);
            }
        }

        // Unlike the tokens above, the last one is kept even when it is empty.
        String lastName = token.toString();
        if (!lastName.equals("int")) {
            variables.add(new Variable(lastName));
        }
    }

    // ***************************************************************************
    // Printing results
    // ***************************************************************************

    private void printResults() {
        for (Condition condition : conditions) {
            System.out.println(condition.getComparisonSign());
            condition.printVariables();
        }
        variables.forEach(System.out::println);
    }
}
