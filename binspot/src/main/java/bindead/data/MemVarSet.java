package bindead.data;

import java.util.Iterator;

import javalx.data.Option;
import javalx.persistentcollections.AVLSet;
import rreil.lang.MemVar;

/**
 * A container for numeric variables.
 */
public final class MemVarSet implements Iterable<MemVar> {
  private static MemVarSet EMPTY = new MemVarSet(AVLSet.<MemVar>empty());

  private final AVLSet<MemVar> vars;

  private MemVarSet (AVLSet<MemVar> s) {
    vars = s;
  }

  public MemVarSet (Iterable<MemVar> regions) {
    this(collect(regions));
  }

  private static AVLSet<MemVar> collect (Iterable<MemVar> regions) {
    AVLSet<MemVar> vars = AVLSet.<MemVar>empty();
    for (MemVar mv : regions)
      vars = vars.add(mv);
    return vars;
  }

  public static MemVarSet empty () {
    return EMPTY;
  }

  public static MemVarSet of (MemVar... vars) {
    MemVarSet result = empty();
    for (MemVar var : vars) {
      result = result.insert(var);
    }
    return result;
  }

  public static MemVarSet from (Iterable<MemVar> vars) {
    MemVarSet result = empty();
    for (MemVar var : vars) {
      result = result.insert(var);
    }
    return result;
  }

  public MemVarSet insertAll (Iterable<MemVar> vars) {
    MemVarSet result = this;
    for (MemVar var : vars) {
      result = result.insert(var);
    }
    return result;
  }


  public MemVarSet insert (MemVar var) {
    return new MemVarSet(vars.add(var));
  }

  public int size () {
    return vars.size();
  }

  public boolean isEmpty () {
    return vars.isEmpty();
  }

  @Override public Iterator<MemVar> iterator () {
    return vars.iterator();
  }

  public boolean contains (MemVar var) {
    return vars.contains(var);
  }

  public MemVarSet difference (MemVarSet other) {
    return new MemVarSet(vars.difference(other.vars));
  }

  public MemVarSet intersection (MemVarSet other) {
    return new MemVarSet(vars.intersection(other.vars));
  }

  public Option<MemVar> getMin () {
    return vars.getMin();
  }

  @Override public String toString () {
    return vars.toString();
  }

  public MemVarSet remove (MemVar memId) {
    return new MemVarSet(vars.remove(memId));
  }

  public boolean containsTheSameAs (MemVarSet other) {
    return vars.equals(other.vars);
  }
}
