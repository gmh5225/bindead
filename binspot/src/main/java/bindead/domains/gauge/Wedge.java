package bindead.domains.gauge;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.mulAdd;
import static javalx.data.Option.none;
import static javalx.data.Option.some;
import static javalx.data.products.P2.tuple2;
import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domains.affine.Substitution;

/**
 * @author sgreben
 */
class Wedge {
  private final P2<Option<Linear>, Option<Linear>> bounds;

  private final boolean is_empty;

  private static final P2<Option<Linear>, Option<Linear>> EMPTY_bounds =
    tuple2(some(linear(BigInt.of(1))),
        some(linear(BigInt.of(0))));
  /** EMPTY: 1 <= x <= 0 */
  public static final Wedge EMPTY =
    new Wedge(EMPTY_bounds);

  private static final Option<Linear> NoneLinear = none();
  /** FULL: -oo <= x <= +oo */
  public static final Wedge FULL =
    new Wedge(tuple2(NoneLinear, NoneLinear));


  public static final Wedge wedge (P2<Option<Linear>, Option<Linear>> bounds) {
    return new Wedge(bounds);
  }

  public Wedge (P2<Option<Linear>, Option<Linear>> bounds) {
    this.bounds = bounds;
    this.is_empty = hasLowerBound() && hasUpperBound() &&
      !leq(lower(), upper());
    if (hasLowerBound() && lower() == null)
      throw new IllegalArgumentException("WHAT THE FUCK.");
    /**
     * Convert empty wedges into the "normal form" 1<=x<=0 (= Wedge.EMPTY)
     * TODO: check if this is reasonable
     */
    // FIXME hsi: shouldn't that be "this.bounds = ..."?
    if (this.is_empty)
      bounds = EMPTY_bounds;
  }

  public VarSet getVars () {
    VarSet lowerVars = VarSet.empty(), upperVars = VarSet.empty();
    if (hasLowerBound())
      lowerVars = lower().getVars();
    if (hasUpperBound())
      upperVars = upper().getVars();
    return lowerVars.union(upperVars);
  }

  public boolean hasLowerBound () {
    return bounds._1().isSome();
  }

  public boolean hasUpperBound () {
    return bounds._2().isSome();
  }

  /**
   * Returns the lower bound if it exists,
   * null if it does not.
   */
  public Linear lower () {
    return bounds._1().getOrElse(null);
  }

  public Option<Linear> lowerOption () {
    return bounds._1();
  }

  /**
   * Returns the upper bound if it exists,
   * null if it does not.
   */
  public Linear upper () {
    return bounds._2().getOrElse(null);
  }

  public Option<Linear> upperOption () {
    return bounds._2();
  }

  public boolean isSingleton () {
    return hasLowerBound() && hasUpperBound() &&
      lower().isConstantOnly() &&
      lower().equals(upper());
  }

  public Wedge add (Wedge other) {
    Option<Linear> sumL = none(), sumU = none();
    if (hasLowerBound() && other.hasLowerBound())
      sumL = some(mulAdd(Bound.ONE, lower(),
          Bound.ONE, other.lower()));
    if (hasUpperBound() && other.hasUpperBound())
      sumU = some(mulAdd(Bound.ONE, upper(),
          Bound.ONE, other.upper()));
    return wedge(tuple2(sumL, sumU));
  }

  public Wedge subtract (Wedge other) {
    Option<Linear> sumL = none(), sumU = none();
    if (hasLowerBound() && other.hasUpperBound())
      sumL = some(mulAdd(Bound.ONE, lower(),
          Bound.MINUSONE, other.upper()));
    if (hasUpperBound() && other.hasLowerBound())
      sumU = some(mulAdd(Bound.ONE, upper(),
          Bound.MINUSONE, other.lower()));
    return wedge(tuple2(sumL, sumU));
  }

  public Wedge multiply (Wedge other) {
    if (other.isSingleton()) {
      BigInt c = other.lower().getConstant();
      if (c.compareTo(Bound.ZERO) >= 0)
        return wedge(tuple2(hasLowerBound() ? some(lower().smul(c)) : NoneLinear,
            hasUpperBound() ? some(upper().smul(c)) : NoneLinear));
      else
        return wedge(tuple2(hasUpperBound() ? some(upper().smul(c)) : NoneLinear,
            hasLowerBound() ? some(lower().smul(c)) : NoneLinear));
    } else if (this.isSingleton())
      return other.multiply(this);
    else
      return Wedge.FULL;
  }


