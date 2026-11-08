package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.print;
import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks that option 1 prints its whole report before handing the graph over for display. */
class ComplexityAnalysisTest {

    @Test
    void printsTheReportThenHandsOverTheGraph() {
        List<String> events = new ArrayList<>();
        List<String> lines = program("sample.c");

        String output = print(out -> ComplexityAnalysis.run(ParsedSource.of(lines), out, graph -> {
            out.flush();
            events.add("shown " + graph.countNodes() + " nodes");
        }));

        assertEquals(List.of("shown 16 nodes"), events);
        assertTrue(output.startsWith("\nCyclomatic Complexity\n"));
        assertTrue(output.endsWith("Opening the graph window. Close it to exit.\n"));
    }
}
