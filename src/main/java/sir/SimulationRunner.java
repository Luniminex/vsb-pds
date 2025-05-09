package sir;

import sir.grid.OutputManager;
import sir.grid.GridSupplier;
import sir.grid.SimulationLogger;
import sir.model.Configuration;
import sir.solver.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class SimulationRunner {

    private static final int GRID_WIDTH = 3000;
    private static final int GRID_HEIGHT = 3000;
    private static final int INITIAL_INFECTED_COUNT = 5;
    private static final double INFECTION_PROBABILITY = 0.1;
    private static final double RECOVERY_PROBABILITY = 0.05;
    private static final Long SEED = 123456789L;
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final String BASE_OUTPUT_DIRECTORY = "src/main/resources/output";
    private static final int NUMBER_OF_REPEATS = 2;

    private static String sanitizeSolverName(String solverName) {
        return solverName.replaceAll("[^a-zA-Z0-9.-]", "_").replaceAll("__+", "_");
    }

    public static void main(String[] args) throws IOException {
        //Create configuration that is same for all of the solvers
        Configuration configuration = new Configuration(
                GRID_WIDTH,
                GRID_HEIGHT,
                INITIAL_INFECTED_COUNT,
                INFECTION_PROBABILITY,
                RECOVERY_PROBABILITY,
                SEED
        );

        //Create output manager that will create a new directory for each run
        OutputManager outputManager = new OutputManager(BASE_OUTPUT_DIRECTORY, configuration);
        //Get the current run generation directory
        Path currentRunGenDir = outputManager.getCurrentRunGenDir();

        System.out.printf("Overall simulation run. Outputting to base directory: %s%n", currentRunGenDir.toAbsolutePath());
        System.out.printf("Each solver will be run %d times.%n%n", NUMBER_OF_REPEATS);

        //Create a grid supplier that will be used to create the grid for each solver
        GridSupplier gridSupplier = new GridSupplier(
                configuration.gridWidth(),
                configuration.gridHeight(),
                configuration.initialInfectedCount(),
                configuration.seed()
        );

        //Create a supplier for each solver
        Supplier<SIRSolver> forkJoinSupplier = createForkJoinSolverSupplier(gridSupplier, configuration, THREAD_COUNT);
        Supplier<SIRSolver> completableFutureSupplier = createCompletableFutureSolverSupplier(gridSupplier, configuration, THREAD_COUNT);
        Supplier<SIRSolver> simpleSequentialSupplier = createSimpleSequentialSolverSupplier(gridSupplier, configuration);
        Supplier<SIRSolver> simpleParallelSupplier = createSimpleParallelSolverSupplier(gridSupplier, configuration, THREAD_COUNT);

        //Run each solver for the specified number of repeats
        runSolverRepeats(forkJoinSupplier, configuration, currentRunGenDir);
        runSolverRepeats(completableFutureSupplier, configuration, currentRunGenDir);
        runSolverRepeats(simpleSequentialSupplier, configuration, currentRunGenDir);
        runSolverRepeats(simpleParallelSupplier, configuration, currentRunGenDir);
        System.out.println("\nAll simulations completed. Overall output in: " + currentRunGenDir.toAbsolutePath());
    }

    private static void runSolverRepeats(Supplier<SIRSolver> solverSupplier,
                                         Configuration config,
                                         Path currentRunGenDir) throws IOException {
        // Create a temporary solver to get the name
        SIRSolver tempSolverForName = solverSupplier.get();
        // Use the solver's name to create a directory
        String sanitizedSolverName = sanitizeSolverName(tempSolverForName.getName());
        Path solverSpecificBaseDir = currentRunGenDir.resolve(sanitizedSolverName);
        Files.createDirectories(solverSpecificBaseDir);
        // Shutdown the temporary solver
        tempSolverForName.shutdown();

        System.out.printf("--- Preparing to run solver: %s ---%n", tempSolverForName.getName());
        for (int repeat = 1; repeat <= NUMBER_OF_REPEATS; repeat++) {
            SIRSolver solver = solverSupplier.get();

            // Create a new log file for each repeat
            Path logPath = solverSpecificBaseDir.resolve("run_" + repeat + "_stats.csv");

            // Create a new output manager for each repeat
            System.out.printf("-- Starting Repeat %d/%d for %s --%n", repeat, NUMBER_OF_REPEATS, solver.getName());

            // Create a new simulation runner and run the simulation
            new SimulationRunnerBuilder()
                    .configuration(config)
                    .solver(solver)
                    .logger(new SimulationLogger(logPath.toString()), logPath)
                    .run();

            if (repeat < NUMBER_OF_REPEATS) {
                System.out.println();
            }
        }
        System.out.println();
    }

    private static Supplier<SIRSolver> createForkJoinSolverSupplier(GridSupplier gridSupplier, Configuration configuration, int numThreads) {
        return () -> new ForkJoinGridSIRSolver(
                gridSupplier.copyNodes(),
                gridSupplier.getWidth(),
                gridSupplier.getHeight(),
                configuration.infectionProbability(),
                configuration.recoveryProbability(),
                numThreads,
                configuration.seed());
    }

    private static Supplier<SIRSolver> createCompletableFutureSolverSupplier(GridSupplier gridSupplier, Configuration configuration, int numThreads) {
        return () -> new CompletableFutureSIRSolver(
                gridSupplier.copyNodes(),
                gridSupplier.getWidth(),
                gridSupplier.getHeight(),
                configuration.infectionProbability(),
                configuration.recoveryProbability(),
                numThreads,
                configuration.seed());
    }

    private static Supplier<SIRSolver> createSimpleSequentialSolverSupplier(GridSupplier gridSupplier, Configuration configuration) {
        return () -> new SimpleSequentialGridSIRSolver(
                gridSupplier.copyNodes(),
                gridSupplier.getWidth(),
                gridSupplier.getHeight(),
                configuration.infectionProbability(),
                configuration.recoveryProbability(),
                configuration.seed());
    }

    private static Supplier<SIRSolver> createSimpleParallelSolverSupplier(GridSupplier gridSupplier, Configuration configuration, int numThreads) {
        return () -> new SimpleParallelGridSIRSolver(
                gridSupplier.copyNodes(),
                gridSupplier.getWidth(),
                gridSupplier.getHeight(),
                configuration.infectionProbability(),
                configuration.recoveryProbability(),
                numThreads,
                configuration.seed());
    }
}