  /** Axel's correct increment function. */
  public Wedge inc (NumVar lambda, BigInt k) {
    if (!hasLowerBound() && !hasUpperBound())
      return FULL;
    else if (hasLowerBound() && !hasUpperBound()) {
      Linear l = lower();
      return wedge(tuple2(some(l.dropConstant().add(l.getConstant().sub(k.mul(l.getCoeff(lambda))))),
          Option.<Linear>none()));
    }
    else if (!hasLowerBound() && hasUpperBound()) {
      Linear u = upper();
      return wedge(tuple2(Option.<Linear>none(),
          some(u.dropConstant().add(u.getConstant().sub(k.mul(u.getCoeff(lambda)))))));
    }
    else /* if (hasLowerBound() && hasUpperBound()) */{
      Linear l = lower();
      Linear u = upper();
      BigInt lc = l.getConstant().sub(k.mul(l.getCoeff(lambda)))
          .min(u.getConstant().sub(k.mul(u.getCoeff(lambda))));
      BigInt uc = l.getConstant().sub(k.mul(l.getCoeff(lambda)))
          .max(u.getConstant().sub(k.mul(u.getCoeff(lambda))));
      return wedge(tuple2(some(l.dropConstant().add(lc)),
          some(u.dropConstant().add(uc))));
    }
  }


  /**
   * Check whether the linear expression [small] is (coefficient-wise) less than [big],
   * the flags [smallUpper] and [bigUpper] give the interpretation of missing bounds:
   * xUpper => (x.isNone() => x interpreted as +Infinity) and
   * not(xUpper) => (x.isNone() => x interpreted as -Infinity)
   *
   * @param smallUpper
   * @param small
   * @param bigUpper
   * @param big
   * @return
   */
  private static boolean leq (boolean smallUpper, Option<Linear> small, boolean bigUpper, Option<Linear> big) {
    if (small.isSome() && big.isSome())
      return leq(small.get(), big.get());
    if (smallUpper && bigUpper)
      if (small.isNone() && big.isSome())
        return false;
    if (smallUpper && !bigUpper)
      if (big.isNone())
        return false;
    if (!smallUpper && !bigUpper)
      if (small.isSome() && big.isNone())
        return false;
    return true;
  }

  /**
   * checks whether for all terms a_i x_i in small, b_i x_i in big,
   * a_i <= b_i
   **/
  private static boolean leq (Linear small, Linear big) {
    /**
     * Since the constant is not included in the Term[], check it
     * separately.
     */
    if (small.getConstant().compareTo(big.getConstant()) > 0)
      return false;
    for (NumVar counter : small.getVars().union(big.getVars()))
      if (small.getCoeff(counter).compareTo(big.getCoeff(counter)) > 0)
        return false;
    return true;
  }

  public boolean subsetOrEqualTo (Wedge other) {
    return leq(false, other.lowerOption(), false, lowerOption()) &&
      leq(true, upperOption(), true, other.upperOption());
  }

  public boolean isEmpty () {
    return is_empty;
  }


  /* TODO: Test this
   *  */
  public Wedge partialJoin (Wedge other, VarSet I) {
    Option<Linear> w_lower, w_upper;
    if (hasLowerBound() && other.hasLowerBound())
      w_lower = some(linear(other.lower().getConstant()
          .min(lower().getConstant())));
    else
      w_lower = none();

    if (hasUpperBound() && other.hasUpperBound())
      w_upper = some(linear(other.upper().getConstant()
          .max(upper().getConstant())));
    else
      w_upper = none();

    VarSet Lambda = getVars().union(other.getVars());
    for (NumVar v : Lambda)
      if (I.contains(v)) {
        if (w_lower.isSome())
          w_lower = some(w_lower.get().addTerm(lower().getCoeff(v).min(other.lower().getCoeff(v)), v));
        if (w_upper.isSome())
          w_upper = some(w_upper.get().addTerm(upper().getCoeff(v).max(other.upper().getCoeff(v)), v));
      } else {
        if (w_lower.isSome())
          w_lower = some(w_lower.get().addTerm(lower().getCoeff(v), v));
        if (w_upper.isSome())
          w_upper = some(w_upper.get().addTerm(upper().getCoeff(v), v));
      }
    return wedge(tuple2(w_lower, w_upper));
  }

