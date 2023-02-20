package satDomain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author hsi
 */
public class AffineSatDomain extends NumDomain<AffineSatDomain> {
  private static boolean interSectionNotEmpty (Set<AffineEntry> value,
      Collection<Integer> fromSet) {
    final HashSet<AffineEntry> s = new HashSet<AffineEntry>(value);
    // FIXME Integer vs. AffineEntry
    s.removeAll(fromSet);
    assert false;
    return !s.isEmpty();
  }

  private static String printPartition (int inner, Set<AffineEntry> s) {
    String txt = null;
    for (final AffineEntry e : s) {
      if (txt == null)
        txt = inner + ": ";
      else
        txt += " = ";
      if (!e.polarity)
        txt += "!";
      txt += e.outerVar;
    }
    return txt + "\n";
  }

  public SatDomain sd;
  final int innerTrueVar;
  // inner variables to outer variables
  Map<Integer, Set<AffineEntry>> partitions;
  // outer variables to inner variables
  Map<Integer, AffineEntry> translation;

  AffineSatDomain () {
    super();
    translation = new HashMap<Integer, AffineEntry>();
    partitions = new HashMap<Integer, Set<AffineEntry>>();
    sd = new SatDomain();
    innerTrueVar = sd.freshTrueVar();
  }

  AffineSatDomain (AffineSatDomain other) {
    super(other);
    final Collection<AffineEntry> v = new ArrayList<AffineEntry>();
    for (final AffineEntry e : other.translation.values())
      v.add(new AffineEntry(e));
    setPartitions(v);
    sd = other.sd.copy();
    innerTrueVar = other.innerTrueVar;
  }

  @Override public void assumeClause (int[] clause) {
    final int[] is = new int[clause.length];
    int length = 0;
    for (int i = 0; i < is.length; i++) {
      is[i] = innerVar(clause[i]);
      assert is[i] != 0;
      if (is[i] == innerTrueVar)
        return;
      else if (is[i] != -innerTrueVar)
        length++;
    }
    int j = 0;
    final int[] cl = new int[length];
    for (final int curr : is)
      if (curr != -innerTrueVar)
        cl[j++] = curr;
    if (length == 1)
      mergePartitions(innerTrueVar, cl[0]);
    else
      sd.assumeClause(cl);
  }

  @Override public void assumeEquality (int v1, int v2) {
    assertProperContents();
    mergePartitions(innerVar(v1), innerVar(v2));
  }

  @Override public void assumeTrue (int v) {
    assert v != 0;
    // Benchmark.log("AffineDomain.assumeTrue" + v);
    mergePartitions(innerTrueVar, innerVar(v));
  }

  @Override public void assumeTwoBitSum (int x1f, int x0f, int y0f, int s1f, int s0f) {
    assumeClause(new int[] {-x1f, s1f});
    assumeClause(new int[] {-x0f, -y0f, s1f});
    assumeClause(new int[] {x1f, x0f, -s1f});
    assumeClause(new int[] {x1f, y0f, -s1f});
    assumeClause(new int[] {x1f, x0f, y0f, -s0f});
    assumeClause(new int[] {s0f, -x0f});
    assumeClause(new int[] {s0f, -y0f});
  }

  @Override public void assumeUnsatisfiable () {
    sd.assumeUnsatisfiable();
  }

  @Override public AffineSatDomain copy () {
    return new AffineSatDomain(this);
  }

  @Override public int duplicate (int f) {
    final int ov = freshVarIndex();
    final int iv = innerVar(f);
    addToPartition(ov, iv);
    return ov;
  }

  @Override public Substitution expand (String reason, Collection<Integer> fromSet) {
//    System.out.println("affine: expand " + fromSet + " in " + this);
    final Collection<Integer> innerExpandees = new HashSet<Integer>();
    final Collection<AffineEntry> outerExpandees = new HashSet<AffineEntry>();
    final Collection<AffineEntry> outerEqual = new HashSet<AffineEntry>();
    final Collection<Integer> equalVars = new HashSet<Integer>();
    for (final Entry<Integer, Set<AffineEntry>> e : partitions.entrySet()) {
      final Set<AffineEntry> v = e.getValue();
      final boolean wholePartition = containsAllEntries(fromSet, v);
      if (wholePartition) {
        innerExpandees.add(e.getKey());
        outerExpandees.addAll(e.getValue());
      } else if (interSectionNotEmpty(e.getValue(), fromSet)) {
        equalVars.add(e.getKey());
        for (AffineEntry h : e.getValue()) {
          if (fromSet.contains(h.outerVar))
            outerEqual.add(h);
        }
      }
    }
    final Substitution innerSubst = sd.expand(reason, innerExpandees);
    final Substitution outerSubst = new HashedSubstitution();
//    System.out.println("affine: outerExpandees " + outerExpandees);
//    System.out.println("affine: outerEqual " + outerEqual);

    for (final AffineEntry e : outerExpandees) {
      final int no = freshVarIndex();
      outerSubst.put(e.outerVar, no);
      addToPartition(no, innerSubst.get(e.innerVar), e.polarity);
    }
    for (final AffineEntry e : outerEqual) {
      final int no = freshVarIndex();
      outerSubst.put(e.outerVar, no);
      addToPartition(no, e.innerVar, e.polarity);
    }
    assertProperContents();
//    System.out.println("affine: expanded returning " + outerSubst + " in "
//      + this);

    return outerSubst;
  }

