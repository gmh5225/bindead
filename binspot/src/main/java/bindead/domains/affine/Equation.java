package bindead.domains.affine;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import bindead.data.NumVar;

public class Equation implements Comparable<Equation> {
  private static final int constVar = 0;
  protected final Term[] terms;

  // for use in Linear
  public Term[] getTerms () {
    return terms;
  }

  public Equation (Term[] terms) {
    BigInt d = findTermsGCD(terms);
    // ensure positive sign of equality
    if (terms.length > 0 && terms[0].id != 0 && terms[0].coeff.isNegative() ||
      terms.length > 1 && terms[0].id == 0 && terms[1].coeff.isNegative())
      if (d == null)
        d = Bound.MINUSONE;
      else
        d = d.negate();
    // divide by gcd and/or -1
    if (d != null) {
      for (int i = 0; i < terms.length; i++) {
        terms[i] = new Term(terms[i].getCoeff().div(d), terms[i].getId());
      }
    }
    this.terms = terms;
    assert isSorted();
  }

  // calculate the GCD of coefficients
  private static BigInt findTermsGCD (Term[] ts) {
    BigInt d = null;
    for (Term t : ts) {
      BigInt coeff = t.coeff.abs();
      if (d == null) {
        d = coeff;
      } else {
        if (d.isOne())
          return null;
        d = d.gcd(coeff);
      }
    }
    return d;
  }

  private boolean isSorted () {
    for (int i = 1; i < terms.length; i++) {
      if (terms[i] == null || terms[i - 1] == null)
        return false;
      if (terms[i].id < terms[i - 1].id)
        return false;
    }
    return true;
  }

  /**
   * Query the coefficient of the variable {@code id} in this linear expression.
   *
   * @param id the variable
   * @return the coefficient in front of this variable
   */
  public BigInt getCoeff (int id) {
    for (int i = 0; i < terms.length; i++) {
      if (terms[i].getId() == id)
        return terms[i].getCoeff();
    }
    return Bound.ZERO;
  }

  /**
   * Return the smallest variable used in this linear expression. This value
   * can be used as a key in data structures.
   *
   * @return the smallest variable or -1 if the linear expression is constant
   */
  public int getKey () {
    if (terms.length == 0)
      return -1;
    if (terms[0].getId() != constVar)
      return terms[0].getId();
    if (terms.length > 1)
      return terms[1].getId();
    return -1;
  }

  /**
   * Return the smallest variable used in this linear expression. This value
   * can be used as a key in data structures.
   *
   * @param intToVar a mapping from numeric indices to NumVars
   * @return the smallest variable or {@code null} if the linear expression is constant
   */
  public NumVar getKey (HashMap<Integer, NumVar> intToVar) {
    return intToVar.get(getKey());
  }

  private int getConstIdx () {
    if (terms.length > 0 && terms[0].id == 0)
      return 0;
    return -1;
  }

  /**
   * Return the constant of this linear expression.
   *
   * @return the constant of the linear expression
   */
  public BigInt getConstant () {
    int cIdx = getConstIdx();
    if (cIdx == -1)
      return Bound.ZERO;
    return terms[cIdx].coeff;
  }

  /**
   * Ask for the largest variable in this term set.
   *
   * @return a variable index that, if one is added, can be added to this term
   *         without affecting the existing terms
   */
  public int getLargestVar () {
    if (terms.length > 0)
      return terms[terms.length - 1].getId();
    return 0;
  }

  /**
   * Ask for the smallest variable in this term set that is larger than the given one.
   *
   * @param var the variable that should not be returned nor any smaller variable
   * @return the smallest variable v with {@code v>var} or -1 if there is no larger variable
   */
  public int getLargerVar (int var) {
    if (var == getLargestVar() || terms.length == 0)// || isConstantOnly())
      return -1;
    int i = 0;
    while (i < terms.length && terms[i].getId() <= var)
      i++;
    return terms[i].getId();
  }

  private static void sortTerms (Term[] termsArray) {
    if (termsArray.length > 0)
      Arrays.sort(termsArray, Equation.Term.$Comparator);
  }

  public static final class Term implements Comparable<Term> {
    public static final Comparator<Term> $Comparator = new Comparator<Term>() {
      @Override public int compare (Term left, Term right) {
        return left.compareTo(right);
      }
    };
    private final int id;
    private final BigInt coeff;

    public Term (BigInt coeff, int id) {
      this.id = id;
      this.coeff = coeff;
    }

    public int getId () {
      return id;
    }

    public BigInt getCoeff () {
      return coeff;
    }

    public Term mul (BigInt scalar) {
      return new Term(coeff.mul(scalar), id);
    }

