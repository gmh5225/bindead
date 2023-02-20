package bindead.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.xml.XmlPrintable;
import bindead.domains.affine.Equation;
import bindead.domains.affine.Substitution;

import com.jamesmurty.utils.XMLBuilder;

public class Linear implements Comparable<Linear>, Iterable<Linear.Term>, XmlPrintable {
  private final Term[] terms;
  private final VarSet variables;
  private static final Term[] EMPTY = new Term[0];
  /**
   * This variable is used as a place-holder for contant values.
   */
  private static final NumVar constVar = new NumVar(-1);
  public static final Linear ZERO = new Linear(EMPTY);
  public static final Linear ONE = linear(Bound.ONE);

  private Linear (Term[] newTerms) {
    if (newTerms.length == 0)
      terms = EMPTY;
    else
      terms = newTerms;
    assert sane();
    VarSet vs = VarSet.empty();
    for (Term t : terms) {
      if (t.id != constVar)
        vs = vs.add(t.id);
    }
    variables = vs;
  }

  private boolean isNormalized () {
    for (Term term : terms) {
      if (term.getCoeff().isZero())
        return false;
    }
    return true;
  }

  public boolean sane () {
    if (!isNormalized())
      return false;
    for (int i = 1; i < terms.length; i++) {
      if (terms[i].id.getStamp() < terms[i - 1].id.getStamp())
        return false;
    }
    return true;
  }

  private static void sortTerms (Term[] termsArray) {
    if (termsArray.length > 0)
      Arrays.sort(termsArray);
  }

  /**
   * Normalize the linear such that the leading variable has a non-negative coefficient.
   * If this linear is only a constant it will not be modified.
   */
  public Linear toEquality () {
    NumVar key = getKey();
    if (key == null)
      return this;
    if (getCoeff(key).isPositive())
      return this.lowestForm();
    Term[] newTerms = new Term[terms.length];
    for (int i = 0; i < terms.length; i++) {
      newTerms[i] = terms[i].negate();
    }
    return new Linear(newTerms).lowestForm();
  }

  public Equation toEquation (HashMap<Integer, NumVar> intToVar) {
    int cIdx = getConstIdx();
    if (cIdx >= 0 && terms[cIdx].coeff.isZero())
      return dropConstant().toEquation(intToVar); // normalize if constant is "0"
    int len = terms.length;
    Equation.Term[] newTerms = new Equation.Term[len];
    int newTermPos = 0;
    if (cIdx >= 0)
      newTerms[newTermPos++] = new Equation.Term(terms[cIdx].coeff, 0);
    for (int i = 0; i < terms.length; i++) {
      if (terms[i].id == constVar)
        continue;
      newTerms[newTermPos++] = new Equation.Term(terms[i].getCoeff(), terms[i].getId().getStamp());
      intToVar.put(terms[i].getId().getStamp(), terms[i].getId());
    }
    return new Equation(newTerms);
  }

  public static Linear linear (Equation eq, HashMap<Integer, NumVar> intToVar) {
    Term[] newTerms = new Term[eq.getTerms().length];
    int newTermsPos = 0;
    for (Equation.Term t : eq.getTerms()) {
      if (t.getId() == 0)
        newTerms[newTermsPos++] = term(t.getCoeff());
      else
        newTerms[newTermsPos++] = term(t.getCoeff(), intToVar.get(t.getId()));
    }
    sortTerms(newTerms);
    return new Linear(newTerms);
  }

  /**
   * Divide this linear expression by the gcd of all its coefficients and the passed-in divisor.
   *
   * @param div the divisor container, may not be {@code null}
   * @return the new linear expression
   */
  public Linear lowestForm (Divisor div) {
    assert div != null;
    BigInt d = findTermsGCD();
    if (d == null)
      return this;
    d = d.gcd(div.divisor);
    if (d.isOne())
      return this;
    div.div(d);
    return divTerms(d);
  }

  /**
   * Divide this linear expression by the gcd of all its coefficients.
   *
   * @return the new linear expression
   */
  public Linear lowestForm () {
    BigInt d = findTermsGCD();
    if (d == null)
      return this;
    if (d.isOne())
      return this;
    return divTerms(d);
  }