  public Wedge applySubstitution (Substitution sigma) {
    P2<Option<Linear>, Option<Linear>> new_bounds = tuple2(bounds._1(), bounds._2());
    if (new_bounds._1().isSome()) {
      Linear lb = new_bounds._1().get();
      lb = lb.applySubstitution(sigma);
      new_bounds = tuple2(some(lb), new_bounds._2());
    }
    if (new_bounds._2().isSome()) {
      Linear ub = new_bounds._2().get();
      ub = ub.applySubstitution(sigma);
      new_bounds = tuple2(new_bounds._1(), some(ub));
    }
    return wedge(new_bounds);
  }

  public Wedge applySubstitution (Substitution sigma_l, Substitution sigma_u) {
    P2<Option<Linear>, Option<Linear>> new_bounds = tuple2(bounds._1(), bounds._2());
    if (new_bounds._1().isSome()) {
      Linear lb = new_bounds._1().get();
      lb = lb.applySubstitution(sigma_l);
      new_bounds = tuple2(some(lb), new_bounds._2());
    }
    if (new_bounds._2().isSome()) {
      Linear ub = new_bounds._2().get();
      ub = ub.applySubstitution(sigma_u);
      new_bounds = tuple2(new_bounds._1(), some(ub));
    }
    return wedge(new_bounds);
  }


  /**
   * Widening using linear interpolation:
   * Estimates a counter coefficient via the difference of bound constants.
   * The counter lambda_i changes from u in "this" to v in "other".
   * FIXME: unstructured exceptions
   * */
  public Wedge widenLI (Wedge other, NumVar lambda_i, BigInt u, BigInt v) {
    return widenLI(other, lambda_i, some(u), some(v));
  }

