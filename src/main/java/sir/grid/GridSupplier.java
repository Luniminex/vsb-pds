package sir.grid;

import sir.model.Node;
import sir.model.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.Random;


public class GridSupplier {
    private final int width;
    private final int height;
    private final List<Node> originalNodes;

    public GridSupplier(int width, int height, int initialInfectedCount, Long seed) {
        this.width = width;
        this.height = height;
        this.originalNodes = new ArrayList<>(width * height);

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                originalNodes.add(new Node(c, r));
            }
        }

        // Randomly infect a specified number of nodes
        if (initialInfectedCount > 0 && !originalNodes.isEmpty()) {
            SplittableRandom splittableRandom = (seed == null) ? new SplittableRandom() : new SplittableRandom(seed);
            Random localRandom = new Random(splittableRandom.nextLong());

            // Shuffle the nodes to randomize the selection
            List<Node> nodesToShuffle = new ArrayList<>(originalNodes);
            Collections.shuffle(nodesToShuffle, localRandom);

            // Infect the specified number of nodes
            for (int i = 0; i < Math.min(initialInfectedCount, nodesToShuffle.size()); i++) {
                nodesToShuffle.get(i).state = State.INFECTED;
            }
        }
    }

    // Creates a deep copy of the original nodes
    public List<Node> copyNodes() {
        List<Node> copiedNodes = new ArrayList<>(originalNodes.size());
        for (Node node : originalNodes) {
            copiedNodes.add(new Node(node));
        }
        return copiedNodes;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getNodeCount() {
        return originalNodes.size();
    }
}