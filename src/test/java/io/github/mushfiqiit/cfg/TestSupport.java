package io.github.mushfiqiit.cfg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/** Shared helpers for the tests: loading test programs, capturing printed text and describing graphs. */
final class TestSupport {

    /** Prevents instantiation; this class only has static methods. */
    private TestSupport() {
    }

    /**
     * Returns the path of a C program in {@code src/test/resources/io/github/mushfiqiit/cfg}.
     *
     * @param name the file name, such as {@code "sample.c"}
     * @return the file's path
     */
    static Path programPath(String name) {
        URL url = TestSupport.class.getResource(name);
        if (url == null) {
            throw new IllegalArgumentException("no test program named " + name);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Reads a C program from {@code src/test/resources/io/github/mushfiqiit/cfg}.
     *
     * @param name the file name, such as {@code "sample.c"}
     * @return the program's lines
     */
    static List<String> program(String name) {
        try {
            return Files.readAllLines(programPath(name), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Runs code that prints to a stream and returns what it printed, with line endings normalized
     * to {@code \\n} so tests pass on every OS.
     *
     * @param code the code to run, given the stream to print to
     * @return the printed text
     */
    static String print(Consumer<PrintStream> code) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            code.accept(out);
        }
        return normalized(buffer);
    }

    /**
     * Returns the text written to a buffer, with line endings normalized to {@code \\n}.
     *
     * @param buffer the buffer to read
     * @return the text
     */
    private static String normalized(ByteArrayOutputStream buffer) {
        return buffer.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    /** What a run of the program printed and the exit status it returned and the exit status it returned. */
    /**
     * What a run of the program printed, the exit status it returned, and the graph it would have
     * shown in a window.
     *
     * @param status the exit status
     * @param output what was printed to the output stream
     * @param errors what was printed to the error stream
     * @param shownGraph the graph handed to the display, or null if none was
     */
    record ProgramRun(int status, String output, String errors, ControlFlowGraph shownGraph) {
    }

    /**
     * Runs {@link Main#run} with the given input, capturing its output and errors and recording
     * the graph instead of opening a window. Line endings are normalized to {@code \\n}.
     *
     * @param input the text to provide as input
     * @param args the command-line arguments
     * @return what the run printed, returned and showed
     */
    static ProgramRun runProgram(String input, String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        ControlFlowGraph[] shown = new ControlFlowGraph[1];
        int status;
        try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
                PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            status = Main.run(args, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), out, err,
                    graph -> shown[0] = graph);
        }
        return new ProgramRun(status, normalized(output), normalized(errors), shown[0]);
    }

    /**
     * Lists a graph's edges as {@code "from->to"} using 1-based line numbers, such as
     * {@code "2->3"}.
     *
     * @param graph the graph to describe
     * @return the edges, sorted
     */
    static Set<String> edges(ControlFlowGraph graph) {
        Set<String> edges = new TreeSet<>();
        for (int from = 0; from < graph.getLineCount(); from++) {
            for (int to = 0; to < graph.getLineCount(); to++) {
                if (graph.hasEdge(from, to)) {
                    edges.add((from + 1) + "->" + (to + 1));
                }
            }
        }
        return edges;
    }

    /**
     * Builds the graph of a program.
     *
     * @param lines the program's lines
     * @return the graph
     */
    static ControlFlowGraph build(List<String> lines) {
        return ControlFlowGraphBuilder.from(ParsedSource.of(lines)).build();
    }

    /**
     * Returns a sorted set, for comparing edge lists.
     *
     * @param edges the expected edges as {@code "from->to"}
     * @return the edges, sorted
     */
    static Set<String> edgeSet(String... edges) {
        return new TreeSet<>(List.of(edges));
    }
}
