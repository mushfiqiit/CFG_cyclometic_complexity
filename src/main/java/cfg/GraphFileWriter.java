package cfg;

import java.io.FileWriter;
import java.io.IOException;

/** Saves a {@link ControlFlowGraph} to the files that {@link GraphViewer} reads. */
final class GraphFileWriter {

    /** Prevents instantiation; this class only has static methods. */
    private GraphFileWriter() {
    }

    /**
     * Writes the levels file: the line count, then one {@code "<node> <level>"} line per node.
     *
     * @param graph the graph to save
     * @throws IOException if the file cannot be written
     */
    static void saveLevels(ControlFlowGraph graph) throws IOException {
        int[] levels = graph.computeLevels();
        try (FileWriter writer = new FileWriter(GraphData.LEVELS_FILE)) {
            writer.write(graph.getLineCount() + "\n");
            for (int i = 0; i < graph.getLineCount(); i++) {
                writer.write(i + " " + levels[i] + "\n");
            }
        }
    }

    /**
     * Writes the edges file, one {@code "<from> <to>"} line per edge.
     *
     * @param graph the graph to save
     * @throws IOException if the file cannot be written
     */
    static void saveEdges(ControlFlowGraph graph) throws IOException {
        try (FileWriter writer = new FileWriter(GraphData.EDGES_FILE)) {
            for (int from = 0; from < graph.getLineCount(); from++) {
                for (int to = 0; to < graph.getLineCount(); to++) {
                    if (graph.hasEdge(from, to)) {
                        writer.write(from + " " + to + "\n");
                    }
                }
            }
        }
    }
}
