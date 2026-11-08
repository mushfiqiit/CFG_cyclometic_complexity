package io.github.mushfiqiit.cfg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

/** Shared helpers for the tests: loading test programs, capturing output and describing graphs. */
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
     * Runs code and returns everything it printed to standard output, with line endings
     * normalized to {@code \n} so tests pass on every OS.
     *
     * @param code the code to run
     * @return the captured output
     */
    static String captureOutput(Runnable code) {
        return captureOutput("", code);
    }

    /**
     * Runs code with the given text as standard input and returns everything it printed to
     * standard output, with line endings normalized to {@code \n}.
     *
     * @param input the text to provide as standard input
     * @param code the code to run
     * @return the captured output
     */
    static String captureOutput(String input, Runnable code) {
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            code.run();
        } finally {
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return buffer.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
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
        return ControlFlowGraphBuilder.from(lines).build();
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
