package sir.grid;

import sir.model.StepStats;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class SimulationLogger implements Closeable {
    private final PrintWriter writer;
    private boolean headerWritten = false;

    public SimulationLogger(String filePath) throws IOException {
        this.writer = new PrintWriter(new FileWriter(filePath));
    }

    public void log(StepStats stats) {
        if (!headerWritten) {
            writer.println("Tick,NewlyInfected,NewlyRecovered,TotalSusceptible,TotalInfected,TotalRecovered,StepTimeNanos");
            headerWritten = true;
        }
        writer.printf("%d,%d,%d,%d,%d,%d,%d%n",
                stats.tick(),
                stats.newlyInfected(),
                stats.newlyRecovered(),
                stats.totalSusceptible(),
                stats.totalInfected(),
                stats.totalRecovered(),
                stats.stepTimeNanos());
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }
}