  /* Admissible cases for  w1.widenLI(w2, lambda_i, u?, v?)
   *  u = some, v = T (none), lambda_i occurs in w2, does not occur in w1
   *  u = some, v = some, lambda_i does not occur in w1 or w2
   */
  public Wedge widenLI (Wedge other, NumVar lambda_i, Option<BigInt> u_option, Option<BigInt> v_option) {
    //System.out.println("[WEDGE-Debug]\t"+"widenLI("+this+", "+other+","+lambda_i+","+u_option+","+v_option+")");
    VarSet Lambda1 = getVars(), Lambda2 = other.getVars();
    VarSet Lambda = Lambda1.union(Lambda2);

    // [FIXME] TEMPORARY "SOLUTION": lose precision completely
    // whenever we widen gauges with missing bounds
    if (!(hasLowerBound() && other.hasLowerBound() &&
      hasUpperBound() && other.hasUpperBound()))
      return Wedge.FULL;

    if (u_option.isNone())
      throw new IllegalArgumentException("Called WidenLI[u=T,v=*]");
    if (Lambda1.contains(lambda_i) && Lambda2.contains(lambda_i))
      throw new IllegalArgumentException(
        "Called WidenLI[u!=T,v!=T] with a counter that has nonzero coefficients in both wedges.");
    if (!Lambda1.contains(lambda_i) && v_option.isNone())
      throw new IllegalArgumentException("Called WidenLI[u=*,v=T] where the counter does not occur on the left.");

    Option<Linear> w_lower, w_upper;
    BigInt alpha = null, beta = null, u = u_option.get();

    if (Lambda1.contains(lambda_i) && v_option.isNone()) {
      alpha = other.lower().getCoeff(lambda_i);
      beta = other.upper().getCoeff(lambda_i);
    } else {
      BigInt v = v_option.get();
      if (v.isEqualTo(u))
        throw new IllegalArgumentException("Called WidenLI with v=u");
      BigInt v_sub_u = v.sub(u);

      if (hasLowerBound() && other.hasLowerBound())
        alpha = other.lower().getConstant()
            .sub(lower().getConstant())
            .div(v_sub_u);

      if (hasUpperBound() && other.hasUpperBound()) {
        BigInt[] divrem = other.upper().getConstant()
            .sub(upper().getConstant())
            .divideAndRemainder(v_sub_u);
        if (divrem[1].isEqualTo(Bound.ZERO))
          beta = divrem[0];
        else
          beta = divrem[0].add(Bound.ONE);
      }
    }
    assert alpha != null && beta != null; // XXX why are they != null?
    BigInt alpha0 = lower().getConstant().sub(alpha.mul(u)), beta0 = upper().getConstant().sub(beta.mul(u));
    BigInt c0 = alpha0.min(beta0), d0 = alpha0.max(beta0);
    w_lower = some(linear(c0));
    w_upper = some(linear(d0));

    Lambda = Lambda.add(lambda_i);
    for (NumVar var : Lambda)
      if (var.equalTo(lambda_i)) {
        if (w_lower.isSome() && !alpha.min(beta).isEqualTo(Bound.ZERO))
          w_lower = some(w_lower.get().addTerm(alpha.min(beta), var));
        if (w_upper.isSome() && !alpha.max(beta).isEqualTo(Bound.ZERO))
          w_upper = some(w_upper.get().addTerm(alpha.max(beta), var));
      } else {
        if (w_lower.isSome()) {
          BigInt coeff = lower().getCoeff(var).min(other.lower().getCoeff(var));
          if (!coeff.isEqualTo(Bound.ZERO))
            w_lower = some(w_lower.get().addTerm(coeff, var));
        }
        if (w_upper.isSome()) {
          BigInt coeff = upper().getCoeff(var).max(other.upper().getCoeff(var));
          if (!coeff.isEqualTo(Bound.ZERO))
            w_upper = some(w_upper.get().addTerm(coeff, var));
        }
      }
    //System.out.println("\t~> "+wedge(tuple2(w_lower, w_upper)));
    return wedge(tuple2(w_lower, w_upper));
  }

  public Wedge intervalWiden (Wedge other) {
    //System.out.print(this+".intervalWiden("+other+") = ");
    Wedge result = null;
    if (other.subsetOrEqualTo(this))
      result = this;
    if (!leq(true, other.upperOption(), true, upperOption()) &&
      leq(false, lowerOption(), true, other.upperOption()))
      result = wedge(tuple2(lowerOption(), NoneLinear));
    if (!leq(false, lowerOption(), false, other.lowerOption()) &&
      leq(true, other.upperOption(), true, upperOption()))
      result = wedge(tuple2(NoneLinear, upperOption()));
    if (result == null)
      result = Wedge.FULL;
    //System.out.println(result);
    return result;

  }

  @Override public boolean equals (Object o) {
    if (!(o instanceof Wedge))
      return false;
    Wedge other = (Wedge) o;
    return other.bounds.equals(bounds);
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("W[");
    if (hasLowerBound())
      builder.append(lower());
    else
      builder.append("-oo");
    builder.append(", ");
    if (hasUpperBound())
      builder.append(upper());
    else
      builder.append("+oo");
    builder.append("]");
    return builder.toString();
  }

  public static Wedge singleton (BigInt value) {
    return wedge(tuple2(some(linear(value)), some(linear(value))));
  }

  public Wedge forgetCounter (NumVar lambda, Range lambda_range) {
    if (!getVars().contains(lambda))
      return this;
    else {
      Interval I = lambda_range.convexHull();
      Substitution sigma_id = new Substitution(lambda, linear(lambda), Bound.ONE);
      Substitution sigma_l = sigma_id;
      Substitution sigma_u = sigma_id;

      if (I.low().isFinite())
        sigma_l = new Substitution(lambda, linear(I.low().asInteger()), Bound.ONE);
      if (I.high().isFinite())
        sigma_u = new Substitution(lambda, linear(I.high().asInteger()), Bound.ONE);
      return applySubstitution(sigma_l, sigma_u);
    }
  }

  public Wedge join (Wedge other) {
    return partialJoin(other, this.getVars().union(other.getVars()));
  }

}
