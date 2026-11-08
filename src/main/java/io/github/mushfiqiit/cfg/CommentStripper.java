package io.github.mushfiqiit.cfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes C comments from source lines, so that only code is analyzed. String and character
 * literals are left untouched, so {@code "//"} inside a string is not mistaken for a comment.
 */
final class CommentStripper {

    /** Prevents instantiation; this class only has static methods. */
    private CommentStripper() {
    }

    // ***************************************************************************
    // Removing comments
    // ***************************************************************************

    /**
     * Removes {@code //} comments and {@code /* *}{@code /} comments, including comments that span
     * several lines. A comment between two pieces of code is replaced by one space. Every line is
     * kept, so line numbers do not change, and trailing whitespace is removed.
     *
     * @param lines the source lines
     * @return the lines without comments, as a read-only list of the same length
     */
    static List<String> strip(List<String> lines) {
        List<String> code = new ArrayList<>(lines.size());
        boolean inBlockComment = false;
        for (String line : lines) {
            StringBuilder kept = new StringBuilder();
            int i = 0;
            while (i < line.length()) {
                if (inBlockComment) {
                    if (line.startsWith("*/", i)) {
                        inBlockComment = false;
                        i += 2;
                    } else {
                        i++;
                    }
                } else if (line.startsWith("//", i)) {
                    break;
                } else if (line.startsWith("/*", i)) {
                    inBlockComment = true;
                    kept.append(' ');
                    i += 2;
                } else if (line.charAt(i) == '"' || line.charAt(i) == '\'') {
                    int end = endOfLiteral(line, i);
                    kept.append(line, i, end);
                    i = end;
                } else {
                    kept.append(line.charAt(i));
                    i++;
                }
            }
            code.add(kept.toString().stripTrailing());
        }
        return List.copyOf(code);
    }

    // ***************************************************************************
    // Helpers
    // ***************************************************************************

    /**
     * Finds where a string or character literal ends, skipping escaped characters such as
     * {@code \"}.
     *
     * @param line the source line
     * @param start index of the literal's opening quote
     * @return the index just after the closing quote, or the line's length if the literal is not
     *         closed on this line
     */
    private static int endOfLiteral(String line, int start) {
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
}
