package bindead.data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javalx.data.products.P3;
import javalx.persistentcollections.AVLMap;

/**
 * A container for numeric variables.
 *
 * REFACTOR hsi: avoid loops that add variables to a VarSet via add(), because they have quadratic memory complexity
 *
 */
public final class VarSet implements Comparable<VarSet>, Iterable<NumVar> {
  private static NumVar[] EMPTY_VEC = new NumVar[0];
  private static VarSet EMPTY = new VarSet();
  private final NumVar[] vars;

  /**
   * Create a fresh, empty variable set.
   */
  private VarSet () {
    vars = EMPTY_VEC;
  }

  private VarSet (NumVar[] vars) {
    this.vars = vars;
  }

  public static VarSet empty () {
    return EMPTY;
  }

  public static VarSet of (NumVar... vars) {
    NumVar[] vs = Arrays.copyOf(vars, vars.length);
    return new VarSet(vs);
  }

  public static VarSet from (Set<NumVar> vars) {
    NumVar[] vs = vars.toArray(EMPTY_VEC);
    return new VarSet(vs);
  }

  public static VarSet from (Iterable<NumVar> vars, int size) {
    NumVar[] vs = new NumVar[size];
    int i = 0;
    for (NumVar var : vars)
      vs[i++] = var;
    assert i == size;
    return new VarSet(vs);
  }

  public static VarSet fromKeys (AVLMap<NumVar, ?> map) {
    return from(map.keys(), map.size());
  }



  public int size () {
    return vars.length;
  }

  public boolean isEmpty () {
    return vars.length == 0;
  }

  public NumVar first () {
    return vars[0];
  }

  public NumVar get (int index) {
    return vars[index];
  }

  /**
   * Returns {@code true} if the variable is in this set.
   *
   * @param var the variable
   * @return {@code true} if contained
   */
  public boolean contains (NumVar var) {
    int pos = Arrays.binarySearch(vars, 0, vars.length, var);
    return pos >= 0;
  }

  /**
   * Check if all elements in the passed-in set are also contained in this set.
   *
   * @param other the passed-in set
   * @return {@code false} if there exists an element in {@code other} that is
   *         not in this set
   */
  public boolean containsAll (VarSet other) {
    int thisIdx = 0;
    int thatIdx = 0;
    while (thisIdx < vars.length && thatIdx < other.vars.length) {
      if (vars[thisIdx] == other.vars[thatIdx]) {
        thisIdx++;
        thatIdx++;
      } else if (vars[thisIdx].compareTo(other.vars[thatIdx]) < 0)
        thisIdx++;
      else
        return false;
    }
    return thatIdx == other.vars.length;
  }

  /**
   * Check if this set contains any of the elements from the other set.
   */
  public boolean containsAny (VarSet other) {
    return !intersection(other).isEmpty();
  }

  /**
   * Insert a variable into this set.
   *
   * @param var the variable to be inserted (can be {@code null})
   * @return the variable set including the given variable
   */
  public VarSet add (NumVar var) {
    if (var == null)
      return this;
    int pos = Arrays.binarySearch(vars, 0, vars.length, var);
    if (pos >= 0)
      return this;
    pos = -pos - 1;
    NumVar[] newVars = new NumVar[vars.length + 1];
    System.arraycopy(vars, 0, newVars, 0, pos);
    newVars[pos] = var;
    int rem = vars.length - pos;
    System.arraycopy(vars, pos, newVars, pos + 1, rem);
    return new VarSet(newVars);
  }

  /**
   * Remove the given variable from the set.
   *
   * @param var the variable to be removed (can be {@code null})
   * @return the variable set without the given variable
   */
  public VarSet remove (NumVar var) {
    if (var == null)
      return this;
    int pos = Arrays.binarySearch(vars, 0, vars.length, var);
    if (pos < 0)
      return this;
    NumVar[] newVars = new NumVar[vars.length - 1];
    System.arraycopy(vars, 0, newVars, 0, pos);
    int rem = vars.length - (pos + 1);
    System.arraycopy(vars, pos + 1, newVars, pos, rem);
    return new VarSet(newVars);
  }

  /**
   * Calculate the union of this and the other variable set.
   *
   * @param other the other variable set
   * @return a new variable set containing the elements of this and the other
   *         variable set
   */
  public VarSet union (VarSet other) {
    if (other.vars.length == 0)
      return this;
    if (vars.length == 0)
      return other;
    int newLength = 0;
    int thisIdx = 0;
    int thatIdx = 0;
    while (thisIdx < vars.length && thatIdx < other.vars.length) {
      newLength++;
      if (vars[thisIdx] == other.vars[thatIdx]) {
        thisIdx++;
        thatIdx++;
      } else if (vars[thisIdx].compareTo(other.vars[thatIdx]) < 0)
        thisIdx++;
      else
        thatIdx++;
    }
    newLength += vars.length - thisIdx + other.vars.length - thatIdx;
    NumVar[] newVars = new NumVar[newLength];
    thisIdx = thatIdx = 0;
    int newIdx = 0;
    while (thisIdx < vars.length && thatIdx < other.vars.length) {
      if (vars[thisIdx] == other.vars[thatIdx]) {
        newVars[newIdx++] = vars[thisIdx];
        thisIdx++;
        thatIdx++;
      } else if (vars[thisIdx].compareTo(other.vars[thatIdx]) < 0)
        newVars[newIdx++] = vars[thisIdx++];
      else
        newVars[newIdx++] = other.vars[thatIdx++];
    }
    while (thisIdx < vars.length)
      newVars[newIdx++] = vars[thisIdx++];
    while (thatIdx < other.vars.length)
      newVars[newIdx++] = other.vars[thatIdx++];
    assert newIdx == newLength;
    return new VarSet(newVars);
  }

