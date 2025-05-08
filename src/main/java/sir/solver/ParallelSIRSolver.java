package sir.solver;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import sir.model.Node;
import sir.model.State;
import sir.model.StepStats;

import java.util.*;
import java.util.concurrent.*;

public class ParallelSIRSolver implements SIRSolver {
    private final List<Node> nodes;
    private final double infectionProb;
    private final double recoveryProb;
    private final SplittableRandom random;
    private final ExecutorService executor;
    private final int threads;

    public ParallelSIRSolver(List<Node> nodes, double infectionProb, double recoveryProb, int threads, Long seed) {
        this.nodes = nodes;
        this.infectionProb = infectionProb;
        this.recoveryProb = recoveryProb;
        this.threads = threads;
        this.executor = Executors.newFixedThreadPool(threads);
        this.random = (seed == null) ? new SplittableRandom() : new SplittableRandom(seed);

        // Detekce komponent a infekce po jedné v každé
        Graph<Node, DefaultEdge> graph = new org.jgrapht.graph.SimpleGraph<>(DefaultEdge.class);
        for (Node node : nodes) graph.addVertex(node);
        for (Node node : nodes) {
            for (Node neighbor : node.neighbors) {
                if (!graph.containsEdge(node, neighbor)) {
                    graph.addEdge(node, neighbor);
                }
            }
        }
        ConnectivityInspector<Node, DefaultEdge> inspector = new ConnectivityInspector<>(graph);
        List<Set<Node>> components = inspector.connectedSets();
        for (Set<Node> component : components) {
            List<Node> list = new ArrayList<>(component);
            Node selected = list.get(random.nextInt(list.size()));
            selected.state = State.INFECTED;
        }
    }

    public ParallelSIRSolver(List<Node> nodes, double infectionProb, double recoveryProb, int threads) {
        this(nodes, infectionProb, recoveryProb, threads, null);
    }

    @Override
    public StepStats step(int tick) {
        long start = System.nanoTime();

        List<Node> toInfect = Collections.synchronizedList(new ArrayList<>());
        List<Node> toRecover = Collections.synchronizedList(new ArrayList<>());

        List<Callable<Void>> tasks = new ArrayList<>();

        int chunkSize = (int) Math.ceil((double) nodes.size() / threads);
        for (int i = 0; i < threads; i++) {
            int startIdx = i * chunkSize;
            int endIdx = Math.min(startIdx + chunkSize, nodes.size());

            tasks.add(() -> {
                SplittableRandom threadRandom = new SplittableRandom();
                for (int j = startIdx; j < endIdx; j++) {
                    Node node = nodes.get(j);
                    if (node.state == State.INFECTED) {
                        for (Node neighbor : node.neighbors) {
                            if (neighbor.state == State.SUSCEPTIBLE &&
                                    threadRandom.nextDouble() < infectionProb) {
                                toInfect.add(neighbor);
                            }
                        }
                        if (threadRandom.nextDouble() < recoveryProb) {
                            toRecover.add(node);
                        }
                    }
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel execution interrupted", e);
        }

        for (Node n : toInfect) n.state = State.INFECTED;
        for (Node n : toRecover) n.state = State.RECOVERED;

        int s = 0, i = 0, r = 0;
        for (Node node : nodes) {
            switch (node.state) {
                case SUSCEPTIBLE -> s++;
                case INFECTED -> i++;
                case RECOVERED -> r++;
            }
        }

        long elapsed = System.nanoTime() - start;
        return new StepStats(tick, toInfect.size(), toRecover.size(), s, i, r, elapsed);
    }

    @Override
    public boolean isFinished() {
        return nodes.stream().noneMatch(n -> n.state == State.INFECTED);
    }

    @Override
    public List<Node> getCurrentState() {
        return nodes;
    }

    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public String getName() {
        return "Parallel SIR Solver";
    }
}