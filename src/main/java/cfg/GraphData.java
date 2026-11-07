package cfg;

/** Limits and file names shared by the graph classes, {@link GraphFileWriter} and {@link GraphViewer}. */
final class GraphData {

    /** Largest number of source lines the fixed-size graph arrays can hold. */
    static final int MAX_NODES = 50;

    /** Level recorded for a node that cannot be reached from the root. */
    static final int UNREACHABLE_LEVEL = 100000000;

    /** File listing each node's level, written by the builder and read by the viewer. */
    static final String LEVELS_FILE = "LeveledNodes.txt";

    /** File listing each edge as a pair of node numbers, written by the builder and read by the viewer. */
    static final String EDGES_FILE = "Edges.txt";

    /** Prevents instantiation; this class only holds constants. */
    private GraphData() {
    }
}
