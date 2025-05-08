package sir.graph;
import sir.model.StepStats;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class SimulationLogger implements AutoCloseable {
    private final BufferedWriter writer;

    public SimulationLogger(String filePath) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(filePath));
        writer.write("tick;newlyInfected;newlyRecovered;susceptible;infected;recovered;stepTimeMs\n");
    }

    public void log(StepStats stats) throws IOException {
        writer.write(String.format("%d;%d;%d;%d;%d;%d;%.3f%n",
                stats.tick(),
                stats.newlyInfected(),
                stats.newlyRecovered(),
                stats.totalSusceptible(),
                stats.totalInfected(),
                stats.totalRecovered(),
                stats.stepTimeNanos() / 1_000_000.0));
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }
}