  @Override public int freshFalseVar () {
    final int v = freshVarIndex();
    addToPartition(v, innerTrueVar, false);
    return v;
  }

  @Override public int freshTopVar () {
    final int inner = sd.freshTopVar();
    final int outer = freshVarIndex();
    addToPartition(outer, inner, true);
    return outer;
  }

  @Override public int freshTrueVar () {
    final int v = freshVarIndex();
    addToPartition(v, innerTrueVar, true);
    return v;
  }

  public Set<AffineEntry> getPartition (int inner) {
    Set<AffineEntry> s = partitions.get(inner);
    if (s == null) {
      s = new HashSet<AffineEntry>();
      partitions.put(inner, s);
    }
    return s;
  }

  @Deprecated public AffineEntry getTranslation (int outer) {
    return translation.get(Math.abs(outer));
  }

  // is this domain a subset of the other?
  public boolean isSubset (AffineSatDomain other) {
    // Benchmark.log("isSubset this " + this);
    // Benchmark.log("isSubset other " + other);
    assert getLastVar() == other.getLastVar();
    makePartitionsCompatible(other);
    // Benchmark.log("ready " + ready);
    final boolean result = sd.isSubset(other.sd);
    // Benchmark.log("compatible Regions this:\n" + showAffineEqualities());
    // Benchmark.log("compatible Regions that:\n"
    // + other.showAffineEqualities());
    return result;
  }

  @Override public boolean isUnsatisfiable () {
    return sd.isUnsatisfiable();
  }

  @Override public void joinWith (AffineSatDomain other) {
    assert getLastVar() == other.getLastVar();
    makePartitionsCompatible(other);
    sd.joinWith(other.sd);
    findConstantValues();
  }

  public void makePartitionsCompatible (AffineSatDomain other) {
    while (sd.getLastVar() < other.sd.getLastVar())
      sd.freshTopVar();
    while (other.sd.getLastVar() < sd.getLastVar())
      other.sd.freshTopVar();
    final int innerLastVar = sd.getLastVar();
    final Set<AffineEntry> thisNewEntries = new HashSet<AffineEntry>();
    final Set<AffineEntry> otherNewEntries = new HashSet<AffineEntry>();
    final int[] innervars = new int[innerLastVar * (innerLastVar + 1) * 2
      + 2];
    for (int ov = 1; ov <= getLastVar(); ov++) {
      final AffineEntry thisEntry = getTranslation(ov);
      final AffineEntry otherEntry = other.getTranslation(ov);
      final boolean samePolarity = thisEntry.polarity == otherEntry.polarity;
      final AffineEntry thisNewEntry;
      final AffineEntry otherNewEntry;
      if (thisEntry.innerVar == otherEntry.innerVar && samePolarity) {
        thisNewEntry = thisEntry;
        otherNewEntry = otherEntry;
      } else {
        final int ti = thisEntry.innerVar;
        final int oi = otherEntry.innerVar;
        final int iv = (ti * innerLastVar + oi) * 2
          + (samePolarity ? 1 : 0);
        if (innervars[iv] == 0) {
          final int inner = duplicateInnerVar(ti);
          final int inner2 = other
              .duplicateInnerVar(samePolarity ? oi : -oi);
          assert inner == inner2;
          innervars[iv] = inner;
        }
        thisNewEntry = new AffineEntry(ov, innervars[iv],
          thisEntry.polarity);
        otherNewEntry = new AffineEntry(ov, innervars[iv],
          thisEntry.polarity);
      }
      thisNewEntries.add(thisNewEntry);
      otherNewEntries.add(otherNewEntry);
    }
    setPartitions(thisNewEntries);
    other.setPartitions(otherNewEntries);
  }

  @Override public void project (Renamer renamer, boolean withCompression) {
    final Set<AffineEntry> entries = new HashSet<AffineEntry>(
      translation.values());
    translation.clear();
    partitions.clear();
    int innerCount = innerTrueVar;
    int outerCount = 0;
    // later: use renamer
    final List<Integer> innerRenamings = new ArrayList<Integer>(
      sd.getLastVar() + 1);
    for (int i = 0; i <= sd.getLastVar(); i++)
      innerRenamings.add(i, 0);
    innerRenamings.set(innerTrueVar, innerTrueVar);
    for (final AffineEntry e : entries) {
      final int newOuter = renamer.get(e.outerVar);
      outerCount = Math.max(newOuter, outerCount);
      if (newOuter != 0) {
        int newInner = innerRenamings.get(e.innerVar);
        if (newInner == 0) {
          newInner = ++innerCount;
          innerRenamings.set(e.innerVar, newInner);
        }
        addToPartition(newOuter, newInner, e.polarity);
      }
    }
    setLastVar(outerCount);
    sd.project(innerRenamings, withCompression);
    findConstantValues();
  }

