package io.github.mushfiqiit.cfg;

import java.util.regex.Pattern;

/** Recognizes the kind of C statement on a source line. Whitespace is ignored unless noted. */
public final class SyntaxIdentifier {

    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern ELSE_KEYWORD = Pattern.compile("^\\s*else\\b");
    private static final Pattern INT_KEYWORD = Pattern.compile("^\\s*int\\s");
    private static final Pattern FUNCTION_HEADER = Pattern.compile("^\\s*int\\s+\\**\\s*[A-Za-z_]\\w*\\s*\\(");

    /** Prevents instantiation; this class only has static methods. */
    private SyntaxIdentifier() {
    }

    // ***************************************************************************
    // Branch statements
    // ***************************************************************************

    /**
     * Tells whether a line starts an {@code if} statement.
     *
     * @param statement the source line to check
     * @return true if the line starts with {@code if(}
     */
    public static boolean isIf(String statement) {
        return startsWith(statement, "if(");
    }

    /**
     * Tells whether a line starts an {@code else if} statement.
     *
     * @param statement the source line to check
     * @return true if the line starts with {@code else if(}
     */
    public static boolean isElseIf(String statement) {
        return startsWith(statement, "elseif(");
    }

    /**
     * Tells whether a line starts with the {@code else} keyword. This is also true for
     * {@code else if}, but not for words that merely start with those letters, such as
     * {@code elsewhere = 1;}.
     *
     * @param statement the source line to check
     * @return true if the line starts with the keyword {@code else}
     */
    public static boolean isElse(String statement) {
        return ELSE_KEYWORD.matcher(statement).find();
    }

    // ***************************************************************************
    // Loop statements
    // ***************************************************************************

    /**
     * Tells whether a line starts a {@code while} loop.
     *
     * @param statement the source line to check
     * @return true if the line starts with {@code while(}
     */
    public static boolean isWhile(String statement) {
        return startsWith(statement, "while(");
    }

    /**
     * Tells whether a line starts a {@code for} loop.
     *
     * @param statement the source line to check
     * @return true if the line starts with {@code for(}
     */
    public static boolean isFor(String statement) {
        return startsWith(statement, "for(");
    }

    /**
     * Tells whether a line starts a {@code for} or {@code while} loop.
     *
     * @param statement the source line to check
     * @return true if the line starts either kind of loop
     */
    public static boolean isLoop(String statement) {
        return isFor(statement) || isWhile(statement);
    }

    // ***************************************************************************
    // Declarations and block ends
    // ***************************************************************************

    /**
     * Tells whether a line declares {@code int} variables, such as {@code int a, b = 2;}. A
     * function header such as <code>int main(void) {</code> is not a declaration, and neither is a word
     * that merely starts with {@code int}, such as {@code integer z;}.
     *
     * @param statement the source line to check
     * @return true if the line starts with the keyword {@code int} and does not declare a function
     */
    public static boolean isIntDeclaration(String statement) {
        return INT_KEYWORD.matcher(statement).find() && !FUNCTION_HEADER.matcher(statement).find();
    }

    /**
     * Tells whether the line closes a block.
     *
     * @param statement the source line to check
     * @return true if the line ends with <code>}</code>; false for a blank line
     */
    public static boolean endsBlock(String statement) {
        String compact = removeWhitespace(statement);
        return !compact.isEmpty() && compact.charAt(compact.length() - 1) == '}';
    }

    // ***************************************************************************
    // Single characters
    // ***************************************************************************

    /**
     * Tells whether a character can be part of a comparison operator.
     *
     * @param character the character to check
     * @return true for {@code <}, {@code >}, {@code =} and {@code !}
     */
    public static boolean isComparisonSign(char character) {
        return character == '<' || character == '>' || character == '=' || character == '!';
    }

    // ***************************************************************************
    // Helpers
    // ***************************************************************************

    /**
     * Tells whether a line starts with a prefix once all whitespace is removed from the line.
     *
     * @param statement the source line to check
     * @param prefix the text to look for, written without whitespace
     * @return true if the line starts with the prefix
     */
    private static boolean startsWith(String statement, String prefix) {
        return removeWhitespace(statement).startsWith(prefix);
    }

    /**
     * Removes every whitespace character from a line.
     *
     * @param statement the source line
     * @return the line without spaces, tabs or other whitespace
     */
    private static String removeWhitespace(String statement) {
        return WHITESPACE.matcher(statement).replaceAll("");
    }
}
