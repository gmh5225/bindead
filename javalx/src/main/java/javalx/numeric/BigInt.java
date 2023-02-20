package javalx.numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Rich wrapper for big integers.
 */
public class BigInt extends Bound {
  private final BigInteger value;

  private BigInt (BigInteger value) {
    this.value = value;
  }

  public BigInteger getValue () {
    return value;
  }

  public static BigInt of (long value) {
    return of(BigInteger.valueOf(value));
  }

  public static BigInt of (BigInteger value) {
    return new BigInt(value);
  }

  public static BigInt of (String value, int base) {
    return of(new BigInteger(value, base));
  }

  public static BigInt powerOfTwo (int power) {
    return Bound.ONE.shl(power);
  }

  private boolean isInRange (long lowerBound, long upperBound) {
    return BigInteger.valueOf(lowerBound).compareTo(value) <= 0
      && value.compareTo(BigInteger.valueOf(upperBound)) <= 0;
  }

  public int intValue () {
    long min = Integer.MIN_VALUE;
    long max = Integer.MAX_VALUE;
    if (!isInRange(min, max))
      throw new ArithmeticException("Value " + value
        + " cannot correctly be converted to fit integer range: "
        + min + ", " + max);
    return value.intValue();
  }

  public long longValue () {
    long min = Long.MIN_VALUE;
    long max = Long.MAX_VALUE;
    if (!isInRange(min, max))
      throw new ArithmeticException("Value " + value
        + " cannot correctly be converted to fit long range: "
        + min + ", " + max);
    return value.longValue();
  }

  public BigInteger bigIntegerValue () {
    return value;
  }

  @Override public String toString () {
    return toDecimalString();
  }

  /**
   * Return the twos complement bits string representation with the most significant bit (MSB) being at the left-most
   * position (e.g. 00101 = 5).
   *
   * @param size Denotes the amount of bits and thus string length that should be used to represent the number.<br>
   * If the specified amount of bits is less than the bits necessary to represent the value then the value is truncated
   * by showing only the least significant bits.<br>
   * If the specified amount of bits is more than the bits necessary to represent the value then the value is
   * sign extended to the number of bits requested (e.g. 1011 becomes 11111011 if size=8).
   */
  public String toBinaryStringSigned (int size) {
    return toBinaryString(size, true);
  }

  /**
   * Return the twos complement bits string representation with the most significant bit (MSB) being at the left-most
   * position (e.g. 00101 = 5).
   *
   * @param size Denotes the amount of bits and thus string length that should be used to represent the number.<br>
   * If the specified amount of bits is less than the bits necessary to represent the value then the value is truncated
   * by showing only the least significant bits.<br>
   * If the specified amount of bits is more than the bits necessary to represent the value then the value is
   * sign extended to the number of bits requested (e.g. 1011 becomes 00001011 if size=8).
   */
  public String toBinaryStringUnsigned (int size) {
    return toBinaryString(size, false);
  }

  private String toBinaryString (int size, boolean signExtend) {
    StringBuilder builder = new StringBuilder();
    byte[] internalByteRepresentation = value.toByteArray();
    for (byte part : internalByteRepresentation) {
      builder.append(toBitsString(part));
    }
    // if more bits than used are requested then fill up the remaining part
    if (builder.length() < size) {
      char fillBit = '0';
      if (signExtend && sign() == -1) {
        // the leftmost bit is the sign bit in BigInteger but that is an implementation detail
        // so be here on the safe side by only using the sign extension when we have a sign
        fillBit = builder.charAt(0);
      }
      int paddingLength = size - builder.length();
      for (int i = 0; i < paddingLength; i++) {
        builder.insert(0, fillBit);
      }
    } else if (builder.length() > size) {
      builder = new StringBuilder(builder.substring(builder.length() - size));
    }
    return builder.toString();
  }

