package cfg;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * Builds the control flow graph of a C source file, reports its complexity and displays it.
 *
 * @author sikde
 */
public class ControlFlowGraph {

    private final List<String> lines;
    private final boolean[] visited;
    private final int[][] adjacency = new int[GraphData.MAX_NODES][GraphData.MAX_NODES];
    private int nextLine = 0;

    public ControlFlowGraph(List<String> lines) {
        this.lines = lines;
        this.visited = new boolean[lines.size()];
    }

    public void analyze() throws IOException {
        Node root = findRoot();
        System.out.println("\n\n\nroot node no: " + root.getNumber() + "\n"
                + "root node statement: " + root.getStatement());

        buildBranch(root, false);
        fillAdjacency(root);
        saveLevels(computeLevels(root));
        printGraph();
        saveEdges();

        System.out.println("Cyclometic complexity is " + ComplexityCalculator.calculate(lines));
        GraphViewer.show();
    }

    private Node findRoot() {
        while (isPreamble(lines.get(nextLine))) {
            nextLine++;
        }
        Node root = new Node(nextLine, lines.get(nextLine));
        nextLine++;
        return root;
    }

    /** Uses charAt, so a blank line in the preamble throws StringIndexOutOfBoundsException. */
    private static boolean isPreamble(String line) {
        return line.contains("intmain(){") || line.charAt(0) == '#' || line.charAt(0) == '/';
    }

    /**
     * Links the lines after {@code branchRoot} until its block closes.
     *
     * @param inLoop whether {@code branchRoot} is a loop, so the last line links back to it
     * @return the node that closes the block, or null if the file ends first
     */
    private Node buildBranch(Node branchRoot, boolean inLoop) {
        Node previous = branchRoot;
        List<Node> openBranchEnds = new ArrayList<>();

        while (nextLine < lines.size()) {
            Node node = new Node(nextLine, lines.get(nextLine));
            String statement = node.getStatement();
            nextLine++;

            if (SyntaxIdentifier.isElse(statement)) {
                previous.addChild(node);
                openBranchEnds.add(buildBranch(node, false));
            } else if (SyntaxIdentifier.isIf(statement)) {
                linkFirstOpenBranch(openBranchEnds, previous, node);
                openBranchEnds.add(buildBranch(node, false));
            } else if (SyntaxIdentifier.isLoop(statement)) {
                linkFirstOpenBranch(openBranchEnds, previous, node);
                openBranchEnds.add(node);
                buildBranch(node, true);
            } else {
                linkAllOpenBranches(openBranchEnds, previous, node);
                if (SyntaxIdentifier.endsBlock(statement)) {
                    if (inLoop) {
                        node.addChild(branchRoot);
                    }
                    return node;
                }
                previous = node;
            }
        }
        return null;
    }

    /** Links only the first open branch to {@code node}; any other open branches are dropped. */
    private static void linkFirstOpenBranch(List<Node> openBranchEnds, Node previous, Node node) {
        if (openBranchEnds.isEmpty()) {
            previous.addChild(node);
        } else {
            openBranchEnds.get(0).addChild(node);
            openBranchEnds.clear();
        }
    }

    private static void linkAllOpenBranches(List<Node> openBranchEnds, Node previous, Node node) {
        if (openBranchEnds.isEmpty()) {
            previous.addChild(node);
        } else {
            for (Node branchEnd : openBranchEnds) {
                branchEnd.addChild(node);
            }
            openBranchEnds.clear();
        }
    }

    /** Records every edge reachable from {@code node} with a depth-first traversal. */
    private void fillAdjacency(Node node) {
        visited[node.getNumber()] = true;

        for (Node child : node.getChildren()) {
            if (!visited[child.getNumber()]) {
                fillAdjacency(child);
            }
        }
        for (Node child : node.getChildren()) {
            adjacency[node.getNumber()][child.getNumber()] = 1;
        }
    }

    /** Finds each node's distance from the root with a breadth-first traversal. */
    private int[] computeLevels(Node root) {
        int[] levels = new int[GraphData.MAX_NODES];
        Arrays.fill(levels, GraphData.UNREACHABLE_LEVEL);
        levels[root.getNumber()] = 1;

        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(root.getNumber());
        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int next = 0; next < GraphData.MAX_NODES; next++) {
                if (hasEdge(current, next) && levels[next] > levels[current] + 1) {
                    levels[next] = levels[current] + 1;
                    queue.add(next);
                }
            }
        }
        return levels;
    }

    private void saveLevels(int[] levels) throws IOException {
        try (FileWriter writer = new FileWriter(GraphData.LEVELS_FILE)) {
            writer.write(lines.size() + "\n");
            for (int i = 0; i < lines.size(); i++) {
                writer.write(i + " " + levels[i] + "\n");
            }
        }
    }

    private void saveEdges() throws IOException {
        try (FileWriter writer = new FileWriter(GraphData.EDGES_FILE)) {
            for (int from = 0; from < lines.size(); from++) {
                for (int to = 0; to < lines.size(); to++) {
                    if (hasEdge(from, to)) {
                        writer.write(from + " " + to + "\n");
                    }
                }
            }
        }
    }

    private void printGraph() {
        printAdjacencyList();
        printAdjacencyMatrix();
        printGraphComplexity();
    }

    private void printAdjacencyList() {
        System.out.println("\nAdjacency List:");
        for (int from = 0; from < lines.size(); from++) {
            System.out.print("\t" + from + "  ->   ");
            for (int to = 0; to < lines.size(); to++) {
                if (hasEdge(from, to)) {
                    System.out.print(to + " ");
                }
            }
            System.out.println();
        }
    }

    private void printAdjacencyMatrix() {
        System.out.println("\nAdjacency Matrix:");
        for (int from = 0; from < lines.size(); from++) {
            System.out.print("\t" + from + "\t");
            for (int to = 0; to < lines.size(); to++) {
                System.out.print(adjacency[from][to] + " ");
            }
            System.out.println();
        }
    }

    /** The "Edges" and "Verteces" labels are printed swapped: each shows the other's count. */
    private void printGraphComplexity() {
        int nodeCount = lines.size() - 1;
        int edgeCount = countEdges();

        System.out.println("Edges " + nodeCount);
        System.out.println("Verteces " + edgeCount);
        System.out.println("Cyclometic complexity using cfg is " + (edgeCount - nodeCount + 2));
    }

    private int countEdges() {
        int edgeCount = 0;
        for (int from = 0; from < lines.size(); from++) {
            for (int to = 0; to < lines.size(); to++) {
                if (hasEdge(from, to)) {
                    edgeCount++;
                }
            }
        }
        return edgeCount;
    }

    private boolean hasEdge(int from, int to) {
        return adjacency[from][to] == 1;
    }
}
