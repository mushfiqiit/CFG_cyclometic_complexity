package io.github.mushfiqiit.cfg;

/**
 * An {@code int} variable declared in the analyzed function, and whether a test case can choose
 * its value.
 *
 * @param name the variable's name
 * @param source where the variable's value comes from
 * @param value the declared value, for a {@link Source#FIXED} variable; otherwise 0
 */
record DeclaredVariable(String name, Source source, int value) {

    /** Where a variable's value comes from. */
    enum Source {
        /** Declared without a value, so a test case chooses it. */
        INPUT,
        /** Declared with a literal value, such as {@code int x = 5;}. */
        FIXED,
        /** Set by an expression or a loop, such as {@code int n = f(1);} or a {@code for} counter. */
        COMPUTED
    }

    /**
     * Creates a variable a test case chooses.
     *
     * @param name the variable's name
     * @return the variable
     */
    static DeclaredVariable input(String name) {
        return new DeclaredVariable(name, Source.INPUT, 0);
    }

    /**
     * Creates a variable whose value is set when it is declared.
     *
     * @param name the variable's name
     * @param value the declared value
     * @return the variable
     */
    static DeclaredVariable fixed(String name, int value) {
        return new DeclaredVariable(name, Source.FIXED, value);
    }

    /**
     * Creates a variable whose value is computed by the code.
     *
     * @param name the variable's name
     * @return the variable
     */
    static DeclaredVariable computed(String name) {
        return new DeclaredVariable(name, Source.COMPUTED, 0);
    }

    /**
     * Describes where the value comes from, for the report.
     *
     * @return {@code "input"}, the declared value, or {@code "computed in the code"}
     */
    String describeSource() {
        return switch (source) {
            case INPUT -> "input";
            case FIXED -> value + " (set when declared)";
            case COMPUTED -> "computed in the code";
        };
    }
}
