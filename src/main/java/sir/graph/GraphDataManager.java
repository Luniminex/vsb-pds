package sir.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import sir.model.Node;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GraphDataManager {

    private static final String RESOURCE_DIR = "src/main/resources/graphs/";

    public static void saveGraph(Graph<Node, DefaultEdge> graph, String filename) throws IOException {
        Path path = Path.of(RESOURCE_DIR + filename);
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (DefaultEdge edge : graph.edgeSet()) {
                Node src = graph.getEdgeSource(edge);
                Node tgt = graph.getEdgeTarget(edge);
                writer.write(src.id + ";" + tgt.id + "\n");
            }
        }
    }

    public static Graph<Node, DefaultEdge> loadGraph(String filename) throws IOException {
        Path path = Path.of(RESOURCE_DIR + filename);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Graph file not found: " + filename);
        }

        // Auto-detect format based on extension or content
        boolean isCsv = filename.endsWith(".csv") || isCsvFormatted(path);

        return isCsv ? loadCsvGraph(path) : loadTxtGraph(path);
    }

    private static boolean isCsvFormatted(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                return line.contains(";");
            }
        }
        return false;
    }

    private static Graph<Node, DefaultEdge> loadCsvGraph(Path path) throws IOException {
        Graph<Node, DefaultEdge> graph = GraphGenerator.createEmptyGraph();
        Map<Integer, Node> idToNode = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(";");
                int srcId = Integer.parseInt(parts[0]);
                int tgtId = Integer.parseInt(parts[1]);

                Node src = idToNode.computeIfAbsent(srcId, Node::new);
                Node tgt = idToNode.computeIfAbsent(tgtId, Node::new);

                graph.addVertex(src);
                graph.addVertex(tgt);
                graph.addEdge(src, tgt);

                src.neighbors.add(tgt);
                tgt.neighbors.add(src);
            }
        }

        return graph;
    }

    private static Graph<Node, DefaultEdge> loadTxtGraph(Path path) throws IOException {
        Graph<Node, DefaultEdge> graph = GraphGenerator.createEmptyGraph();
        Map<Integer, Node> idToNode = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;

                int srcId = Integer.parseInt(parts[0]);
                int tgtId = Integer.parseInt(parts[1]);

                Node src = idToNode.computeIfAbsent(srcId, Node::new);
                Node tgt = idToNode.computeIfAbsent(tgtId, Node::new);

                graph.addVertex(src);
                graph.addVertex(tgt);
                graph.addEdge(src, tgt);

                src.neighbors.add(tgt);
                tgt.neighbors.add(src);
            }
        }

        return graph;
    }
}