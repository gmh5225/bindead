package bindead.domains.pointsto;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javalx.data.Option;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domains.pointsto.PointsToSet.PointsToEntry;
import bindead.exceptions.DomainStateException;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The {@code PointsTo} domain state. A {@code PointsTo} domain consists of a
 * set of address variables and a mapping from non-address-variables to a set of
 * pairs (Integer flag, address-variable). The domain also contains the reverse
 * mapping between an address and a set of program variables that use an
 * address.
 */
public class PointsToState extends FunctorState {
  public static final PointsToState EMPTY = new PointsToState();

  final private PointsToMap pointsToMap;

  private PointsToState () {
    this(new PointsToMap());
  }

  PointsToState (PointsToMap pts) {
    this.pointsToMap = pts;
  }

  PointsToMap getPointsToMap () {
    return pointsToMap;
  }

  @Override final public String toString () {
    StringBuilder builder = new StringBuilder();
    Set<PointsToSet> sorted = StringHelpers.sortLexically(pointsToMap);
    Iterator<PointsToSet> iterator = sorted.iterator();
    builder.append("#");
    builder.append(pointsToMap.pointersCount());
    builder.append(" {");
    while (iterator.hasNext()) {
      PointsToSet pts = iterator.next();
      if (!pts.isBoring()) { // do not print everything in the map to have less clutter in the output
        builder.append(pts.var);
        builder.append("=");
        builder.append(pts);
        if (iterator.hasNext())
          builder.append(", ");
      }
    }
    return builder.append('}').toString();
  }

  @Override public XMLBuilder toXML (final XMLBuilder builder) {
    XMLBuilder xml = builder;
    xml = xml.e("POINTSTO");
    for (PointsToSet pts : pointsToMap) {
      if (pts.isBoring())
        continue; // do not print everything in the map to have less clutter in the output
      xml = xml.e("Entry")
          .e("Variable")
          .t(pts.var.toString())
          .up();
      xml = xml.e("pointer");
      for (PointsToEntry entry : pts) {
        xml = xml.e("flag")
            .t(entry.flag.toString())
            .up()
            .e("address")
            .t(entry.address.toString())
            .up();
      }
      if (!pts.sumOfFlags.isConstantZero())
        xml = xml.e("flagsSum").t(pts.sumOfFlags.numVar.toString()).up();
      xml = xml.up().up();
    }
    xml = xml.up();
    return xml;
  }

  VarSet varsNotExportingEqualities () {
    VarSet vars = VarSet.empty();
    for (PointsToSet pts : pointsToMap)
      vars = vars.union(pts.varsNotExportingEqualities());
    return vars;
  }

  VarSet varsInChildDomain () {
    VarSet vars = VarSet.empty();
    for (PointsToSet pts : pointsToMap) {
      vars = vars.union(pts.varsInChildDomain());
    }
    return vars;
  }

  final boolean hasPts (NumVar var) {
    return pointsToMap.get(var).isSome();
  }

  final PointsToSet getPts (NumVar var) {
    Option<PointsToSet> pts = pointsToMap.get(var);
    if (pts.isNone())
      throw new DomainStateException.VariableSupportSetException();
    return pts.get();
  }

  final boolean isLocal (NumVar var) {
    return !pointsToMap.contains(var);
  }

  final boolean isScalar (NumVar var) {
    return getPts(var).isScalar();
  }

  final boolean isScalar (Linear lin) {
    VarSet linvars = lin.getVars();
    for (NumVar var : linvars) {
      if (!isScalar(var))
        return false;
    }
    return true;
  }

  VarSet getNonScalars () {
    return pointsToMap.getNonScalars();
  }

  Collection<AddrVar> findAllPossibleEdges (NumVar id) {
    return getPts(id).allAddresses();
  }


  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    // nop
  }

  public PointsToState removePtsEntry (NumVar src, AddrVar tgt) {
    return new PointsToState(pointsToMap.removeEntry(src, tgt));
  }

  HyperPointsToSet translate (int lhsSize, Rhs rhs, WarningsContainer wc) {
    return new RhsTranslator(lhsSize, this, wc).run(rhs);
  }

  public VarSet getSupport () {
    return pointsToMap.getSupport();
  }
}
