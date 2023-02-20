package javalx.digraph.algorithms.dfs;

import java.util.HashMap;

import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;

public class Dfs {
  private static final DfsVertexVisitor nullVisitor =
      new DfsVertexVisitor() {
        @Override public void visitWhenFinished (Vertex v) {
        }

        @Override public void visitOnDiscovery (Vertex v) {
        }
      };
  private final DfsVertexVisitor visitor;
  private HashMap<Vertex, VertexColor> vertexColors = null;
  private HashMap<Edge, EdgeKind> edgeKinds = null;

  public Dfs () {
    this(nullVisitor);
  }

  public Dfs (DfsVertexVisitor visitor) {
    this.visitor = visitor;
  }

  public void run (Vertex rootVertex) {
    prepareState();
    visit(visitor, rootVertex);
  }

  private void prepareState () {
    vertexColors = new HashMap<Vertex, VertexColor>();
    edgeKinds = new HashMap<Edge, EdgeKind>();
  }

  private void visit (DfsVertexVisitor visitor, Vertex v) {
    // {v} is being processed.
    vertexColors.put(v, VertexColor.Gray);
    visitor.visitOnDiscovery(v);
    for (Edge outgoingEdge : v) {
      Vertex w = outgoingEdge.getTarget();
      switch (colorOfVertex(w)) {
        case White:
          visit(visitor, w);
          break;
        case Gray:
          edgeKinds.put(outgoingEdge, EdgeKind.Backward);
          break;
        case Black:
          break;
      }
    }
    // {v} is finished.
    vertexColors.put(v, VertexColor.Black);
    visitor.visitWhenFinished(v);
  }

  private VertexColor colorOfVertex (Vertex v) {
    VertexColor c = vertexColors.get(v);
    return c == null ? VertexColor.White : c;
  }

public boolean isBackwardEdge (Edge edge) {
    return edgeKinds.get(edge) == EdgeKind.Backward;
  }
}
