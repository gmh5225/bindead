package javalx.numeric;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javalx.data.Option;
import javalx.xml.XmlPrintable;

import com.jamesmurty.utils.XMLBuilder;

public final class Interval implements Comparable<Interval>, Iterable<BigInt>, XmlPrintable {
  public static final Interval TOP = new Interval(Bound.NEGINF, Bound.POSINF);
  public static final Interval ONE = of(Bound.ONE);
  public static final Interval MINUSONE = of(Bound.MINUSONE);
  public static final Interval ZERO = of(Bound.ZERO);
  public static final Interval BOOLEANTOP = ZERO.join(ONE);
  public static final Interval LESS_THAN_OR_EQUAL_TO_ZERO = new Interval(Bound.NEGINF, Bound.ZERO);

  public static final Interval GREATER_THAN_OR_EQUAL_TO_ZERO = new Interval(Bound.ZERO, Bound.POSINF);
  public static final Interval ZEROorONE = new Interval(Bound.ZERO, Bound.ONE);

  final Bound low;
  final Bound high;

  private Interval (Bound lo, Bound up) {
    this.low = lo;
    this.high = up;
    assert isWellFormed(this.low, this.high) : "Interval bounds are not well formed: [" + lo + ", " + up + "]";
  }

  /**
   * Parse intervals from strings.
   *
   * @author Bogdan Mihaila
   */
  private static final class StringParser {

    public static Interval parseInterval (String interval) {
      interval = removeBraces(interval);
      String[] split = interval.split(",");
      if (split.length == 1) {
        return Interval.of(parseNumber(split[0].trim()));
      } else {
        String left = split[0].trim();
        String right = split[1].trim();
        boolean leftIsInfinity = left.equals(Bound.NEGINF.toString());
        boolean rightIsInfinity = right.equals(Bound.POSINF.toString());
        if (leftIsInfinity && rightIsInfinity)
          return Interval.top();
        if (leftIsInfinity)
          return Interval.downFrom(parseNumber(right));
        if (rightIsInfinity)
          return Interval.upFrom(parseNumber(left));
        return Interval.of(parseNumber(left), parseNumber(right));
      }
    }

    private static long parseNumber (String number) {
      int radix = 10;
      if (number.toLowerCase().startsWith("0x")) {
        radix = 16;
        number = number.substring(2);
      }
      return Long.parseLong(number, radix);
    }

    private static String removeBraces (String interval) {
      boolean hasOpeningBrace = interval.startsWith("[");
      boolean hasClosingBrace = interval.endsWith("]");
      if (hasOpeningBrace != hasClosingBrace)
        throw new NumberFormatException("\"" + interval +
          "\" needs to either be enclosed by braces or not have braces at all.");
      if (hasOpeningBrace)
        return interval.substring(1, interval.length() - 1);
      return interval;
    }
  }

  /**
   * Parse and build an interval from a string.
   * Possible ways to specify an interval are:
   * [-1, 14]<br>
   * [5]<br>
   * 8<br>
   * [-oo, 10]<br>
   * [0, +oo]<br>
   * [-3, 0x12]<br>
   * Numbers can also be specified in hexadecimal by using the "0x" prefix.
   *
   * @param interval A string of an interval value.
   * @return An interval or a parsing exception
   */
  public static Interval of (String interval) {
    return StringParser.parseInterval(interval);
  }

  /**
   * Build a new interval with the lower and the upper bound set to {@code c}.
   */
  public static Interval of (long c) {
    return of(BigInt.of(c));
  }

  /**
   * Build a new interval with the lower and the upper bound set to {@code c}.
   */
  public static Interval of (BigInt c) {
    Bound bound = c;
    return new Interval(bound, bound);
  }

  /**
   * Build a new interval with {@code lo} as lower bound and {@code up} as upper bound.
   */
  public static Interval of (long lo, long up) {
    return of(BigInt.of(lo), BigInt.of(up));
  }

  /**
   * Build a new interval with {@code lo} as lower bound and {@code up} as upper bound.
   */
  public static Interval of (BigInt lo, BigInt up) {
    return of(lo, (Bound) up);
  }

  /**
   * Build a new interval with {@code lo} as lower bound and {@code up} as upper bound.
   */
  public static Interval of (Bound lo, Bound up) {
    return new Interval(lo, up);
  }

