package bindead.domains.sat;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javalx.data.Option;
import javalx.numeric.BigInt;
import satDomain.Flag;
import satDomain.HashedSubstitution;
import satDomain.Numeric;
import satDomain.Renamer;
import satDomain.Substitution;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.data.FoldMap;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;

public class SatStateBuilder {
  final protected Numeric cnf;
  // translates global variable indices to CNF variable indices.
  final protected HashMap<Integer, Integer> translation;

  private boolean isTracked (int v) {
    return translation.containsKey(v);
  }

  boolean isTracked (NumVar v) {
    return isTracked(v.getStamp());
  }

  private int getInner (int v) {
    assert isTracked(v);
    return translation.get(v);
  }

  private int getInner (NumVar v) {
    return getInner(v.getStamp());
  }

  private boolean equalSupport (SatStateBuilder other) {
    return translation.keySet().equals(other.translation.keySet());
  }

  public SatStateBuilder (SatState ctx) {
    cnf = new Numeric(ctx.cnf);
    translation = new HashMap<Integer, Integer>(ctx.translation);
  }

  public SatState build () {
    System.out.println("build() built new SatState: " + translation + "\n"
      + cnf);
    return new SatState(cnf, translation);
  }

  @Override public String toString () {
    return "SatStateBuilder{\n" + cnf + '}';
  }

  public boolean subsetOrEqual (SatStateBuilder other) {
    assert equalSupport(other);
    other.cnf.rename(makeRenamerForSecondTranslation(translation,
        other.translation));
    return cnf.isSubset(other.cnf);
  }

  private Map<Integer, Integer> makeRenamerForSecondTranslation (
      HashMap<Integer, Integer> t1, HashMap<Integer, Integer> t2) {
    Map<Integer, Integer> renaming = new HashMap<Integer, Integer>();
    for (Entry<Integer, Integer> e : t1.entrySet()) {
      Integer other = t2.get(e.getKey());
      renaming.put(other, e.getValue());
    }
    return renaming;
  }

  private void joinWith (SatStateBuilder other, boolean compression) {
    assert equalSupport(other);
    Renamer rl = new Renamer();
    Renamer rr = new Renamer();
    for (Entry<Integer, Integer> e : translation.entrySet()) {
      int outer = e.getKey();
      int il = rl.add(e.getValue());
      int ir = rr.add(other.getInner(outer));
      assert il == ir;
      translation.put(outer, il);
      other.translation.put(outer, ir);
    }
    cnf.project(rl, compression);
    other.cnf.project(rr, compression);
    cnf.joinWith(other.cnf);
  }

  public void joinWith (SatStateBuilder other) {
    joinWith(other, false);
  }

  public void widenWith (SatStateBuilder other) {
    joinWith(other, true);
  }

  // add x, replace y
  public void substitute (NumVar x, NumVar y) {
    assert isTracked(y);
    assert !isTracked(x);
    translation.put(x.getStamp(), translation.get(y.getStamp()));
    translation.remove(y.getStamp());
  }

  public void eval (Test test) {
    System.out.println("SatStateBuilder: test " + test);
    List<List<Integer>> clauses = new RhsTranslator(this).visitTest(test, true);
    System.out.println("SatStateBuilder: translated to clauses " + clauses);
    if (clauses != null)
      restrictCnf(translateClauses(clauses));
  }

  public void eval (Assign stmt) {
    NumVar lhs = stmt.getLhs().getId();
    if (!isTracked(lhs))
      return;
    translation.put(lhs.getStamp(), cnf.freshTopVar().flag);
    restrictHalf(lhs, stmt.getRhs(), -1);
    restrictHalf(lhs, stmt.getRhs(), 1);
  }

  private void restrictHalf (NumVar lhs, Rhs rhs, int polarity) {
    List<List<Integer>> negRhs = rhs.accept(new RhsTranslator(this), polarity < 0);
    if (negRhs != null) {
      for (List<Integer> c : negRhs) {
        c.add(polarity * lhs.getStamp());
      }
      restrictCnf(translateClauses(negRhs));
    }
  }

  private List<int[]> translateClauses (List<List<Integer>> cls) {
    assert cls != null;
    LinkedList<int[]> t = new LinkedList<int[]>();
    for (List<Integer> clause : cls) {
      t.add(translateClause(clause));
    }
    System.out.println("translated clauses " + t);
    return t;
  }

  private int[] translateClause (List<Integer> clause) {
    int[] c = new int[clause.size()];
    int i = 0;
    for (Integer l : clause)
      c[i++] = translateLiteral(l);
    return c;
  }

  private Integer translateLiteral (Integer l) {
    assert l != null;
    Integer t = translation.get(Math.abs(l));
    assert t != null;
    return l > 0 ? t : -t;
  }

  private void restrictCnf (List<int[]> list) {
    cnf.assumeClauses(list);
  }

  public void copyAndPaste (VarSet vars, SatState state) {
    // TODO hsi implement copyAndPaste
    assert false;
  }

  public void fold (FoldMap vars) {
    HashedSubstitution subst = new HashedSubstitution();
    for (VarPair p : vars) {
      subst.put(getInner(p.getEphemeral()), getInner(p.getPermanent()));
    }
    Substitution s = cnf.summarize(subst);
    System.out.println("fold: substitution " + s);
    for (Entry<Integer, Integer> t : translation.entrySet()) {
      Integer v = t.getValue();
      t.setValue(s.get(v));
    }
    System.out.println("fold: vars " + vars);
    System.out.println("fold: translation " + translation);
    for (VarPair vp : vars)
      translation.remove(vp.getEphemeral().getStamp());
    System.out.println("fold: translation " + translation);
  }

  public void expand (FoldMap vars) {
    System.out.println("expand: vars " + vars);
    System.out.println("expand: in domain " + translation + " " + cnf);
    Collection<Flag> fromSet = new HashSet<Flag>();
    for (VarPair v : vars) {
      int internalPermanent = translation
          .get(v.getPermanent().getStamp());
      fromSet.add(new Flag(internalPermanent));
      System.out.println("expand: fromSet has " + internalPermanent);
    }
    Substitution s = cnf.expand("expanding SatStateBuilder", fromSet);
    System.out.println("expand: substitution " + s);
    System.out.println("expand: trans " + translation);
    // rename substituted vars in translation
    for (VarPair v : vars) {
      int newO = v.getEphemeral().getStamp();
      int newI = s.get(translation.get(v.getPermanent().getStamp()));
      translation.put(newO, newI);
      System.out.println("expand: new translation " + newO + " -> "
        + newI);

    }
    System.out.println("expand: in domain " + translation + " " + cnf);
  }

  public void remove (NumVar variable) {
    if (isTracked(variable))
      translation.remove(variable.getStamp());
  }

  public void introduce (NumVar variable, Option<BigInt> value) {
    assert !isTracked(variable);
    final int iv;
    if (value.isNone())
      iv = cnf.freshTopVar().flag;
    else if (value.get().isZero())
      iv = cnf.freshFalseVar().flag;
    else
      iv = cnf.freshTrueVar().flag;
    translation.put(variable.getStamp(), iv);
    System.out.println("introduced " + iv);
  }
}
