package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Simple;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import io.github.mushfiqiit.cfg.TestCaseGenerator.TestCase;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Runs the test case data option: lists the function's {@code int} variables and the conditions
 * of its {@code if}, {@code else if} and {@code while} statements, then generates a test case for
 * each outcome of each condition.
 */
public final class VariableConditionAnalysis {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private final Map<String, DeclaredVariable> variables = new LinkedHashMap<>();
    private List<ConditionSite> conditions = List.of();

    /** Creates an empty analysis. Use {@link #run(ParsedSource, PrintStream)} to analyze a source file. */
    private VariableConditionAnalysis() {
    }

    // ***************************************************************************
    // Running the analysis
    // ***************************************************************************

    /**
     * Analyzes a source file and prints its variables, its conditions and a test case for each
     * outcome of each condition.
     *
     * @param source the source to analyze
     * @param out where to print the report
     */
    static void run(ParsedSource source, PrintStream out) {
        VariableConditionAnalysis analysis = new VariableConditionAnalysis();
        analysis.collect(source);
        analysis.printResults(out);
    }

    /**
     * Collects the declared variables and the conditions, with the paths that lead to them.
     *
     * @param source the source to scan
     */
    private void collect(ParsedSource source) {
        collectDeclarations(source.statements());
        conditions = ConditionPaths.find(source);
    }

    /**
     * Adds the variables declared in a list of statements and in the blocks inside them, in line
     * order: those in {@code int} declarations and in {@code for} loop initializers.
     *
     * @param statements the statements to scan
     */
    private void collectDeclarations(List<Statement> statements) {
        for (Statement statement : statements) {
            if (statement instanceof Simple simple && SyntaxIdentifier.isIntDeclaration(simple.line().text())) {
                parseDeclaration(simple.line().text(), false);
            } else if (statement instanceof Loop loop) {
                if (SyntaxIdentifier.isFor(loop.header().text())) {
                    parseForInitializer(loop.header().text());
                }
                collectDeclarations(loop.body().statements());
            } else if (statement instanceof IfChain chain) {
                chain.branches().forEach(branch -> collectDeclarations(branch.body().statements()));
            }
        }
    }

    // ***************************************************************************
    // Parsing declarations
    // ***************************************************************************

    /**
     * Adds the variables declared in a {@code for} loop's initializer, such as {@code i} in
     * {@code for (int i = 0; i < n; i++)}. The loop changes them, so they are computed.
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
            parseDeclaration(initializer, true);
        }
    }

    /**
     * Adds each variable in an {@code int} declaration, such as {@code a} and {@code b} in
     * {@code int a = 5, b;}. A variable without a value is an input, one set to an integer is
     * fixed, and one set by any other expression is computed. A name declared twice keeps its
     * first declaration.
     *
     * @param declaration text that starts with the keyword {@code int}
     * @param loopCounter whether the variables belong to a {@code for} loop, which changes them
     */
    private void parseDeclaration(String declaration, boolean loopCounter) {
        String declarators = declaration.strip().substring("int".length());
        for (String declarator : splitDeclarators(declarators)) {
            String name = declaredName(declarator);
            if (name.isEmpty() || variables.containsKey(name)) {
                continue;
            }
            String[] parts = declarator.split("=", 2);
            String initializer = parts.length == 2 ? parts[1].strip() : "";
            DeclaredVariable variable;
            if (loopCounter || (!initializer.isEmpty() && !INTEGER.matcher(initializer).matches())) {
                variable = DeclaredVariable.computed(name);
            } else if (initializer.isEmpty()) {
                variable = DeclaredVariable.input(name);
            } else {
                variable = DeclaredVariable.fixed(name, Integer.parseInt(initializer));
            }
            variables.put(name, variable);
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

    /**
     * Prints the variables, the conditions to cover and the generated test cases.
     *
     * @param out where to print
     */
    private void printResults(PrintStream out) {
        ConsoleText.printTitle(out, "Test Case Data");
        printVariables(out);
        printConditions(out);
        printTestCases(out);
    }

    /**
     * Prints each variable a test case uses and where its value comes from.
     *
     * @param out where to print
     */
    private void printVariables(PrintStream out) {
        ConsoleText.printSection(out, "Variables");
        List<String> undeclared = TestCaseGenerator.inputNames(conditions, variables).stream()
                .filter(name -> !variables.containsKey(name)).toList();
        if (variables.isEmpty() && undeclared.isEmpty()) {
            out.println("  No int variables were declared.");
            return;
        }
        ConsoleTable table = new ConsoleTable("Variable", "Value");
        variables.values().forEach(variable -> table.addRow(variable.name(), variable.describeSource()));
        undeclared.forEach(name -> table.addRow(name, "input (used but not declared)"));
        table.print(out);
    }

    /**
     * Prints each condition with its operator and operands.
     *
     * @param out where to print
     */
    private void printConditions(PrintStream out) {
        ConsoleText.printSection(out, "Conditions to cover");
        if (conditions.isEmpty()) {
            out.println("  No if, else if or while conditions were found.");
            return;
        }
        ConsoleTable table = new ConsoleTable("#", "Line", "Condition", "Operator", "Operands").alignRight(0, 1);
        for (int i = 0; i < conditions.size(); i++) {
            Condition condition = conditions.get(i).condition();
            table.addRow(i + 1, conditions.get(i).lineNumber(), orDash(condition.getText()),
                    orDash(condition.getComparisonSign()), orDash(String.join(", ", condition.getOperands())));
        }
        table.print(out);
    }

    /**
     * Prints one test case per outcome of each condition, with the inputs that produce it.
     *
     * @param out where to print
     */
    private void printTestCases(PrintStream out) {
        ConsoleText.printSection(out, "Test cases");
        if (conditions.isEmpty()) {
            out.println("  No test cases: there are no conditions to cover.");
            return;
        }
        out.println("  Each condition is made true in one test case and false in another, using inputs");
        out.println("  that reach it. Inputs are the values at the start of the function; assignments");
        out.println("  made before a condition is checked are not taken into account.");
        out.println();

        List<TestCase> cases = TestCaseGenerator.generate(conditions, variables);
        ConsoleTable table = new ConsoleTable("#", "Covers", "Outcome", "Inputs").alignRight(0);
        for (int i = 0; i < cases.size(); i++) {
            TestCase testCase = cases.get(i);
            table.addRow(i + 1, testCase.site().describe(), testCase.outcome(), describeInputs(testCase));
        }
        table.print(out);

        long covered = cases.stream().filter(testCase -> testCase.inputs().isPresent()).count();
        out.println();
        out.println("  Values found for " + covered + " of " + cases.size() + " condition outcomes.");
    }

    /**
     * Describes a test case's inputs, such as {@code "a=1, b=0"}.
     *
     * @param testCase the test case
     * @return the inputs, {@code "none needed"} if there are no input variables, or the reason no
     *         values were found
     */
    private static String describeInputs(TestCase testCase) {
        if (testCase.inputs().isEmpty()) {
            return "no values: " + testCase.reason();
        }
        Map<String, Integer> inputs = testCase.inputs().get();
        if (inputs.isEmpty()) {
            return "none needed";
        }
        return inputs.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
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