  /**
   * Build a new interval going from {@code -oo} to {@code upperBound}.
   */
  public static Interval downFrom (long upperBound) {
    return downFrom(BigInt.of(upperBound));
  }

  /**
   * Build a new interval going from {@code -oo} to {@code upperBound}.
   */
  public static Interval downFrom (BigInt upperBound) {
    return downFrom((Bound) upperBound);
  }

  /**
   * Build a new interval going from {@code -oo} to {@code upperBound}.
   */
  public static Interval downFrom (Bound upperBound) {
    return new Interval(Bound.NEGINF, upperBound);
  }

  /**
   * Build a new interval going from {@code lowerBound} to {@code +oo}.
   */
  public static Interval upFrom (long lowerBound) {
    return upFrom(BigInt.of(lowerBound));
  }

  /**
   * Build a new interval going from {@code lowerBound} to {@code +oo}.
   */
  public static Interval upFrom (BigInt lowerBound) {
    return new Interval(lowerBound, Bound.POSINF);
  }


  private static boolean isWellFormed (Bound lower, Bound higher) {
    if (!lower.isFinite() && !higher.isFinite())
      return lower.isLessThan(higher);
    if (lower.isFinite() && higher.isFinite())
      return lower.asInteger().isLessThanOrEqualTo(higher.asInteger());
    return true;
  }

  /**
   * Returns the interval {@code [-oo, +oo]}
   */
  public static Interval top () {
    return TOP;
  }

  /**
   * Returns the interval {@code [-2^(size-1), 2^(size-1) - 1]}.
   *
   * @param size The size of the signed interval span in bits.
   */
  public static Interval signedTop (int size) {
    final BigInt base = BigInt.powerOfTwo(size - 1);
    final BigInt upperBound = base.sub(BigInt.of(1));
    final BigInt lowerBound = base.mul(BigInt.of(-1));
    return of(lowerBound, upperBound);
  }

  /**
   * Returns the interval {@code [0, 2^size - 1]}.
   *
   * @param size The size of the unsigned interval span in bits.
   */
  public static Interval unsignedTop (int size) {
    final BigInt upperBound = BigInt.powerOfTwo(size).sub(BigInt.of(1));
    final BigInt lowerBound = Bound.ZERO;
    return of(lowerBound, upperBound);
  }

  /**
   * Wrap an interval such that its values (seen as bit patterns) are only in the given range.
   */
  public Interval wrap (Interval range) {
    if (!isFinite())
      return range;
    BigInt max = range.high().asInteger().sub(range.low().asInteger()).add(Bound.ONE);
    BigInt bl = range.low().asInteger();
    BigInt l = low().asInteger();
    BigInt h = high().asInteger();
    BigInt[] divAndRem = l.sub(bl).divideAndRemainder(max);
    BigInt ql = divAndRem[1].isNegative() ? divAndRem[0].sub(Bound.ONE) : divAndRem[0];
    divAndRem = h.sub(bl).divideAndRemainder(max);
    BigInt qu = divAndRem[1].isNegative() ? divAndRem[0].sub(Bound.ONE) : divAndRem[0];
    BigInt k = qu.sub(ql);
    if (!Bound.ONE.isLessThan(k)) {
      Interval x = sub(Interval.of(max.mul(ql))).meet(range).get();
      for (BigInt q = ql.add(Bound.ONE); !qu.isLessThan(q); q = q.add(Bound.ONE)) {
        x = x.join(sub(Interval.of(max.mul(q))).meet(range).get());
      }
      return x;
    } else {
      return range;
    }
  }

  public Bound low () {
    return low;
  }

  public Bound high () {
    return high;
  }

  /**
   * Not a "semantically correct" ordering, but just the lexicographic order on (lower, upper), so that we can store
   * intervals in collections.
   * */
  @Override public int compareTo (Interval b) {
    int cmp1 = low.compareTo(b.low);
    return cmp1 == 0 ? high.compareTo(b.high) : cmp1;
  }

  public boolean overlaps (Interval other) {
    return low.isLessThanOrEqualTo(other.high) && other.low.isLessThanOrEqualTo(high);
  }

