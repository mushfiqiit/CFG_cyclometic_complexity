package io.github.mushfiqiit.cfg;

import java.util.List;
import java.util.Optional;

/**
 * The structure of a C function, read once by {@link SourceParser}: which lines are statements,
 * which open {@code if} chains and loops, and which close each block. The graph builder, the
 * format checker and the condition finder all work from this structure instead of reading the
 * lines themselves.
 */
final class SourceTree {

    /** Prevents instantiation; this class only groups the structure's types. */
    private SourceTree() {
    }

    /**
     * One source line.
     *
     * @param index the line's position in the file, counting from 0; also its node number
     * @param text the line's text
     */
    record Line(int index, String text) {

        /**
         * Returns the line number shown to the user.
         *
         * @return the line's position counting from 1, as in an editor
         */
        int number() {
            return index + 1;
        }
    }

    /**
     * The analyzed function.
     *
     * @param header the line that opens the function, such as <code>int main(void) {</code>
     * @param body the function's statements and closing line
     */
    record Function(Line header, Block body) {
    }

    /**
     * The statements between an opening line and its closing <code>}</code>.
     *
     * @param statements the statements, in order
     * @param closing the line that closes the block, or an empty value if the file ends first
     */
    record Block(List<Statement> statements, Optional<Line> closing) {

        /**
         * Creates a block, keeping a read-only copy of the statements.
         *
         * @param statements the statements, in order
         * @param closing the line that closes the block, if any
         */
        Block {
            statements = List.copyOf(statements);
        }
    }

    /** A statement in a block: a simple line, an {@code if} chain or a loop. */
    sealed interface Statement permits Simple, IfChain, Loop {
    }

    /**
     * A line that does not open a block, such as {@code a = 1;}.
     *
     * @param line the statement's line
     */
    record Simple(Line line) implements Statement {
    }

    /**
     * An {@code if} with the {@code else if} and {@code else} branches that follow it. In code
     * that has not been checked, the first branch can also be an {@code else} without an
     * {@code if}.
     *
     * @param branches the branches in order; never empty
     */
    record IfChain(List<Branch> branches) implements Statement {

        /**
         * Creates a chain, keeping a read-only copy of the branches.
         *
         * @param branches the branches in order
         */
        IfChain {
            branches = List.copyOf(branches);
        }
    }

    /**
     * One branch of an {@code if} chain.
     *
     * @param header the {@code if}, {@code else if} or {@code else} line
     * @param body the branch's block
     */
    record Branch(Line header, Block body) {
    }

    /**
     * A {@code for} or {@code while} loop.
     *
     * @param header the loop's line
     * @param body the loop's block
     */
    record Loop(Line header, Block body) implements Statement {
    }
}
