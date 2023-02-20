package bindead.domains.fields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.digraph.Digraph;
import javalx.digraph.algorithms.dfs.Dfs;
import javalx.digraph.algorithms.dfs.DfsVertexVisitor;
import javalx.numeric.Bound;
import javalx.numeric.FiniteRange;
import javalx.persistentcollections.tree.OverlappingRanges;

/**
 * Helper class for resolving partitionings from overlapping fields using interval trees represented as DAGs.
 */
public class FieldGraph extends Digraph {
  private final Map<Bound, Vertex> bounds = new HashMap<Bound, Vertex>();
  private final Map<Edge, P2<FiniteRange, VariableCtx>> weights = new HashMap<Edge, P2<FiniteRange, VariableCtx>>();

  private FieldGraph () {
  }

  public static FieldGraph build (OverlappingRanges<VariableCtx> overlapping) {
    FieldGraph g = new FieldGraph();
    for (P2<FiniteRange, VariableCtx> field : overlapping) {
      g.bind(field);
    }
    return g;
  }

  private void bind (P2<FiniteRange, VariableCtx> field) {
    FiniteRange key = field._1();
    Vertex u = vertexOf(key.low());
    Vertex v = vertexOf(key.high().add(Bound.ONE));
    Edge e = createEdge(u, v);
    weights.put(e, field);
  }

  private Vertex vertexOf (Bound bound) {
    Vertex v = bounds.get(bound);
    if (v == null) {
      v = createVertex();
      bounds.put(bound, v);
    }
    return v;
  }

  /**
   * Try to find a partitioning starting at {@code root} with endpoint {@code target}.
   *
   * @param root
   * @param target
   * @return
   */
  public Partitioning findPartitioning (Bound root, Bound target) {
    Vertex rootVertex = bounds.get(root);
    Vertex targetVertex = bounds.get(target.add(Bound.ONE));
    if (rootVertex == null || targetVertex == null)
      return new Partitioning();
    return findPath(rootVertex, targetVertex);
  }

  private Partitioning findPath (Vertex root, Vertex target) {
    PathFinder path = new PathFinder();
    Dfs dfs = new Dfs(path);
    dfs.run(root);
    return resolvePath(path.resolvePath(target));
  }

  private Partitioning resolvePath (List<Vertex> path) {
    Partitioning fields = new Partitioning();
    Iterator<Vertex> i = path.iterator();
    Vertex u, v = i.next();
    while (i.hasNext()) {
      u = v;
      v = i.next();
      Edge e = resolveEdge(u, v);
      fields.add(weights.get(e));
    }
    return fields;
  }

  private static Edge resolveEdge (Vertex u, Vertex v) {
    for (Edge e : u.outgoing()) {
      if (e.getTarget().equals(v))
        return e;
    }
    return null;
  }

  private static class PathFinder implements DfsVertexVisitor {
    private final Map<Vertex, Vertex> parents = new HashMap<Vertex, Vertex>();
    private Vertex current;

    @Override public void visitOnDiscovery (Vertex v) {
      parents.put(v, current);
      current = v;
    }

    @Override public void visitWhenFinished (Vertex v) {
      current = parents.get(v);
    }

    public List<Vertex> resolvePath (Vertex target) {
      LinkedList<Vertex> path = new LinkedList<Vertex>();
      Vertex v = target;
      for (; parents.get(v) != null; v = parents.get(v)) {
        path.push(v);
      }
      // Add the root node to the path.
      path.push(v);
      return path;
    }
  }

  public static class Partitioning extends ArrayList<P2<FiniteRange, VariableCtx>> {
    private static final long serialVersionUID = 1L;

    /**
     * Returns an interval spanning the complete partitioning or none if the partitioning is empty.
     */
    public Option<FiniteRange> span () {
      if (isEmpty())
        return Option.none();
      FiniteRange start = get(0)._1();
      FiniteRange end = get(size() - 1)._1();
      return Option.some(start.join(end));
    }

    @Override public String toString () {
      return "Partitioning{" + super.toString() + '}';
    }
  }
}
