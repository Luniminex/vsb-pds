package sir.solver;

import sir.model.Node;
import sir.model.OptNode;
import sir.model.StepResult;
import sir.model.StepStats;

import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class ForkJoinGridSIRSolver implements SIRSolver {
    private final OptNode[] nodes;
    private final int gridWidth;
    private final int gridHeight;
    private final double infectionProb;
    private final double recoveryProb;
    private final ForkJoinPool pool;
    private final OptNode[] grid;
    private final SplittableRandom baseRandom;
    private final int threshold = 2000;

    private static final int[] dx = {0, 0, 1, -1};
    private static final int[] dy = {1, -1, 0, 0};

    public ForkJoinGridSIRSolver(List<Node> inputNodes,
                                 int gridWidth,
                                 int gridHeight,
                                 double infectionProb,
                                 double recoveryProb,
                                 int threads,
                                 Long seed) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.infectionProb = infectionProb;
        this.recoveryProb = recoveryProb;
        this.pool = new ForkJoinPool(threads);
        this.baseRandom = (seed == null) ? new SplittableRandom() : new SplittableRandom(seed);

        this.nodes = new OptNode[inputNodes.size()];
        this.grid = new OptNode[gridWidth * gridHeight];

        for (int i = 0; i < inputNodes.size(); i++) {
            Node n = inputNodes.get(i);
            // Converts Node to optimized version OptNode
            OptNode opt = new OptNode(n.x, n.y, (byte) n.state.ordinal());
            nodes[i] = opt;
            grid[n.y * gridWidth + n.x] = opt;
        }
    }

    @Override
    public StepStats step(int tick) {
        long start = System.nanoTime();
        StepResult result = pool.invoke(new StepTask(0, nodes.length, baseRandom.split()));

        long elapsed = System.nanoTime() - start;
        return new StepStats(tick, result.infected(), result.recovered(), result.s(), result.i(), result.r(), elapsed);
    }

    private class StepTask extends RecursiveTask<StepResult> {
        private final int start;
        private final int end;
        private final SplittableRandom rand;

        StepTask(int start, int end, SplittableRandom rand) {
            this.start = start;
            this.end = end;
            this.rand = rand;
        }

        @Override
        protected StepResult compute() {
            // If the task is small enough, process it directly
            if (end - start <= threshold) {
                int inf = 0, rec = 0, s = 0, i = 0, r = 0;
                // Process each node in the range
                for (int idx = start; idx < end; idx++) {
                    OptNode node = nodes[idx];
                    // Check the state of the node
                    if (node.state == OptNode.INFECTED) {
                        // Go over the neighbors
                        for (int d = 0; d < 4; d++) {
                            int nx = node.x + dx[d];
                            int ny = node.y + dy[d];
                            // Check bounds
                            if (nx >= 0 && nx < gridWidth && ny >= 0 && ny < gridHeight) {
                                OptNode neighbor = grid[ny * gridWidth + nx];
                                // Check if the neighbor is susceptible and if it gets infected
                                if (neighbor.state == OptNode.SUSCEPTIBLE && rand.nextDouble() < infectionProb) {
                                    // Use synchronized block to ensure thread safety
                                    synchronized (neighbor) {
                                        if (neighbor.state == OptNode.SUSCEPTIBLE) {
                                            neighbor.state = OptNode.INFECTED;
                                            inf++;
                                        }
                                    }
                                }
                            }
                        }
                        // Check if the infected node recovers
                        if (rand.nextDouble() < recoveryProb) {
                            node.state = OptNode.RECOVERED;
                            rec++;
                        }
                    }
                }
                // Count the states after processing
                for (int idx = start; idx < end; idx++) {
                    switch (nodes[idx].state) {
                        case OptNode.SUSCEPTIBLE -> s++;
                        case OptNode.INFECTED -> i++;
                        case OptNode.RECOVERED -> r++;
                    }
                }

                return new StepResult(inf, rec, s, i, r);
            } else {
                // If the task is too large, split it into smaller tasks
                int mid = (start + end) / 2;
                StepTask left = new StepTask(start, mid, rand.split());
                StepTask right = new StepTask(mid, end, rand.split());

                // Fork the left task and compute the right task
                left.fork();
                StepResult rightResult = right.compute();
                StepResult leftResult = left.join();

                return leftResult.merge(rightResult);
            }
        }
    }

    @Override
    public boolean isFinished() {
        for (OptNode node : nodes) {
            if (node.state == OptNode.INFECTED) return false;
        }
        return true;
    }

    @Override
    public List<Node> getCurrentState() {
        return java.util.Arrays.stream(nodes)
                .map(n -> new Node(n.x, n.y, n.state))
                .toList();
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public String getName() {
        return "ForkJoin Grid SIR Solver";
    }

}
