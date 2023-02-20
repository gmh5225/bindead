package rreil.cfa.util;

import java.io.PrintWriter;
import java.util.regex.Matcher;

import javalx.digraph.Digraph;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import rreil.cfa.Cfa;
import rreil.cfa.util.GmlWriter.EdgeLabelRenderer;
import rreil.cfa.util.GmlWriter.VertexLabelRenderer;

/**
 * Dumps digraphs to the Graphviz dot format.
 *
 * @author Bogdan Mihaila
 */
public class DotWriter {
  private final Digraph graph;
  private final VertexLabelRenderer vertexRenderer;
  private final EdgeLabelRenderer edgeRenderer;
  private final boolean blockEdges;

  public DotWriter (Cfa graph) {
    this(graph, graph, graph, true);
  }

  public DotWriter (Cfa graph, boolean blockEdges) {
    this(graph, graph, graph, blockEdges);
  }

  public DotWriter (Digraph graph, VertexLabelRenderer vertexRenderer, EdgeLabelRenderer edgeRenderer,
      boolean blockEdges) {
    this.graph = graph;
    this.vertexRenderer = vertexRenderer;
    this.edgeRenderer = edgeRenderer;
    this.blockEdges = blockEdges;
  }

  public void renderVertex (PrintWriter out, Vertex v) {
    String labelOfVertex = vertexRenderer.labelOfVertex(v);
    String color = "lightgray";

    if (labelOfVertex.startsWith(Cfa.$EntryVertexLabel)) {
      out.println(" subgraph entry_point { rank=source; " + v.getId() + "; }");
      color = "chartreuse4";
    }

    if (labelOfVertex.startsWith(Cfa.$ExitVertexLabel)) {
      out.println(" subgraph exit_point { rank=sink; " + v.getId() + "; }");
      color = "sienna";
    }

    if (labelOfVertex.startsWith("UNKN"))
      color = "yellow";

    out.println("\t" + v.getId() + " [label=\"" + labelOfVertex +
        "\", shape=rectangle, style=\"filled,rounded\", fillcolor=" + color + "];");
  }

  public void renderEdgeAsNode (PrintWriter out, Edge e) {
    String edgeNodeName = "edgeNode_" + e.getId();
    String edgeNodeLabel = edgeRenderer.labelOfEdge(e).replaceAll("\n", Matcher.quoteReplacement("\\l")) + "\\l";
    String edgeNode = "\t" + edgeNodeName + " [label=\"" + edgeNodeLabel + "\", shape=box, color=black]";
    out.println(edgeNode);
    out.println(e.getSource().getId() + " -> " + edgeNodeName + " [arrowhead=obox]");
    out.println(edgeNodeName + " -> " + e.getTarget().getId());
  }

  public void renderEdge (PrintWriter out, Edge e) {
    if (blockEdges)
      renderEdgeAsNode(out, e);
    else
      renderEdgeWithLabel(out, e);
  }

  public void renderEdgeWithLabel (PrintWriter out, Edge e) {
    String edge = e.getSource().getId() + " -> " + e.getTarget().getId();
    String edgeLabel = edgeRenderer.labelOfEdge(e).replaceAll("\n", "<BR/>").replaceAll("&", "&amp;");
    out.print("\t" + edge + " [");
    out.print("label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\"><TR><TD BALIGN=\"left\">");
    out.print(edgeLabel);
    out.print("</TD></TR></TABLE>>");
    out.println("];");
  }

  public void renderTo (PrintWriter out) {
    out.println("digraph {");
    out.println(" dpi=" + 72 + ";");
    out.println(" charset=\"UTF-8\"");
    out.println(" pencolor=transparent");

    for (Vertex v : graph.vertices()) {
      renderVertex(out, v);
    }

    for (Vertex v : graph.vertices()) {
      for (Edge e : v) {
        renderEdge(out, e);
      }
    }

    out.println("}");
  }
}
