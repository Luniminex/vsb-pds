package sir.model;

// Record used to store the result of a simulation step for fork join solver
public record StepResult(
        int infected,
        int recovered,
        int s,
        int i,
        int r
) {
    public StepResult merge(StepResult other) {
        return new StepResult(
                this.infected + other.infected,
                this.recovered + other.recovered,
                this.s + other.s,
                this.i + other.i,
                this.r + other.r
        );
    }
}