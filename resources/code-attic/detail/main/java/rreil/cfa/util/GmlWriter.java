package rreil.cfa.util;

import java.io.PrintWriter;

import javalx.digraph.Digraph;
import rreil.cfa.Cfa;

public class GmlWriter {
  private final Digraph graph;
  private final VertexLabelRenderer vertexRenderer;
  private final EdgeLabelRenderer edgeRenderer;

  public GmlWriter (Cfa graph) {
    this(graph, graph, graph);
  }

  public GmlWriter (Digraph graph, VertexLabelRenderer vertexRenderer, EdgeLabelRenderer edgeRenderer) {
    this.graph = graph;
    this.vertexRenderer = vertexRenderer;
    this.edgeRenderer = edgeRenderer;
  }

  public void renderVertex (PrintWriter out, Digraph.Vertex v) {
    out.println(" node [");
    out.println("  id " + v.getId());
    out.println("  label " + "\"" + vertexRenderer.labelOfVertex(v) + "\"");
    out.println("  graphics [");
    out.println("   type \"roundrectangle\"");
    out.println("   fill \"#C0C0C0\"");
    out.println("   outline \"#000000\"");
    out.println("  ]");
    out.println(" ]");
  }

  public void renderEdge (PrintWriter out, Digraph.Edge e) {
    out.println(" edge [");
    out.println("  source " + e.getSource().getId());
    out.println("  target " + e.getTarget().getId());
    out.println("  label \"" + edgeRenderer.labelOfEdge(e).replaceAll("&", "&amp;") + "\"");
    out.println("  LabelGraphics [");
    out.println("   outline \"#000000\"");
    out.println("   fill \"#FAE9B9\"");
    out.println("   fontSize 12");
    out.println("   fontName \"Envy Code R\"");
    out.println("   model \"center_slider\"");
    out.println("   alignment \"left\"");
    out.println("  ]");
    out.println(" ]");
  }

  public void renderTo (PrintWriter out) {
    out.println("graph [");
    out.println(" directed 1");

    for (Digraph.Vertex v : graph.vertices()) {
      renderVertex(out, v);
    }

    for (Digraph.Vertex v : graph.vertices()) {
      for (Digraph.Edge e : v) {
        renderEdge(out, e);
      }
    }

    out.println("]");
  }

  public static interface VertexLabelRenderer {
    public String labelOfVertex (Digraph.Vertex v);
  }

  public static interface EdgeLabelRenderer {
    public String labelOfEdge (Digraph.Edge e);
  }
}
