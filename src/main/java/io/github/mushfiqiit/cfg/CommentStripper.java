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
                } else if (CLiterals.isQuote(line.charAt(i))) {
                    int end = CLiterals.endOf(line, i);
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
}
