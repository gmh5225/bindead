package satDomain;

import java.util.Arrays;
import java.util.Collection;

import org.sat4j.core.VecInt;

public class Clause {

  final int[] clause;

  Clause (Clause other) {
    int[] oc = other.getClause();
    clause = Arrays.copyOf(oc, oc.length);
  }

  public Clause (int[] clause) {
    this.clause = clause;
  }

  Clause append (int v) {
    final int[] cls = Arrays.copyOf(clause, clause.length + 1);
    cls[clause.length] = v;
    return new Clause(cls);
  }

  public int[] getClause () {
    return clause;
  }

  Clause mergeVars (int to, int from) {
    final int[] cl = new int[clause.length];
    for (int i = 0; i < clause.length; i++) {
      final int v = clause[i];
      if (v == from)
        cl[i] = to;
      else if (v == -from)
        cl[i] = -to;
      else
        cl[i] = v;
    }
    return new Clause(cl);
  }

  Clause renamedClause (Substitution subst) {
    final int cl[] = new int[clause.length];
    for (int i = 0; i < clause.length; i++)
      cl[i] = subst.newValue(clause[i]);
    return new Clause(cl);
  }

  /**
   * Show a single clause in DIMACS format. Appends a new line.
   */
  @Override public String toString () {
    String res = "";
    String sep = "";
    for (final int element : clause) {
      res += sep + element;
      sep = " ";
    }
    res += " 0\n";
    return res;
  }

  /**
   * @param fromSet
   *          list of positive variable numbers
   */
  boolean containsAnyOf (Collection<Integer> fromSet) {
    for (final int element : clause)
      if (fromSet.contains(Math.abs(element)))
        return true;
    return false;
  }

  String showDisjunction () {
    String res = "";
    String sep = "";
    for (final int element : clause) {
      assert element != 0;
      final String elm = "x" + Math.abs(element);
      res += sep + (element > 0 ? " " + elm : "!" + elm);
      sep = " ";
    }
    return res;
  }

  VecInt toVecInt () {
    assert allNonzero();
    return new VecInt(clause);
  }

  /**
   * @throws Error
   */
  private boolean allNonzero () throws Error {
    for (final int i : clause)
      if (i == 0)
        return false;
    return true;
  }
}
