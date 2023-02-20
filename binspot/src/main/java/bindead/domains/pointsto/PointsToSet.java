package bindead.domains.pointsto;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javalx.data.products.P2;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import javalx.xml.XmlPrintable;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.debug.XmlPrintHelpers;
import bindead.domainnetwork.combinators.FiniteSequence;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.pointsto.PointsToSet.PointsToEntry;

import com.jamesmurty.utils.XMLBuilder;

/**
 * A set of flag,address tuples; used in mappings from variables to points-to sets.
 *
 * @author Holger Siegel
 */
public class PointsToSet implements Iterable<PointsToEntry>, XmlPrintable {
  final private static FiniteFactory fin = FiniteFactory.getInstance();

  private final static boolean DEBUG = PointsToProperties.INSTANCE.debugOther.isTrue();

  static class PointsToEntry {
    final AddrVar address;
    final NumVar flag;

    PointsToEntry (AddrVar a, NumVar f) {
      address = a;
      flag = f;
    }

    @Override public String toString () {
      return flag + "*" + address;
    }

    public void appendInfo (StringBuilder builder, PrettyDomain childDomain) {
      childDomain.varToCompactString(builder, flag);
      builder.append("*");
      childDomain.varToCompactString(builder, address);
    }
  }

  final static <D extends FiniteDomain<D>> PointsToSet empty (NumVar v) {
    return new PointsToSet(v, NumVarOrZero.zero());
  }

  final static <D extends FiniteDomain<D>> PointsToSet empty (NumVar v, NumVar s) {
    return new PointsToSet(v, new NumVarOrZero(s));
  }

  final NumVar var;
  // a partial mapping from an abstract address to a flag
  final AVLMap<AddrVar, PointsToEntry> entryMap;

  // the sum of outgoing flags; in the presence of summary nodes, this sum may be greater than the sum of pts flags.
  final NumVarOrZero sumOfFlags;

  // TODO hsi also use in-edges; conceptually not required, but it adds the spice
  final NumVar outFlagOfGhostNode;

  private PointsToSet (NumVar v, NumVarOrZero sum) {
    this(v, sum, null, AVLMap.<AddrVar, PointsToEntry>empty());
  }

  PointsToSet (NumVar var, NumVarOrZero sum, NumVar ghostNode, AVLMap<AddrVar, PointsToEntry> pts) {
    this.var = var;
    sumOfFlags = sum;
    entryMap = pts;
    outFlagOfGhostNode = ghostNode;
  }

  Set<AddrVar> allAddresses () {
    Set<AddrVar> s = new HashSet<AddrVar>();
    for (AddrVar k : entryMap.keys()) {
      s.add(k);
    }
    return s;
  }

  PointsToSet bind (AddrVar adr, NumVar flag) {
    assert adr != null;
    assert flag != null;
    return bind(new PointsToEntry(adr, flag));
  }

  PointsToSet bind (PointsToEntry entry) {
    assert entry.address != null;
    return new PointsToSet(var, sumOfFlags, outFlagOfGhostNode, entryMap.bind(entry.address, entry));
  }

  PointsToEntry getEntry (AddrVar a) {
    if (a == null)
      return null;
    return entryMap.get(a).getOrNull();
  }

  boolean isScalar () {
    return entryMap.isEmpty();
  }

  public boolean isEmpty () {
    return isScalar();
  }

  @Override public Iterator<PointsToEntry> iterator () {
    return entryMap.values().iterator();
  }

  VarSet localVars () {
    VarSet res = ptsFlags();
    if (!sumOfFlags.isConstantZero()) {
      res = res.add(sumOfFlags.numVar);
    }
    return res;
  }

  private VarSet ptsFlags () {
    VarSet vs = VarSet.empty();
    for (PointsToEntry e : entryMap.values()) {
      vs = vs.add(e.flag);
    }
    if (outFlagOfGhostNode != null) {
      vs = vs.add(outFlagOfGhostNode);
    }
    return vs;
  }


  @SuppressWarnings("unused") private static void msg (String s) {
    if (DEBUG) {
      System.out.println("PointsToSet: " + s + "\n");
    }
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    XMLBuilder xml = builder;
    xml.e("PointsToSet");
    for (PointsToEntry lvalue : entryMap.values()) {
      xml = XmlPrintHelpers.p2AsElement(builder, "LValue", new P2<AddrVar, NumVar>(lvalue.address, lvalue.flag));
    }
    xml = xml.up();
    return xml;
  }

  PointsToSet remove (PointsToEntry entry) {
    AddrVar address = entry.address;
    return remove(address);
  }

  PointsToSet remove (AddrVar address) {
    return new PointsToSet(var, sumOfFlags, outFlagOfGhostNode, entryMap.remove(address));
  }

  ThreeWaySplit<AVLMap<AddrVar, PointsToEntry>> splitPts (PointsToSet sndPts) {
    return entryMap.split(sndPts.entryMap);
  }

