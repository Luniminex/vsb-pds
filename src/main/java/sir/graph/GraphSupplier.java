package sir.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import sir.model.Node;

import java.util.ArrayList;
import java.util.List;

public class GraphSupplier {
    private final Graph<Node, DefaultEdge> graph;
    private final List<Node> originalNodes;

    public GraphSupplier(Graph<Node, DefaultEdge> graph) {
        this.graph = graph;
        this.originalNodes = new ArrayList<>(graph.vertexSet());
    }

    public GraphSupplier(GraphGenerator.GraphType type, int nodeCount, double param) {
        this.graph = GraphGenerator.generate(type, nodeCount, param);
        this.originalNodes = new ArrayList<>(graph.vertexSet());
    }

    public Graph<Node, DefaultEdge> getGraph() {
        return graph;
    }

    public List<Node> copyNodes() {
        return NodeCopier.deepCopyNodes(originalNodes);
    }

    public int getNodeCount() {
        return originalNodes.size();
    }

    public int getEdgeCount() {
        return graph.edgeSet().size();
    }
}
