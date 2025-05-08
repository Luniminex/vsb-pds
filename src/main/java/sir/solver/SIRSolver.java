package sir.solver;

import sir.model.Node;
import sir.model.StepStats;

import java.util.List;

public interface SIRSolver {

    String getName();
    StepStats step(int tick);
    boolean isFinished();
    List<Node> getCurrentState();

    default void shutdown() {}
}
