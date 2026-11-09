package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.Function;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Simple;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Source code read once: its lines, with comments already removed, and the {@link SourceTree} of
 * its function. Every analysis works from this instead of reading the lines again.
 *
 * @param lines the source lines, as a read-only list
 * @param function the function read from the lines, or an empty value if there is no code
 */
record ParsedSource(List<String> lines, Optional<Function> function) {

    /**
     * Creates a parsed source, keeping a read-only copy of the lines.
     *
     * @param lines the source lines
     * @param function the function read from the lines, if any
     */
    ParsedSource {
        lines = List.copyOf(lines);
    }

    /**
     * Removes the comments from source code and reads it into its tree. This is the only place
     * the source is parsed.
     *
     * @param lines the source lines as read from the file
     * @return the lines without comments, and their tree
     */
    static ParsedSource of(List<String> lines) {
        List<String> code = CommentStripper.strip(lines);
        return new ParsedSource(code, SourceParser.parse(code));
    }

    /**
     * Returns the function, for analyses that need code to work on.
     *
     * @return the function
     * @throws IllegalArgumentException if every line is blank or preamble
     */
    Function requireFunction() {
        return function.orElseThrow(() -> new IllegalArgumentException("there is no code to analyze"));
    }

    /**
     * Returns the statements to analyze: normally the function's body. If the first line of code
     * is itself an {@code if}, {@code else} or loop, that line and its block are the only
     * statement; if it is an ordinary statement rather than a function header, it comes first.
     *
     * @return the top-level statements, in order; empty if there is no code
     */
    List<Statement> statements() {
        if (function.isEmpty()) {
            return List.of();
        }
        Function code = function.orElseThrow();
        String header = code.header().text();
        if (SyntaxIdentifier.isLoop(header)) {
            return List.of(new Loop(SourceParser.loopKind(header), code.header(), code.body()));
        }
        if (SyntaxIdentifier.isIf(header) || SyntaxIdentifier.isElse(header)) {
            Branch branch = new Branch(SourceParser.branchKind(header), code.header(), code.body());
            return List.of(new IfChain(List.of(branch)));
        }
        if (header.strip().endsWith("{")) {
            return code.body().statements();
        }
        List<Statement> statements = new ArrayList<>(List.of(new Simple(code.header())));
        statements.addAll(code.body().statements());
        return List.copyOf(statements);
    }
}
