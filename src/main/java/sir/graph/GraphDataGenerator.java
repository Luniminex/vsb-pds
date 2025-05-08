package sir.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import sir.model.Node;

import java.io.IOException;
import java.util.List;

public class GraphDataGenerator {

    public static void generateAndSaveGraphs(int count, int startSize, int multiplier) throws IOException {
        List<GraphGenerator.GraphType> types = List.of(
                GraphGenerator.GraphType.SCALE_FREE,
                GraphGenerator.GraphType.ERDOS_RENYI
        );

        for (GraphGenerator.GraphType type : types) {
            int nodeCount = startSize;
            for (int i = 0; i < count; i++) {
                double edgeParam = switch (type) {
                    case SCALE_FREE -> 0;
                    case ERDOS_RENYI -> 0.0005;
                };

                Graph<Node, DefaultEdge> graph = GraphGenerator.generate(type, nodeCount, edgeParam);

                String filename = String.format("%s_%d.csv", type.name().toLowerCase(), nodeCount);
                GraphDataManager.saveGraph(graph, filename);
                System.out.printf("Saved %s with %d nodes to %s\n", type, nodeCount, filename);

                nodeCount *= multiplier;
            }
        }
    }
}
