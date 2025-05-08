package sir.model;

import java.util.ArrayList;
import java.util.List;

public class Node {
    public final int id;
    public State state = State.SUSCEPTIBLE;
    public List<Node> neighbors = new ArrayList<>();

    public Node(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Node(" + id + ", " + state + ")";
    }
}
