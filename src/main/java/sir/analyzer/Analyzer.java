package sir.analyzer;

import sir.model.Configuration;
import sir.model.RunStats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Analyzes simulation results from multiple generations and solvers and puts them into CSV files
public class Analyzer {

    private static final String BASE_FOLDER = "src/main/resources/output/";
    public static final String OUTPUT_FOLDER = "src/main/resources/processed/";

    public static void main(String[] args) throws IOException {
        Path rootDir = Paths.get(BASE_FOLDER);
        Path outputDir = Paths.get(OUTPUT_FOLDER);

        Files.createDirectories(outputDir);

        Map<String, List<RunStats>> allStatsPerSolver = new HashMap<>();

        // Process each generation folder
        try (Stream<Path> genFolders = Files.list(rootDir)) {
            genFolders
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("gen"))
                    .forEach(genPath -> {
                        try {
                            // Load configuration for the generation
                            Configuration config = ConfigLoader.load(genPath.resolve("config.txt"));
                            processSolvers(genPath, config, allStatsPerSolver);
                        } catch (IOException e) {
                            System.err.println("Skipping " + genPath + ": " + e.getMessage());
                        }
                    });
        }

        // Write all statistics to CSV files
        for (var entry : allStatsPerSolver.entrySet()) {
            Path outputFile = outputDir.resolve(entry.getKey() + ".csv");
            CsvWriter.writeToCsv(outputFile, entry.getValue());
        }
    }

    private static void processSolvers(Path genPath, Configuration config, Map<String, List<RunStats>> statsMap) throws IOException {
        try (Stream<Path> solverFolders = Files.list(genPath)) {
            solverFolders.filter(Files::isDirectory).forEach(solverPath -> {
                String solverName = solverPath.getFileName().toString();

                try {
                    // Load run statistics for the solver
                    List<RunStats> runStats = RunStatsLoader.loadRuns(
                            solverPath,
                            genPath.getFileName().toString(),
                            config
                    );

                    statsMap.computeIfAbsent(solverName, k -> new ArrayList<>()).addAll(runStats);
                } catch (IOException e) {
                    System.err.println("Failed processing " + solverName + ": " + e.getMessage());
                }
            });
        }
    }
}