  private static String toBitsString (byte val) {
    StringBuilder builder = new StringBuilder();
    byte mask = 1;
    for (int i = 0; i <= 7; i++) {
      if ((val & mask) != (byte) 0)
        builder.insert(0, '1');
      else
        builder.insert(0, '0');
      mask = (byte) (mask << 1);
    }
    return builder.toString();
  }

  public String toDecimalString () {
    return value.toString();
  }

  public String toHexString () {
    return value.toString(16);
  }

  public String toStringUsingRadix (int radix) {
    return value.toString(radix);
  }

  public boolean isLessThan (BigInt other) {
    return value.compareTo(other.value) < 0;
  }

  public boolean isGreaterThan (BigInt other) {
    return value.compareTo(other.value) > 0;
  }

  @Override public BigInt negate () {
    return of(value.negate());
  }

  public BigInt sub (long other) {
    return sub(of(other));
  }

  public BigInt sub (BigInt other) {
    return of(value.subtract(other.value));
  }

  public BigInt mul (long other) {
    return mul(of(other));
  }

  public BigInt mul (BigInt other) {
    return of(value.multiply(other.value));
  }

  public BigInt pow (int exponent) {
    return of(value.pow(exponent));
  }

  public BigInt pow (BigInt exponent) {
    return of(value.pow(exponent.intValue()));
  }

  /**
   * Integer division that rounds to zero.
   * Same as {@link #divRoundZero(BigInt)}.
   */
  public BigInt div (BigInt other) {
    return divRoundZero(other);
  }

  /**
   * Integer division that rounds up (towards +infinity).
   */
  @Override public BigInt divRoundUp (BigInt other) {
    BigDecimal a = new BigDecimal(this.value);
    BigDecimal b = new BigDecimal(other.value);
    BigDecimal result = a.divide(b, RoundingMode.CEILING);
    return of(result.toBigInteger());
  }

  /**
   * Integer division that rounds down (towards -infinity).
   */
  public BigInt divRoundDown (BigInt other) {
    BigDecimal a = new BigDecimal(this.value);
    BigDecimal b = new BigDecimal(other.value);
    BigDecimal result = a.divide(b, RoundingMode.FLOOR);
    return of(result.toBigInteger());
  }

  /**
   * Integer division that rounds towards 0.
   */
  public BigInt divRoundZero (BigInt other) {
    return of(value.divide(other.value));
  }

  /**
   * Integer division that rounds to the nearest integer.
   */
  public BigInt divRoundNearest (BigInt other) {
    BigDecimal a = new BigDecimal(this.value);
    BigDecimal b = new BigDecimal(other.value);
    BigDecimal result = a.divide(b, RoundingMode.HALF_EVEN);
    return of(result.toBigInteger());
  }

  /**
   * Integer division that does not perform rounding.
   * If the division cannot be performed exactly then {@code null} is returned.
   */
  public BigInt divRoundNone (BigInt other) {
    BigDecimal a = new BigDecimal(this.value);
    BigDecimal b = new BigDecimal(other.value);
    try {
      BigDecimal result = a.divide(b, RoundingMode.UNNECESSARY);
      return of(result.toBigInteger());
    } catch (ArithmeticException e) {
      if (other.isZero()) // division by zero should be signaled with an exception
        throw e;
      else
        return null;
    }
  }

  /**
   * Returns the modulo when dividing this by other. The modulo is always positive. If you need a possibly negative
   * remainder use {@link #remainder(BigInt)}.
   */
  public BigInt mod (BigInt other) {
    return of(value.mod(other.value));
  }

  /**
   * Returns the positive or negative remainder when dividing this by other.
   *
   * @see #mod(BigInt)
   */
  public BigInt remainder (BigInt other) {
    return of(value.remainder(other.value));
  }

  /**
   * Returns the result of the division and the remainder as a tuple. The result is an array containing
   * the division result as first element and the remainder as the second element.
   */
  public BigInt[] divideAndRemainder (BigInt other) {
    BigInteger[] res = value.divideAndRemainder(other.value);
    return new BigInt[] {of(res[0]), of(res[1])};
  }

