package sir.model;

// Represents a node in the simulation grid
public class Node {
    public final int x;
    public final int y;
    public State state;

    public Node(int x, int y) {
        this.x = x;
        this.y = y;
        this.state = State.SUSCEPTIBLE;
    }

    public Node(Node other) {
        this.x = other.x;
        this.y = other.y;
        this.state = other.state;
    }

    public Node(int x, int y, byte state) {
        this.x = x;
        this.y = y;
        this.state = switch (state) {
            case 0 -> State.SUSCEPTIBLE;
            case 1 -> State.INFECTED;
            case 2 -> State.RECOVERED;
            default -> throw new IllegalArgumentException("Invalid state: " + state);
        };
    }

    @Override
    public String toString() {
        return "Node(" + x + "," + y + ", " + state + ')';
    }
}