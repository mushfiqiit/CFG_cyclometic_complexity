package io.github.mushfiqiit.cfg;

import java.math.BigInteger;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * The C tokens the program recognizes, defined once so that every class reads them the same way.
 * Each pattern describes the token alone; use {@link Pattern#pattern()} to build larger patterns
 * from it.
 */
final class CTokens {

    /** A variable or function name, such as {@code count} or {@code _tmp1}. */
    static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");

    /**
     * A decimal integer with an optional minus sign directly before it, such as {@code 42} or
     * {@code -5}. Source text that puts spaces after the minus sign, as in {@code - 5}, must go
     * through {@link #joinMinusSigns(String)} first.
     */
    static final Pattern INTEGER = Pattern.compile("-?\\d+");

    /**
     * The start of a function header: a return type, which may include {@code *} and several
     * words such as {@code unsigned int}, then the function's name and {@code (}. For example,
     * <code>int main(void) {</code> or <code>static char *name(int i) {</code>. Use it with
     * {@link java.util.regex.Matcher#lookingAt()} on a line without leading whitespace.
     */
    static final Pattern FUNCTION_HEADER =
            Pattern.compile("[A-Za-z_][\\w\\s*]*[\\s*]" + IDENTIFIER.pattern() + "\\s*\\(");

    private static final Pattern SPACED_MINUS = Pattern.compile("-\\s+(?=\\d)");

    /** Prevents instantiation; this class only holds patterns and helpers that use them. */
    private CTokens() {
    }

    /**
     * Tells whether text is a variable or function name.
     *
     * @param text the text to check
     * @return true for a name such as {@code k} or {@code count_2}
     */
    static boolean isIdentifier(String text) {
        return IDENTIFIER.matcher(text).matches();
    }

    /**
     * Reads an integer that fits in an {@code int}. Larger numbers are not read, so they never
     * cause an exception.
     *
     * @param text the text to read
     * @return the integer, or an empty value if the text is not a whole number or does not fit
     *         in an {@code int}
     */
    static OptionalInt parseInteger(String text) {
        if (!INTEGER.matcher(text).matches()) {
            return OptionalInt.empty();
        }
        BigInteger value = new BigInteger(text);
        return value.bitLength() < Integer.SIZE ? OptionalInt.of(value.intValue()) : OptionalInt.empty();
    }

    /**
     * Tells whether a line starts with a function header.
     *
     * @param line the source line; leading whitespace is ignored
     * @return true if the line starts with a return type, a name and {@code (}
     */
    static boolean startsFunctionHeader(String line) {
        return FUNCTION_HEADER.matcher(line.strip()).lookingAt();
    }

    /**
     * Removes the spaces between a minus sign and the digits after it, so {@code a < - 5} becomes
     * {@code a < -5} and the number matches {@link #INTEGER}.
     *
     * @param text the source text
     * @return the text with each minus sign joined to the number that follows it
     */
    static String joinMinusSigns(String text) {
        return SPACED_MINUS.matcher(text).replaceAll("-");
    }
}