  private Linear divTerms (BigInt d) {
    Term[] newTerms = new Term[terms.length];
    for (int i = 0; i < terms.length; i++) {
      newTerms[i] = terms[i].div(d);
    }
    return linear(newTerms);
  }

  // calculate the GCD of coefficient
  private BigInt findTermsGCD () {
    BigInt d = null;
    for (Term t : terms) {
      BigInt coeff = t.coeff.abs();
      if (d == null) {
        d = coeff;
        continue;
      }
      if (d.isOne())
        break;
      d = d.gcd(coeff);
    }
    return d;
  }

  /**
   * Query the coefficient of the variable {@code id} in this linear expression.
   *
   * @param id the variable
   * @return the coefficient in front of this variable
   */
  public BigInt getCoeff (NumVar id) {
    int i = findVarIdx(id);
    if (i < terms.length && terms[i].getId() == id)
      return terms[i].getCoeff();
    return Bound.ZERO;
  }

  /**
   * Return the smallest variable used in this linear expression. This value
   * can be used as a key in data structures.
   *
   * @return the smallest variable or {@code null} if the linear expression is constant
   */
  public NumVar getKey () {
    if (terms.length == 0)
      return null;
    if (terms[0].getId() != constVar)
      return terms[0].getId();
    if (terms.length > 1)
      return terms[1].getId();
    return null;
  }

  /**
   * Get the set of all variables occurring in this expression.
   *
   * @return a set of variables
   */
  public VarSet getVars () {
    return variables;
  }

  /**
   * @return {@code true} if this linear expression contains the given variable.
   */
  public boolean contains (NumVar variable) {
    return getVars().contains(variable);
  }

  /**
   * Return {@code true} if the constant term in this linear is 0 and not stored as coeff * var.
   */
  private boolean constantIsImplicit () {
    return getConstIdx() == -1;
  }

  /**
   * Return the index into the terms array of the constant or -1 if the constant is implicitly 0.
   */
  private int getConstIdx () {
    int i = 0;
    while (i < terms.length && terms[i].getId() != constVar)
      i++;
    if (i < terms.length)
      return i;
    return -1;
  }

  /**
   * Check if this constraint only consists of constant ZERO and no variable.
   *
   * @return {@code true} if there are no variables in this linear expression and the constant coefficient is 0
   */
  public boolean isZero () {
    if (constantIsImplicit() && terms.length == 0)
      return true;
    if (terms.length != 1)
      return false;
    Term constTerm = terms[0];
    return constTerm.coeff.isZero();
  }

