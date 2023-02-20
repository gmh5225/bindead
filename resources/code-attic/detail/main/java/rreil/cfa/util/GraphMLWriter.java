package rreil.cfa.util;

import com.jamesmurty.utils.XMLBuilder;
import java.io.PrintWriter;
import java.util.Properties;

import javalx.digraph.Digraph;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.xml.XmlPrintable;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import rreil.cfa.Cfa;
import rreil.cfa.util.GmlWriter.EdgeLabelRenderer;
import rreil.cfa.util.GmlWriter.VertexLabelRenderer;

public class GraphMLWriter implements XmlPrintable {
  private static final Properties outputProperties = new Properties();
  private static final String $LabelKeyId = "d0";

  static {
    outputProperties.put(javax.xml.transform.OutputKeys.INDENT, "yes");
    outputProperties.put("{http://xml.apache.org/xslt}indent-amount", "2");
  }

  public static String asString (final String root, final XmlPrintable printable) {
    try {
      final XMLBuilder xml = XMLBuilder.create(root);
      return printable.pretty(xml).asString(outputProperties);
    } catch (ParserConfigurationException ex) {
      throw new IllegalStateException("XMLBuilder failure");
    } catch (FactoryConfigurationError ex) {
      throw new IllegalStateException("XMLBuilder failure");
    } catch (TransformerException ex) {
      throw new IllegalStateException("XMLBuilder failure");
    }
  }
  private final Digraph graph;
  private final VertexLabelRenderer vertexRenderer;
  private final EdgeLabelRenderer edgeRenderer;

  public GraphMLWriter (Cfa graph) {
    this(graph, graph, graph);
  }

  public GraphMLWriter (Digraph graph, VertexLabelRenderer vertexRenderer, EdgeLabelRenderer edgeRenderer) {
    this.graph = graph;
    this.vertexRenderer = vertexRenderer;
    this.edgeRenderer = edgeRenderer;
  }

  private XMLBuilder renderVertex (XMLBuilder xml, Digraph.Vertex v) {
    XMLBuilder out = xml;
    out = out.e("node").a("id", String.valueOf(v.getId()));
    out = out.e("data").a("key", $LabelKeyId).t(vertexRenderer.labelOfVertex(v));
    out = out.up().up();
    return out;
  }

  public XMLBuilder renderEdge (XMLBuilder xml, Edge e) {
    XMLBuilder out = xml;
    Vertex u = e.getSource();
    Vertex v = e.getTarget();

    // Render edge as node introducing "nodeForEdge" as virtual node
    String nodeForEdge = "nodeForEdge" + String.valueOf(e.getId());
    String source = String.valueOf(u.getId());
    String target = String.valueOf(v.getId());
    out = out.e("node").a("id", nodeForEdge);
    out = out.e("data").a("key", $LabelKeyId).t(edgeRenderer.labelOfEdge(e));
    out = out.up().up();

    // Render edges to and from the virtual node
    out = out.e("edge").a("source", source).a("target", nodeForEdge).up();
    out = out.e("edge").a("source", nodeForEdge).a("target", target).up();

    return out;
  }

  public void renderTo (PrintWriter out) {
    String asXml = asString("graphml", this);
    out.println(asXml);
  }

  @Override public XMLBuilder pretty (XMLBuilder builder) {
    XMLBuilder out = builder;

    out = out.e("graph").a("id", "G").a("edgedefault", "directed");
    out = out.e("key").a("id", $LabelKeyId).a("for", "node").a("attr.name", "label").a("attr.type", "string").up();

    for (Vertex v : graph.vertices()) {
      out = renderVertex(out, v);
    }
    for (Edge e : graph.edges()) {
      out = renderEdge(out, e);
    }

    return out.up();
  }
}
