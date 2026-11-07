package cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pins down how each kind of statement is recognized, including the known quirks. */
class SyntaxIdentifierTest {

    @ParameterizedTest
    @ValueSource(strings = {"if(a>b){", "  if (a > b) {", "\tif\t(x)"})
    void recognizesIfIgnoringWhitespace(String line) {
        assertTrue(SyntaxIdentifier.isIf(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {"iffy=2;", "else if(a){", "a = if;"})
    void doesNotMistakeOtherLinesForIf(String line) {
        assertFalse(SyntaxIdentifier.isIf(line));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "else{          | true  | false",
        "else if(a==b){ | true  | true",
        "elsewhere=1;   | true  | false",
        "if(a){         | false | false"})
    void elseAlsoMatchesElseIfAndWordsStartingWithElse(String line, boolean isElse, boolean isElseIf) {
        assertEquals(isElse, SyntaxIdentifier.isElse(line));
        assertEquals(isElseIf, SyntaxIdentifier.isElseIf(line));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "while(k>0){         | true  | false",
        "for(i=0;i<3;i++){   | false | true",
        "whileaway=4;        | false | false",
        "format=3;           | false | false"})
    void recognizesLoops(String line, boolean isWhile, boolean isFor) {
        assertEquals(isWhile, SyntaxIdentifier.isWhile(line));
        assertEquals(isFor, SyntaxIdentifier.isFor(line));
        assertEquals(isWhile || isFor, SyntaxIdentifier.isLoop(line));
    }

    @Test
    void intIgnoresSpacesButNotTabs() {
        assertTrue(SyntaxIdentifier.isInt("    int a, b;"));
        assertFalse(SyntaxIdentifier.isInt("\tint main(){"));
    }

    @Test
    void endsBlockLooksAtTheLastNonWhitespaceCharacter() {
        assertTrue(SyntaxIdentifier.endsBlock("    }  "));
        assertFalse(SyntaxIdentifier.endsBlock("    a=1;"));
    }

    @Test
    void endsBlockThrowsOnABlankLine() {
        assertThrows(StringIndexOutOfBoundsException.class, () -> SyntaxIdentifier.endsBlock("   "));
    }

    @ParameterizedTest
    @CsvSource({"<, true", ">, true", "=, true", "!, true", "a, false", "+, false"})
    void recognizesComparisonCharacters(char character, boolean expected) {
        assertEquals(expected, SyntaxIdentifier.isComparisonSign(character));
    }
}
