package io.github.mushfiqiit.cfg;

/**
 * How many decision points a source file has, and the cyclomatic complexity they give. A plain
 * {@code else} is not a decision point: it is the false branch of the condition before it.
 *
 * @param ifs number of {@code if} lines
 * @param elseIfs number of {@code else if} lines
 * @param loops number of {@code for} and {@code while} lines
 */
public record DecisionCounts(int ifs, int elseIfs, int loops) {

    /** No decision points at all. */
    static final DecisionCounts NONE = new DecisionCounts(0, 0, 0);

    /**
     * Adds two sets of counts.
     *
     * @param other the counts to add
     * @return the sum of each count
     */
    DecisionCounts plus(DecisionCounts other) {
        return new DecisionCounts(ifs + other.ifs, elseIfs + other.elseIfs, loops + other.loops);
    }

    /**
     * Returns the number of decision points: all the counted lines together.
     *
     * @return the sum of the three counts
     */
    public int total() {
        return ifs + elseIfs + loops;
    }

    /**
     * Returns the cyclomatic complexity: the number of decision points plus one.
     *
     * @return the complexity
     */
    public int complexity() {
        return total() + 1;
    }
}
