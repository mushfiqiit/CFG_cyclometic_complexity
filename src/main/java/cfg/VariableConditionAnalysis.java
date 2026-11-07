package cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Runs the test case data option: lists the {@code int} variables declared in C source lines,
 * including those declared in a {@code for} loop, each with a test value of 0, and the conditions
 * of their {@code if}, {@code else if} and {@code while} statements.
 */
public final class VariableConditionAnalysis {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");

    /** A condition and the 1-based number of the line it was found on. */
    private record FoundCondition(int lineNumber, Condition condition) {
    }

    private final List<FoundCondition> conditions = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();

    /** Creates an empty analysis. Use {@link #run(List)} to analyze a source file. */
    private VariableConditionAnalysis() {
    }

    // ***************************************************************************
    // Running the analysis
    // ***************************************************************************

    /**
     * Analyzes a source file and prints the test input values and the conditions to cover.
     *
     * @param lines the source lines to analyze
     */
    public static void run(List<String> lines) {
        VariableConditionAnalysis analysis = new VariableConditionAnalysis();
        analysis.collect(lines);
        analysis.printResults();
    }

    /**
     * Collects the conditions and declared variables from every line.
     *
     * @param lines the source lines to scan
     */
    private void collect(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (isConditional(line)) {
                conditions.add(new FoundCondition(index + 1, Condition.from(line)));
            } else if (SyntaxIdentifier.isFor(line)) {
                parseForInitializer(line);
            } else if (SyntaxIdentifier.isIntDeclaration(line)) {
                parseDeclaration(line);
            }
        }
    }

    /**
     * Tells whether a line holds a condition to parse.
     *
     * @param line the source line to check
     * @return true for {@code if}, {@code else if} and {@code while} lines; {@code for} lines are
     *         not included
     */
    private static boolean isConditional(String line) {
        return SyntaxIdentifier.isIf(line) || SyntaxIdentifier.isElseIf(line) || SyntaxIdentifier.isWhile(line);
    }

    // ***************************************************************************
    // Parsing declarations
    // ***************************************************************************

    /**
     * Adds the variables declared in a {@code for} loop's initializer, such as {@code i} in
     * {@code for (int i = 0; i < n; i++)}.
     *
     * @param line a {@code for} line
     */
    private void parseForInitializer(String line) {
        int open = line.indexOf('(');
        int end = line.indexOf(';', open + 1);
        if (open < 0 || end < 0) {
            return;
        }
        String initializer = line.substring(open + 1, end);
        if (SyntaxIdentifier.isIntDeclaration(initializer)) {
            parseDeclaration(initializer);
        }
    }

    /**
     * Adds the name of each variable in an {@code int} declaration, such as {@code a} and
     * {@code b} in {@code int a = 5, b;}. Initial values, pointer stars and array sizes are not
     * part of a name.
     *
     * @param declaration text that starts with the keyword {@code int}
     */
    private void parseDeclaration(String declaration) {
        String declarators = declaration.strip().substring("int".length());
        for (String declarator : splitDeclarators(declarators)) {
            String name = declaredName(declarator);
            if (!name.isEmpty()) {
                variables.add(Variable.from(name));
            }
        }
    }

    /**
     * Splits the text after {@code int} at the commas between declarators, stopping at the
     * {@code ;} that ends the declaration. Commas inside parentheses, brackets or braces, as in
     * {@code a[2] = {1, 2}}, do not split.
     *
     * @param declarators the text after the {@code int} keyword
     * @return each declarator's text
     */
    private static List<String> splitDeclarators(String declarators) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (char character : declarators.toCharArray()) {
            if (depth == 0 && character == ';') {
                break;
            }
            if (depth == 0 && character == ',') {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            if ("([{".indexOf(character) >= 0) {
                depth++;
            } else if (")]}".indexOf(character) >= 0 && depth > 0) {
                depth--;
            }
            current.append(character);
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * Extracts the variable name from one declarator, such as {@code x} from {@code *x = 5} or
     * {@code arr} from {@code arr[10]}.
     *
     * @param declarator one comma-separated part of a declaration
     * @return the name, or an empty string if the declarator has none
     */
    private static String declaredName(String declarator) {
        String target = declarator.split("=", 2)[0].strip().replaceFirst("^\\*+\\s*", "");
        Matcher name = IDENTIFIER.matcher(target);
        return name.lookingAt() ? name.group() : "";
    }

    // ***************************************************************************
    // Printing results
    // ***************************************************************************

    /** Prints the test input values, then the conditions to cover. */
    private void printResults() {
        ConsoleText.printTitle("Test Case Data");

        ConsoleText.printSection("Test input values");
        if (variables.isEmpty()) {
            System.out.println("  No int variables were declared.");
        } else {
            ConsoleTable table = new ConsoleTable("Variable", "Value").alignRight(1);
            variables.forEach(variable -> table.addRow(variable.name(), variable.value()));
            table.print();
        }

        ConsoleText.printSection("Conditions to cover");
        if (conditions.isEmpty()) {
            System.out.println("  No if, else if or while conditions were found.");
        } else {
            ConsoleTable table = new ConsoleTable("#", "Line", "Condition", "Operator", "Operands")
                    .alignRight(0, 1);
            for (int i = 0; i < conditions.size(); i++) {
                FoundCondition found = conditions.get(i);
                Condition condition = found.condition();
                table.addRow(i + 1, found.lineNumber(), orDash(condition.getText()),
                        orDash(condition.getComparisonSign()), operandNames(condition));
            }
            table.print();
        }
    }

    /**
     * Lists a condition's operand names separated by commas.
     *
     * @param condition the condition to describe
     * @return the operand names, or {@code -} if there are none
     */
    private static String operandNames(Condition condition) {
        String names = condition.getOperands().stream().map(Variable::name).collect(Collectors.joining(", "));
        return orDash(names);
    }

    /**
     * Replaces a missing or empty value with {@code -}.
     *
     * @param value the value to show
     * @return the value, or {@code -} if it is null or empty
     */
    private static String orDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }
}
