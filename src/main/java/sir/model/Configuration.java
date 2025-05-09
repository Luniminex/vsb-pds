package sir.model;

public record Configuration(
        int gridWidth,
        int gridHeight,
        int initialInfectedCount,
        double infectionProbability,
        double recoveryProbability,
        Long seed
) {

    public void printStats(String solverName) {
        System.out.println("--- Simulation Configuration & Run ---");
        System.out.printf("Solver: %s%n", solverName);
        System.out.printf("Grid Dimensions: %d x %d%n", gridWidth, gridHeight);
        System.out.printf("Initial Infected: %d%n", initialInfectedCount);
        System.out.printf("Infection Probability: %.3f%n", infectionProbability);
        System.out.printf("Recovery Probability: %.3f%n", recoveryProbability);
        System.out.printf("Seed: %s%n", (seed == null ? "Random" : seed.toString()));
    }
}