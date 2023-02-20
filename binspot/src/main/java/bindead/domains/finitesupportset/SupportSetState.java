package bindead.domains.finitesupportset;

import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.FunctorState;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The domain state to track currently known variables.
 */
public class SupportSetState extends FunctorState {
  private static final SupportSetState EMPTY = new SupportSetState(VarSet.empty());
  private final VarSet vars;

  private SupportSetState (VarSet vs) {
    this.vars = vs;
  }

  static SupportSetState empty () {
    return EMPTY;
  }

  public VarSet getSupportSet () {
    return vars;
  }

  void have (Linear lin) {
    have(lin.getVars());
  }

  void have (NumVar v) {
    assert vars.contains(v) : "variable " + v + " is not in support set";
  }

  void have (Rlin ptr) {
    have(ptr.getVars());
  }

  void have (VarSet vs) {
    VarSet difference = vs.difference(vars);
    assert difference.isEmpty() : "vars missing: "+difference;
  }

  void haveNot (NumVar v) {
    assert !vars.contains(v) : "variable " + v + " should not be in support set";
  }

  void haveNot (VarSet vs) {
    VarSet toomuch = vars.intersection(vs);
    assert toomuch.isEmpty() : "variables " + toomuch + " should not be in support set";
  }

  void sameAs (SupportSetState other) {
    VarSet thisWithoutOther = vars.difference(other.vars);
    VarSet otherWithoutThis = other.vars.difference(vars);
    assert otherWithoutThis.isEmpty() : "first set  " + vars + " does not contain " + otherWithoutThis;
    assert thisWithoutOther.isEmpty() : "second set " + other.vars + " does not contain " + thisWithoutOther;
  }

  SupportSetState with (NumVar v) {
    haveNot(v);
    return new SupportSetState(vars.add(v));
  }

  SupportSetState with (VarSet vs) {
    haveNot(vs);
    return new SupportSetState(vars.union(vs));
  }

  SupportSetState without (NumVar v) {
    have(v);
    return new SupportSetState(vars.remove(v));
  }

  @Override public XMLBuilder toXML (final XMLBuilder builder) {
    XMLBuilder xml = builder;
    xml = xml.e("SupportSet")
        .e("Entry");
    xml = xml.a("type", "SupportSet");
    for (NumVar variable : getSupportSet()) {
      xml = xml.e("Variable")
          .t(variable.toString());
      xml = xml.up();
    }

    xml = xml.up();
    xml = xml.up();

    return xml;
  }

  @Override final public String toString () {
    return "#" + vars.size() + " " + vars.toString();
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    // no need to print the support set in compact string
  }
}
