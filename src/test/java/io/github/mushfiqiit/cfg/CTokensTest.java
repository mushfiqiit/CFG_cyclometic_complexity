package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks the shared C token patterns. */
class CTokensTest {

    @ParameterizedTest
    @ValueSource(strings = {"a", "_tmp", "count2", "Main"})
    void identifiers(String text) {
        assertTrue(CTokens.IDENTIFIER.matcher(text).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2a", "a-b", "", "-x"})
    void notIdentifiers(String text) {
        assertFalse(CTokens.IDENTIFIER.matcher(text).matches());
    }

    @Test
    void integersHaveTheirMinusSignDirectlyBeforeTheDigits() {
        assertTrue(CTokens.INTEGER.matcher("42").matches());
        assertTrue(CTokens.INTEGER.matcher("-5").matches());
        assertFalse(CTokens.INTEGER.matcher("- 5").matches());
        assertFalse(CTokens.INTEGER.matcher("5a").matches());
    }

    @Test
    void joinMinusSignsOnlyJoinsASignToTheNumberAfterIt() {
        assertEquals("a < -5", CTokens.joinMinusSigns("a < - 5"));
        assertEquals("a < -5", CTokens.joinMinusSigns("a < -\t5"));
        assertEquals("a - b", CTokens.joinMinusSigns("a - b"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"int main(void) {", "  int main(){", "static char *name(int i) {", "unsigned int f (void)",
            "int const *f(void);"})
    void functionHeaders(String line) {
        assertTrue(CTokens.startsFunctionHeader(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {"int x = f(3);", "int a, b(", "if (a > b) {", "while(k>0){", "f(x);", "int (*fp)(int);"})
    void notFunctionHeaders(String line) {
        assertFalse(CTokens.startsFunctionHeader(line));
    }
}
