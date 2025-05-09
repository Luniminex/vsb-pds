package sir.model;

// Optimized node class for the simulation used by fork join solver
public class OptNode {
    public final int x, y;
    public byte state;

    public static final byte SUSCEPTIBLE = 0;
    public static final byte INFECTED = 1;
    public static final byte RECOVERED = 2;

    public OptNode(int x, int y, byte state) {
        this.x = x;
        this.y = y;
        this.state = state;
    }
}