  public void setPartitions (Collection<AffineEntry> v) {
    translation = new HashMap<Integer, AffineEntry>();
    partitions = new HashMap<Integer, Set<AffineEntry>>();
    for (final AffineEntry e : v)
      addToPartition(e.outerVar, e.innerVar, e.polarity);
  }

  public String showAffineEqualities () {
    String txt = "affine equalities(" + getLastVar() + ")\n";
    for (final Entry<Integer, Set<AffineEntry>> e : partitions.entrySet())
      txt += printPartition(e.getKey(), e.getValue());
    return txt;
  }

  @Override public int summarize (int discr, int a, int b) {
    final int ai = innerVar(a);
    final int bi = innerVar(b);
    if (ai == 1 && bi == 1)
      return freshTrueVar();
    else if (ai == -1 && bi == -1)
      return freshFalseVar();
    else if (ai == 1 && bi == -1)
      return duplicate(-discr);
    else if (ai == -1 && bi == 1)
      return duplicate(discr);
    else {
      final int nf = freshTopVar();
      assumeClause(new int[] {discr, -nf, b});
      assumeClause(new int[] {discr, nf, -b});
      assumeClause(new int[] {-discr, -nf, a});
      assumeClause(new int[] {-discr, nf, -a});
      return nf;
    }
  }

  @Override public String toDetailedString () {
    return sd.toDetailedString(); // + showAffineEqualities();
  }

  @Override public String toString () {
    return sd.toString() + showAffineEqualities();
  }

  // only used in satDomain.formula
  void addEquality (Flag v1, Flag v2) {
    assumeEquality(v1.flag, v2.flag);
  }

  void assertProperContents () {
    for (final Entry<Integer, AffineEntry> e : translation.entrySet())
      assert e.getKey() == e.getValue().outerVar;
    for (final Entry<Integer, Set<AffineEntry>> e : partitions.entrySet()) {
      final Set<AffineEntry> s = e.getValue();
      for (final AffineEntry ae : s) {
        assert e.getKey() == ae.innerVar;
        if (translation.get(ae.outerVar).innerVar != e.getKey())
          assert false;
      }
    }
  }

  boolean containsAllEntries (Collection<Integer> set, Set<AffineEntry> entries) {
    for (final AffineEntry e : entries)
      if (!set.contains(e.outerVar))
        return false;
    return true;
  }

  @Override String showFlag (int v) {
    final int iv = innerVar(v);
    // Benchmark.log("showFlag " + v + " -> " + innerVar(v));
    if (iv == 1)
      return "1";
    else if (iv == -1)
      return "0";
    return sd.showFlag(iv);
  }

  private void addToPartition (int outer, int inner) {
    addToPartition(outer, inner > 0 ? inner : -inner, inner > 0);
  }

  private void addToPartition (int outer, int inner, boolean polarity) {
    assert inner > 0;
    final AffineEntry e = new AffineEntry(outer, inner, polarity);
    assert e != null;
    final Set<AffineEntry> p = getPartition(e.innerVar);
    p.add(e);
    translation.put(e.outerVar, e);
  }

  private int duplicateInnerVar (int v) {
    if (v == innerTrueVar)
      return sd.freshTrueVar();
    else if (v == -innerTrueVar)
      return sd.freshFalseVar();
    else
      return sd.duplicate(v);
  }

  private void findConstantValues () {
    final List<Integer> consts = sd.findConstants();
    if (consts.size() < 2)
      return;
    // Benchmark.log("**********" + consts + "\n" + this);
    for (final int c : consts)
      mergePartitions(innerTrueVar, c);
    // Benchmark.log("**********" + consts + "\n" + this);
  }

  private int innerVar (int v) {
    assert v != 0;
    final AffineEntry h = getTranslation(v);
    return v < 0 == h.polarity ? -h.innerVar : h.innerVar;
  }

  private void mergePartitions (int i, int j) {
    sd.mergeVars(i, j);
    // sd.assumeEquality(i, j);
    if (i == j || i == -j)
      return;
    final int i1 = Math.abs(i);
    final int i2 = Math.abs(j);
    assertProperContents();
    final Set<AffineEntry> s2 = getPartition(i2);
    final Set<AffineEntry> s1 = getPartition(i1);
    for (final AffineEntry e : s2) {
      e.innerVar = i1;
      e.polarity = e.polarity == (j > 0 == i > 0);
      s1.add(e);
    }
    partitions.remove(i2);
    assertProperContents();
  }

  public void rename (Map<Integer, Integer> renamer) {
    if (renamer.size() == 0)
      return;
    assert false;
    // TODO hsi implement rename
  }

  public void assumeClauses (List<int[]> clauses) {
    for (int[] c : clauses) {
      assumeClause(c);
    }
  }
}