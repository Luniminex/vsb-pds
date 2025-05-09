package sir.solver;

import sir.model.Node;
import sir.model.State;
import sir.model.StepStats;
import sir.solver.SIRSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class CompletableFutureSIRSolver implements SIRSolver {
    private final List<Node> nodes;
    private final int gridWidth;
    private final int gridHeight;
    private final double infectionProb;
    private final double recoveryProb;
    private final Node[][] grid;
    private final int threads;
    private final SplittableRandom baseRandom;
    private static final int[] dx = {0, 0, 1, -1};
    private static final int[] dy = {1, -1, 0, 0};
    public CompletableFutureSIRSolver(List<Node> nodes, int gridWidth, int gridHeight,
                                      double infectionProb, double recoveryProb,
                                      int threads, Long seed) {
        this.nodes = nodes;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.infectionProb = infectionProb;
        this.recoveryProb = recoveryProb;
        this.threads = threads;
        this.grid = new Node[gridHeight][gridWidth];
        this.baseRandom = (seed == null) ? new SplittableRandom() : new SplittableRandom(seed);

        for (Node node : nodes) {
            grid[node.y][node.x] = node;
        }
    }

    @Override
    public StepStats step(int tick) {
        long start = System.nanoTime();

        // Chunk the nodes for parallel processing
        int chunkSize = (int) Math.ceil((double) nodes.size() / threads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<Node> toInfect = new CopyOnWriteArrayList<>();
        List<Node> toRecover = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            int startIdx = i * chunkSize;
            int endIdx = Math.min(startIdx + chunkSize, nodes.size());
            List<Node> subList = nodes.subList(startIdx, endIdx);

            SplittableRandom threadRandom = baseRandom.split();

            // Create a CompletableFuture for each chunk of nodes
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // Process each node in the chunk
                for (Node node : subList) {
                    if (node.state == State.INFECTED) {
                        for (int k = 0; k < dx.length; k++) {
                            int nx = node.x + dx[k];
                            int ny = node.y + dy[k];
                            // Check bounds
                            if (nx >= 0 && nx < gridWidth && ny >= 0 && ny < gridHeight) {
                                Node neighbor = grid[ny][nx];
                                // Check if the neighbor is susceptible and if it gets infected
                                if (neighbor.state == State.SUSCEPTIBLE && threadRandom.nextDouble() < infectionProb) {
                                    toInfect.add(neighbor);
                                }
                            }
                        }
                        // Check if the infected node recovers
                        if (threadRandom.nextDouble() < recoveryProb) {
                            toRecover.add(node);
                        }
                    }
                }
            });
            // Add the CompletableFuture to the list
            futures.add(future);
        }

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Create atomic counters for new infections and recoveries
        AtomicInteger newInfected = new AtomicInteger();
        AtomicInteger newRecovered = new AtomicInteger();

        // Update the states of the nodes synchronously
        toInfect.forEach(n -> {
            if (n.state == State.SUSCEPTIBLE) {
                n.state = State.INFECTED;
                newInfected.incrementAndGet();
            }
        });

        toRecover.forEach(n -> {
            if (n.state == State.INFECTED) {
                n.state = State.RECOVERED;
                newRecovered.incrementAndGet();
            }
        });

        int s = 0, i = 0, r = 0;
        for (Node node : nodes) {
            switch (node.state) {
                case SUSCEPTIBLE -> s++;
                case INFECTED -> i++;
                case RECOVERED -> r++;
            }
        }

        long elapsed = System.nanoTime() - start;
        return new StepStats(tick, newInfected.get(), newRecovered.get(), s, i, r, elapsed);
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
        return "CompletableFuture Grid SIR Solver";
    }
}