  /**
   * @return iff this.high + 1 == other.low || this.low - 1 == other.high
   */
  public boolean isAdjacent (Interval other) {
    final boolean thisHighIsOneLess;
    final boolean thisLowIsOneMore;

    if (high().isFinite() && other.low().isFinite()) {
      final BigInt thisHighPlusOne = high().asInteger().add(Bound.ONE);
      thisHighIsOneLess = thisHighPlusOne.isEqualTo(other.low().asInteger());
    } else {
      thisHighIsOneLess = false;
    }

    if (low().isFinite() && other.high().isFinite()) {
      final BigInt thisLowMinusOne = low().asInteger().sub(Bound.ONE);
      thisLowIsOneMore = thisLowMinusOne.isEqualTo(other.high().asInteger());
    } else {
      thisLowIsOneMore = false;
    }

    return thisHighIsOneLess || thisLowIsOneMore;
  }

  public boolean contains (Interval other) {
    return low.isLessThanOrEqualTo(other.low) && other.high.isLessThanOrEqualTo(high);
  }

  public boolean contains (BigInt value) {
    return low.isLessThanOrEqualTo(value) && value.isLessThanOrEqualTo(high);
  }

  public Interval join (Interval other) {
    return new Interval(low.min(other.low), high.max(other.high));
  }

  public Interval widen (Interval other) {
    Bound lo = other.low.isLessThan(this.low) ? Bound.NEGINF : this.low;
    Bound up = this.high.isLessThan(other.high) ? Bound.POSINF : this.high;
    return new Interval(lo, up);
  }

  public Option<Interval> meet (Interval other) {
    return wellFormedOrNone(low.max(other.low), high.min(other.high));
  }

  /**
   * Return an interval for the given bounds if {@code lower <= higher} or {@code none} if not.
   */
  private static Option<Interval> wellFormedOrNone (Bound lower, Bound higher) {
    return isWellFormed(lower, higher) ? Option.<Interval>some(new Interval(lower, higher)) : Option.<Interval>none();
  }

  public boolean subsetOrEqual (Interval other) {
    return other.low.isLessThanOrEqualTo(low) && high.isLessThanOrEqualTo(other.high);
  }

  @Override public boolean equals (Object o) {
    return o instanceof Interval && this.isEqualTo((Interval) o);
  }

  @Override public int hashCode () {
    int hash = 5;
    hash = 23 * hash + this.low.hashCode();
    hash = 23 * hash + this.high.hashCode();
    return hash;
  }

  public boolean isEqualTo (Interval other) {
    return low.isEqualTo(other.low) && high.isEqualTo(other.high);
  }

  public boolean isLessThanZero () {
    return low.isNegative() && high.isNegative();
  }

  public boolean hasNegativeValues () {
    return low.isNegative();
  }

  public boolean isConstant () {
    return isFinite() && low.asInteger().isEqualTo(high.asInteger());
  }

  public boolean isFinite () {
    return low.isFinite() && high.isFinite();
  }

  /**
   * Return the "span" of this interval that is hi - lo + 1, if this interval is finite or null if this is
   * interval has an infinite span.
   *
   * @return The span of this interval or null for infinite span intervals.
   */
  public BigInt getSpan () {
    if (isFinite())
      return high.asInteger().sub(low.asInteger()).add(Bound.ONE);
    return null;
  }

  /**
   * Return the constant if this interval is a constant (e.g [c, c]) or null.
   *
   * @return The constant denoted by this interval or null.
   */
  public BigInt getConstant () {
    if (isConstant())
      return low.asInteger();
    return null;
  }

  /**
   * [0, 0] <= 0 |-> 1 <br>
   * [0, 1] <= 0 |-> [0, 1] <br>
   * [1, 1] <= 0 |-> 0 <br>
   * [-1, 0] <= 0 |-> 1 <br>
   * [-1, -1] <= 0 |-> 1 <br>
   */
  public Interval evalLessThanOrEqualToZero () {
    if (low.isPositive()) // lo > 0 implies up > 0
      return ZERO;
    if (high.isPositive())
      return BOOLEANTOP;
    return ONE;
  }

  /**
   * [0, 0] < 0 |-> 0 <br>
   * [0, 1] < 0 |-> 0 <br>
   * [1, 1] < 0 |-> 0 <br>
   * [-1, 0] < 0 |-> [0, 1] <br>
   * [-1, -1] < 0 |-> 1 <br>
   */
  public Interval evalLessThanZero () {
    if (low.isNegative())
      if (high.isNegative())
        return ONE;
      else
        return BOOLEANTOP;
    else
      // lo >= 0 implies up >= 0
      return ZERO;
  }

