package sir.model;

// Record used to store statistics for each simulation step
public record StepStats(
        int tick,
        int newlyInfected,
        int newlyRecovered,
        int totalSusceptible,
        int totalInfected,
        int totalRecovered,
        Long stepTimeNanos
) {
    @Override
    public String toString() {
        return String.format("Step %d: +%d infected; +%d recovered; | Susceptible=%d; Total Infected=%d; Total Recovered=%d| Step Time: %.3f ms",
                tick, newlyInfected, newlyRecovered, totalSusceptible, totalInfected, totalRecovered, stepTimeNanos % 1_000_000.0);
    }
}