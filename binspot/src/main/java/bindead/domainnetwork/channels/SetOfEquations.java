package bindead.domainnetwork.channels;

import java.util.Iterator;

import javalx.data.Option;
import javalx.persistentcollections.AVLSet;
import bindead.data.Linear;
import bindead.data.VarSet;

public class SetOfEquations implements Iterable<Linear> {
  private static final SetOfEquations EMPTY = new SetOfEquations();
  private final AVLSet<Linear> equalities;

  private SetOfEquations () {
    this(AVLSet.<Linear>empty());
  }

  private SetOfEquations (AVLSet<Linear> eqs) {
    equalities = eqs;
  }

  public static SetOfEquations empty () {
    return EMPTY;
  }

  public Option<Linear> getFirst () {
    return equalities.getMin();
  }

  public VarSet getVars () {
    VarSet vars = VarSet.empty();
    for (Linear eq : this)
      vars = vars.union(eq.getVars());
    return vars;
  }

  public SetOfEquations removeVariables (VarSet toRemove) {
    AVLSet<Linear> eqs = AVLSet.<Linear>empty();
    for (Linear eq : this) {
      if (eq.getVars().intersection(toRemove).isEmpty())
        eqs = eqs.add(eq);
    }
    return new SetOfEquations(eqs);
  }

  public SetOfEquations add (Linear equation) {
    return new SetOfEquations(equalities.add(equation));
  }

  public boolean isEmpty () {
    return equalities.isEmpty();
  }

  public SetOfEquations difference (SetOfEquations other) {
    return new SetOfEquations(equalities.difference(other.equalities));
  }

  public SetOfEquations union (SetOfEquations other) {
    return new SetOfEquations(equalities.union(other.equalities));
  }

  public SetOfEquations intersect (SetOfEquations other) {
    return new SetOfEquations(equalities.intersection(other.equalities));
  }

  @Override public Iterator<Linear> iterator () {
    return equalities.iterator();
  }

  @Override public String toString () {
    return equalities.toString();
  }
}
