package cfg;

/** Recognizes the kind of C statement on a source line. Whitespace is ignored unless noted. */
public final class SyntaxIdentifier {

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
     * Tells whether a line starts with {@code else}. This is also true for {@code else if}, and for
     * any other line starting with those letters, such as {@code elsewhere = 1;}.
     *
     * @param statement the source line to check
     * @return true if the line starts with {@code else}
     */
    public static boolean isElse(String statement) {
        return startsWith(statement, "else");
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
     * Tells whether a line starts with {@code int}, such as an {@code int} declaration.
     *
     * <p>Only spaces are ignored here, so a tab-indented declaration does not match.
     *
     * @param statement the source line to check
     * @return true if the line starts with {@code int}
     */
    public static boolean isInt(String statement) {
        return statement.replace(" ", "").startsWith("int");
    }

    /**
     * Tells whether the line closes a block.
     *
     * @param statement the source line to check
     * @return true if the line ends with <code>}</code>
     * @throws StringIndexOutOfBoundsException if the line is blank
     */
    public static boolean endsBlock(String statement) {
        String compact = removeWhitespace(statement);
        return compact.charAt(compact.length() - 1) == '}';
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
        return statement.replaceAll("\\s", "");
    }
}