  /**
   * [0, 0] == 0 |-> 1 <br>
   * [0, 1] == 0 |-> [0, 1] <br>
   * [1, 1] == 0 |-> 0 <br>
   */
  public Interval evalEqualToZero () {
    return this.equals(ZERO) ? ONE : ZERO.subsetOrEqual(this) ? BOOLEANTOP : ZERO;
  }

  /**
   * [0, 0] == 0 |-> 0 <br>
   * [0, 1] == 0 |-> [0, 1] <br>
   * [1, 1] == 0 |-> 1 <br>
   */
  public Interval evalNotEqualToZero () {
    return this.equals(ZERO) ? ZERO : ZERO.subsetOrEqual(this) ? BOOLEANTOP : ONE;
  }

  /**
   * [a, b] + [c, d] = [a + c, b + d]
   */
  public Interval add (Interval other) {
    return new Interval(low.add(other.low), high.add(other.high));
  }

  public Interval add (BigInt scalar) {
    return this.add(of(scalar));
  }

  /**
   * [a, b] − [c, d] = [a − d, b − c]
   */
  public Interval sub (Interval other) {
    return new Interval(low.sub(other.high), high.sub(other.low));
  }

  public Interval sub (BigInt scalar) {
    return this.sub(of(scalar));
  }

  /**
   * [a, b] * [c, d] = [min (ac, ad, bc, bd), max (ac, ad, bc, bd)]
   */
  public Interval mul (Interval other) {
    Bound w = low.mul(other.low), x = low.mul(other.high), y = high.mul(other.low), z = high.mul(other.high);
    return new Interval(w.min(x.min(y.min(z))), w.max(x.max(y.max(z))));
  }

  public Interval mul (BigInt scalar) {
    return this.mul(of(scalar));
  }

  /**
   * Divide this interval by a scalar. Note that interval bounds get rounded to integers after division
   * such that the resulting interval is the smallest possible intervals containing the integer solutions.
   * If the division cannot be performed exactly {@code null} is returned. Some examples:<br>
   *
   * Round intervals after division such that the they get smaller,
   * i.e. lower bound towards +oo and upper bound towards -oo.
   * If the interval is not well formed afterwards, it returns {@code null}.
   *
   * <pre>
   * [9, 15] / 4 = [2.25, 3.75] -> [3, 3]
   * [10, 11] / 3 = [3.333, 3.666] -> null as there are no integer solutions in the interval
   * </pre>
   *
   * A division by zero (0 is contained in the divisor interval) is ignored to implement the ARM semantics,
   * namely dividing by zero results in zero.
   */
  public Interval divRoundInvards (BigInt scalar) {
    if (scalar.isZero())
      return Interval.ZERO;
    BigInt divisor = scalar;
    Interval interval = this;
    if (divisor.isNegative()) {
      // with a negative divisor the interval bounds would get flipped around and the sanity test below would not work
      interval = interval.negate();
      divisor = divisor.negate();
    }
    Bound newLow = interval.low.divRoundUp(divisor);
    Bound newHigh = interval.high.divRoundDown(divisor);
    if (newHigh.isLessThan(newLow))
      return null; // this can happen when low ~= high and not integer. When both get rounded we might get high < low
    return new Interval(newLow, newHigh);
  }

  /**
   * Divide this interval by another interval and round the result towards zero.
   * A division by zero (0 is contained in the divisor interval) is ignored to implement the ARM semantics,
   * namely dividing by zero results in zero. Thus an interval containing zero will contain zero in the result, too.<br>
   * The classic interval division for divisors without 0 is defined as:<br>
   * [a, b] / [c, d] = [min (a/c, a/d, b/c, b/d), max (a/c, a/d, b/c, b/d)]
   */
  public Interval divRoundZero (Interval divisor) {
    Interval dividend = this;
    // remove the 0 from the divisor
    Option<Interval> rightOfZero = divisor.meet(Interval.upFrom(1));
    Option<Interval> leftOfZero = divisor.meet(Interval.downFrom(-1));
    Option<Interval> result = Option.none();
    if (leftOfZero.isSome()) // use rounding down and keep sign by negating both operands
      result = join(result, dividend.negate().divRoundDown(leftOfZero.get().negate()));
    if (rightOfZero.isSome())
      result = join(result, dividend.divRoundDown(rightOfZero.get()));
    if (divisor.contains(Bound.ZERO)) // ARM semantics can return 0 when dividing by 0
      result = join(result, Interval.ZERO);
    return result.get();
  }

