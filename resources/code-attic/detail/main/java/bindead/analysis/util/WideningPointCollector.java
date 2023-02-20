package bindead.analysis.util;

import java.util.HashSet;
import java.util.Set;

import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.digraph.algorithms.dfs.Dfs;
import rreil.cfa.Cfa;

public class WideningPointCollector {
  private final Cfa cfa;
  private final Set<Vertex> wideningPoints;

  public WideningPointCollector (Cfa cfa) {
    this.cfa = cfa;
    this.wideningPoints = new HashSet<Vertex>();
  }

  public void run () {
    Dfs dfs = new Dfs();
    dfs.run(cfa.getEntry());

    for (Vertex v : cfa.vertices()) {
      if (hasIncomingBackwardEdge(dfs, v))
        wideningPoints.add(v);
    }
  }

  public boolean isWideningPoint (Vertex v) {
    return wideningPoints.contains(v);
  }

  private static boolean hasIncomingBackwardEdge (Dfs dfs, Vertex v) {
    for (Edge e : v.incoming()) {
      if (dfs.isBackwardEdge(e))
        return true;
    }
    return false;
  }
}
