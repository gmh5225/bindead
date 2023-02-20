package bindead.domains.affine;

import javalx.numeric.BigInt;
import bindead.data.Linear;
import bindead.data.NumVar;

/**
 * A structure holding information about a linear substitution of the form {@code [var \ (expr / fac)]} where the
 * backslash is substitution and the slash division of a linear expression by a constant.
 */
public class Substitution implements Comparable<Substitution> {
  private final NumVar var;
  private final Linear expr;
  private final BigInt fac;

  public Substitution (NumVar var, Linear expr, BigInt fac) {
    this.var = var;
    if (fac.isZero())
      throw new IllegalArgumentException("Using a zero factor is not defined.");
    if (fac.isPositive()) {
      this.fac = fac;
      this.expr = expr;
    } else {
      this.fac = fac.negate();
      this.expr = expr.negate();
    }
  }

  /**
   * Construct an inverting substitution in which the variable {@code var} that must occur in the terms of
   * {@code linear} is replaced by the remaining terms and the term {@code fac * var}.
   *
   * @param linear the expression from which the term with variable {@code var} is extracted
   * @param fac the factor of the term {@code fac * var}
   * @param var the variable
   */
  public static Substitution invertingSubstitution (Linear linear, BigInt fac, NumVar var) {
    Linear.Term[] terms = linear.getTerms();
    Linear.Term[] newTs = new Linear.Term[terms.length];
    BigInt prevFac = null;
    for (int i = 0; i < terms.length; i++) {
      if (terms[i].getId() != var)
        newTs[i] = terms[i];
      else {
        prevFac = terms[i].getCoeff();
        newTs[i] = Linear.term(fac.negate(), var);
      }
    }
    assert (prevFac != null);
    return new Substitution(var, Linear.linear(linear.getConstant(), newTs), prevFac.negate());
  }

  public NumVar getVar () {
    return var;
  }

  public Linear getExpr () {
    return expr;
  }

  public BigInt getFac () {
    return fac;
  }

  /**
   * Check if this substitution merely replaces one variable by another
   *
   * @return {@code true} if this substitution is of the form [a/b]
   */
  public boolean isSimple () {
    if (!expr.isSingleTerm())
      return false;
    if (!expr.getConstant().isZero())
      return false;
    NumVar key = expr.getKey();
    BigInt kCoeff = expr.getCoeff(key);
    return kCoeff.isEqualTo(fac);
  }

  /**
   * Check if this substitution is invertible.
   *
   * @return {@code true} if this substitution is of the form [a/e(a)] where e(a) is a linear expression containing a
   */
  public boolean isInvertible () {
    return !expr.getCoeff(var).isZero();
  }

  public Substitution applySubst (Substitution subst) {
    Linear.Divisor d = new Linear.Divisor(fac);
    Linear newRhs = expr.applySubstitution(subst).lowestForm(d);
    return new Substitution(var, newRhs, d.get());
  }

  @Override public int compareTo (Substitution o) {
    int cmp = var.compareTo(o.var);
    if (cmp != 0)
      return cmp;
    cmp = fac.compareTo(o.fac);
    if (cmp != 0)
      return cmp;
    cmp = expr.compareTo(o.expr);
    return cmp;
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fac == null) ? 0 : fac.hashCode());
    result = prime * result + ((expr == null) ? 0 : expr.hashCode());
    result = prime * result + ((var == null) ? 0 : var.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Substitution))
      return false;
    Substitution other = (Substitution) obj;
    return compareTo(other) == 0;
  }

  @Override public String toString () {
    if (fac.isOne())
      return "[" + var + "\\" + expr + "]";
    return "[" + var + "\\ (" + expr + ") / " + fac + "]";
  }

}
