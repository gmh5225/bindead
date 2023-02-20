package javalx.numeric;

import javalx.data.Option;
import javalx.fn.Fn2;

/**
 * A congruence contains the multiplicative scale {@code s} and the offset {@code o} for a variable {@code v},
 * i.e. {@code v * s + o}.
 */
public class Congruence {
  private final BigInt scale;
  private final BigInt offset;
  public static final Congruence ZERO = new Congruence(0, 0);
  public static final Congruence ONE = new Congruence(1, 0);

  public static final Fn2<Congruence, Congruence, Congruence> join = new Fn2<Congruence, Congruence, Congruence>() {
    @Override public Congruence apply (Congruence a, Congruence b) {
      return a.join(b);
    }
  };

  /**
   * A new congruence with the given scale and offset i.e. {@code v * scale + offset}.
   */
  public Congruence (long scale, long offset) {
    this(BigInt.of(scale), BigInt.of(offset));
  }

  /**
   * A new congruence with the given scale and offset i.e. {@code v * scale + offset}.
   */
  public Congruence (BigInt scale, BigInt offset) {
    assert !scale.isNegative();
    // normalization
    if (!scale.isZero()) {
      if (offset.isNegative()) {
        offset = offset.remainder(scale);
        if (offset.isNegative())
          offset = offset.add(scale);
      } else if (!offset.isLessThan(scale)) { // scale greater or equal than offset
        offset = offset.remainder(scale);
      }
    }
    this.offset = offset;
    this.scale = scale;
  }

  public BigInt getOffset () {
    return offset;
  }

  public BigInt getScale () {
    return scale;
  }

  /**
   * Returns {@code true} if the number {@code x} satisfies this congruence, i.e. {@code x % scale = offset}
   */
  public boolean isCongruent (BigInt x) {
    if (scale.isZero())
      return x.isEqualTo(offset);
    Congruence xCongruence = new Congruence(scale, x);
    return xCongruence.equals(this);
  }

  /**
   * Return the number of lower bits of a variable having this congruence.
   */
  public int lowerZeroBits () {
    if (!offset.isZero())
      return 0;
    BigInt x = Bound.TWO;
    while (x.isLessThan(scale))
      x = x.shl(1);
    x = x.gcd(scale);
    return Long.numberOfTrailingZeros(x.longValue());
  }

  public Congruence join (Congruence other) {
    return new Congruence(offset.sub(other.offset).abs().gcd(scale.gcd(other.scale)), offset.min(other.offset));
  }

  /**
   * Return the congrurence of the common elements (intersection) of both if there are any.
   */
  public Option<Congruence> meet (Congruence other) {
    // existence of solution if a1 ≡ a2 (mod gcd (b1, b2))
    // see Chinese Remainder Theorem: http://en.wikipedia.org/wiki/Chinese_remainder_theorem
    if (!new Congruence(scale.gcd(other.scale), other.offset).isCongruent(offset))
      return Option.none();
    // computing the solution using the Solve simultaneous congruences method
    // see Method of Successive Substitution: http://en.wikipedia.org/wiki/Method_of_successive_substitution
    // if a2-a1 < 0, normalize it modulo b2
    BigInt a2_a1 = other.offset.sub(offset);
    if (a2_a1.isNegative() && !other.scale.isZero())
      a2_a1 = a2_a1.remainder(other.scale).add(other.scale);
    BigInt gcd = scale.gcd(other.scale).gcd(a2_a1);
    if (gcd.isZero())
      return Option.some(this);

    Congruence meet = new Congruence(lcm(scale, other.scale), offset.add(scale.mul(a2_a1.div(gcd))
        .mul(modularInverse(scale.div(gcd), other.scale.div(gcd)))));
    return Option.some(meet);
  }

  private static BigInt lcm (BigInt x, BigInt y) {
    if (!x.gcd(y).isZero())
      return x.mul(y).div(x.gcd(y));
    return Bound.ZERO;
  }

