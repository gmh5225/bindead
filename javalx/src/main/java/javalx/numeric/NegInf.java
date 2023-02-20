package javalx.numeric;

/**
 * Arithmetics with infinity are as defined in http://en.wikipedia.org/wiki/Affinely_extended_real_number_system
 */
final class NegInf extends Bound {
  NegInf () {
  }

  @Override public String toString () {
    return "-oo";
  }

  @Override public Bound max (Bound other) {
    return other;
  }

  @Override public Bound max (BigInt other) {
    return other;
  }

  @Override public Bound min (Bound other) {
    return this;
  }

  @Override public Bound min (BigInt other) {
    return this;
  }

  @Override public Bound add (Bound other) {
    if (other.equals(POSINF))
      throw new ArithmeticException(NEGINF + " + " + POSINF + " is not defined.");
    return this;
  }

  @Override public Bound add (BigInt other) {
    return this;
  }

  @Override public Bound mul (Bound other) {
    if (other.isEqualTo(Bound.ZERO))
      return Bound.ZERO;
    if (other.isNegative())
      return POSINF;
    else
      return this;
  }

  private Bound div (Bound other) {
    if (other.isEqualTo(Bound.ZERO))
      throw new ArithmeticException(this + "/0 is not defined.");
    if (other.isNegative())
      return POSINF;
    else
      return NEGINF;
  }

  @Override public Bound divRoundUp (BigInt other) {
    return div(other);
  }

  @Override public Bound divRoundDown (Bound other) {
    return div(other);
  }

  @Override public Bound divRoundZero (Bound other) {
    return div(other);
  }

  @Override public Bound divRoundNearest (Bound other) {
    return div(other);
  }

  @Override public Bound divRoundNone (Bound other) {
    return div(other);
  }

  @Override public Bound shl (Bound other) {
    return this;
  }

  @Override public Bound negate () {
    return POSINF;
  }

  @Override public int sign () {
    return -1;
  }

  @Override public int hashCode () {
    return 3;
  }

  @Override public int compareTo (Bound other) {
    return other instanceof NegInf ? 0 : -1;
  }

  @Override public int compareTo (BigInt other) {
    return -1;
  }

}