package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.Block;
import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.Function;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Line;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Simple;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads C source lines into a {@link SourceTree}. This is the only class that walks through the
 * lines in order; everything after it works from the tree.
 *
 * <p>The first line that is not blank or preamble opens the function. Each line that ends with
 * <code>}</code> closes the current block, each {@code if}, {@code else if} or {@code else} line
 * starts or continues an {@code if} chain, each {@code for} or {@code while} line starts a loop,
 * and every other non-blank line is a simple statement. Blank lines are skipped.
 */
final class SourceParser {

    private final List<String> lines;
    private int nextLine;

    /**
     * Creates a parser that starts reading at a given line.
     *
     * @param lines the source lines
     * @param firstLine index of the first line to read
     */
    private SourceParser(List<String> lines, int firstLine) {
        this.lines = lines;
        this.nextLine = firstLine;
    }

    // ***************************************************************************
    // Parsing a file
    // ***************************************************************************

    /**
     * Reads the function in the source lines. Lines after the function's closing line are not
     * read.
     *
     * @param lines the source lines
     * @return the function, or an empty value if every line is blank or preamble
     */
    static Optional<Function> parse(List<String> lines) {
        int header = 0;
        while (header < lines.size() && isPreamble(lines.get(header))) {
            header++;
        }
        if (header == lines.size()) {
            return Optional.empty();
        }
        SourceParser parser = new SourceParser(lines, header + 1);
        return Optional.of(new Function(new Line(header, lines.get(header)), parser.readBlock()));
    }

    /**
     * Tells whether a line comes before the function: a blank line, a line containing
     * {@code intmain()}, or one starting with {@code #} or {@code /}.
     *
     * @param line the source line to check
     * @return true if the line is part of the preamble
     */
    private static boolean isPreamble(String line) {
        return line.isBlank() || line.contains("intmain(){") || line.charAt(0) == '#' || line.charAt(0) == '/';
    }

    // ***************************************************************************
    // Reading blocks and statements
    // ***************************************************************************

    /**
     * Reads statements up to and including the line that closes the current block.
     *
     * @return the block, without a closing line if the file ends first
     */
    private Block readBlock() {
        List<Statement> statements = new ArrayList<>();
        while (nextLine < lines.size()) {
            String text = lines.get(nextLine);
            if (text.isBlank()) {
                nextLine++;
            } else if (SyntaxIdentifier.endsBlock(text)) {
                return new Block(statements, Optional.of(takeLine()));
            } else if (SyntaxIdentifier.isIf(text) || SyntaxIdentifier.isElse(text)) {
                statements.add(readIfChain());
            } else if (SyntaxIdentifier.isLoop(text)) {
                statements.add(new Loop(takeLine(), readBlock()));
            } else {
                statements.add(new Simple(takeLine()));
            }
        }
        return new Block(statements, Optional.empty());
    }

    /**
     * Reads an {@code if} chain: its first branch, then each {@code else if} or {@code else}
     * branch that follows. The chain ends after an {@code else}, or before any other line.
     *
     * @return the chain
     */
    private IfChain readIfChain() {
        List<Branch> branches = new ArrayList<>();
        Line header = takeLine();
        branches.add(new Branch(header, readBlock()));
        boolean canContinue = true;
        while (canContinue && nextNonBlankLineIsElse()) {
            header = takeLine();
            branches.add(new Branch(header, readBlock()));
            canContinue = SyntaxIdentifier.isElseIf(header.text());
        }
        return new IfChain(branches);
    }

    // ***************************************************************************
    // Moving through the lines
    // ***************************************************************************

    /**
     * Returns the next line and moves past it.
     *
     * @return the line
     */
    private Line takeLine() {
        Line line = new Line(nextLine, lines.get(nextLine));
        nextLine++;
        return line;
    }

    /**
     * Moves past blank lines, then tells whether the next line starts with {@code else}.
     *
     * @return true if the next non-blank line continues an {@code if} chain
     */
    private boolean nextNonBlankLineIsElse() {
        while (nextLine < lines.size() && lines.get(nextLine).isBlank()) {
            nextLine++;
        }
        return nextLine < lines.size() && SyntaxIdentifier.isElse(lines.get(nextLine));
    }
}