  @Override public String toString () {
    String e;
    Iterator<PointsToEntry> i = entryMap.values().iterator();
    String suf = sumOfFlags.isConstantZero() ? "" : "(#" + sumOfFlags.numVar + ")";
    suf += outFlagOfGhostNode == null ? "" : " " + outFlagOfGhostNode + "*other +";
    if (!i.hasNext())
      return "{" + suf + "}";
    else {
      for (e = "{" + i.next().toString(); i.hasNext();) {
        e = e + " + " + i.next().toString();
      }
      return e + " " + suf + "}";
    }
  }

  static public PointsToSet bendToOutFlag (PointsToSet pts, FoldMap innerVars, AddrVar other) {
    PointsToEntry o = pts.getEntry(other);
    if (o == null)
      return pts;
    if (pts.outFlagOfGhostNode != null) {
      innerVars.add(o.flag, pts.outFlagOfGhostNode);
    }
    AVLMap<AddrVar, PointsToEntry> em = pts.entryMap.remove(other);
    return new PointsToSet(pts.var, pts.sumOfFlags, o.flag, em);
  }

  PointsToSet renameAddress (AddrVar from, AddrVar to) {
    PointsToEntry o = getEntry(from);
    assert getEntry(to) == null;
    if (o == null)
      return this;
    return remove(from).bind(to, o.flag);
  }

  Test sumRestriction () {
    return fin.unsignedLessThanOrEqualTo(0, Linear.sumOf(ptsFlags()), sumOfFlags.getLinear());
  }

  boolean hasGhostNode () {
    return outFlagOfGhostNode != null;
  }

  P2<PointsToSet, FoldMap> cloneWithNewVars (NumVar newV) {
    FoldMap substs = new FoldMap();
    NumVarOrZero newSum = cloneSum(substs);
    NumVar ghost = cloneGhost(substs);
    PointsToSet newPts = new PointsToSet(newV, newSum).withGhostNode(ghost);
    for (PointsToEntry e : this) {
      NumVar nv = substs.freshSubstitute(e.flag);
      newPts = newPts.bind(e.address, nv);
    }
    return P2.<PointsToSet, FoldMap>tuple2(newPts, substs);
  }

  private NumVar cloneGhost (FoldMap substs) {
    NumVar ghost = null;
    if (outFlagOfGhostNode != null) {
      ghost = substs.freshSubstitute(outFlagOfGhostNode);
    }
    return ghost;
  }

  private NumVarOrZero cloneSum (FoldMap substs) {
    NumVarOrZero newSum;
    if (!sumOfFlags.isConstantZero()) {
      NumVar fv = substs.freshSubstitute(sumOfFlags.numVar);
      newSum = new NumVarOrZero(fv);
    } else {
      newSum = NumVarOrZero.zero();
    }
    return newSum;
  }

  boolean isBoring () {
    return isScalar() && sumOfFlags.isConstantZero() && outFlagOfGhostNode == null;
  }

  void killLocalVars (FiniteSequence co) {
    for (NumVar oldFlag : localVars()) {
      co.addKill(oldFlag);
    }
  }

  boolean hasEntry (AddrVar a) {
    return entryMap.contains(a);
  }

  PointsToSet substituteAddress (AddrVar toAddr, AddrVar fromAddr, FiniteSequence childOps) {
    assert !hasEntry(toAddr);
    PointsToEntry entry = getEntry(fromAddr);
    PointsToSet newPts;
    if (entry != null) {
      NumVar newFlag = NumVar.fresh();
      childOps.addSubst(entry.flag, newFlag);
      newPts = remove(fromAddr).bind(toAddr, newFlag);
    } else
      newPts = null;
    return newPts;
  }

  PointsToSet withGhostNode (NumVar outFlagOfGhostNode2) {
    return new PointsToSet(var, sumOfFlags, outFlagOfGhostNode2, entryMap);
  }

  PointsToSet withSumVar (NumVar s) {
    assert sumOfFlags.isConstantZero();
    return new PointsToSet(var, new NumVarOrZero(s), outFlagOfGhostNode, entryMap);
  }

  void appendInfo (StringBuilder builder, PrettyDomain childDomain) {
    if (entryMap.isEmpty() && sumOfFlags.isConstantZero() && outFlagOfGhostNode == null) {
      childDomain.varToCompactString(builder, var);
      return;
    }
    builder.append("{");
    if (!sumOfFlags.isConstantZero()) {
      builder.append("(#");
      childDomain.varToCompactString(builder, sumOfFlags.numVar);
      builder.append(") ");
    }
    if (outFlagOfGhostNode != null) {
      childDomain.varToCompactString(builder, outFlagOfGhostNode);
      builder.append("*other +");
    }
    for (PointsToEntry e : entryMap.values()) {
      e.appendInfo(builder, childDomain);
      builder.append(" + ");
    }
    childDomain.varToCompactString(builder, var);
    builder.append("}");
  }

  VarSet varsInChildDomain () {
    return localVars().add(var);
  }

  boolean knownToBeScalar () {
    return isScalar() || sumOfFlags.isConstantZero();
  }

  VarSet varsNotExportingEqualities () {
    VarSet l = localVars();
    if (!knownToBeScalar())
      l = l.add(var);
    return l;
  }
}
