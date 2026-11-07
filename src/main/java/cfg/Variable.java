package cfg;

/**
 * A variable found in the source code, with its test value.
 *
 * @param name the variable's name as written in the source
 * @param value the test value assigned to the variable
 */
public record Variable(String name, int value) {

    /**
     * Creates a variable that has not been given a test value yet, so its value is 0.
     *
     * @param name the variable's name as written in the source
     * @return a variable with the given name and a value of 0
     */
    public static Variable from(String name) {
        return new Variable(name, 0);
    }

    /**
     * Returns the name and value separated by a space, such as {@code "a 0"}.
     *
     * @return the printable form of this variable
     */
    @Override
    public String toString() {
        return name + " " + value;
    }
}
