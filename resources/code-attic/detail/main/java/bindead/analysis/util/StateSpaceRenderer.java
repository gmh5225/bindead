package bindead.analysis.util;

import bindead.domainnetwork.interfaces.RootDomain;
import com.jamesmurty.utils.XMLBuilder;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;
import javalx.digraph.Digraph.Vertex;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import rreil.cfa.Cfa;

/**
 */
public class StateSpaceRenderer {
  private static final Properties outputProperties = new Properties();

  static {
    outputProperties.put(javax.xml.transform.OutputKeys.INDENT, "yes");
    outputProperties.put("{http://xml.apache.org/xslt}indent-amount", "2");
  }

  /**
   *
   * @param out
   * @param states
   */
  public static void renderTo (PrintStream out, Cfa cfa, Map<Vertex, ? extends RootDomain<?>> states) {
    try {
      for (Vertex v : states.keySet()) {
        XMLBuilder xml = XMLBuilder.create("Vertex").a("label", cfa.labelOfVertex(v));
        xml = states.get(v).pretty(xml);
        out.println(xml.up().asString(outputProperties));
      }
    } catch (ParserConfigurationException e) {
    } catch (TransformerException e) {
    }
  }
}
