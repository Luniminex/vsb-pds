package sir.solver;

import sir.model.Node;
import sir.model.State;
import sir.model.StepStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimpleParallelGridSIRSolver implements SIRSolver {
    private final List<Node> nodes;
    private final int gridWidth;
    private final int gridHeight;
    private final double infectionProb;
    private final double recoveryProb;
    private final SplittableRandom randomGenerator;
    private final ExecutorService executor;
    private final int threads;
    private final Node[][] grid;
    private static final int[] dx = {0, 0, 1, -1};
    private static final int[] dy = {1, -1, 0, 0};
    public SimpleParallelGridSIRSolver(List<Node> nodes, int gridWidth, int gridHeight, double infectionProb, double recoveryProb, int threads, Long seed) {
        this.nodes = nodes;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.infectionProb = infectionProb;
        this.recoveryProb = recoveryProb;
        this.threads = threads;
        this.executor = Executors.newFixedThreadPool(threads);
        this.randomGenerator = (seed == null) ? new SplittableRandom() : new SplittableRandom(seed);
        this.grid = new Node[gridHeight][gridWidth];

        for (Node node : nodes) {
            this.grid[node.y][node.x] = node;
        }
    }

    @Override
    public StepStats step(int tick) {
        long start = System.nanoTime();

        // Create lists to hold nodes to infect and recover
        List<Node> toInfect = Collections.synchronizedList(new ArrayList<>());
        List<Node> toRecover = Collections.synchronizedList(new ArrayList<>());

        // Split the nodes into chunks for parallel processing
        List<Callable<Void>> tasks = new ArrayList<>();
        int chunkSize = (int) Math.ceil((double) nodes.size() / threads);

        // Create tasks for each chunk
        for (int i = 0; i < threads; i++) {
            int startIdx = i * chunkSize;
            int endIdx = Math.min(startIdx + chunkSize, nodes.size());
            List<Node> chunk = nodes.subList(startIdx, endIdx);

            tasks.add(() -> {
                SplittableRandom threadRand = randomGenerator.split();
                List<Node> localInfect = new ArrayList<>();
                List<Node> localRecover = new ArrayList<>();

                // Process each node in the chunk
                for (Node node : chunk) {
                    if (node.state == State.INFECTED) {
                        for (int d = 0; d < dx.length; d++) {
                            int nx = node.x + dx[d];
                            int ny = node.y + dy[d];
                            if (nx >= 0 && nx < gridWidth && ny >= 0 && ny < gridHeight) {
                                Node neighbor = grid[ny][nx];
                                // Check if the neighbor is susceptible and if it gets infected
                                if (neighbor.state == State.SUSCEPTIBLE && threadRand.nextDouble() < infectionProb) {
                                    localInfect.add(neighbor);
                                }
                            }
                        }
                        // Check if the infected node recovers
                        if (threadRand.nextDouble() < recoveryProb) {
                            localRecover.add(node);
                        }
                    }
                }
                toInfect.addAll(localInfect);
                toRecover.addAll(localRecover);
                return null;
            });
        }

        // Execute all tasks in parallel
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel execution interrupted", e);
        }

        // Update the states of the nodes synchronously
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
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getName() {
        return "Simple Parallel Grid SIR Solver (" + threads + " threads)";
    }
}
