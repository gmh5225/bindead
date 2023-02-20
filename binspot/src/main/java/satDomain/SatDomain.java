package satDomain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import bindead.debug.Benchmark;

/**
 * @author hsi
 */
public class SatDomain extends NumDomain<SatDomain> {
  private List<Clause> Cclauses;

  public SatDomain () {
    super();
    Cclauses = new LinkedList<Clause>();
  }

  private SatDomain (SatDomain other) {
    super(other);
    Cclauses = new LinkedList<Clause>();
    for (final Clause c : other.Cclauses) {
      assumeClause(new Clause(c));
    }
  }

  public void addClausesToInverter (final NativeInverter p) {
    for (final Clause c : Cclauses) {
      final int[] clause = c.getClause();
      p.addclause(clause);
    }
  }

  @Override public void assumeClause (int[] is) {
    for (final int v : is) {
      assert v != 0;
      assert Math.abs(v) <= getLastVar();
    }
    assumeClause(new Clause(is));
  }

  // c must not be shared
  public void assumeClause (Clause c) {
    Cclauses.add(c);
  }

  @Override public void assumeEquality (int iv1, int iv2) {
    if (iv1 == -iv2)
      assumeUnsatisfiable();
    else if (iv1 != iv2) {
      assumeClause(new int[] {-iv1, iv2});
      assumeClause(new int[] {iv1, -iv2});
    }
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
    csetToBottom();
  }

  @Override public SatDomain copy () {
    return new SatDomain(this);
  }

  @Override public Substitution expand (String reason, Collection<Integer> fromSet) {
    // Benchmark.log(showType() + ".expand(fromSet: " + fromSet.size() +
    // ")");
    final HashedSubstitution renaming = new HashedSubstitution();
    for (final int from : fromSet)
      renaming.put(from, freshTopVar());
    final ListIterator<Clause> iter = Cclauses.listIterator();
    while (iter.hasNext()) {
      final Clause clause = iter.next();
      if (clause.containsAnyOf(fromSet))
        iter.add(clause.renamedClause(renaming));
    }
    return renaming;
  }

  public List<Integer> findConstants () {
    final List<Integer> r = new ArrayList<Integer>();
    for (final Clause c : Cclauses)
      if (c.clause.length == 1)
        r.add(c.clause[0]);
    return r;
  }

  @Override public int freshTopVar () {
    Benchmark.count("fresh SAT vars", 1);
    return freshVarIndex();
  }

  public boolean isSubset (SatDomain other) {
    // Benchmark.log("isSubset: this " + this);
    // Benchmark.log("isSubset: other " + other);
    assert other.getLastVar() == getLastVar();
    // Benchmark.log(showType() + ".joinNumeric(" + other.showType() + ")");
    final SatDomain thisN = new SatDomain(this);
    final SatDomain otherN = new SatDomain(other);
    otherN.invert("isSubset", getLastVar());
    // Benchmark.log("other inverted: " + thisN);
    for (final Clause c : otherN.Cclauses)
      thisN.Cclauses.add(c);
    // Benchmark.log("inverted otherN with thisN: " + thisN);
    final boolean ready = thisN.isUnsatisfiable();
    // Benchmark.log("isSubset: result " + ready);
    return ready;
  }

  // later: use minisat for this
  @Override public boolean isUnsatisfiable () {
    // Benchmark.log(showType() + ".isBottom()");
    final boolean satisfiable = createSolver().checkSat();
    return !satisfiable;
  }

  @Override public void joinWith (SatDomain other) {
    final int d = freshTopVar();
    final List<Clause> cls = Cclauses;
    Cclauses = new LinkedList<Clause>();
    caddAllWithLiteral(d, cls);
    caddAllWithLiteral(-d, other.Cclauses);
    setLastVar(Math.max(getLastVar(), other.getLastVar()));
  }

  public void mergeVars (int i, int j) {
    // later: make functional
    if (i == -j)
      assumeUnsatisfiable();
    else if (i != j) {
      Benchmark.log("mergeVars " + i + " == " + j);
      assert i != j;
      assert i != -j;
      final LinkedList<Clause> cls = new LinkedList<Clause>();
      for (final Clause c : Cclauses)
        cls.add(c.mergeVars(i, j));
      Cclauses = cls;
      // assumeEquality(i, j);
    }
  }

  public void project (List<Integer> renamings, boolean withCompression) {
    Benchmark.count("project", 1);
    // Benchmark.log(showType() + ".projectCNF(onto: #" + renamings.size() +
    // ")");
    int last = getLastVar();
    while (renamings.size() < last + 1)
      renamings.add(0);
    final int[] onto = new int[last + 1];
    for (int i = 1; i < onto.length; i++) {
      final int nv = renamings.get(i);
      onto[i] = nv != 0 ? nv : last--;
    }
    crename(onto);
    if (withCompression) {
      invert("project 1", last);
      invert("project 2", last);
      assert getLastVar() == last;
    }
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
  @Override public void project (Renamer renamer, boolean withCompression) {
    project(renamer.renamings, withCompression);
  }

  @Override public int summarize (final int discr, int a, int b) {
    final int nf = freshTopVar();
    assumeClause(new int[] {discr, -nf, b});
    assumeClause(new int[] {discr, nf, -b});
    assumeClause(new int[] {-discr, -nf, a});
    assumeClause(new int[] {-discr, nf, -a});
    return nf;
  }

  @Override public String toString () {
    // System.out.println("cnf.toString calling Demo ");
    // System.out.println("noninverted " + clauses.showDimacs());
    // ClauseContainer cls = NativeInverter.invert(this);
    // System.out.println("inverted " + cls.showDimacs());
    if (!Config.showCNF)
      return showInfo();
    return toDetailedString();
  }

  void crename (int[] onto) {
    final LinkedList<Clause> cls = new LinkedList<Clause>();
    final ListSubstitution subst = new ListSubstitution(onto);
    for (final Clause c : Cclauses)
      cls.add(c.renamedClause(subst));
    Cclauses = cls;
  }

  void csetToBottom () {
    csetToTop();
    Cclauses.add(new Clause(new int[0]));
  }

  void csetToTop () {
    Cclauses.clear();
  }

  boolean isTriviallyFalse () {
    for (final Clause c : Cclauses)
      if (c.clause.length == 0)
        return true;
    return false;
  }

  @Override String showFlag (int f) {
    return (f > 0 ? "" : "!") + "x" + Math.abs(f);
  }

  @Override String toDetailedString () {
    // assert !isInverted;
    String s1 = "";
    for (final Clause clause : Cclauses)
      s1 += clause.showDisjunction() + '\n';
    final String s = s1;
    return "CNF(" + getLastVar() + "), " + Cclauses.size() + " clauses\n"
      + s;
  }

  private void caddAllWithLiteral (int i, List<Clause> cls) {
    for (final Clause c : cls)
      Cclauses.add(c.append(i));
  }

  private CNFSolver createSolver () {
    // Benchmark.log(showType() + ".createSolver()");
    final CNFSolver s = new CNFSolver(getLastVar());
    for (final Clause clause : Cclauses)
      s.addClause(clause);
    return s;
  }

  private void invert (String reason, int last) {
    Benchmark.count("invert: " + reason, 1);
    Benchmark.count("invert", 1);
    if (Cclauses.size() == 0)
      csetToBottom();
    else if (isTriviallyFalse())
      csetToTop();
    else
      NativeInverter.invert(this, last);
    setLastVar(last);
  }

  private String showInfo () {
    final String s = "<CNF ";
    return s + "lastVar=" + getLastVar() + ", clauses=" + Cclauses.size()
      + ">";
  }
}