  public BigInt gcd (BigInt other) {
    return of(value.gcd(other.value));
  }

  public BigInt shl (int n) {
    return of(value.shiftLeft(n));
  }

  public BigInt shl (BigInt n) {
    return of(value.shiftLeft(n.intValue()));
  }

  public BigInt shr (int n) {
    return of(value.shiftRight(n));
  }

  public BigInt shr (BigInt n) {
    return of(value.shiftRight(n.intValue()));
  }

  /**
   * Bitwise not, , i.e. {@code ~value}.
   */
  public BigInt not () {
    return of(value.not());
  }

  public BigInt and (BigInt other) {
    return of(value.and(other.value));
  }

  public BigInt or (BigInt other) {
    return of(value.or(other.value));
  }

  public BigInt xor (BigInt other) {
    return of(value.xor(other.value));
  }

  public BigInt abs () {
    return of(value.abs());
  }

  @Override public int sign () {
    return value.signum();
  }

  public boolean isOne () {
    return isEqualTo(Bound.ONE);
  }

  public static BigInt minOr (BigInt a, BigInt b, BigInt c, BigInt d) {
    int bitLength = a.xor(c).value.bitLength();
    return minOr(a, b, c, d, powerOfTwo(bitLength));
  }

  public static BigInt minOr (BigInt a, BigInt b, BigInt c, BigInt d, int size) {
    return minOr(a, b, c, d, powerOfTwo(size - 1));
  }

  public static BigInt minOr (BigInt a, BigInt b, BigInt c, BigInt d, BigInt m) {
    BigInt t;
    while (!m.isZero()) {
      if (!a.not().and(c).and(m).isZero()) {
        t = a.or(m).and(m.negate());
        if (t.compareTo(b) <= 0) {
          a = t;
          break;
        }
      } else if (a.and(c.not()).and(m).compareTo(Bound.ZERO) != 0) {
        t = c.or(m).and(m.negate());
        if (t.compareTo(d) <= 0) {
          c = t;
          break;
        }
      }
      m = m.shr(1);
    }
    return a.or(c);
  }

  public static BigInt maxOr (BigInt a, BigInt b, BigInt c, BigInt d) {
    int bitLength = b.and(d).value.bitLength();
    return maxOr(a, b, c, d, powerOfTwo(bitLength));
  }

  public static BigInt maxOr (BigInt a, BigInt b, BigInt c, BigInt d, int size) {
    return maxOr(a, b, c, d, powerOfTwo(size - 1));
  }

  public static BigInt maxOr (BigInt a, BigInt b, BigInt c, BigInt d, BigInt m) {
    BigInt t;
    while (!m.isZero()) {
      if (!b.and(d).and(m).isZero()) {
        t = b.sub(m).or(m.sub(Bound.ONE));
        if (t.compareTo(a) >= 0) {
          b = t;
          break;
        }
        t = d.sub(m).or(m.sub(Bound.ONE));
        if (t.compareTo(c) >= 0) {
          d = t;
          break;
        }
      }
      m = m.shr(1);
    }
    return b.or(d);
  }

  public static BigInt minAnd (BigInt a, BigInt b, BigInt c, BigInt d) {
    return maxOr(b.not(), a.not(), d.not(), c.not()).not();
  }

  public static BigInt maxAnd (BigInt a, BigInt b, BigInt c, BigInt d) {
    return minOr(b.not(), a.not(), d.not(), c.not()).not();
  }

  public static BigInt minXor (BigInt a, BigInt b, BigInt c, BigInt d) {
    int bitLength = a.xor(c).value.bitLength();
    return minXor(a, b, c, d, powerOfTwo(bitLength));
  }

  public static BigInt minXor (BigInt a, BigInt b, BigInt c, BigInt d, int size) {
    return minXor(a, b, c, d, powerOfTwo(size - 1));
  }

