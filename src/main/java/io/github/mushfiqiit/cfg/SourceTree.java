package io.github.mushfiqiit.cfg;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    record Function(Line header, Block body) implements BlockOpener {
    }

    /**
     * A line that opens a block, together with that block: the function, a branch of an
     * {@code if} chain, or a loop.
     */
    interface BlockOpener {

        /**
         * Returns the line that opens the block.
         *
         * @return the opening line
         */
        Line header();

        /**
         * Returns the block this line opens.
         *
         * @return the block
         */
        Block body();
    }

    /**
     * The statements after an opening line: either closed by a <code>}</code> line, or cut off
     * because the file ends first.
     */
    sealed interface Block permits Block.Closed, Block.Unclosed {

        /**
         * A block that ends with its closing <code>}</code> line.
         *
         * @param statements the statements, in order
         * @param closingLine the line that closes the block
         */
        record Closed(List<Statement> statements, Line closingLine) implements Block {

            /**
             * Creates a block, keeping a read-only copy of the statements.
             *
             * @param statements the statements, in order
             * @param closingLine the line that closes the block
             */
            public Closed {
                statements = List.copyOf(statements);
            }

            /**
             * Returns the line that closes the block.
             *
             * @return the closing line
             */
            @Override
            public Optional<Line> closing() {
                return Optional.of(closingLine);
            }
        }

        /**
         * A block that the file ends inside, before its closing <code>}</code>.
         *
         * @param statements the statements, in order
         */
        record Unclosed(List<Statement> statements) implements Block {

            /**
             * Creates a block, keeping a read-only copy of the statements.
             *
             * @param statements the statements, in order
             */
            public Unclosed {
                statements = List.copyOf(statements);
            }

            /**
             * Returns no closing line, because the file ends first.
             *
             * @return an empty value
             */
            @Override
            public Optional<Line> closing() {
                return Optional.empty();
            }
        }

        /**
         * Returns the statements in the block.
         *
         * @return the statements, in order, as a read-only list
         */
        List<Statement> statements();

        /**
         * Returns the line that closes the block.
         *
         * @return the closing line, or an empty value if the file ends first
         */
        Optional<Line> closing();

        /**
         * Returns every statement in this block, including those in nested blocks, in line order.
         *
         * @return each statement, followed by the statements nested inside it
         */
        default Stream<Statement> allStatements() {
            return statements().stream().flatMap(Statement::withNested);
        }
    }

    /** A statement in a block: a simple line, an {@code if} chain or a loop. */
    sealed interface Statement permits Simple, IfChain, Loop {

        /**
         * Returns the blocks directly inside this statement, each with the line that opens it.
         *
         * @return the branches of an {@code if} chain, the loop itself, or nothing for a simple
         *         statement
         */
        List<BlockOpener> nestedBlocks();

        /**
         * Returns this statement followed by every statement nested inside it, in line order.
         *
         * @return this statement and the statements in its blocks, at any depth
         */
        default Stream<Statement> withNested() {
            return Stream.concat(Stream.of(this),
                    nestedBlocks().stream().flatMap(opener -> opener.body().allStatements()));
        }
    }

    /**
     * A line that does not open a block, such as {@code a = 1;}.
     *
     * @param line the statement's line
     */
    record Simple(Line line) implements Statement {

        /**
         * Returns no blocks, because a simple statement does not open one.
         *
         * @return an empty list
         */
        @Override
        public List<BlockOpener> nestedBlocks() {
            return List.of();
        }
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

        /**
         * Returns the branches, each of which opens a block.
         *
         * @return the branches in order
         */
        @Override
        public List<BlockOpener> nestedBlocks() {
            return List.copyOf(branches);
        }
    }

    /**
     * One branch of an {@code if} chain.
     *
     * @param kind whether the branch is an {@code if}, an {@code else if} or an {@code else}
     * @param header the {@code if}, {@code else if} or {@code else} line
     * @param body the branch's block
     */
    record Branch(Kind kind, Line header, Block body) implements BlockOpener {

        /** The keyword that opens a branch. */
        enum Kind {
            /** An {@code if} line. */
            IF,
            /** An {@code else if} line. */
            ELSE_IF,
            /** An {@code else} line. */
            ELSE;

            /**
             * Tells whether a branch of this kind tests a condition.
             *
             * @return {@code true} for {@code if} and {@code else if}, {@code false} for {@code else}
             */
            boolean hasCondition() {
                return this != ELSE;
            }
        }
    }

    /**
     * A {@code for} or {@code while} loop.
     *
     * @param kind whether the loop is a {@code for} or a {@code while}
     * @param header the loop's line
     * @param body the loop's block
     */
    record Loop(Kind kind, Line header, Block body) implements Statement, BlockOpener {

        /**
         * Returns the loop itself, which opens the loop body.
         *
         * @return a list holding only this loop
         */
        @Override
        public List<BlockOpener> nestedBlocks() {
            return List.of(this);
        }

        /** The keyword that opens a loop. */
        enum Kind {
            /** A {@code for} line. */
            FOR,
            /** A {@code while} line. */
            WHILE
        }
    }
}
