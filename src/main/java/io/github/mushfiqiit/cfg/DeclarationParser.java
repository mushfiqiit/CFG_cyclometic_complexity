package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Simple;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;

/**
 * Finds the {@code int} variables a function declares, in {@code int} declarations and in
 * {@code for} loop initializers, and where each one's value comes from.
 */
final class DeclarationParser {

    private final Map<String, DeclaredVariable> variables = new LinkedHashMap<>();

    /** Creates a parser with no variables yet. Use {@link #parse(ParsedSource)}. */
    private DeclarationParser() {
    }

    // ***************************************************************************
    // Finding declarations
    // ***************************************************************************

    /**
     * Finds the variables a source file declares, in line order. A name declared twice keeps its
     * first declaration.
     *
     * @param source the source to scan
     * @return the declared variables by name, in line order, as a read-only map
     */
    static Map<String, DeclaredVariable> parse(ParsedSource source) {
        DeclarationParser parser = new DeclarationParser();
        source.statements().stream().flatMap(Statement::withNested).forEach(parser::collectDeclaration);
        return Collections.unmodifiableMap(parser.variables);
    }

    /**
     * Adds the variables a single statement declares: those in an {@code int} declaration or in
     * a {@code for} loop initializer. Called for each statement in line order, so the first
     * declaration of a name wins.
     *
     * @param statement the statement
     */
    private void collectDeclaration(Statement statement) {
        if (statement instanceof Simple simple && SyntaxIdentifier.isIntDeclaration(simple.line().text())) {
            parseDeclaration(simple.line().text(), false);
        } else if (statement instanceof Loop loop && loop.kind() == Loop.Kind.FOR) {
            parseForInitializer(loop.header().text());
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
     * {@code int a = 5, b;}. A variable without a value is an input, one set to an integer that
     * fits in an {@code int} is fixed, and one set by any other expression is computed. A name declared twice keeps its
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
            OptionalInt value = Comparison.parseInteger(initializer);
            DeclaredVariable variable;
            if (loopCounter || (!initializer.isEmpty() && value.isEmpty())) {
                variable = DeclaredVariable.computed(name);
            } else if (initializer.isEmpty()) {
                variable = DeclaredVariable.input(name);
            } else {
                variable = DeclaredVariable.fixed(name, value.getAsInt());
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
        Matcher name = CTokens.IDENTIFIER.matcher(target);
        return name.lookingAt() ? name.group() : "";
    }
}