  private static Option<Interval> join (Option<Interval> first, Interval second) {
    if (first.isNone())
      return Option.some(second);
    else
      return Option.some(first.get().join(second));
  }

  /**
   * Divide this interval by another interval that is strictly positive! You need to split and negate the divisor
   * accordingly and join the resulting intervals.
   * The result is rounded towards minus infinity.
   *
   * @param other the other interval that only contains positive values
   * @return the divided and rounded interval
   */
  private Interval divRoundDown (Interval other) {
    assert !other.hasNegativeValues();
    Interval newLow = divRoundDown(other.low);
    Interval newHigh = divRoundDown(other.high);
    return newLow.join(newHigh);
  }

  private Interval divRoundDown (Bound scalar) {
    Bound lower = low.divRoundDown(scalar);
    Bound upper = high.divRoundDown(scalar);
    return of(lower, upper);
  }

  /**
   * [a, b] << [c, d] = [min (a<<c, a<<d, b<<c, b<<d), max (a<<c, a<<d, b<<c, b<<d)]
   */
  public Interval shl (Interval other) {
    Bound w = low.shl(other.low), x = low.shl(other.high), y = high.shl(other.low), z = high.shl(other.high);
    return new Interval(w.min(x.min(y.min(z))), w.max(x.max(y.max(z))));
  }

  public Interval shr (Interval other) {
    // the bound must be capped to an integer as we use 2^value
    Bound highest = BigInt.of(Integer.MAX_VALUE);
    int hDiv = other.high.min(highest).asInteger().intValue();
    // ignoring the negative values as we assume wrapping takes care of that
    assert !other.hasNegativeValues();
    Bound lowest = Bound.ZERO;
    int lDiv = other.low.max(lowest).asInteger().intValue();
    Interval divisor = of(BigInt.powerOfTwo(lDiv), BigInt.powerOfTwo(hDiv));
    return divRoundDown(divisor);
  }

  public Interval negate () {
    return new Interval(high.negate(), low.negate());
  }

  public Interval slowUnsignedOr (Interval other) {
    if (!isFinite() || hasNegativeValues())
      return GREATER_THAN_OR_EQUAL_TO_ZERO;
    BigInt min = high.add(other.high).asInteger();
    BigInt max = Bound.ZERO;
    for (BigInt x : this) {
      for (BigInt y : other) {
        BigInt t = x.or(y);
        if (t.isLessThan(min))
          min = t;
        if (max.isLessThan(t))
          max = t;
      }
    }
    return of(min, max);
  }

  public Interval slowUnsignedAnd (Interval other) {
    if (!isFinite() || hasNegativeValues())
      return GREATER_THAN_OR_EQUAL_TO_ZERO;
    BigInt min = high.add(other.high).asInteger();
    BigInt max = Bound.ZERO;
    for (BigInt x : this) {
      for (BigInt y : other) {
        BigInt t = x.and(y);
        if (t.isLessThan(min))
          min = t;
        if (max.isLessThan(t))
          max = t;
      }
    }
    return of(min, max);
  }

  public Interval slowUnsignedXor (Interval other) {
    if (!isFinite() || hasNegativeValues())
      return GREATER_THAN_OR_EQUAL_TO_ZERO;
    BigInt min = high.add(other.high).asInteger();
    BigInt max = Bound.ZERO;
    for (BigInt x : this) {
      for (BigInt y : other) {
        BigInt t = x.xor(y);
        if (t.isLessThan(min))
          min = t;
        if (max.isLessThan(t))
          max = t;
      }
    }
    return of(min, max);
  }

  public Interval unsignedOr (Interval other) {
    if (!isFinite() || hasNegativeValues())
      return GREATER_THAN_OR_EQUAL_TO_ZERO;
    BigInt a = low.asInteger();
    BigInt b = high.asInteger();
    BigInt c = other.low.asInteger();
    BigInt d = other.high.asInteger();
    BigInt minOr = BigInt.minOr(a, b, c, d);
    BigInt maxOr = BigInt.maxOr(a, b, c, d);
    return of(minOr, maxOr);
  }

