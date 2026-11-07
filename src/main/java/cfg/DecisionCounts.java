package cfg;

/**
 * How many lines of each branching kind a source file has, and the cyclomatic complexity they
 * give.
 *
 * @param ifs number of {@code if} lines
 * @param elseIfs number of {@code else if} lines; always 0, because those lines are counted as
 *        {@code else}
 * @param elses number of lines starting with {@code else}, including {@code else if}
 * @param loops number of {@code for} and {@code while} lines
 */
public record DecisionCounts(int ifs, int elseIfs, int elses, int loops) {

    /**
     * Returns the number of decision points: all the counted lines together.
     *
     * @return the sum of the four counts
     */
    public int total() {
        return ifs + elseIfs + elses + loops;
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
