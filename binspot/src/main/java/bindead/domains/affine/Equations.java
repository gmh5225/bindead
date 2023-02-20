package bindead.domains.affine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.persistentcollections.AVLMap;
import bindead.data.Linear;
import bindead.data.NumVar;

/**
 * A container for a system of equalities. The equalities are ordered by their key.
 */
public class Equations extends Vector<Equation> {
  private static final long serialVersionUID = 28345902384L;

  public Equations (int capacity) {
    super(capacity);
  }

  protected Equations (HashMap<Integer,NumVar> intToVar, Linear... ls) {
    super(ls.length);
    for (Linear l : ls) add(l.toEquation(intToVar));
    elementCount = ls.length;
    Arrays.sort(elementData, 0, elementCount);
  }

  public Equations (AVLMap<NumVar,Linear> c1, AVLMap<NumVar,Linear> c2, HashMap<Integer,NumVar> intToVar) {
    super(c1.size() + c2.size());
    // iterate simultaneously through both maps and copy the equalities into
    // the vector in ascending order
    Iterator<P2<NumVar, Linear>> iter1 = c1.iterator();
    Iterator<P2<NumVar, Linear>> iter2 = c2.iterator();
    P2<NumVar, Linear> e1 = null;
    P2<NumVar, Linear> e2 = null;
    while (true) {
      if (e1 == null)
        if (iter1.hasNext())
          e1 = iter1.next();
        else {
          if (e2 != null)
            add(e2._2().toEquation(intToVar));
          while (iter2.hasNext())
            add(iter2.next()._2().toEquation(intToVar));
          break;
        }
      if (e2 == null)
        if (iter2.hasNext())
          e2 = iter2.next();
        else {
          if (e1 != null)
            add(e1._2().toEquation(intToVar));
          while (iter1.hasNext())
            add(iter1.next()._2().toEquation(intToVar));
          break;
        }
      if (e1._1().compareTo(e2._1()) < 0) {
        add(e1._2().toEquation(intToVar));
        e1 = null;
      } else {
        add(e2._2().toEquation(intToVar));
        e2 = null;
      }
    }
  }

  /**
   * Create a system with all equalities in {@code eqs} that are not in {@code common}.
   *
   * @param common a (sorted) equality system
   * @param eqs a (sorted) equality system
   */
  private Equations (Equations common, Equations eqs) {
    super(Math.max(0, common.size() - eqs.size()));
    int cIdx = 0;
    int eIdx = 0;
    while (cIdx < common.size() && eIdx < eqs.size()) {
      int cKey = common.get(cIdx).getKey();
      int eKey = eqs.get(eIdx).getKey();
      if (cKey < eKey)
        cIdx++;
      else if (cKey == eKey)
        eIdx++;
      else
        add(eqs.get(eIdx++));
    }
    while (eIdx < eqs.size())
      add(eqs.get(eIdx++));
  }

  /**
   * A class that holds the three equality systems that are returned by {@link #affineHull}.
   */
  static final class AffineHullResult {
    public Equations common;
    public Equations onlyFst;
    public Equations onlySnd;

    public AffineHullResult (Equations common) {
      this.common = common;
      this.onlyFst = new Equations(0);
      this.onlySnd = onlyFst;
    }

    private AffineHullResult (Equations common, Equations cons1, Equations cons2) {
      this.common = common;
      onlyFst = new Equations(common, cons1);
      onlySnd = new Equations(common, cons2);
    }

    @Override public String toString () {
      return "common:\n" + common.toString() + "only in first:\n" +
        onlyFst.toString() + "only in second:\n" + onlySnd.toString();
    }
  }

  static AffineHullResult affineHull (Equations cons1, Equations cons2) {
    if (cons1.isEmpty() && cons2.isEmpty())
      return new AffineHullResult(cons1);
    int lambda;
    {
      int max = -1;
      for (Equation l : cons1) {
        int largest = l.getLargestVar();
        if (largest > max)
          max = largest;
      }
      for (Equation l : cons2) {
        int largest = l.getLargestVar();
        if (largest > max)
          max = largest;
      }
      assert max >= 0;
      lambda = max + 1;
    }
    Equations matrix = new Equations(cons1.size() + cons2.size());
    for (Equation l : cons1)
      matrix.add(l.transformForAffineHullFirst(lambda));
    for (Equation l : cons2)
      matrix.add(l.transformForAffineHullSecond(lambda));
    // System.out.println("composed matrices:\n"+matrix);
    matrix.gauss(cons1.size());
    int out = 0;
    for (int i = 0; i < matrix.size(); i++) {
      Equation row = matrix.get(i).transformForAffineHullBack(lambda);
      if (row != null)
        matrix.set(out++, row);
    }
    matrix.setSize(out);
    return new AffineHullResult(matrix, cons1, cons2);
  }


