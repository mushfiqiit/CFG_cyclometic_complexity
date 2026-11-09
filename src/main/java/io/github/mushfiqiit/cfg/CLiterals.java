package io.github.mushfiqiit.cfg;

/**
 * Finds C string and character literals, such as {@code "text"} and <code>'}'</code>, so that the
 * comments and braces inside them are not mistaken for code.
 */
final class CLiterals {

    /** Prevents instantiation; this class only has static methods. */
    private CLiterals() {
    }

    // ***************************************************************************
    // Finding literals
    // ***************************************************************************

    /**
     * Tells whether a character opens a literal.
     *
     * @param character the character to check
     * @return true for a double or single quote
     */
    static boolean isQuote(char character) {
        return character == '"' || character == '\'';
    }

    /**
     * Finds where a literal ends, skipping escaped characters such as {@code \"}.
     *
     * @param line the source line
     * @param start index of the literal's opening quote
     * @return the index just after the closing quote, or the line's length if the literal is not
     *         closed on this line
     */
    static int endOf(String line, int start) {
        char quote = line.charAt(start);
        int i = start + 1;
        while (i < line.length()) {
            char character = line.charAt(i);
            if (character == '\\') {
                i += 2;
            } else if (character == quote) {
                return i + 1;
            } else {
                i++;
            }
        }
        return line.length();
    }

    /**
     * Replaces every literal in a line, quotes included, with spaces, so only code is left.
     *
     * @param line the source line
     * @return the line with its literals blanked out and surrounding whitespace removed
     */
    static String blankOut(String line) {
        StringBuilder result = new StringBuilder(line.length());
        int i = 0;
        while (i < line.length()) {
            if (isQuote(line.charAt(i))) {
                int end = endOf(line, i);
                result.append(" ".repeat(end - i));
                i = end;
            } else {
                result.append(line.charAt(i));
                i++;
            }
        }
        return result.toString().strip();
    }
}
