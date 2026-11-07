package cfg;

/** Limits shared by {@link ControlFlowGraph} and {@link GraphViewer}. */
final class GraphData {

    /** Largest number of source lines the fixed-size graph arrays can hold. */
    static final int MAX_NODES = 50;

    /** Level recorded for a node that cannot be reached from the root. */
    static final int UNREACHABLE_LEVEL = 100000000;

    /** Prevents instantiation; this class only holds constants. */
    private GraphData() {
    }
}
