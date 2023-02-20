package satDomain;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bindead.debug.Benchmark;

/**
 * @author hsi
 */
public class Numeric {
  public final AffineSatDomain internal;
  final private Set<Integer> attic;

  public Numeric () {
    internal = new AffineSatDomain();
    attic = new HashSet<Integer>();
  }

  public Numeric (Numeric other) {
    internal = other.internal.copy();
    attic = new HashSet<Integer>(other.attic);
  }

  public Numeric assertNonzero (Flag f) {
    Numeric temp = new Numeric(this);
    // Benchmark.log("assuming that " + f + " is false in " + temp);
    temp.assumeFalse(f);
    // Benchmark.log("assumed that " + f + " is false in " + temp);
    if (temp.isBottom())
      temp = null;
    // Benchmark.log("resulting temp " + temp);
    return temp;
  }

  public void assumeAllFalse (final Collection<Flag> collection) {
    for (final Flag f : collection)
      assumeFalse(f);
  }

  public void assumeEqual (Flag flag, Flag to) {
    assumeEqual(flag.flag, to.flag);
  }

  public void assumeEqual (int flag, int to) {
    internal.assumeEquality(flag, to);
  }

  public void assumeExactCount (Flag nonZero, Collection<Flag> collection) {
    if (Config.useRefcount) {
      final int[] cls = new int[collection.size() + 1];
      cls[0] = -nonZero.flag;
      int i = 1;
      for (final Flag f : collection)
        cls[i++] = f.flag;
      internal.assumeClause(cls);
    }
  }

  public void assumeFalse (Flag f) {
    // Benchmark.log("Numeric.assumeFalse " + f);
    internal.assumeTrue(-f.flag);
  }

  public void assumeNotAllFalse (Collection<Flag> pts) {
    final int[] cls = new int[pts.size()];
    int i = 0;
    for (final Flag f : pts)
      cls[i++] = f.flag;
    internal.assumeClause(cls);
  }

  public void assumeOnlyOne (final Flag e, final Flag ce) {
    internal.assumeClause(new int[] {-e.flag, -ce.flag});
  }

  public void assumeSaturatedSum (Flag x1, Flag x0, Flag y0, Flag s1, Flag s0) {
    internal.assumeTwoBitSum(x1.flag, x0.flag, y0.flag, s1.flag, s0.flag);
  }

  public void assumeTrue (Flag f) {
    internal.assumeTrue(f.flag);
  }

  public Flag duplicate (Flag flag) {
    return new Flag(internal.duplicate(flag.flag));
  }

  /**
   * @param reason
   * @param fromSet
   *          the set of variables to be expanded
   * @return a mapping from the old variables to their copies
   */
  public Substitution expand (String reason, Collection<Flag> fromSet) {
    final Collection<Integer> f2 = new HashSet<Integer>();
    for (final Flag f : fromSet)
      f2.add(f.flag);
    f2.addAll(attic);
    final Substitution r = internal.expand(reason, f2);
    final HashSet<Integer> attic2 = new HashSet<Integer>(attic);
    for (final Integer i : attic2)
      attic.add(r.get(i));
    Benchmark.count("expand with attic size " + attic.size(), 1);
    return r;
  }

  public Flag freshFalseVar () {
    return new Flag(internal.freshFalseVar());
  }

  public Flag freshTopVar () {
    return new Flag(internal.freshTopVar());
  }

  public Flag freshTrueVar () {
    return new Flag(internal.freshTrueVar());
  }

  public int getLastVar () {
    return internal.getLastVar();
  }

  public boolean isBottom () {
    return internal.isUnsatisfiable();
  }

  // is this domain a subset of the other?
  public boolean isSubset (Numeric other) {
    assert getLastVar() == other.getLastVar();
    return internal.isSubset(other.internal);
  }

  public void joinWith (Numeric other) {
    internal.joinWith(other.internal);
  }

  public void moveToAttic (Flag f) {
    if (f != null)
      attic.add(f.flag);
  }

  /**
   * Calculate a #CNFSystem that only ranges over a subset of this system.
   * This function calculates existential quantification.
   *
   * @param onto
   *          a vector of variables positions that, for each variable of
   *          this system, contains 0 if the variable is to be projected out
   *          or {@code i} if the variable should appear at position {@code i} in the output. The output positions must
   *          start and
   *          one, may not be duplicated and should be consecutive.
   */
  public void project (Renamer renamer, boolean withCompression) {
    internal.project(renamer, withCompression);
    attic.clear();
  }

  public void setToBottom () {
    internal.assumeUnsatisfiable();
  }

  public String showFlag (Flag f) {
    return internal.showFlag(f.flag);
  }

  public Substitution summarize (HashedSubstitution subst) {
    // Benchmark.log("numeric summarize " + subst);
    // Benchmark.log("numeric summarize in domain " + this);
    final int discr = freshTopVar().flag;
    final Substitution newS = new HashedSubstitution();
    for (final Entry<Integer, Integer> e : subst.entries()) {
      final int a = e.getKey();
      final int b = e.getValue();
      newS.put(b, internal.summarize(discr, a, b));
    }
    // Benchmark.log("resulting substitution " + newS);
    // Benchmark.log("resulting domain " + this);
    return newS;
  }

  public String toDetailedString () {
    return internal.toDetailedString();
  }

  @Override public String toString () {
    final String s = "Attic: " + attic.toString() + "\n";
    return s + internal.toString();
  }

  public void rename (Map<Integer, Integer> renamer) {
    internal.rename(renamer);
    renameAttic(renamer);
  }

  private static void renameAttic (Map<Integer, Integer> renamer) {
    if (renamer.size() == 0)
      return;
    // hsi HSI implement
    assert false;
  }

  public void assumeClauses (List<int[]> clauses) {
    internal.assumeClauses(clauses);
  }
}