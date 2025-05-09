package sir.grid;

import sir.model.Configuration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class OutputManager {
    private final Path currentRunGenDir;

    public OutputManager(String baseOutputDirectoryPath, Configuration configuration) throws IOException {
        // Create the base output directory if it doesn't exist
        Path outputBase = Paths.get(baseOutputDirectoryPath);
        Files.createDirectories(outputBase);

        // Determine the next generation number based on existing directories
        int nextGenNumber = 1;
        Pattern genPattern = Pattern.compile("^gen(\\d+)$");

        if (Files.exists(outputBase) && Files.isDirectory(outputBase)) {
            try (Stream<Path> stream = Files.list(outputBase)) {
                // Find the maximum generation number from existing directories
                nextGenNumber = stream
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .map(genPattern::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> Integer.parseInt(matcher.group(1)))
                        .max(Comparator.naturalOrder())
                        .orElse(0) + 1;
            } catch (IOException e) {
                System.err.println("Error reading output directory: " + e.getMessage() +
                        ". Using default gen" + nextGenNumber + " or creating new.");
            }
        }
        this.currentRunGenDir = outputBase.resolve("gen" + nextGenNumber);
        Files.createDirectories(this.currentRunGenDir);

        saveConfigurationToFile(configuration, this.currentRunGenDir);
    }

    private void saveConfigurationToFile(Configuration config, Path directory) throws IOException {
        Path configFilePath = directory.resolve("config.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFilePath.toFile()))) {
            writer.write("Width: " + config.gridWidth());
            writer.newLine();
            writer.write("Height: " + config.gridHeight());
            writer.newLine();
            writer.write("InitialInfectedCount: " + config.initialInfectedCount());
            writer.newLine();
            writer.write(String.format("InfectionProbability: %.5f", config.infectionProbability()));
            writer.newLine();
            writer.write(String.format("RecoveryProbability: %.5f", config.recoveryProbability()));
            writer.newLine();
            writer.write("Seed: " + (config.seed() == null ? "Random (null)" : config.seed().toString()));
            writer.newLine();
        }
        System.out.println("Configuration saved to: " + configFilePath.toAbsolutePath());
    }

    public Path getCurrentRunGenDir() {
        return this.currentRunGenDir;
    }

    public Path getLogFilePath(String fileName) {
        return this.currentRunGenDir.resolve(fileName);
    }
}