    @Override public String toString () {
      if (coeff.isOne())
        return "x" + id;
      else if (coeff.isEqualTo(Bound.MINUSONE))
        return "-" + "x" + id;
      else
        return coeff + "x" + id;
    }

    /**
     * Compare two terms lexicographically, that is, considering both, variable and coefficient.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override public int compareTo (Term other) {
      int cmp = id == other.id ? 0 : id < other.id ? -1 : 1;
      if (cmp == 0)
        return coeff.compareTo(other.coeff);
      return cmp;
    }
  }

  /**
   * Calculate the weighted sum of two linear expressions. See {@link #mulAdd}.
   *
   * @param thisFac the factor in front of the first linear expression
   * @param thisLin the argument of the first linear expression
   * @param otherFac the factor in front of the argument linear expression
   * @param otherLin the argument linear expression
   * @return {@code thisFac*thisLin+otherFac*otherLin} with coefficients in their lowest form
   */
  public static Equation mulAdd (BigInt thisFac, Equation thisLin, BigInt otherFac,
      Equation otherLin) {
    Term[] ts = weightedSum(thisFac, thisLin.terms, otherFac, otherLin.terms);
    return new Equation(ts);
  }

  private static Term[] weightedSum (BigInt f1, Term[] ts1, BigInt f2, Term[] ts2) {
    assert !f1.isZero();
    assert !f2.isZero();
    Term[] res = new Term[ts1.length + ts2.length];
    int i = 0;
    int i1 = 0;
    int i2 = 0;
    while (i1 < ts1.length && i2 < ts2.length) {
      int varDiff = ts1[i1].id - ts2[i2].id;
      if (varDiff < 0)
        res[i++] = ts1[i1++].mul(f1);
      else if (varDiff > 0)
        res[i++] = ts2[i2++].mul(f2);
      else {
        int var = ts1[i1].id;
        BigInt coeff = ts1[i1++].coeff.mul(f1).add(ts2[i2++].coeff.mul(f2));
        if (!coeff.isZero())
          res[i++] = new Term(coeff, var);
      }
    }
    while (i1 < ts1.length)
      res[i++] = ts1[i1++].mul(f1);
    while (i2 < ts2.length)
      res[i++] = ts2[i2++].mul(f2);
    if (i < res.length) {
      res = Arrays.copyOf(res, i);
    }
    return res;
  }


  public Equation transformForAffineHullFirst (int lambda) {
    int shift = lambda + 1;
    Term[] result = Arrays.copyOf(terms, 2 * terms.length);
    int ctIdx = getConstIdx();
    int i = terms.length;
    if (ctIdx != -1) {
      result[ctIdx] = new Term(terms[ctIdx].coeff.negate(), constVar);
      result[i++] = new Term(terms[ctIdx].coeff, lambda);
    }
    for (Term t : terms)
      if (t.id != constVar)
        result[i++] = new Term(t.coeff.negate(), t.id + shift);
    assert i == 2 * terms.length;
    return new Equation(result);
  }

  public Equation transformForAffineHullSecond (int lambda) {
    int cIdx = getConstIdx();
    if (cIdx == -1)
      return this;
    Term[] result = new Term[terms.length];
    int i = 0;
    for (Term t : terms)
      if (t.id != constVar)
        result[i++] = t;
    result[terms.length - 1] = new Term(terms[cIdx].coeff, lambda);
    return new Equation(result);
  }

  public Equation transformForAffineHullBack (int lambda) {
    int shift = lambda + 1;
    int min = getKey();
    if (min <= lambda)
      return null;
    Term[] result = new Term[terms.length];
    int i = 0;
    for (Term t : terms) {
      if (t.id == constVar)
        result[i++] = t;
      else
        result[i++] = new Term(t.coeff, t.id - shift);
    }
    sortTerms(result);
    return new Equation(result);
  }

  @Override public int compareTo (Equation e) {
    return Integer.signum(getKey() - e.getKey());
  }

  // FIXME define hashCode that respects contract
  @Override public boolean equals (Object obj) {
    if (obj == null)
      return false;
    Equation e = (Equation) obj;
    if (terms.length != e.terms.length)
      return false;
    for (int i = 0; i < terms.length; i++)
      if (terms[i].id != e.terms[i].id ||
        !terms[i].coeff.isEqualTo(e.terms[i].coeff))
        return false;
    return true;
  }

  @Override public String toString () {
    String c = "";
    String res = "";
    for (Term t : terms) {
      String coeff = (t.coeff.isNegative() ? "" : "+") + t.coeff.toString();
      if (t.id == 0)
        c = coeff;
      else
        res = res + coeff + "x" + t.id;
    }
    return res + c;
  }
}
