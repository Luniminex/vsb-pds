package sir;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import sir.graph.SimulationLogger;
import sir.model.Node;
import sir.model.StepStats;
import sir.solver.SIRSolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimulationRunnerBuilder {
    private Graph<Node, DefaultEdge> graph;
    private SIRSolver solver;
    private SimulationLogger logger;

    public SimulationRunnerBuilder graph(Graph<Node, DefaultEdge> graph) {
        this.graph = graph;
        return this;
    }

    public SimulationRunnerBuilder solver(SIRSolver solver) {
        this.solver = solver;
        return this;
    }

    public SimulationRunnerBuilder logger(SimulationLogger logger) {
        this.logger = logger;
        return this;
    }

    public void run() {
        if (graph == null || solver == null || logger == null) {
            throw new IllegalStateException("Graph, solver, and logger must be set before running the simulation.");
        }

        int tick = 0;
        long totalNanos = 0;
        long startWall = System.nanoTime();
        long lastReport = startWall;

        try (SimulationLogger log = logger) {
            while (!solver.isFinished()) {
                StepStats stats = solver.step(tick);
                log.log(stats);
                totalNanos += stats.stepTimeNanos();
                tick++;

                long now = System.nanoTime();
                if ((now - lastReport) > 10_000_000_000L) {
                    System.out.printf("[progress] Tick %d, infected: %d, recovered: %d%n",
                            stats.tick(), stats.totalInfected(), stats.totalRecovered());
                    lastReport = now;
                }
            }

            System.out.printf("Simulation for %s ended in %d steps, taking %.3f seconds.%n",
                    solver.getName(), tick, totalNanos / 1_000_000_000.0);
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        } finally {
            solver.shutdown();
        }
    }
}
