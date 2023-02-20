package bindead.domains.affine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.persistentcollections.AVLMap;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.interfaces.FunctorState;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The implementation of the affine domain. The affine domain consists of a map from leading variables in the
 * affine constraint system to the corresponding constraints and a second map, mapping non-leading variables
 * to the constraints that they occur in. Both mappings are kept in sync at all times.
 */
class AffineState extends FunctorState {
  public static final AffineState EMPTY = new AffineState(AVLMap.<NumVar, Linear>empty(),
    AVLMap.<NumVar, VarSet>empty(), VarSet.empty());
  protected final AVLMap<NumVar, Linear> affine;
  protected final AVLMap<NumVar, VarSet> reverse;
  protected final VarSet newEqualities;

  protected AffineState (AVLMap<NumVar, Linear> affine, AVLMap<NumVar, VarSet> reverse, VarSet newEqualities) {
    this.affine = affine;
    this.reverse = reverse;
    this.newEqualities = newEqualities;
  }

  AffineState withoutEqualities () {
    return new AffineState(affine, reverse, VarSet.empty());
  }

  /**
   * Insert all relevant equalities into the given expression.
   *
   * @param con the expression
   * @param d the divisor with which the expression was scaled down (may be {@code null})
   * @return a new expression that only contains leading variables of the equality system
   */
  public Linear inlineIntoLinear (Linear con, Linear.Divisor d) {
    return inlineIntoLinear(con, d, affine);
  }

  /**
   * Insert all relevant equalities of the given equality system into the given expression.
   *
   * @param con the expression
   * @param d the divisor with which the expression was scaled down (may be {@code null})
   * @param affine the map of affine equations that are to be inlined
   * @return a new expression that only contains leading variables of the equality system
   */
  public static Linear inlineIntoLinear (Linear con, Linear.Divisor d, AVLMap<NumVar, Linear> affine) {
    Linear res = con;
    for (NumVar x : con.getVars()) {
      Linear eq = affine.get(x).getOrNull();
      if (eq != null) {
        res = Linear.mulAdd(eq.getCoeff(x), res, res.getCoeff(x).negate(), eq);
        if (d != null)
          d.mul(eq.getCoeff(x));
      }
    }
    res = res.lowestForm(d);
    return res;
  }

  /**
   * Return all linear constraints that the variable occurs in. It does not matter if
   * the variable is a leading variable or not.
   */
  public List<Linear> getConstraints (NumVar var) {
    List<Linear> result = new ArrayList<Linear>();
    if (affine.contains(var))
      result.add(affine.get(var).get());
    if (reverse.contains(var)) {
      for (NumVar idVar : reverse.get(var).get()) {
        result.add(affine.get(idVar).get());
      }
    }
    return result;
  }

  /**
   * Return {@code true} if the affine domain does not contain this variable. The variable may still occur in
   * the child domain, though. This function is only for debugging.
   *
   * @param var the variable to be checked
   * @return {@code false} if this variable is mapped somewhere
   */
  protected boolean notInSupport (NumVar var) {
    return !inSupport(var);
  }

  public boolean inSupport (NumVar var) {
    return inForwardMapping(var) || inReverseMapping(var);
  }

  private boolean inForwardMapping (NumVar var) {
    return affine.contains(var);
  }

  private boolean inReverseMapping (NumVar var) {
    Option<VarSet> usedIn = reverse.get(var);
    if (usedIn.isNone())
      return false;
    return usedIn.get().size() > 0;
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + (affine == null ? 0 : affine.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof AffineState))
      return false;
    AffineState other = (AffineState) obj;
    if (affine == null) {
      if (other.affine != null)
        return false;
    } else {
      Iterator<P2<NumVar, Linear>> iter1 = affine.iterator();
      Iterator<P2<NumVar, Linear>> iter2 = other.affine.iterator();
      while (iter1.hasNext() && iter2.hasNext()) {
        P2<NumVar, Linear> p1 = iter1.next();
        P2<NumVar, Linear> p2 = iter2.next();
        if (!p1._2().equals(p2._2()))
          return false;
      }
      if (!iter1.hasNext() && !iter2.hasNext())
        return true;
    }
    return false;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(Affine.NAME);
    for (Linear linear : affine.values()) {
      builder = builder.e("Entry")
          .a("type", "equation")
          .e("Value"); // TODO: remove the value tag as it is redundant
      builder = linear.toXML(builder);
      builder = builder.up().up();
    }
    return builder.up();
  }

  @Override public String toString () {
    return "#" + affine.size() + " " + contentToString();
  }

  private String contentToString () {
    Set<NumVar> sorted = StringHelpers.sortLexically(affine.keys());
    Iterator<NumVar> iterator = sorted.iterator();
    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      NumVar key = iterator.next();
      Linear value = affine.getOrNull(key);
      builder.append(value.toEquationString());
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    builder.append(domainName + ": #" + affine.size() + " {");
    Iterator<P2<NumVar, Linear>> iterator = affine.iterator();
    while (iterator.hasNext()) {
      P2<NumVar, Linear> element = iterator.next();
      NumVar variable = element._1();
      Linear equation = element._2();
      if (equation.isSingleTerm() && equation.getCoeff(variable).isOne())
        continue;
      if (equation.isZero()) {
        builder.append("0");
      } else {
        boolean printedFirstVariable = false;
        for (Term term : equation) {
          if (!term.getCoeff().isNegative() && printedFirstVariable)
            builder.append("+");
          if (term.getCoeff().isEqualTo(BigInt.MINUSONE))
            builder.append("-");
          else if (!term.getCoeff().isOne())
            builder.append(term.getCoeff().toString()+"*");
          // XXX bm: for the normal affine the variable will not exist in the child if it is a leading variable
          // However with the redundant affine it will exist. Would need to make a distinction here between
          // the two affine domain implementations. But this also works for now.
          if (!inForwardMapping(term.getId()))
            childDomain.varToCompactString(builder, term.getId());
          else
            builder.append(term.getId());
          printedFirstVariable = true;
        }
        builder.append("=");
        builder.append(equation.getConstant().negate());
      }
      builder.append(", ");
    }
    builder.setLength(builder.length() - 2);
    builder.append("}\n");
  }

  void appendVar (StringBuilder builder, NumVar var, PrettyDomain child) {
    Option<Linear> value = affine.get(var);
    if (value.isNone()) {
      child.varToCompactString(builder, var);
      return;
    }
    Linear linear = value.get();
    if (!linear.isSingleTerm() || !linear.getCoeff(var).isOne()) {
      child.varToCompactString(builder, var);
    } else {
      BigInt c = linear.getConstant();
      builder.append(c.negate().toString());
    }
  }

}