  // if keysInRangeAllLargerThanEqVars is true then inlining the equality cannot change the order
  // of the given equality range nor can it make any of these equalities turn to zero
  private int substEqInEqs (int start, int end, Equation eq, boolean keysInRangeAllLargerThanEqVars) {
    int key = eq.getKey();
    if (key < 0)
      return end;
    BigInt negKeyCoeff = eq.getCoeff(key).negate();
    int i = start;
    while (i < end) {
      Equation cur = get(i);
      BigInt coeff = cur.getCoeff(key);
      if (!coeff.isZero()) {
        cur = Equation.mulAdd(negKeyCoeff, cur, coeff, eq);
        // nuke this equation if it is contains no more vars
        if (cur.getKey() < 0) {
          assert !keysInRangeAllLargerThanEqVars;
          if (i != --end)
            set(i, get(end));
          continue;
        } else {
          set(i, cur);
        }
      }
      i++;
    }
    if (!keysInRangeAllLargerThanEqVars)
      Arrays.sort(this.elementData, start, end);
    return end;
  }

  public Equation substEqsInEq (Equation eq) {
    return substEqsInEq(0, size(), eq);
  }

  private Equation substEqsInEq (int start, int end, Equation eq) {
    int lower = start;
    for (bindead.domains.affine.Equation.Term t : eq.terms) {
      int curVar = t.getId();
      int upper = end;
      while (lower < upper) {
        int middle = (lower + upper) / 2;
        if (get(middle).getKey() < curVar)
          lower = middle + 1;
        else
          upper = middle;
      }
      if (lower == end)
        break;
      Equation other = get(lower);
      if (curVar != other.getKey())
        continue;
      eq = Equation.mulAdd(other.getCoeff(curVar).negate(), eq, eq.getCoeff(curVar), other);
    }
    return eq;
  }

  /**
   * Put the equation system into upper triangular form.
   */
  public void gauss () {
    gauss(0);
  }

  private void gauss (int current) {
    while (current < size()) {
      Equation pivot = substEqsInEq(0, current, get(current));
      // System.out.println("pivot is row "+current+" which is now "+pivot);
      int startSnd = current + 1;
      int endFst = substEqInEqs(0, current, pivot, true);
      assert current == endFst;
      int endSnd = substEqInEqs(startSnd, size(), pivot, true);
      // we add the new equality to the first set of equalities unless it is empty
      if (pivot.getKey() != -1) {
        // store the new pivot right after the first equality set; here endFst may be different to current if the
        // first equality set shrank during inlining
        set(endFst, pivot);
        // System.out.println("matrix after inlining pivot in rows [0,"+endFst+"[ and in ["+startSnd+","+endSnd+"[:\n"+this);
        // sift the pivot into the first set of equalities, thereby ensuring sortedness
        for (int i = endFst; i > 0 && get(i).getKey() < get(i - 1).getKey(); i--)
          set(i - 1, set(i, get(i - 1)));
        endFst++;
      }
      // close any gap between the first and second set of equalities
      if (startSnd > endFst) {
        int length = endSnd - startSnd;
        System.arraycopy(elementData, startSnd, elementData, endFst, length);
        setSize(endFst + length);
      }
      current = endFst;
    }
  }

  @Override public String toString () {
    String res = "";
    for (Equation eq : this) {
      if (eq==null) res = res + "n/a\n"; else
        res = res + eq.toString() + "\n";
    }
    return res;
  }

  @Override public boolean equals (Object o) {
    if (!(o instanceof Equations))
      return false;
    Equations other = (Equations) o;
    if (other.size() != size())
      return false;
    for (int i = 0; i < size(); i++)
      if (!elementAt(i).equals(other.elementAt(i)))
        return false;
    return true;
  }

  @Override public int hashCode () {
    int hash = 3;
    for (Equation l : this)
      hash += l.hashCode();
    return hash;
  }
}