  /**
   * Euclidean Modular Multiplicative Inverse
   *          (http://en.wikipedia.org/wiki/Modular_multiplicative_inverse)
   * a1*x + b1*y = 1
   * given a1 and b1, find x - the modular multiplicative inverse of a1 modulo b1
   *
   * Extended Euclidean algorithm
   *          (http://en.wikipedia.org/wiki/Extended_Euclidean_algorithm)
   *
   * Algorithm:
   *  Apply Euclidean algorithm, and let qn(n starts from 1) be a finite list of quotients in the division.
   *  Initialize x0, x1 as 1, 0, and y0, y1 as 0,1 respectively.
   *  Then for each i so long as qi is defined,
   *  Compute xi+1 = xi−1 − qixi
   *  Compute yi+1 = yi−1 − qiyi
   *  Repeat the above after incrementing i by 1.
   *  The answers are the second-to-last of xn and yn.
   */
  private static BigInt modularInverse (BigInt a1, BigInt b1) {
    BigInt x = Bound.ZERO;
    BigInt lastx = Bound.ONE;
    BigInt y = Bound.ONE;
    BigInt lasty = Bound.ZERO;
    BigInt quotient;
    BigInt temp;
    while (!b1.isZero()) {
      quotient = a1.div(b1);
      temp = b1;
      b1 = a1.remainder(b1);
      a1 = temp;

      temp = x;
      x = lastx.sub(x.mul(quotient));
      lastx = temp;

      temp = y;
      y = lasty.sub(y.mul(quotient));
      lasty = temp;
    }
    //
    // // if negative, normalize the inverse lastx modulo b1
    // if (lastx.compareTo(BigInt.ZERO) < 0 && !b1.equals(BigInt.ZERO))
    // lastx = lastx.remainder(b1).add(b1);
    return lastx;
  }

  public boolean subsetOrEqual (Congruence other) {
    if (this.isConstantOnly() && other.isConstantOnly())
      return this.getOffset().isEqualTo(other.getOffset());
    if (this.isConstantOnly() || other.isConstantOnly())
      return true; // needs to be decided in child domain
    // (b2 | gcd(a1-a2, b1))
    return scale.gcd(offset.sub(other.offset).abs()).remainder(other.scale).isZero();
  }

  public boolean isConstantOnly () {
    return scale.isZero();
  }

  public Congruence add (Congruence other) {
    // a1+a2 + gcd(b1, b2)Z
    return new Congruence(scale.gcd(other.scale), offset.add(other.offset));
  }

  public Congruence mul (Congruence other) {
    if (isConstantOnly())
      return other.mul(getOffset());
    if (other.isConstantOnly())
      return mul(other.getOffset());
    // a1*a2 + gcd(a1*b2, a2*b1, b1*b2)Z
    return new Congruence(offset.mul(other.scale).gcd(scale.mul(other.offset)).gcd(scale.mul(other.scale)),
      offset.mul(other.offset));
  }

  public Congruence sub (BigInt c) {
    return new Congruence(scale, offset.sub(c));
  }

  /**
   * Multiply a congruence with a scalar.
   * The sign of the scalar only matters if the congruence is a constant otherwise the absolute value is used as
   * the multiplier. This behavior is useful when evaluating congruences in linear expressions that can contain
   * negative coefficients and we need to change the sign of a constant congruence.
   */
  public Congruence mul (BigInt c) {
    // (a + b Z) * c = (a*c + b*|c| Z)
    return new Congruence(scale.mul(c.abs()), offset.mul(c));
  }

  /**
   * Division of congruence by a constant. Undefined for constant congruences when the divisor is not {@code 1}.
   */
  public Congruence div (BigInt c) {
    if (c.isEqualTo(Bound.ONE))
      return this;
    assert !scale.isZero() : "Division of constant congruences ill defined.";
    BigInt divisor = c.abs();
    if (divisor.isGreaterThan(getScale()))
      return Congruence.ONE;
    BigInt[] dividedScale = scale.divideAndRemainder(divisor);
    BigInt[] dividedOffset= offset.divideAndRemainder(divisor);
    if (dividedScale[1].isZero() && dividedOffset[1].isZero())
      return new Congruence(dividedScale[0], dividedOffset[0]);
    return Congruence.ONE;
  }

  public Congruence shr (BigInt c) {
    if (isConstantOnly())
      return new Congruence(Bound.ZERO, offset.shr(c));
    return div(Bound.TWO.pow(c));
  }

  public Congruence shl (BigInt c) {
    return mul(Bound.TWO.pow(c));
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((offset == null) ? 0 : offset.hashCode());
    result = prime * result + ((scale == null) ? 0 : scale.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Congruence))
      return false;
    Congruence other = (Congruence) obj;
    if (offset == null) {
      if (other.offset != null)
        return false;
    } else if (!offset.isEqualTo(other.offset))
      return false;
    if (scale == null) {
      if (other.scale != null)
        return false;
    } else if (!scale.isEqualTo(other.scale))
      return false;
    return true;
  }

  @Override public String toString () {
    String offsetString = offset.isZero() ? "" : (offset.isNegative() ? "" : "+") + offset.toString();
    return "*" + scale.toString() + offsetString;
  }

}
