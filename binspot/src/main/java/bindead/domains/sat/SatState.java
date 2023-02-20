package bindead.domains.sat;

import java.util.HashMap;

import satDomain.Numeric;
import bindead.domainnetwork.interfaces.FunctorState;

import com.jamesmurty.utils.XMLBuilder;

public final class SatState extends FunctorState {
  final Numeric cnf;

  // translates global variable indices to CNF variable indices.
  final protected HashMap<Integer, Integer> translation;

  public static SatState empty () {
    return new SatState();
  }

  public SatState (Numeric from, HashMap<Integer, Integer> t) {
    cnf = from;
    translation = t;
  }

  public SatState () {
    cnf = new Numeric();
    translation = new HashMap<Integer, Integer>();
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    final XMLBuilder xml = builder;
    /*
     * for (P2<Integer, Test> binding : available) { Integer variable =
     * binding._1(); Test expression = binding._2(); assert binding != null;
     * xml = XmlPrintables.binding(xml, variable, expression); }
     */
    xml.comment(cnf.toString());
    return xml;
  }

  @Override public String toString () {
    return "SatCtx{available=" + translation + "\ncnf=\n" + cnf + "\n}";
  }

}
