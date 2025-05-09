package sir.analyzer;

import sir.model.Configuration;
import sir.model.RunStats;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// This class is responsible for loading and processing run statistics from CSV files
public class RunStatsLoader {

    private RunStatsLoader() { }
    public static List<RunStats> loadRuns(Path solverDir, String generation, Configuration config) throws IOException {
        List<RunStats> results = new ArrayList<>();

        try (var files = Files.list(solverDir)) {
            // Filter for CSV files that start with "run_"
            for (Path csv : files.filter(p -> p.getFileName().toString().startsWith("run_")).toList()) {
                List<Long> times = new ArrayList<>();
                int tickCount = 0;

                try (BufferedReader reader = Files.newBufferedReader(csv)) {
                    // skip header
                    String header = reader.readLine();
                    String line;
                    // Read each line of the CSV file
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length < 7) continue;
                        long stepTime = Long.parseLong(parts[6]);
                        times.add(stepTime);
                        tickCount++;
                    }
                }

                // Calculate statistics
                if (!times.isEmpty()) {
                    long total = times.stream().mapToLong(Long::longValue).sum();
                    long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
                    long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
                    long avg = total / times.size();

                    String fileName = csv.getFileName().toString();
                    int runNumber = extractRunNumber(fileName);

                    results.add(new RunStats(
                            generation,
                            solverDir.getFileName().toString(),
                            runNumber,
                            tickCount,
                            total,
                            avg,
                            max,
                            min,
                            config
                    ));
                }
            }
        }

        return results;
    }

    private static int extractRunNumber(String filename) {
        try {
            String number = filename.replaceAll("[^0-9]", "");
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}