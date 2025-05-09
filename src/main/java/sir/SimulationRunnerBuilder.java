package sir;

import sir.grid.SimulationLogger;
import sir.model.Configuration;
import sir.model.StepStats;
import sir.solver.SIRSolver;

import java.io.IOException;
import java.nio.file.Path;

public class SimulationRunnerBuilder {
    private SIRSolver solver;
    private SimulationLogger logger;
    private Path outputPath;
    private Configuration configuration;

    public SimulationRunnerBuilder solver(SIRSolver solver) {
        this.solver = solver;
        return this;
    }

    public SimulationRunnerBuilder logger(SimulationLogger logger, Path outputPath) {
        this.logger = logger;
        this.outputPath = outputPath;
        return this;
    }

    public SimulationRunnerBuilder configuration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public void run() {
        if (solver == null || logger == null) {
            throw new IllegalStateException("Solver and logger must be set before running the simulation.");
        }
        if (configuration == null) {
            throw new IllegalStateException("Simulation configuration must be set.");
        }

        configuration.printStats(solver.getName());

        if (outputPath != null) {
            System.out.printf("Output Log: %s%n", outputPath.toAbsolutePath());
        }

        int tick = 0;
        long totalNanos = 0;
        long startWall = System.nanoTime();
        long lastReport = startWall;

        // Start the simulation
        try (SimulationLogger log = logger) {
            while (!solver.isFinished()) {
                StepStats stats = solver.step(tick);
                log.log(stats);
                totalNanos += stats.stepTimeNanos();
                tick++;

                // Report progress every 5 seconds
                long now = System.nanoTime();
                if ((now - lastReport) > 5_000_000_000L) {
                    System.out.printf("[%s progress] Tick %d, S: %d, I: %d, R: %d%n",
                            solver.getName(), stats.tick(), stats.totalSusceptible(), stats.totalInfected(), stats.totalRecovered());
                    lastReport = now;
                }
            }
            long endWall = System.nanoTime();
            System.out.printf("Simulation for %s ended in %d steps. Solver CPU time: %.3f s. Wall clock time: %.3f s.%n",
                    solver.getName(), tick, totalNanos / 1_000_000_000.0, (endWall - startWall) / 1_000_000_000.0);

        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        } finally {
            solver.shutdown();
        }
    }
}