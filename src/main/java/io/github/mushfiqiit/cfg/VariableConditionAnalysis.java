package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.TestCaseGenerator.TestCase;
import java.util.List;
import java.util.Map;

/**
 * The result of the test case data option: the function's {@code int} variables, the conditions
 * of its {@code if}, {@code else if} and {@code while} statements, and a test case for each
 * outcome of each condition. Printing is left to {@link TestCaseReport}, so the result can be
 * shown or checked in any other way too.
 *
 * @param variables the declared variables, in line order
 * @param undeclaredInputs variables the conditions use without declaring them, which are treated
 *        as inputs
 * @param conditions the conditions, in line order, with the paths that lead to them
 * @param testCases two test cases per condition, the true outcome first
 */
record VariableConditionAnalysis(List<DeclaredVariable> variables, List<String> undeclaredInputs,
        List<ConditionSite> conditions, List<TestCase> testCases) {

    /**
     * Creates an analysis, keeping read-only copies of the lists.
     *
     * @param variables the declared variables
     * @param undeclaredInputs the undeclared variables used as inputs
     * @param conditions the conditions
     * @param testCases the test cases
     */
    VariableConditionAnalysis {
        variables = List.copyOf(variables);
        undeclaredInputs = List.copyOf(undeclaredInputs);
        conditions = List.copyOf(conditions);
        testCases = List.copyOf(testCases);
    }

    /**
     * Analyzes a source file: finds its variables and conditions, and generates the test cases.
     *
     * @param source the source to analyze
     * @return the analysis
     */
    static VariableConditionAnalysis of(ParsedSource source) {
        Map<String, DeclaredVariable> declared = DeclarationParser.parse(source);
        List<ConditionSite> conditions = ConditionPaths.find(source);
        List<String> undeclared = TestCaseGenerator.inputNames(conditions, declared).stream()
                .filter(name -> !declared.containsKey(name))
                .toList();
        return new VariableConditionAnalysis(List.copyOf(declared.values()), undeclared, conditions,
                TestCaseGenerator.generate(conditions, declared));
    }

    /**
     * Counts the test cases for which input values were found.
     *
     * @return the number of test cases with values
     */
    long coveredCount() {
        return testCases.stream().filter(testCase -> testCase.inputs().isPresent()).count();
    }
}
