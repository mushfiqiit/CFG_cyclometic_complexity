package cfg;

/** Limits and file names shared by the graph builder and the graph viewer. */
final class GraphData {

    /** Largest number of source lines the fixed-size graph arrays can hold. */
    static final int MAX_NODES = 50;

    /** Level recorded for a node that cannot be reached from the root. */
    static final int UNREACHABLE_LEVEL = 100000000;

    static final String LEVELS_FILE = "LeveledNodes.txt";
    static final String EDGES_FILE = "Edges.txt";

    private GraphData() {
    }
}
