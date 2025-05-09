package sir.analyzer;

import sir.model.RunStats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class CsvWriter {

    private CsvWriter() {}
    public static void writeToCsv(Path csvPath, List<RunStats> statsList) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            writer.write("Generation,Solver,Run,Width,Height,InitInfected,InfProb,RecProb,Seed,Ticks,TotalTimeNs,AvgStepNs,MaxStepNs,MinStepNs");
            writer.newLine();

            for (RunStats stats : statsList) {
                var c = stats.config();
                writer.write(String.format(Locale.US,
                        "%s,%s,%d,%d,%d,%d,%.5f,%.5f,%s,%d,%d,%d,%d,%d",
                        stats.generation(),
                        stats.solverName(),
                        stats.runNumber(),
                        c.gridWidth(),
                        c.gridHeight(),
                        c.initialInfectedCount(),
                        c.infectionProbability(),
                        c.recoveryProbability(),
                        c.seed() == null ? "Random" : c.seed().toString(),
                        stats.ticks(),
                        stats.totalTimeNs(),
                        stats.avgStepNs(),
                        stats.maxStepNs(),
                        stats.minStepNs()
                ));
                writer.newLine();
            }
        }
    }
}
