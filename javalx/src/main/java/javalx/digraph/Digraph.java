package javalx.digraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class Digraph {
  private final Set<Vertex> vertices = new HashSet<Vertex>();
  private final Set<Edge> edges = new HashSet<Edge>();

  public Edge createEdge (Vertex source, Vertex target) {
    Edge edge = source.link(target);
    edges.add(edge);
    return edge;
  }

  public Vertex createVertex () {
    Vertex vertex = new Vertex();
    vertices.add(vertex);
    return vertex;
  }

  public static final class Edge {
    private final Vertex source;
    private final Vertex target;

    private Edge (Vertex source, Vertex target) {
      this.source = source;
      this.target = target;
    }

    public Vertex getSource () {
      return source;
    }

    public Vertex getTarget () {
      return target;
    }

    public int getId () {
      return hashCode();
    }

    @Override public String toString () {
      return "Edge{id=" + this.getId() + '}';
    }

  }

  public static final class Vertex implements Iterable<Edge> {
    private final Set<Edge> incoming = new LinkedHashSet<Edge>();
    private final Set<Edge> outgoing = new LinkedHashSet<Edge>(3);
    private Vertex () {
    }

    private Edge link (Vertex target) {
      Edge edge = new Edge(this, target);
      this.outgoing.add(edge);
      target.incoming.add(edge);
      return edge;
    }



    public Collection<Edge> outgoing () {
      return Collections.unmodifiableSet(outgoing);
    }

    @Override public Iterator<Edge> iterator () {
      return outgoing().iterator();
    }

    public int getId () {
      return hashCode();
    }

    @Override public String toString () {
      return "Vertex{id=" + this.getId() + '}';
    }
  }
}
