package sir.model;


public record RunStats(
        String generation,
        String solverName,
        int runNumber,
        int ticks,
        long totalTimeNs,
        long avgStepNs,
        long maxStepNs,
        long minStepNs,
        Configuration config
) {
}