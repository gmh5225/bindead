package satDomain;

import java.util.Collection;

abstract class NumDomain<T> {
  private int lastVar;

  NumDomain () {
    lastVar = 0;
  }

  NumDomain (NumDomain<T> other) {
    lastVar = other.lastVar;
  }

  abstract public void assumeTwoBitSum (int x1f, int x0f, int y0f, int s1f,
      int s0f);

  abstract public T copy ();

  public int duplicate (int f) {
    final int v = freshTopVar();
    assumeEquality(f, v);
    return v;
  }

  public int freshTrueVar () {
    final int v = freshTopVar();
    assumeTrue(v);
    return v;
  }

  abstract void assumeClause (int[] fs);

  abstract void assumeEquality (int f1, int f2);

  void assumeTrue (int v) {
    // Benchmark.log("NumDomain.assumeTrue " + v);
    assumeClause(new int[] {v});
  }

  abstract void assumeUnsatisfiable ();

  abstract Substitution expand (String reason, Collection<Integer> fromSet);

  int freshFalseVar () {
    final int v = freshTopVar();
    assumeTrue(-v);
    return v;
  }

  abstract int freshTopVar ();

  final int freshVarIndex () {
    return ++lastVar;
  }

  final int getLastVar () {
    return lastVar;
  }

  abstract boolean isUnsatisfiable ();

  abstract void joinWith (T other);

  abstract void project (Renamer renamer, boolean withCompression);

  final void setLastVar (int lv) {
    lastVar = lv;
  }

  abstract String showFlag (int f);

  abstract int summarize (final int discr, int a, int b);

  abstract String toDetailedString ();
}