  /**
   * Check if this constraint only consists of a constant and no variable.
   *
   * @return {@code true} if there are no variables in this linear expression
   */
  public boolean isConstantOnly () {
    return constantIsImplicit() ? terms.length == 0 : terms.length == 1;
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
   * Check if this constraint consists of only a single variable and possibly a constant.
   *
   * @return {@code true} if there is exactly one variable in this linear expression
   */
  public boolean isSingleTerm () {
    return constantIsImplicit() ? terms.length == 1 : terms.length == 2;
  }

  /**
   * If the linear expression consists of a single variable with a coefficient of one, return it.
   *
   * @return the variable or {@code null}
   */
  public NumVar getSingleVarOrNull () {
    if (constantIsImplicit() && terms.length == 1 && terms[0].coeff.isOne())
      return terms[0].getId();
    return null;
  }

  /**
   * Multiply this linear expression by a factor. Note that if this linear
   * expression is normalized (the leading coefficient is positive) then
   * multiplying by a scalar is identical to multiplication by the absolute of
   * the scalar.
   *
   * @param scalar the factor
   * @return the new scaled linear expression
   */

  public Linear smul (BigInt scalar) {
    if (scalar.isZero())
      return ZERO;
    if (scalar.isOne())
      return this;
    final Term[] ts = new Term[terms.length];
    for (int i = 0; i < terms.length; i++) {
      ts[i] = terms[i].mul(scalar);
    }
    return new Linear(ts);
  }

  public Linear add (Linear offset) {
    return mulAdd(Bound.ONE, this, Bound.ONE, offset);
  }

  public Linear sub (Linear offset) {
    return mulAdd(Bound.ONE, this, Bound.MINUSONE, offset);
  }

  public Linear sub (BigInt offset) {
    return add(offset.negate());
  }

  public Linear add (BigInt offset) {
    if (offset.isZero())
      return this;
    return addTerm(offset, constVar);
  }

  public Linear sub (NumVar var) {
    return addTerm(Bound.MINUSONE, var);
  }

  public Linear add (NumVar var) {
    return addTerm(Bound.ONE, var);
  }



  public Linear subTerm (BigInt coeff, NumVar var) {
    return addTerm(coeff.negate(), var);
  }

  /**
   * Construct a linear expression from an existing linear expression and a some term.
   *
   * @param coeff the coefficient of the term to be added
   * @param var the variable of the term
   * @return a new linear expression with the given term
   */
  public Linear addTerm (BigInt coeff, NumVar var) {
    if (coeff.isZero())
      return this;

    // check if var occurs in this linear expression
    int prefixEnd = findVarIdx(var);

    // determine if the term is new or if it merges or cancels with another term
    if (prefixEnd < terms.length && terms[prefixEnd].getId().equalTo(var)) {
      BigInt newCoeff = terms[prefixEnd].getCoeff().add(coeff);
      if (newCoeff.isZero()) {
        return new Linear(dropTermAt(prefixEnd));
      } else {
        final Term newTerm = term(newCoeff, var);
        return new Linear(replaceTermAt(prefixEnd, newTerm));
      }
    } else {
      final Term newTerm = term(coeff, var);
      return new Linear(insertTermAt(prefixEnd, newTerm));
    }
  }

  public Linear negate () {
    return smul(BigInt.MINUSONE);
  }

  private Term[] insertTermAt (int position, final Term newTerm) {
    Term[] newTerms = new Term[terms.length + 1];
    System.arraycopy(terms, 0, newTerms, 0, position);
    newTerms[position] = newTerm;
    System.arraycopy(terms, position, newTerms, position + 1, terms.length - position);
    return newTerms;
  }

  private Term[] replaceTermAt (int position, final Term newTerm) {
    Term[] newTerms = Arrays.copyOf(terms, terms.length);
    newTerms[position] = newTerm;
    return newTerms;
  }

  /**
   * Construct a linear expression without the given variable. The result is
   * never normalized.
   *
   * @param var the variable that is to be removed
   * @return the expression without the variable (or this expression if the
   *         variable did not occur in the set of terms)
   */
  public Linear dropTerm (NumVar var) {
    int idx = findVarIdx(var);
    if (idx == terms.length || !terms[idx].id.equalTo(var))
      return this;

    Term[] newTerms = dropTermAt(idx);
    return new Linear(newTerms);
  }

  private Term[] dropTermAt (int idx) {
    Term[] newTerms = new Term[terms.length - 1];
    System.arraycopy(terms, 0, newTerms, 0, idx);
    System.arraycopy(terms, idx + 1, newTerms, idx, terms.length - idx - 1);
    return newTerms;
  }


  /**
   * Find the insertion position of the given var.
   *
   * @param var the variable to be searched
   * @return the index where the varible is or where it should be inserted
   */
  private int findVarIdx (NumVar var) {
    assert var != null;
    int lower = 0;
    int upper = terms.length;
    while (lower < upper) {
      int mid = (lower + upper) / 2;
      if (terms[mid].getId().compareTo(var) < 0)
        lower = mid + 1;
      else
        upper = mid;
    }
    return lower;
  }

  /**
   * Return a linear expression which has the constant set to 0. See
   * #dropTerm(int).
   *
   * @return the new linear expression which is not necessarily normalized
   */
  public Linear dropConstant () {
    return dropTerm(constVar);
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
  public static Linear mulAdd (BigInt thisFac, Linear thisLin, BigInt otherFac, Linear otherLin) {
    return weightedSum(thisFac, thisLin.terms, otherFac, otherLin.terms);
  }

  private static Linear weightedSum (BigInt f1, Term[] ts1, BigInt f2, Term[] ts2) {
    assert !f1.isZero();
    assert !f2.isZero();
    Term[] res = new Term[ts1.length + ts2.length];
    int i = 0;
    int i1 = 0;
    int i2 = 0;
    while (i1 < ts1.length && i2 < ts2.length) {
      int varDiff = ts1[i1].id.compareTo(ts2[i2].id);
      if (varDiff < 0)
        res[i++] = ts1[i1++].mul(f1);
      else if (varDiff > 0)
        res[i++] = ts2[i2++].mul(f2);
      else {
        NumVar var = ts1[i1].id;
        BigInt coeff = ts1[i1++].coeff.mul(f1).add(ts2[i2++].coeff.mul(f2));
        if (!coeff.isZero())
          res[i++] = term(coeff, var);
      }
    }
    while (i1 < ts1.length)
      res[i++] = ts1[i1++].mul(f1);
    while (i2 < ts2.length)
      res[i++] = ts2[i2++].mul(f2);
    if (i < res.length) {
      res = Arrays.copyOf(res, i);
    }
    return new Linear(res);
  }

  /**
   * Create a substitution {@code [k / (exp/-d)]} from a linear expression {@code d*k+exp}.
   *
   * @param kill the variable k
   * @return the substitution replacing k
   */
  public Substitution genSubstitution (NumVar kill) {
    assert getVars().contains(kill) : "The linear expression must contain the variable to substitute.";
    BigInt varFac = null;
    Term[] newTs = new Term[terms.length - 1];
    for (int i = 0, j = 0; i < terms.length; i++) {
      if (terms[i].getId() != kill)
        newTs[j++] = terms[i];
      else
        varFac = terms[i].coeff;
    }
    assert varFac != null;
    return new Substitution(kill, new Linear(newTs), varFac.negate());
  }

  /**
   * Create a substitution {@code [k / (exp/-d)]} from a linear expression {@code d*k+exp}.
   *
   * @param kill the variable k
   * @return the substitution replacing k or null if this linear expression does not contain k.
   */
  public Substitution genSubstitutionOrNull (NumVar kill) {
    if (!getVars().contains(kill))
      return null;
    return genSubstitution(kill);
  }

  public Linear applySubstitution (NumVar y, NumVar x) {
    BigInt coeff = getCoeff(x);
    Linear newLin = dropTerm(x);
    newLin = newLin.addTerm(coeff, y);
    return newLin;
  }

  /**
   * Apply a substitution to this linear expression. If the variable to be substituted is not part of this
   * linear expression then it will return the unchanged expression.
   *
   * @param subst the substitution
   * @return the new linear expression
   */
  public Linear applySubstitution (Substitution subst) {
    NumVar var = subst.getVar();
    int thisIdx = 0;
    while (thisIdx < terms.length && terms[thisIdx].id != var)
      thisIdx++;
    if (thisIdx == terms.length)
      return this;

    BigInt thisFac = terms[thisIdx].coeff;
    Term[] theseTerms = dropTermAt(thisIdx);

    Linear ts;
    if (!subst.getFac().isNegative())
      ts = weightedSum(subst.getFac(), theseTerms, thisFac, subst.getExpr().terms);
    else
      // looks clumsy, but avoids multiplying the result with a negative coefficient
      ts = weightedSum(subst.getFac().negate(), theseTerms, thisFac.negate(), subst.getExpr().terms);
    return ts;
  }

  private Term[] lazyReplaceAt (int idx, NumVar newVarName, Term[] newTerms) {
    if (newTerms == null)
      newTerms = Arrays.copyOf(terms, terms.length);
    newTerms[idx] = term(newTerms[idx].coeff, newVarName);
    return newTerms;
  }

  /**
   * Create a new linear expression in which several variables have been renamed.
   *
   * @param vars list of pairs of variables that states which variable should be renamed
   *          to which other, the ordering within the pair does not matter
   * @return the linear expression with all possible renamings applied of {@code null} if none of the pairs matched
   */
  public Linear renameVar (FoldMap vars) {
    Term[] newTerms = null;
    Term[] oldTerms = terms;
    for (VarPair vp : vars) {
      NumVar one = vp.getEphemeral();
      NumVar two = vp.getPermanent();
      assert one != two;
      for (int idx = 0; idx < terms.length; idx++) {
        if (oldTerms[idx].id == one) {
          newTerms = lazyReplaceAt(idx, two, newTerms);
          oldTerms = newTerms;
        } else if (oldTerms[idx].id == two) {
          newTerms = lazyReplaceAt(idx, one, newTerms);
          oldTerms = newTerms;
        }
      }
    }
    if (newTerms != null) {
      Linear.sortTerms(newTerms);
      return new Linear(newTerms);
    }
    return null;
  }

  public static BigInt num (long c) {
    return BigInt.of(c);
  }

  private static Term term (BigInt constant) {
    return term(constant, constVar);
  }

  /**
   * Construct a term with coefficient one.
   *
   * @param id the variable identifier
   * @return the new term
   */
  public static Term term (NumVar id) {
    return term(Bound.ONE, id);
  }

  /**
   * Construct a term.
   *
   * @param coeff the coefficient
   * @param id the variable identifier
   * @return the new term
   */
  public static Term term (BigInt coeff, NumVar id) {
    return new Term(coeff, id);
  }

  /**
   * Construct a linear expression that is just a constant.
   *
   * @param constant the value of the linear expression, see {@code num}
   * @return a new linear expression
   */
  public static Linear linear (BigInt constant) {
    if (constant.isZero())
      return ZERO;
    return linear(term(constant));
  }

  /**
   * Construct a linear expression from a single variable.
   *
   * @param variable The variable.
   * @return A linear expression.
   */
  public static Linear linear (NumVar variable) {
    return linear(term(variable));
  }

  public static Linear linear (BigInt coefficient, NumVar variable) {
    return linear(term(coefficient, variable));
  }

  /**
   * Construct a linear expression with no constant offset.
   *
   * @param terms the terms, see {@code term}
   * @return a linear expression
   */
  public static Linear linear (Term... terms) {
    sortTerms(terms);
    Term[] result = new Term[terms.length];
    int j = -1;
    for (int i = 0; i < terms.length; i++) {
      if (j > -1 && terms[i].getId() == result[j].getId()) {
        BigInt coeff = result[j].getCoeff().add(terms[i].getCoeff());
        if (coeff.isZero())
          result[j--] = null;
        else
          result[j] = term(coeff, result[j].getId());
      } else {
        BigInt coeff = terms[i].getCoeff();
        if (!coeff.isZero())
          result[++j] = terms[i];
      }
    }
    if (++j < result.length) {
      result = Arrays.copyOf(result, j);
    }
    return new Linear(result);
  }

  /**
   * Construct a linear expression from a constant {@code c0} and several terms {@code c1*x1, ..., cn*xn}.
   *
   * @param constant the constant, see {@code num}
   * @param terms the terms, see {@code term}
   * @return a linear expression
   */
  public static Linear linear (BigInt constant, Term... terms) {
    return linear(terms).add(constant);
  }

  /**
   * Calculate the difference in the support set of the two given expressions.
   *
   * @param pos the linear expression whose variables lead to positive entries
   * @param neg the linear expression whose variables lead to negative entries
   * @return a list of indices {@code <b,i>} such that {@code i} occurs only in
   *         the {@code pos} linear expression if {@code b} is {@code false} and {@code i} occurs only in the {@code neg}
   *         linear
   *         expression if {@code b} is {@code true}.
   */
  public static Vector<P2<Boolean, NumVar>> diffVars (Linear pos, Linear neg) {
    Vector<P2<Boolean, NumVar>> diff = new Vector<P2<Boolean, NumVar>>(pos.terms.length + neg.terms.length);
    int pIdx = 0;
    int nIdx = 0;
    while (true) {
      if (nIdx == neg.terms.length) {
        while (pIdx < pos.terms.length) {
          if (pos.terms[pIdx].id == constVar)
            pIdx++;
          else
            diff.add(P2.<Boolean, NumVar>tuple2(Boolean.FALSE, pos.terms[pIdx++].id));
        }
        break;
      }
      if (pIdx == pos.terms.length) {
        while (nIdx < neg.terms.length) {
          if (neg.terms[nIdx].id == constVar)
            nIdx++;
          else
            diff.add(P2.<Boolean, NumVar>tuple2(Boolean.TRUE, neg.terms[nIdx++].id));
        }
        break;
      }
      if (pos.terms[pIdx].id == constVar)
        pIdx++;
      else if (neg.terms[nIdx].id == constVar)
        nIdx++;
      else if (pos.terms[pIdx].id == neg.terms[nIdx].id) {
        pIdx++;
        nIdx++;
      } else if (pos.terms[pIdx].id.compareTo(neg.terms[nIdx].id) < 0)
        diff.add(P2.<Boolean, NumVar>tuple2(Boolean.FALSE, pos.terms[pIdx++].id));
      else
        diff.add(P2.<Boolean, NumVar>tuple2(Boolean.TRUE, neg.terms[nIdx++].id));
    }
    return diff;
  }

  /**
   * Eliminate the given variables from this equality system.
   *
   * @param vars the variables to be removed
   * @return a set of equations that need to be executed as assignments on the child domain
   */
  public static Vector<Linear> eliminate (Vector<Linear> eqs, VarSet vars) {
    Vector<Linear> res = new Vector<Linear>(vars.size());

    for (int i = eqs.size() - 1; i >= 0; i--) {
      Linear eq = eqs.get(i);
      VarSet toKillVars = eq.getVars().intersection(vars);
      if (toKillVars.size() != 0) {
        NumVar toKill = toKillVars.get(toKillVars.size() - 1);
        eqs.set(i, null);
        res.add(eq);
        Substitution subst = eq.genSubstitution(toKill);
        for (int j = 0; j < eqs.size(); j++) {
          Linear cur = eqs.get(j);
          if (cur == null)
            continue;
          eqs.set(j, cur.applySubstitution(subst));
        }
      }
    }
    int lastIdx = 0;
    for (int i = 0; i < eqs.size(); i++) {
      Linear cur = eqs.get(i);
      if (cur == null)
        continue;
      eqs.set(lastIdx++, cur);
    }
    eqs.setSize(lastIdx);
    return res;
  }

  public static Linear sumOf (VarSet vars) {
    Term[] ts = new Term[vars.size()];
    int i = 0;
    for (NumVar v : vars)
      ts[i++] = term(v);
    return new Linear(ts);
  }

  /**
   * A class containing a mutable number that denotes a divisor. This class
   * may be passed to {@link #mulAdd} to retrieve the divisor that was used to
   * bring the coefficients into their lowest form.
   */
  public static class Divisor {
    private BigInt divisor;

    /**
     * Create a divisor with an initial value.
     *
     * @param fac the initial divisor
     */
    public Divisor (BigInt fac) {
      divisor = fac;
    }

    /**
     * Create a divisor storage which is set to one.
     */
    public static Divisor one () {
      return new Divisor(Bound.ONE);
    }

    /**
     * Retrieve the stored divisor.
     *
     * @return the divisor
     */
    public BigInt get () {
      return divisor;
    }

    public void mul (BigInt c) {
      divisor = divisor.mul(c);
    }

    private void div (BigInt c) {
      divisor = divisor.div(c);
    }

    @Override public String toString () {
      return divisor.toString();
    }

    public void negate () {
      divisor = divisor.negate();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (divisor == null ? 0 : divisor.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Divisor))
        return false;
      Divisor other = (Divisor) obj;
      if (divisor == null) {
        if (other.divisor != null)
          return false;
      } else if (!divisor.equals(other.divisor))
        return false;
      return true;
    }

  }

  public static final class Term implements Comparable<Term> {
    private final NumVar id;
    private final BigInt coeff;

    private Term (BigInt coeff, NumVar id) {
      this.id = id;
      this.coeff = coeff;
    }

    public boolean isConstTerm () {
      return getId() == constVar;
    }

    public NumVar getId () {
      return id;
    }

    public BigInt getCoeff () {
      return coeff;
    }

    public Term mul (BigInt scalar) {
      return term(coeff.mul(scalar), id);
    }

    @Override public String toString () {
      String result;
      final String idstring = id.toString();
      if (coeff.isOne())
        result = idstring;
      else if (coeff.isEqualTo(Bound.MINUSONE))
        result = "-" + idstring;
      else
        result = coeff + idstring;
      return result;
    }

    /**
     * Compare two terms lexicographically, that is, considering both, variable and coefficient.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override public int compareTo (Term other) {
      int cmp = id.compareTo(other.id);
      if (cmp == 0)
        return coeff.compareTo(other.coeff);
      return cmp;
    }

    Term div (BigInt d) {
      return term(getCoeff().div(d), getId());
    }

    Term negate () {
      return term(getCoeff().negate(), getId());
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (coeff == null ? 0 : coeff.hashCode());
      result = prime * result + (id == null ? 0 : id.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Term))
        return false;
      Term other = (Term) obj;
      if (coeff == null) {
        if (other.coeff != null)
          return false;
      } else if (!coeff.equals(other.coeff))
        return false;
      if (id == null) {
        if (other.id != null)
          return false;
      } else if (!id.equalTo(other.id))
        return false;
      return true;
    }

  }

  private class TermIter implements Iterator<Term> {
    int idx;

    public TermIter () {
      idx = 0;
    }

    @Override public boolean hasNext () {
      if (idx >= terms.length)
        return false;
      if (idx == terms.length - 1)
        return terms[idx].getId() != constVar;
      return true;
    }

    @Override public Term next () {
      while (terms[idx].getId() == constVar) {
        idx++;
      }
      return terms[idx++];
    }

    @Override public void remove () {
      throw new UnsupportedOperationException("Terms cannot be removed from a Linear expression using an iterator");
    }
  }

  /**
   * Return all the terms (coeff * var) in this linear. The constant term is not returned!
   */
  @Override public Iterator<Term> iterator () {
    return new TermIter();
  }

  /**
   * Retrieves an array of coefficient, variable pairs. The constant term is not returned!
   *
   * @return an array of the non-constant terms of this linear expression which must not be modified.
   */
  public Term[] getTerms () {
    if (constantIsImplicit())
      return terms;
    Term[] newTs = new Term[terms.length - 1];
    int j = 0;
    for (Term t : terms)
      if (!t.isConstTerm())
        newTs[j++] = t;
    assert j == newTs.length;
    return newTs;
  }

  @Override public int compareTo (Linear l) {
    int cmp = Integer.signum(terms.length - l.terms.length);
    if (cmp != 0)
      return cmp;
    for (int i = 0; i < terms.length; i++) {
      cmp = terms[i].compareTo(l.terms[i]);
      if (cmp != 0)
        return cmp;
    }
    return 0;
  }

  @Override public boolean equals (Object o) {
    if (!(o instanceof Linear))
      return false;
    Linear l = (Linear) o;
    Linear result = sub(l);
    return result.terms.length == 0;
  }

  @Override public int hashCode () {
    int hash = 7;
    hash = 37 * hash + Arrays.deepHashCode(this.terms);
    return hash;
  }

  @Override public String toString () {
    return toTermsString();
  }

  /**
   * Pretty prints this linear as a sum of its terms e.g. {@code ax+by+...+c}
   *
   * @return The pretty printed linear.
   */
  public String toTermsString () {
    if (terms.length == 0)
      return "0";
    StringBuilder builder = new StringBuilder();
    BigInt constant = Bound.ZERO;
    boolean printedFirstVariable = false;
    boolean printConst = false;
    for (Term t : terms) {
      if (t.getId() == constVar) {
        constant = t.getCoeff();
        if (!constant.isZero())
          printConst = true;
      } else {
        if (!t.getCoeff().isNegative() && printedFirstVariable)
          builder = builder.append("+");
        builder = builder.append(t.toString());
        printedFirstVariable = true;
      }
    }
    if (printConst) {
      if (!constant.isNegative() && !isConstantOnly())
        builder.append("+");
      builder = builder.append(constant);
    }
    return builder.toString();
  }

  /**
   * Pretty prints this linear as an equation of the form {@code ax+by+...=c}
   *
   * @return The pretty printed linear.
   */
  public String toEquationString () {
    StringBuilder builder = new StringBuilder();
    if (terms.length == 0) {
      builder.append("0");
    } else {
      BigInt constant = Bound.ZERO;
      boolean printedFirstVariable = false;
      for (Term term : terms) {
        if (term.getId() == constVar) {
          constant = term.getCoeff();
        } else {
          if (!term.getCoeff().isNegative() && printedFirstVariable)
            builder = builder.append("+");
          builder = builder.append(term.toString());
          printedFirstVariable = true;
        }
      }
      builder.append("=");
      builder.append(constant.negate());
    }
    return builder.toString();
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    for (Term term : this) {
      builder = builder.e("Term")
          .e("Variable")
          .t(term.getId().toString())
          .up()
          .e("Coefficient")
          .t(term.getCoeff().toString());
      builder = builder.up().up();
    }
    builder = builder.e("Constant").t(getConstant().toString()).up();
    return builder;
  }
}
