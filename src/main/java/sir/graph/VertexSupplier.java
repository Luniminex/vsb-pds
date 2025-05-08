package sir.graph;

import sir.model.Node;

import java.util.function.Supplier;

public class VertexSupplier  implements Supplier<Node> {
    private int id = 0;
    @Override
    public Node get() {
        return new Node(id++);
    }
}
