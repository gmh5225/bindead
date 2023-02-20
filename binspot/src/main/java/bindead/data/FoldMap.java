package bindead.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javalx.data.products.P2;
import rreil.lang.MemVar;
import bindead.data.NumVar.AddrVar;

public class FoldMap implements Iterable<VarPair> {
  private final List<MemVarPair> memlist;
  private final List<VarPair> list;        // created on demand
  private final Map<NumVar, NumVar> map;   // maps perms to ephemerals
  private final Set<NumVar> ephemerals;    // quick test for double entries

  public FoldMap () {
    this.memlist = new LinkedList<MemVarPair>();
    this.list = new LinkedList<VarPair>();
    this.map = new HashMap<NumVar, NumVar>();
    this.ephemerals = new HashSet<NumVar>();
  }

  public FoldMap (FoldMap f) {
    this.memlist = new LinkedList<MemVarPair>(f.memlist);
    this.list = new LinkedList<VarPair>(f.list);
    this.map = new HashMap<NumVar, NumVar>(f.map);
    this.ephemerals = new HashSet<NumVar>(f.ephemerals);
  }

  public static FoldMap empty () {
    return new FoldMap();
  }

  public static FoldMap fromList (List<VarPair> inner) {
    FoldMap fm = new FoldMap();
    for (VarPair vp : inner)
      fm.add(vp);
    return fm;
  }

  public static FoldMap fromMemList (List<MemVarPair> inner) {
    FoldMap fm = new FoldMap();
    for (MemVarPair vp : inner)
      fm.add(vp);
    return fm;
  }

  public static FoldMap singleton (NumVar p, NumVar e) {
    FoldMap m = new FoldMap();
    m.add(p, e);
    return m;
  }

  public List<MemVarPair> getMemVarPairs () {
    return memlist;
  }


  public void add (MemVar p, MemVar e) {
    memlist.add(new MemVarPair(p, e));
  }

  public void add (MemVarPair p) {
    assert !contained(p);
    memlist.add(p);
  }

  private boolean contained (MemVarPair p) {
    for (MemVarPair mvp : memlist) {
      if (mvp.getEphemeral().equals(p.getEphemeral()))
        return true;
      if (mvp.getEphemeral().equals(p.getPermanent()))
        return true;
      if (mvp.getPermanent().equals(p.getEphemeral()))
        return true;
      if (mvp.getPermanent().equals(p.getPermanent()))
        return true;
    }
    return false;
  }

  public void add (NumVar permanent, NumVar ephemeral) {
    add(new VarPair(permanent, ephemeral));
  }

  public void add (VarPair p) {
    NumVar permanent = p.getPermanent();
    NumVar ephemeral = p.getEphemeral();
    assert !map.containsKey(permanent);
    assert !map.containsKey(ephemeral);
    assert !ephemerals.contains(permanent);
    assert !ephemerals.contains(ephemeral);
    list.add(p);
    map.put(permanent, ephemeral);
    ephemerals.add(ephemeral);
  }

  public VarSet getAllVars () {
    return getPermanent().union(getEphemeral());
  }

  public VarSet getPermanent () {
    return VarSet.from(map.keySet());
  }

  public VarSet getEphemeral () {
    return VarSet.from(ephemerals);
  }

  public NumVar getEphemeral (NumVar perm) {
    assert perm != null;
    NumVar eph = map.get(perm);
    assert eph != null;
    return eph;
  }

  @Override public Iterator<VarPair> iterator () {
    return list.iterator();
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("<");
    if (!list.isEmpty()) {
      builder.append("v:");
      builder.append(list);
      if (!memlist.isEmpty())
        builder.append(", ");
    }
    if (!memlist.isEmpty()) {
      builder.append("m:");
      builder.append(memlist);
    }
    builder.append(">");
    return builder.toString();
  }

  public List<P2<AddrVar, AddrVar>> getAddressPairs () {
    LinkedList<P2<AddrVar, AddrVar>> ali = new LinkedList<P2<AddrVar, AddrVar>>();
    for (VarPair vp : list) {
      NumVar permanent = vp.getPermanent();
      NumVar ephemeral = vp.getEphemeral();
      boolean pia = permanent.isAddress();
      assert pia == ephemeral.isAddress();
      if (pia) {
        ali.add(P2.<AddrVar, AddrVar>tuple2((AddrVar) permanent, (AddrVar) ephemeral));
      }
    }
    return ali;
  }

  public void assertCorrectFoldMap () {
    VarSet ephs = VarSet.empty();
    VarSet perms = VarSet.empty();
    for (VarPair vp : this) {
      NumVar p = vp.getPermanent();
      assert !ephs.contains(p) : this + " already contains ephemeral " + p;
      assert !perms.contains(p) : this + " already contains permanent " + p;
      perms = perms.add(p);
      NumVar e = vp.getEphemeral();
      assert !ephs.contains(e) : ephs + " already contains " + e;
      assert !perms.contains(e) : perms + " already contains " + e;
      ephs = ephs.add(e);
    }
  }

  public NumVar freshSubstitute (NumVar ov) {
    NumVar fv = NumVar.fresh();
    add(new VarPair(ov, fv));
    return fv;
  }

  public boolean isEphemeral (NumVar var) {
    return ephemerals.contains(var);
  }

  public boolean isPermanent (NumVar var) {
    return map.keySet().contains(var);
  }
}
