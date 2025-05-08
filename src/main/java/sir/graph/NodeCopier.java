package sir.graph;

import sir.model.Node;

import java.util.*;

public class NodeCopier {
    public static List<Node> deepCopyNodes(List<Node> original) {
        Map<Integer, Node> idToCopy = new HashMap<>();

        for (Node node : original) {
            idToCopy.put(node.id, new Node(node.id));
        }

        for (Node node : original) {
            Node copy = idToCopy.get(node.id);
            for (Node neighbor : node.neighbors) {
                copy.neighbors.add(idToCopy.get(neighbor.id));
            }
        }

        return new ArrayList<>(idToCopy.values());
    }
}