  /**
   * Calculate the set difference between this and the other variable set.
   *
   * @param other the other variable set that is to be subtracted from this set
   * @return a new variable set containing those elements that occur in this
   *         but not in the other variable set
   */
  public VarSet difference (VarSet other) {
    if (other.vars.length == 0)
      return this;
    int newLength = 0;
    int thisIdx = 0;
    int thatIdx = 0;
    while (thisIdx < vars.length && thatIdx < other.vars.length) {
      if (vars[thisIdx] == other.vars[thatIdx]) {
        thisIdx++;
        thatIdx++;
      } else if (vars[thisIdx].compareTo(other.vars[thatIdx]) < 0) {
        thisIdx++;
        newLength++;
      } else
        thatIdx++;
    }
    newLength += vars.length - thisIdx;
    NumVar[] newVars = new NumVar[newLength];
    thisIdx = thatIdx = 0;
    int newIdx = 0;
    while (thisIdx < vars.length && thatIdx < other.vars.length) {
      if (vars[thisIdx] == other.vars[thatIdx]) {
        thisIdx++;
        thatIdx++;
      } else if (vars[thisIdx].compareTo(other.vars[thatIdx]) < 0) {
        newVars[newIdx++] = vars[thisIdx++];
      } else
        thatIdx++;
    }
    while (thisIdx < vars.length)
      newVars[newIdx++] = vars[thisIdx++];
    assert newIdx == newLength;
    return new VarSet(newVars);
  }

  /**
   * Calculate the intersection of this and the other variable set.
   *
   * @param other the other variable set
   * @return a new variable set containing those elements that occur in this
   *         and the other variable set
   */
  public VarSet intersection (VarSet other) {
    if (vars.length == 0)
      return this;
    if (other.vars.length == 0)
      return other;
    int newLength = 0;
    int thisIdx = 0;
    int thatIdx = 0;
    while (thisIdx < vars.length && thatIdx < other.vars.length) {
      if (vars[thisIdx] == other.vars[thatIdx]) {
        thisIdx++;
        thatIdx++;
        newLength++;
      } else if (vars[thisIdx].compareTo(other.vars[thatIdx]) < 0)
        thisIdx++;
      else
        thatIdx++;
    }
    NumVar[] newVars = new NumVar[newLength];
    thisIdx = thatIdx = 0;
    int newIdx = 0;
    while (thisIdx < vars.length && thatIdx < other.vars.length) {
      if (vars[thisIdx] == other.vars[thatIdx]) {
        newVars[newIdx++] = vars[thisIdx];
        thisIdx++;
        thatIdx++;
      } else if (vars[thisIdx].compareTo(other.vars[thatIdx]) < 0)
        thisIdx++;
      else
        thatIdx++;
    }
    assert newIdx == newLength;
    return new VarSet(newVars);
  }

  /**
   * Perform a three way split of this set with other. Returns a 3-tuple {@code <a, b, c>} where {@code a} are the
   * elements only in this, {@code b} all common elements, and {@code c} the elements only in other.
   *
   * @param other The other set
   * @return A 3-way split of this and other.
   */
  public P3<VarSet, VarSet, VarSet> split (VarSet other) {
    VarSet common = this.intersection(other);
    VarSet onlyInFirst = this.difference(common);
    VarSet onlyInSecond = other.difference(common);
    return P3.tuple3(onlyInFirst, common, onlyInSecond);
  }

  /**
   * Return the subset of this variable set that contains only variables
   * strictly smaller than the one given.
   *
   * @param var the pivot variable
   * @return a new set containing only variables smaller than the pivot
   */
  public VarSet getSmaller (NumVar var) {
    int pos = Arrays.binarySearch(vars, 0, vars.length, var);
    if (pos < 0)
      pos = -(pos + 1);
    if (pos == vars.length)
      return this;
    return new VarSet(Arrays.copyOf(vars, pos));
  }

  /**
   * Substitute the variables in this set by other variables.
   *
   * @param mapping Maps variables to their substitute.
   * @return a new set with the substitution applied
   */
  public VarSet substitute (Map<NumVar, NumVar> mapping) {
    NumVar[] newVars = Arrays.copyOf(vars, vars.length);
    for (int i = 0; i < newVars.length; i++) {
      NumVar substitute = mapping.get(vars[i]);
      if (substitute != null)
        newVars[i] = substitute;
    }
    Arrays.sort(newVars);
    return new VarSet(newVars);
  }

  @Override public int compareTo (VarSet other) {
    int diff = other.vars.length - vars.length;
    if (diff != 0)
      return diff;
    for (int i = 0; i < vars.length; i++) {
      if (vars[i] != other.vars[i])
        return vars[i].compareTo(other.vars[i]);
    }
    return 0;
  }

  /**
   * Checks if the two sets contain the same elements.
   */
  @Override public boolean equals (Object t) {
    if (t == null)
      return false;
    return compareTo((VarSet) t) == 0;
  }

  @Override public int hashCode () {
    int hash = 7;
    hash = 37 * hash + Arrays.hashCode(this.vars);
    return hash;
  }

  @Override public String toString () {
    String res = "{";
    String sep = "";
    for (int i = 0; i < vars.length; i++) {
      res += sep + vars[i];
      sep = ", ";
    }
    return res + "}";
  }

  private class Iter implements Iterator<NumVar> {
    int idx = 0;

    @Override public boolean hasNext () {
      return idx < vars.length;
    }

    @Override public NumVar next () {
      return vars[idx++];
    }

    @Override public void remove () {
      throw new UnsupportedOperationException();
    }
  }

  @Override public Iterator<NumVar> iterator () {
    return new Iter();
  }
}
