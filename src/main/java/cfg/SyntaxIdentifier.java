package cfg;

/** Recognizes the kind of C statement on a source line. Whitespace is ignored unless noted. */
public final class SyntaxIdentifier {

    private SyntaxIdentifier() {
    }

    public static boolean isIf(String statement) {
        return startsWith(statement, "if(");
    }

    public static boolean isElseIf(String statement) {
        return startsWith(statement, "elseif(");
    }

    /** Matches any line starting with {@code else}, so this is also true for {@code else if}. */
    public static boolean isElse(String statement) {
        return startsWith(statement, "else");
    }

    public static boolean isWhile(String statement) {
        return startsWith(statement, "while(");
    }

    public static boolean isFor(String statement) {
        return startsWith(statement, "for(");
    }

    public static boolean isLoop(String statement) {
        return isFor(statement) || isWhile(statement);
    }

    /**
     * Tells whether the line closes a block.
     *
     * @throws StringIndexOutOfBoundsException if the line is blank
     */
    public static boolean endsBlock(String statement) {
        String compact = removeWhitespace(statement);
        return compact.charAt(compact.length() - 1) == '}';
    }

    public static boolean isComparisonSign(char character) {
        return character == '<' || character == '>' || character == '=' || character == '!';
    }

    /** Only spaces are ignored here, so a tab-indented declaration does not match. */
    public static boolean isInt(String statement) {
        return statement.replace(" ", "").startsWith("int");
    }

    private static boolean startsWith(String statement, String prefix) {
        return removeWhitespace(statement).startsWith(prefix);
    }

    private static String removeWhitespace(String statement) {
        return statement.replaceAll("\\s", "");
    }
}
