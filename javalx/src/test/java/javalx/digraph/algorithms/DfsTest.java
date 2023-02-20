package javalx.digraph.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javalx.digraph.Digraph;
import javalx.digraph.algorithms.dfs.Dfs;

import org.junit.Test;


public class DfsTest {
  private static final Digraph g = new Digraph();
  private static final Digraph.Vertex a = g.createVertex();
  private static final Digraph.Vertex b = g.createVertex();
  private static final Digraph.Vertex c = g.createVertex();
  private static final Digraph.Vertex d = g.createVertex();
  private static final Digraph.Vertex e = g.createVertex();
  private static final Digraph.Vertex f = g.createVertex();
  private static final Digraph.Edge aa = g.createEdge(a, a);
  private static final Digraph.Edge ab = g.createEdge(a, b);
  private static final Digraph.Edge ac = g.createEdge(a, c);
  private static final Digraph.Edge af = g.createEdge(a, f);
  private static final Digraph.Edge cd = g.createEdge(c, d);
  private static final Digraph.Edge ce = g.createEdge(c, e);
  private static final Digraph.Edge da = g.createEdge(d, a);
  private static final Digraph.Edge ed = g.createEdge(e, d);


  @Test
  public void whenFinishedVertices () {
    DfsVertexCollector collector = new DfsVertexCollector();
    Dfs dfs = new Dfs(collector);
    dfs.run(a);

    List<Digraph.Vertex> whenFinished = collector.getWhenFinishedStack();

    assertEquals(Integer.valueOf(6), Integer.valueOf(whenFinished.size()));
    assertTrue(whenFinished.contains(a));
    assertTrue(whenFinished.contains(b));
    assertTrue(whenFinished.contains(c));
    assertTrue(whenFinished.contains(d));
    assertTrue(whenFinished.contains(e));
    assertTrue(whenFinished.contains(f));
  }

  @Test
  public void onDiscoveryVertices () {
    DfsVertexCollector collector = new DfsVertexCollector();
    Dfs dfs = new Dfs(collector);
    dfs.run(a);

    List<Digraph.Vertex> onDiscovery = collector.getOnDiscoveryStack();

    assertEquals(Integer.valueOf(6), Integer.valueOf(onDiscovery.size()));
    assertTrue(onDiscovery.contains(a));
    assertTrue(onDiscovery.contains(b));
    assertTrue(onDiscovery.contains(c));
    assertTrue(onDiscovery.contains(d));
    assertTrue(onDiscovery.contains(e));
    assertTrue(onDiscovery.contains(f));
  }

  @Test
  public void backwardEdges () {
    DfsVertexCollector collector = new DfsVertexCollector();
    Dfs dfs = new Dfs(collector);
    dfs.run(a);

    assertTrue(dfs.isBackwardEdge(aa));
    assertTrue(!dfs.isBackwardEdge(ab));
    assertTrue(!dfs.isBackwardEdge(ac));
    assertTrue(!dfs.isBackwardEdge(af));
    assertTrue(!dfs.isBackwardEdge(cd));
    assertTrue(!dfs.isBackwardEdge(ce));
    assertTrue(dfs.isBackwardEdge(da));
    assertTrue(!dfs.isBackwardEdge(ed));
  }
}
