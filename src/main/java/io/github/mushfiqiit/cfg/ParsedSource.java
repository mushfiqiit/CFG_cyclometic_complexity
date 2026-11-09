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
 * its function. Every analysis works from this instead of reading the lines again. Source code
 * with no function, because every line is blank or preamble, has no parsed source.
 *
 * @param lines the source lines, as a read-only list
 * @param function the function read from the lines
 */
record ParsedSource(List<String> lines, Function function) {

    /**
     * Creates a parsed source, keeping a read-only copy of the lines.
     *
     * @param lines the source lines
     * @param function the function read from the lines
     */
    ParsedSource {
        lines = List.copyOf(lines);
    }

    /**
     * Removes the comments from source code and reads it into its tree. This is the only place
     * the source is parsed.
     *
     * @param lines the source lines as read from the file
     * @return the lines without comments, and their tree; an empty value if every line is blank
     *         or preamble, so there is no code to analyze
     */
    static Optional<ParsedSource> of(List<String> lines) {
        List<String> code = CommentStripper.strip(lines);
        return SourceParser.parse(code).map(function -> new ParsedSource(code, function));
    }

    /**
     * Returns the statements to analyze: normally the function's body. If the first line of code
     * is itself an {@code if}, {@code else} or loop, that line and its block are the only
     * statement; if it is an ordinary statement rather than a function header, it comes first.
     *
     * @return the top-level statements, in order
     */
    List<Statement> statements() {
        String header = function.header().text();
        if (SyntaxIdentifier.isLoop(header)) {
            return List.of(new Loop(SourceParser.loopKind(header), function.header(), function.body()));
        }
        if (SyntaxIdentifier.isIf(header) || SyntaxIdentifier.isElse(header)) {
            Branch branch = new Branch(SourceParser.branchKind(header), function.header(), function.body());
            return List.of(new IfChain(List.of(branch)));
        }
        if (header.strip().endsWith("{")) {
            return function.body().statements();
        }
        List<Statement> statements = new ArrayList<>(List.of(new Simple(function.header())));
        statements.addAll(function.body().statements());
        return List.copyOf(statements);
    }
}
