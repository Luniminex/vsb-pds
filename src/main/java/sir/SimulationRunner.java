package sir;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import sir.graph.GraphDataManager;
import sir.graph.GraphSupplier;
import sir.graph.SimulationLogger;
import sir.model.Node;
import sir.solver.ParallelSIRSolver;
import sir.solver.SequentialSIRSolver;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

public class SimulationRunner {

    private static final Double infectionRate = 0.1;
    private static final Double recoveryRate = 0.05;
    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            runSingleGraph(args[0]);
        } else {
            runAllGraphs();
        }
    }

    private static void runSingleGraph(String filename) throws Exception {
        Graph<Node, DefaultEdge> graph = GraphDataManager.loadGraph(filename);
        GraphSupplier supplier = new GraphSupplier(graph);
        long seed = 12345L;

        System.out.println("Running simulation for: " + filename);

        new SimulationRunnerBuilder()
                .graph(graph)
                .solver(new SequentialSIRSolver(supplier.copyNodes(), 0.05, 0.05, seed))
                .logger(new SimulationLogger("output/single_seq.csv"))
                .run();

        new SimulationRunnerBuilder()
                .graph(graph)
                .solver(new ParallelSIRSolver(supplier.copyNodes(), 0.05, 0.05, Runtime.getRuntime().availableProcessors(), seed))
                .logger(new SimulationLogger("output/single_par.csv"))
                .run();
    }

    private static void runAllGraphs() throws IOException {
        Path inputDir = Path.of("src/main/resources/graphs");
        Path outputBase = Path.of("src/main/resources/output");

        int nextGen = Files.list(outputBase)
                .filter(Files::isDirectory)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.startsWith("gen"))
                .map(name -> name.substring(3))
                .mapToInt(Integer::parseInt)
                .max().orElse(0) + 1;

        Path thisRunDir = outputBase.resolve("gen" + nextGen);
        Files.createDirectories(thisRunDir);

        List<Path> graphFiles = Files.list(inputDir)
                .filter(p -> p.toString().endsWith(".csv") || p.toString().endsWith(".txt"))
                .sorted(Comparator.comparing(Path::toString))
                .collect(Collectors.toList());

        long seed = new SplittableRandom(System.currentTimeMillis()).nextLong();

        for (Path graphFile : graphFiles) {
            String name = graphFile.getFileName().toString();
            String baseName = name.substring(0, name.lastIndexOf("."));
            System.out.println("Loading graph: " + name);
            Graph<Node, DefaultEdge> graph = GraphDataManager.loadGraph(name);
            GraphSupplier supplier = new GraphSupplier(graph);

            System.out.println("Running on: " + baseName + " with " + graph.vertexSet().size() + " nodes and " + graph.edgeSet().size() + " edges.");
            new SimulationRunnerBuilder()
                    .graph(graph)
                    .solver(new SequentialSIRSolver(supplier.copyNodes(), infectionRate , recoveryRate, seed))
                    .logger(new SimulationLogger(thisRunDir.resolve(baseName + "_seq.csv").toString()))
                    .run();

            new SimulationRunnerBuilder()
                    .graph(graph)
                    .solver(new ParallelSIRSolver(supplier.copyNodes(), infectionRate, recoveryRate, Runtime.getRuntime().availableProcessors(), seed))
                    .logger(new SimulationLogger(thisRunDir.resolve(baseName + "_par.csv").toString()))
                    .run();
        }

        System.out.println("All simulations completed. Output in: " + thisRunDir);
    }
}