  public static BigInt minXor (BigInt a, BigInt b, BigInt c, BigInt d, BigInt m) {
    BigInt t;
    while (!m.isZero()) {
      if (!a.not().and(c).and(m).isZero()) {
        t = a.or(m).and(m.negate());
        if (t.compareTo(b) <= 0) {
          a = t;
        }
      } else if (!a.and(c.not()).and(m).isZero()) {
        t = c.or(m).and(m.negate());
        if (t.compareTo(d) <= 0) {
          c = t;
        }
      }
      m = m.shr(1);
    }
    return a.xor(c);
  }

  public static BigInt maxXor (BigInt a, BigInt b, BigInt c, BigInt d) {
    int bitLength = b.and(d).value.bitLength();
    return maxXor(a, b, c, d, powerOfTwo(bitLength));
  }

  public static BigInt maxXor (BigInt a, BigInt b, BigInt c, BigInt d, int size) {
    return maxXor(a, b, c, d, powerOfTwo(size - 1));
  }

  public static BigInt maxXor (BigInt a, BigInt b, BigInt c, BigInt d, BigInt m) {
    BigInt t;
    while (!m.isZero()) {
      if (!b.and(d).and(m).isZero()) {
        t = b.sub(m).or(m.sub(Bound.ONE));
        if (t.compareTo(a) >= 0)
          b = t;
        else {
          t = d.sub(m).or(m.sub(Bound.ONE));
          if (t.compareTo(c) >= 0)
            d = t;
        }
      }
      m = m.shr(1);
    }
    return b.xor(d);
  }

  @Override public BigInt asInteger () {
    return this;
  }

  @Override public Bound max (Bound other) {
    return other.max(this);
  }

  @Override public BigInt max (BigInt other) {
    return of(value.max(other.value));
  }

  @Override public Bound min (Bound other) {
    return other.min(this);
  }

  @Override public BigInt min (BigInt other) {
    return of(value.min(other.value));
  }

  @Override public Bound add (Bound other) {
    return other.add(this);
  }

  @Override
  public BigInt add (BigInt other) {
    return of(value.add(other.value));
  }

  @Override public Bound sub (Bound other) {
    if (other.isFinite())
      return sub(other.asInteger());
    else
      return add(other.negate());
  }

  @Override public Bound divRoundDown (Bound other) {
    if (other.isFinite()) {
      return divRoundDown(other.asInteger());
    } else {
      // This is ok because other is unbounded
      return other.divRoundDown(this);
    }
  }

  @Override public Bound divRoundZero (Bound other) {
    if (other.isFinite()) {
      return divRoundZero(other.asInteger());
    } else {
      // This is ok because other is unbounded
      return other.divRoundDown(this);
    }
  }

  @Override public Bound divRoundNearest (Bound other) {
    if (other.isFinite()) {
      return divRoundNearest(other.asInteger());
    } else {
      // This is ok because other is unbounded
      return other.divRoundDown(this);
    }
  }

  @Override public Bound divRoundNone (Bound other) {
    if (other.isFinite()) {
      BigInt result = divRoundNone(other.asInteger());
      if (result == null)
        return null;
      return result;
    } else {
      // This is ok because other is unbounded
      return other.divRoundDown(this);
    }
  }

  @Override public Bound mul (Bound other) {
    if (other.isFinite())
      return mul(other.asInteger());
    else
      return other.mul(this);
  }

  @Override public Bound shl (Bound other) {
    if (other.isFinite())
      return shl(other.asInteger());
    else
      return POSINF;
  }

  public boolean isEqualTo (BigInt other) {
    return value.equals(other.value);
  }

  @Override public int hashCode () {
    return value.hashCode();
  }

  @Override public int compareTo (Bound other) {
    return -other.compareTo(this);
  }

  @Override public int compareTo (BigInt other) {
    return value.compareTo(other.value);
  }

}
