package javalx.numeric;

import java.util.NoSuchElementException;

public abstract class Bound implements Comparable<Bound> {
  public static final Bound NEGINF = new NegInf();
  public static final Bound POSINF = new PosInf();
  public static final BigInt MINUSONE = BigInt.of(-1);
  public static final BigInt ZERO = BigInt.of(0);
  public static final BigInt ONE = BigInt.of(1);
  public static final BigInt TWO = BigInt.of(2);
  public static final BigInt EIGHT = BigInt.of(8);

  public BigInt asInteger () {
    throw new NoSuchElementException("Unbounded values cannot be casted to integers");
  }

  public final boolean isFinite () {
    return this instanceof BigInt;
  }

  public abstract Bound max (Bound other);

  public abstract Bound max (BigInt other);

  public abstract Bound min (Bound other);

  public abstract Bound min (BigInt other);

  public abstract Bound add (Bound other);

  public abstract Bound add (BigInt other);

  public Bound sub (Bound other) {
    return add(other.negate());
  }

  public abstract Bound mul (Bound other);

  /**
   * Divide this bound by another bound. Uses integer division that rounds up (towards +infinity).
   */
  public abstract Bound divRoundUp (BigInt other);

  /**
   * Divide this bound by another bound. Uses integer division that rounds down(towards -infinity).
   */
  public abstract Bound divRoundDown (Bound other);

  /**
   * Divide this bound by another bound. Uses integer division that rounds towards 0.
   */
  public abstract Bound divRoundZero (Bound other);

  /**
   * Divide this bound by another bound. Uses integer division that rounds to the nearest integer.
   */
  public abstract Bound divRoundNearest (Bound other);

  /**
   * Divide this bound by another bound. Uses integer division that does not round, thus if the division cannot
   * be performed exactly {@code null} is returned.
   */
  public abstract Bound divRoundNone (Bound other);

  public abstract Bound shl (Bound other);

  public abstract Bound negate ();

  public abstract int sign ();

  public final boolean isNegative () {
    return sign() < 0;
  }

  public final boolean isZero () {
    return sign() == 0;
  }

  public final boolean isPositive () {
    return sign() > 0;
  }

  @Override public abstract int compareTo (Bound other);

  public abstract int compareTo (BigInt other);

  public final boolean isEqualTo (Bound other) {
    return compareTo(other) == 0;
  }

  public final boolean isLessThan (Bound other) {
    return compareTo(other) < 0;
  }

  public final boolean isLessThanOrEqualTo (Bound other) {
    return compareTo(other) <= 0;
  }

  @Override public final boolean equals (Object obj) {
    return obj instanceof Bound && isEqualTo((Bound) obj);
  }

  @Override public abstract int hashCode ();
}
