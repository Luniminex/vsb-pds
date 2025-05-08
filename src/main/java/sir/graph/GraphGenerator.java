package sir.graph;

import org.jgrapht.Graph;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.util.SupplierUtil;
import sir.model.Node;

import java.util.Random;
import java.util.function.Supplier;

public class GraphGenerator {

    public enum GraphType {
        SCALE_FREE,
        ERDOS_RENYI
    }

    public static Graph<Node, DefaultEdge> generate(GraphType type, int nodeCount, double param) {
        Supplier<Node> vertexSupplier = new VertexSupplier();
        Graph<Node, DefaultEdge> graph = createEmptyGraph(vertexSupplier);

        switch (type) {
            case SCALE_FREE -> {
                ScaleFreeGraphGenerator<Node, DefaultEdge> generator = new ScaleFreeGraphGenerator<>(nodeCount);
                generator.generateGraph(graph);
            }
            case ERDOS_RENYI -> {
                GnpRandomGraphGenerator<Node, DefaultEdge> generator = new GnpRandomGraphGenerator<>(nodeCount, param);
                generator.generateGraph(graph);
            }
        }

        for (DefaultEdge edge : graph.edgeSet()) {
            Node src = graph.getEdgeSource(edge);
            Node tgt = graph.getEdgeTarget(edge);
            src.neighbors.add(tgt);
            tgt.neighbors.add(src);
        }

        return graph;
    }

    public static Graph<Node, DefaultEdge> generateScaleFree(int nodeCount) {
        return generate(GraphType.SCALE_FREE, nodeCount, 0);
    }

    public static Graph<Node, DefaultEdge> createEmptyGraph() {
        return createEmptyGraph(new VertexSupplier());
    }

    public static Graph<Node, DefaultEdge> createEmptyGraph(Supplier<Node> vertexSupplier) {
        return new DefaultUndirectedGraph<>(vertexSupplier, SupplierUtil.createDefaultEdgeSupplier(), false);
    }
}