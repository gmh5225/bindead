package javalx.digraph.algorithms.dfs;

import javalx.digraph.Digraph;

public interface DfsVertexVisitor {
  public void visitOnDiscovery (Digraph.Vertex v);

  public void visitWhenFinished (Digraph.Vertex v);
}
