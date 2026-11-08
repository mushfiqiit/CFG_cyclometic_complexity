package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks that comments are removed while code, strings and line numbers are kept. */
class CommentStripperTest {

    @Test
    void removesLineCommentsAndTrailingWhitespace() {
        assertEquals(List.of("    a = 1;", "}", ""),
                CommentStripper.strip(List.of("    a = 1; // set a", "} // end while", "// only a comment")));
    }

    @Test
    void replacesAnInlineBlockCommentWithASpace() {
        assertEquals(List.of("int a;", "int b = 2;"),
                CommentStripper.strip(List.of("int/* type */a;", "int b = 2; /* two */")));
    }

    @Test
    void removesBlockCommentsSpanningSeveralLinesButKeepsTheLines() {
        assertEquals(List.of("a = 1;", "", "", "b = 2;"),
                CommentStripper.strip(List.of("a = 1; /* start", "   { still a comment }", "   end */", "b = 2;")));
    }

    @Test
    void leavesCommentMarkersInsideStringsAndCharacters() {
        List<String> code = List.of(
                "printf(\"// not a comment\");",
                "printf(\"/* nor this */\");",
                "char c = '\"'; // a quote",
                "printf(\"say \\\"hi\\\" // still a string\");");

        assertEquals(List.of(code.get(0), code.get(1), "char c = '\"';", code.get(3)),
                CommentStripper.strip(code));
    }

    @Test
    void resultCannotBeChanged() {
        List<String> code = CommentStripper.strip(List.of("a = 1;"));

        assertThrows(UnsupportedOperationException.class, () -> code.add("b = 2;"));
    }
}
