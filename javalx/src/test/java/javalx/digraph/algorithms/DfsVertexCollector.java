package javalx.digraph.algorithms;

import java.util.ArrayList;
import java.util.List;

import javalx.digraph.Digraph;
import javalx.digraph.algorithms.dfs.DfsVertexVisitor;

public class DfsVertexCollector implements DfsVertexVisitor {
  private final List<Digraph.Vertex> whenFinishedStack = new ArrayList<Digraph.Vertex>();
  private final List<Digraph.Vertex> onDiscoveryStack = new ArrayList<Digraph.Vertex>();

  @Override
  public void visitOnDiscovery (Digraph.Vertex v) {
    onDiscoveryStack.add(v);
  }

  @Override
  public void visitWhenFinished (Digraph.Vertex v) {
    whenFinishedStack.add(v);
  }

  public List<Digraph.Vertex> getWhenFinishedStack () {
    return whenFinishedStack;
  }

  public List<Digraph.Vertex> getOnDiscoveryStack () {
    return onDiscoveryStack;
  }
}