  public Interval unsignedAnd (Interval other) {
    if (!isFinite() || hasNegativeValues())
      return GREATER_THAN_OR_EQUAL_TO_ZERO;
    BigInt a = low.asInteger();
    BigInt b = high.asInteger();
    BigInt c = other.low.asInteger();
    BigInt d = other.high.asInteger();
    BigInt minAnd = BigInt.minAnd(a, b, c, d);
    BigInt maxAnd = BigInt.maxAnd(a, b, c, d);
    return of(minAnd, maxAnd);
  }

  public Interval unsignedXor (Interval other) {
    if (!isFinite() || hasNegativeValues())
      return GREATER_THAN_OR_EQUAL_TO_ZERO;
    BigInt a = low.asInteger();
    BigInt b = high.asInteger();
    BigInt c = other.low.asInteger();
    BigInt d = other.high.asInteger();
    BigInt minXor = BigInt.minXor(a, b, c, d);
    BigInt maxXor = BigInt.maxXor(a, b, c, d);
    return of(minXor, maxXor);
  }

  /**
   * Returns an iterator over all discrete values in this intervals range / convex hull.
   * Throws {@link UnsupportedOperationException} if this interval is not a finite interval.
   *
   * @return An iterator over all enclosed values.
   */
  @Override public Iterator<BigInt> iterator () throws UnsupportedOperationException {
    if (!isFinite())
      throw new UnsupportedOperationException("Can not interate over infinite intervals");
    return new IntervalIterator(low.asInteger(), high.asInteger());
  }

  public Iterator<BigInt> iteratorWithStride (BigInt stride) {
    if (!isFinite())
      throw new UnsupportedOperationException("Can not interate over infinite intervals");
    return new IntervalIterator(low.asInteger(), high.asInteger(), stride);
  }

  private static class IntervalIterator implements Iterator<BigInt> {
    private BigInt low;
    private final BigInt high;
    private final BigInt stride;

    private IntervalIterator (BigInt low, BigInt high, BigInt stride) {
      this.low = low;
      this.high = high;
      assert !stride.isNegative();
      if (stride.isZero()) {
        assert low.isEqualTo(high);
        this.stride = BigInt.ONE; // needed to step over the first value
      } else {
        this.stride = stride;
      }
    }

    private IntervalIterator (BigInt low, BigInt high) {
      this(low, high, Bound.ONE);
    }

    @Override public boolean hasNext () {
      return !high.isLessThan(low);
    }

    @Override public BigInt next () {
      if (!hasNext())
        throw new NoSuchElementException();

      BigInt next = low;
      low = low.add(stride);
      return next;
    }

    @Override public void remove () {
      throw new UnsupportedOperationException();
    }
  }


  @Override public XMLBuilder toXML (XMLBuilder builder) {
    return builder.e("Interval").t(toString()).up();
  }

  @Override public String toString () {
    if (isConstant()) {
      String fmt = "[%s]";
      return String.format(fmt, low);
    } else {
      String fmt = "[%s, %s]";
      return String.format(fmt, low, high);
    }
  }

  public void toStringDotted (StringBuilder builder) {
    if (!isConstant())
      builder.append('[');
    builder.append(low.toString());
    if (isConstant())
      return;
    builder.append("..");
    builder.append(high.toString());
    builder.append(']');
  }

  public boolean isTop () {
    return !low.isFinite() && !high.isFinite();
  }

  public int intervalSize () {
    assert isFinite();
    int offset = low().asInteger().intValue();
    int size = high().asInteger().intValue() - offset + 1;
    return size;
  }

  /**
   * Cuts everything off that does not strictly meet the congruence!
   *
   * @return A {@link Interval} that is a subset of the given one that meets the congruence c
   */
  public Interval applyCongruence (Congruence c) {
    // adjust finite interval bounds to satisfy the congruence
    // uses integer division (rounding) and subsequent multiplication to set the bounds right
    Bound lo;
    Bound hi;
    BigInt offset = c.getOffset();
    BigInt scale = c.getScale();
    if (low.isFinite()) // lo = ((lo - o) /+oo s) * s + o
      lo = low.asInteger().sub(offset).divRoundUp(scale).mul(scale).add(offset);
    else
      lo = low;
    if (high.isFinite()) // hi = ((hi - o) /-oo s) * s + o
      hi = high.asInteger().sub(offset).divRoundDown(scale).mul(scale).add(offset);
    else
      hi = high;
    return of(lo, hi);
  }
}
