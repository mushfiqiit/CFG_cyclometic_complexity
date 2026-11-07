package cfg;

/** A variable found in the source code, with its test value. */
public record Variable(String name, int value) {

    public Variable(String name) {
        this(name, 0);
    }

    @Override
    public String toString() {
        return name + " " + value;
    }
}
