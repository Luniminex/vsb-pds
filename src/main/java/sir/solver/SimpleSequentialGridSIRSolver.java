package sir.solver;

import sir.model.Node;
import sir.model.State;
import sir.model.StepStats;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public class SimpleSequentialGridSIRSolver implements SIRSolver {
    private final List<Node> nodes;
    private final int gridWidth;
    private final int gridHeight;
    private final double infectionProb;
    private final double recoveryProb;
    private final SplittableRandom randomGenerator;
    private final Node[][] grid;
    private static final int[] dx = {0, 0, 1, -1};
    private static final int[] dy = {1, -1, 0, 0};

    public SimpleSequentialGridSIRSolver(List<Node> nodes, int gridWidth, int gridHeight, double infectionProb, double recoveryProb, Long seed) {
        this.nodes = nodes;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.infectionProb = infectionProb;
        this.recoveryProb = recoveryProb;
        this.randomGenerator = (seed == null) ? new SplittableRandom() : new SplittableRandom(seed);
        this.grid = new Node[gridHeight][gridWidth];

        for (Node node : nodes) {
            this.grid[node.y][node.x] = node;
        }
    }

    @Override
    public StepStats step(int tick) {
        long start = System.nanoTime();

        List<Node> toInfect = new ArrayList<>();
        List<Node> toRecover = new ArrayList<>();

        // Iterate over all nodes
        for (Node node : nodes) {
            if (node.state == State.INFECTED) {
                for (int i = 0; i < dx.length; i++) {
                    int nx = node.x + dx[i];
                    int ny = node.y + dy[i];
                    // Check bounds
                    if (nx >= 0 && nx < gridWidth && ny >= 0 && ny < gridHeight) {
                        Node neighbor = grid[ny][nx];
                        // Check if the neighbor is susceptible and if it gets infected
                        if (neighbor.state == State.SUSCEPTIBLE && randomGenerator.nextDouble() < infectionProb) {
                            toInfect.add(neighbor);
                        }
                    }
                }
                // Check if the infected node recovers
                if (randomGenerator.nextDouble() < recoveryProb) {
                    toRecover.add(node);
                }
            }
        }

        //synchronously update the states
        int newlyInfected = 0;
        for (Node n : toInfect) {
            if (n.state == State.SUSCEPTIBLE) {
                n.state = State.INFECTED;
                newlyInfected++;
            }
        }

        int newlyRecovered = 0;
        for (Node n : toRecover) {
            if (n.state == State.INFECTED) {
                n.state = State.RECOVERED;
                newlyRecovered++;
            }
        }

        int s = 0, i = 0, r = 0;
        for (Node node : nodes) {
            switch (node.state) {
                case SUSCEPTIBLE -> s++;
                case INFECTED -> i++;
                case RECOVERED -> r++;
            }
        }

        long elapsed = System.nanoTime() - start;
        return new StepStats(tick, newlyInfected, newlyRecovered, s, i, r, elapsed);
    }

    @Override
    public boolean isFinished() {
        return nodes.stream().noneMatch(n -> n.state == State.INFECTED);
    }

    @Override
    public List<Node> getCurrentState() {
        return nodes;
    }

    @Override
    public void shutdown() {
        //ignored
    }

    @Override
    public String getName() {
        return "Simple Sequential Grid SIR Solver";
    }
}