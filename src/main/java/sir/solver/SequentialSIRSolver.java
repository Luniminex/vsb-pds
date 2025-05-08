package sir.solver;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import sir.model.Node;
import sir.model.State;
import sir.model.StepStats;

import java.util.*;

public class SequentialSIRSolver implements SIRSolver {
    private final List<Node> nodes;
    private final double infectionProb;
    private final double recoveryProb;
    private final SplittableRandom random;

    /**
     * Vytvoří novou SIR simulaci.
     *
     * @param nodes         seznam všech uzlů grafu
     * @param infectionProb pravděpodobnost přenosu nákazy při kontaktu (0–1)
     * @param recoveryProb  pravděpodobnost uzdravení infikovaného uzlu za 1 časový krok (0–1)
     * @param seed          seed pro generátor náhodných čísel (pro opakovatelnost), může být null pro náhodný běh
     */
    public SequentialSIRSolver(List<Node> nodes, double infectionProb, double recoveryProb, Long seed) {
        this.nodes = nodes;
        this.infectionProb = infectionProb;
        this.recoveryProb = recoveryProb;
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

    public SequentialSIRSolver(List<Node> nodes, double infectionProb, double recoveryProb) {
        this(nodes, infectionProb, recoveryProb, null);
    }

    @Override
    public StepStats step(int tick) {
        long start = System.nanoTime();

        List<Node> toInfect = new ArrayList<>();
        List<Node> toRecover = new ArrayList<>();

        for (Node node : nodes) {
            if (node.state == State.INFECTED) {
                for (Node neighbor : node.neighbors) {
                    if (neighbor.state == State.SUSCEPTIBLE && random.nextDouble() < infectionProb) {
                        toInfect.add(neighbor);
                    }
                }
                if (random.nextDouble() < recoveryProb) {
                    toRecover.add(node);
                }
            }
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

    @Override
    public String getName() {
        return "Sequential SIR Solver";
    }
}