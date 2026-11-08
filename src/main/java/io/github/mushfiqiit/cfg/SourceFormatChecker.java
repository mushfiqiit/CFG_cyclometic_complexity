package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.Block;
import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.Function;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Line;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Checks that C source code uses the layout the analyses understand, so that a file they would
 * misread is rejected with clear messages instead of giving wrong results.
 *
 * <p>The supported layout is one function, optionally preceded by {@code #} lines such as
 * {@code #include}, with one statement per line. Each {@code if}, {@code else if}, {@code else},
 * {@code for} and {@code while} line ends with <code>{</code>, and each <code>}</code> is on its
 * own line. The source must have been read from code whose comments {@link CommentStripper} removed.
 */
final class SourceFormatChecker {

    /** A line that breaks the supported layout, with an explanation. */
    record Problem(int lineNumber, String message) {

        /**
         * Returns the problem as {@code "line 4: message"}.
         *
         * @return the printable form of the problem
         */
        @Override
        public String toString() {
            return "line " + lineNumber + ": " + message;
        }
    }

    /** A return type, then a name followed by "(", as in "int main(" or "static char *name(". */
    private static final Pattern FUNCTION_HEADER = Pattern.compile("[A-Za-z_][\\w\\s*]*[\\s*][A-Za-z_]\\w*\\s*\\(");

    /** Prevents instantiation; this class only has static methods. */
    private SourceFormatChecker() {
    }

    // ***************************************************************************
    // Checking a file
    // ***************************************************************************

    /**
     * Checks source code against the supported layout. Problems within single lines are reported
     * for every line; problems with the overall structure are only checked when every line is
     * well formed, because a misplaced brace would make them misleading.
     *
     * @param source the source to check, with comments removed; line numbers count from 1
     * @return the problems found, in line order; empty if the code can be analyzed
     */
    static List<Problem> check(ParsedSource source) {
        List<String> code = source.lines();
        int functionLine = firstCodeLine(code);
        if (functionLine < 0) {
            return List.of(new Problem(Math.max(code.size(), 1),
                    "there is no code to analyze; the file needs a function such as \"int main(void) {\""));
        }
        String header = withoutLiterals(code.get(functionLine).strip());
        if (!isFunctionHeader(header)) {
            return List.of(new Problem(functionLine + 1,
                    "expected the function to analyze, such as \"int main(void) {\", with { at the end of the line;"
                    + " only # lines such as #include may come before it"));
        }

        List<Problem> problems = new ArrayList<>();
        for (int index = functionLine + 1; index < code.size(); index++) {
            checkLine(code.get(index).strip(), index + 1, problems);
        }
        if (problems.isEmpty()) {
            source.function().ifPresent(function -> checkStructure(code, function, problems));
        }
        return problems;
    }

    /**
     * Finds the first line that is neither blank nor a {@code #} line.
     *
     * @param code the source lines with comments removed
     * @return the line's index, or -1 if there is none
     */
    private static int firstCodeLine(List<String> code) {
        for (int index = 0; index < code.size(); index++) {
            String line = code.get(index).strip();
            if (!line.isEmpty() && !line.startsWith("#")) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Tells whether a line opens a function, such as <code>int main(void) {</code>.
     *
     * @param line the line, stripped and with literals blanked out
     * @return true for a line with a return type, a name and parentheses that ends with its only
     *         <code>{</code>
     */
    private static boolean isFunctionHeader(String line) {
        return FUNCTION_HEADER.matcher(line).lookingAt() && line.endsWith("{") && count(line, '{') == 1
                && count(line, '}') == 0 && !isControlHeader(line);
    }

    // ***************************************************************************
    // Checking single lines
    // ***************************************************************************

    /**
     * Checks where a line's braces are and whether its block is supported.
     *
     * @param line the line, stripped, with comments removed
     * @param lineNumber the line's 1-based number
     * @param problems where to add any problem found
     */
    private static void checkLine(String line, int lineNumber, List<Problem> problems) {
        if (line.isEmpty()) {
            return;
        }
        String bare = withoutLiterals(line);
        int opens = count(bare, '{');
        int closes = count(bare, '}');

        if (opens > 0 && opens == closes && bare.endsWith(";")) {
            return; // An initializer such as "int a[2] = {1, 2};" is an ordinary statement.
        }
        if (closes > 0 && !bare.equals("}")) {
            problems.add(new Problem(lineNumber, "put each } on a line of its own"
                    + (opens > 0 ? ", and start the next block on a new line, as in:  }  then  else {" : "")));
        } else if (bare.equals("{")) {
            problems.add(new Problem(lineNumber,
                    "put { at the end of the if, else, for or while line above, not on a line of its own"));
        } else if (opens > 0 && (opens > 1 || !bare.endsWith("{"))) {
            problems.add(new Problem(lineNumber, "put each statement on its own line; { must be the last character"));
        } else if (opens == 1 && isFunctionHeader(bare)) {
            problems.add(new Problem(lineNumber,
                    "only one function can be analyzed; put each function in a file of its own"));
        } else if (opens == 1 && !isControlHeader(line)) {
            problems.add(new Problem(lineNumber,
                    "only if, else if, else, for and while blocks are supported, not do, switch or others"));
        } else if (opens == 0 && closes == 0 && isControlHeader(line)) {
            problems.add(new Problem(lineNumber,
                    "end this line with {; an if, else, for or while without braces is not supported"));
        }
    }

    // ***************************************************************************
    // Checking the overall structure
    // ***************************************************************************

    /**
     * Checks the function's structure: that each {@code else} follows the end of an {@code if} or
     * {@code else if}, that nothing comes after the function, and that every block is closed.
     * Only the first of these problems in the file is reported, because the others usually
     * follow from it.
     *
     * @param code the source lines with comments removed
     * @param function the function's structure
     * @param problems where to add the problem found, if any
     */
    private static void checkStructure(List<String> code, Function function, List<Problem> problems) {
        List<Problem> found = new ArrayList<>();
        findElsesWithoutIf(function.body(), found);
        function.body().closing().ifPresent(end -> firstLineAfter(code, end).ifPresent(line -> found.add(new Problem(
                line.number(), "only one function can be analyzed, and this line comes after the function that ends on line "
                        + end.number()))));

        found.stream().min(Comparator.comparingInt(Problem::lineNumber)).ifPresentOrElse(problems::add,
                () -> innermostUnclosed(function.header(), function.body()).ifPresent(line -> problems.add(
                        new Problem(line.number(), "this block is never closed; add a } on its own line"))));
    }

    /**
     * Finds every {@code if} chain that starts with an {@code else}, at any depth.
     *
     * @param block the block to search
     * @param found where to add a problem for each one
     */
    private static void findElsesWithoutIf(Block block, List<Problem> found) {
        for (Statement statement : block.statements()) {
            if (statement instanceof IfChain chain) {
                Line first = chain.branches().get(0).header();
                if (SyntaxIdentifier.isElse(first.text())) {
                    found.add(new Problem(first.number(),
                            "this else has no matching if; it must come right after the } that closes an if or else if"));
                }
                chain.branches().forEach(branch -> findElsesWithoutIf(branch.body(), found));
            } else if (statement instanceof Loop loop) {
                findElsesWithoutIf(loop.body(), found);
            }
        }
    }

    /**
     * Finds the first non-blank line after the function.
     *
     * @param code the source lines with comments removed
     * @param end the function's closing line
     * @return the line, or an empty value if only blank lines follow
     */
    private static Optional<Line> firstLineAfter(List<String> code, Line end) {
        for (int index = end.index() + 1; index < code.size(); index++) {
            if (!code.get(index).isBlank()) {
                return Optional.of(new Line(index, code.get(index)));
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the innermost block that the file ends inside. Such blocks are always the last
     * statement of the block around them.
     *
     * @param opener the line that opens {@code block}
     * @param block the block to search
     * @return the opening line of the innermost unclosed block, or an empty value if
     *         {@code block} is closed
     */
    private static Optional<Line> innermostUnclosed(Line opener, Block block) {
        if (block.closing().isPresent()) {
            return Optional.empty();
        }
        List<Statement> statements = block.statements();
        Statement last = statements.isEmpty() ? null : statements.get(statements.size() - 1);
        Optional<Line> inner = Optional.empty();
        if (last instanceof Loop loop) {
            inner = innermostUnclosed(loop.header(), loop.body());
        } else if (last instanceof IfChain chain) {
            Branch lastBranch = chain.branches().get(chain.branches().size() - 1);
            inner = innermostUnclosed(lastBranch.header(), lastBranch.body());
        }
        return inner.isPresent() ? inner : Optional.of(opener);
    }

    // ***************************************************************************
    // Helpers
    // ***************************************************************************

    /**
     * Tells whether a line starts an {@code if}, {@code else if}, {@code else}, {@code for} or
     * {@code while}.
     *
     * @param line the line to check
     * @return true for those lines
     */
    private static boolean isControlHeader(String line) {
        return SyntaxIdentifier.isIf(line) || SyntaxIdentifier.isElse(line) || SyntaxIdentifier.isLoop(line);
    }

    /**
     * Replaces the contents of string and character literals with spaces, so braces inside them,
     * as in {@code printf("}")}, are not counted.
     *
     * @param line the line to clean
     * @return the line with each literal's characters replaced, quotes included
     */
    private static String withoutLiterals(String line) {
        StringBuilder result = new StringBuilder(line.length());
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (quote == 0) {
                if (character == '"' || character == '\'') {
                    quote = character;
                    result.append(' ');
                } else {
                    result.append(character);
                }
            } else {
                if (character == '\\') {
                    i++;
                    result.append(' ');
                } else if (character == quote) {
                    quote = 0;
                }
                result.append(' ');
            }
        }
        return result.toString().strip();
    }

    /**
     * Counts how often a character appears in a line.
     *
     * @param line the line to search
     * @param character the character to count
     * @return the number of times it appears
     */
    private static int count(String line, char character) {
        return (int) line.chars().filter(c -> c == character).count();
    }
}
