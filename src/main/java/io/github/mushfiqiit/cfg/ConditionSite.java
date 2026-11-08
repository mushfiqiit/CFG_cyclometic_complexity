package io.github.mushfiqiit.cfg;

import java.util.List;

/**
 * A condition in the source and what must hold for the program to reach it.
 *
 * @param lineNumber the 1-based line the condition is on
 * @param condition the condition
 * @param path the outcomes of the earlier conditions that lead to this one: enclosing
 *        {@code if}, {@code else if} and {@code while} conditions are true, and earlier conditions
 *        in the same {@code if} chain are false
 */
record ConditionSite(int lineNumber, Condition condition, List<Requirement> path) {

    /**
     * A condition that must have a given outcome.
     *
     * @param lineNumber the 1-based line the condition is on
     * @param condition the condition
     * @param outcome whether the condition must be true or false
     */
    record Requirement(int lineNumber, Condition condition, boolean outcome) {
    }

    /**
     * Creates a site, keeping a read-only copy of the path.
     *
     * @param lineNumber the 1-based line the condition is on
     * @param condition the condition
     * @param path the outcomes that lead to the condition
     */
    ConditionSite {
        path = List.copyOf(path);
    }

    /**
     * Describes the site for the report, such as {@code "line 4: k>0"}.
     *
     * @return the line number and the condition's text
     */
    String describe() {
        return "line " + lineNumber + ": " + condition.getText();
    }
}
