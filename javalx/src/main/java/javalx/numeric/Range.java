package javalx.numeric;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Wrapper class for range-based values.
 */
public abstract class Range implements Iterable<BigInt> {
  public static final Range ZERO = from(0);
  public static final Range TOP = from(Interval.TOP);
  protected Congruence congruence;

  public static Range from (long value) {
    return from(BigInt.of(value));
  }

  public static Range from (long lo, long hi) {
    return from(Interval.of(lo, hi));
  }

  public static Range from (BigInt value) {
    return from(Interval.of(value));
  }

  public static Range from (Interval value) {
    return new IntervalRange(value);
  }

  public static IntervalSetRange from (IntervalSet intervalSet) {
    return new IntervalSetRange(intervalSet);
  }

  /**
   * Returns the convex hull of this {Range}.
   */
  public abstract Interval convexHull ();

  /**
   * Returns this range as a compact set of its values.
   */
  public abstract IntervalSet asSet ();

  /**
   * Returns {@code true} if this range contains a given value.
   */
  public abstract boolean contains (BigInt value);

  /**
   * Returns the congruence information associated with this range.
   */
  public Congruence getCongruence () {
    return congruence;
  }

  /**
   * Returns true is the concretization of this contains a finite amount of distinct values.
   */
  public abstract boolean isFinite ();

  /**
   * Returns the minimum value of this range. Throws exception if this range is not finite.
   */
  public abstract BigInt getMin ();

  /**
   * Returns the maximum value of this range. Throws exception if this range is not finite.
   */
  public abstract BigInt getMax ();

  /**
   * Returns the union of two ranges.
   *
   * @param other A range to union with this range
   * @return The union of this range with the other range
   */
  public abstract Range union (Range other);

  /**
   * Returns the number of discrete values in the concretization of this. Throws exception
   * if this is not a finite range.
   */
  public abstract BigInt numberOfDiscreteValues ();

  /**
   * Return the number of least-significant bits that are definitely zero.
   *
   * @param noOfBits the number of bits of this variable
   * @return the number of bits that are zero, starting at bit 0
   */
  public abstract int lowerZeroBits (int noOfBits);

  /**
   * Return the number of most-significant bits that are definitely zero.
   *
   * @param noOfBits the number of bits of this variable
   * @return the sequence of bits that are zero, starting at bit {@code noOfBits-1}
   */
  public abstract int upperZeroBits (int noOfBits);

  /**
   * Return the number of most-significant bits that are definitely one.
   *
   * @param noOfBits the number of bits of this variable
   * @return the sequence of bits that are one, starting form {@code noOfBits-1}
   */
  public abstract int upperOneBits (int noOfBits);

  /**
   * Returns true if the range consists of individual values that have not undergone a convex approximation.
   *
   * @return true if jumping to values in this range makes sense
   */
  public abstract boolean isNonConvexApproximation ();

  /**
   * Makes sure the range satisfies the given congruence.
   *
   * @param c congruence information to be applied to the range
   * @return new Range object containing congruence information
   */
  public abstract Range ensureCongruent (Congruence c);

  /**
   * {@code true} if this range is a constant. i.e. a single number.
   */
  public boolean isConstant () {
    return isFinite() && numberOfDiscreteValues().isOne();
  }

  public boolean isZero () {
    BigInt c = getConstantOrNull();
    if (c == null)
      return false;
    return c.isZero();
  }

  public boolean isOne () {
    BigInt c = getConstantOrNull();
    if (c == null)
      return false;
    return c.isOne();
  }

  /**
   * Returns the constant {@code c} if this range is a constant range ([c,c]), otherwise {@code null} is returned.
   *
   * @return c iff {@code this} ~= [c, c]; {@code null} otherwise.
   */
  public BigInt getConstantOrNull () {
    return isConstant() ? getMin() : null;
  }

  private boolean isFiniteInRange (Interval fullSignedRange) {
    return isFinite() && fullSignedRange.contains(convexHull());
  }

  public Range wrapUnsigned (int size) {
    return wrapToInterval(size, Interval.unsignedTop(size));
  }

  public Range wrapSigned (int size) {
    return wrapToInterval(size, Interval.signedTop(size));
  }

  private Range wrapToInterval (int size, Interval unSignedTop) {
    return isFiniteInRange(unSignedTop) ? this : wrap(size, unSignedTop);
  }

  private Range wrap (int size, Interval top) {
    BigInt max = BigInt.powerOfTwo(size);
    if (!isFinite())
      return from(top);
    BigInt bl = top.low().asInteger();
    Interval x = convexHull();
    BigInt l = x.low().asInteger();
    BigInt h = x.high().asInteger();
    BigInt[] divAndRem = l.sub(bl).divideAndRemainder(max);
    BigInt ql = divAndRem[1].isNegative() ? divAndRem[0].sub(Bound.ONE) : divAndRem[0];
    divAndRem = h.sub(bl).divideAndRemainder(max);
    BigInt qu = divAndRem[1].isNegative() ? divAndRem[0].sub(Bound.ONE) : divAndRem[0];
    BigInt k = qu.sub(ql);
    if (!Bound.ONE.isLessThan(k)) {
      Interval i = x.sub(Interval.of(max.mul(ql))).meet(top).get();
      for (BigInt q = ql.add(Bound.ONE); !qu.isLessThan(q); q = q.add(Bound.ONE)) {
        i = i.join(x.sub(Interval.of(max.mul(q))).meet(top).get());
      }
      return from(i);
    } else {
      return from(top);
    }
  }

  public Range unsignedOr (Range other, int size) {
    Interval top = Interval.unsignedTop(size);
    Range fst = wrapUnsigned(size);
    Range snd = other.wrapUnsigned(size);
    Interval result = fst.convexHull().unsignedOr(snd.convexHull());
    return from(result.meet(top).get());
  }

  public Range unsignedXor (Range other, int size) {
    Interval top = Interval.unsignedTop(size);
    Range fst = wrapUnsigned(size);
    Range snd = other.wrapUnsigned(size);
    Interval result = fst.convexHull().unsignedXor(snd.convexHull());
    return from(result.meet(top).get());
  }

  public abstract Range add (Range other);

  public abstract Range mul (Range other);

  public abstract Range mul (BigInt c);

  public abstract Range sub (BigInt c);

  public abstract Range divRoundInvards (BigInt c);

  @Override public abstract String toString ();

  public static Range top () {
    return from(Interval.TOP);
  }

  private static final class IntervalRange extends Range {
    private final Interval interval;

    private IntervalRange (Interval interval) {
      this.interval = interval;
      this.congruence = Congruence.ONE;
    }

    private IntervalRange (Interval interval, Congruence congruence) {
      this.interval = interval;
      this.congruence = congruence;
    }

    @Override public Interval convexHull () {
      return interval;
    }

    @Override public IntervalSet asSet () {
      return IntervalSet.valueOf(interval);
    }

    @Override public boolean contains (BigInt value) {
      return interval.contains(value);
    }

    @Override public Range union (Range other) {
      return from(this.interval.join(other.convexHull())).ensureCongruent(congruence.join(other.congruence));
    }

    @Override public Range add (Range other) {
      return from(this.interval.add(other.convexHull())).ensureCongruent(congruence.add(other.congruence));
    }

    @Override public Range mul (Range other) {
      return from(this.interval.mul(other.convexHull())).ensureCongruent(congruence.mul(other.congruence));
    }

    @Override public Range mul (BigInt c) {
      return from(this.interval.mul(c)).ensureCongruent(congruence.mul(c));
    }

    @Override public Range sub (BigInt c) {
      // subtraction needs the offset of the congruence adjusted
      Congruence newCongruence = getCongruence().sub(c);
      return new IntervalRange(interval.sub(c), newCongruence);
    }

    @Override public Range divRoundInvards (BigInt c) {
      Congruence newCongruence = getCongruence();
      Interval newInterval = interval.divRoundInvards(c);
      if (newInterval == null)
        return null;
      if (!newCongruence.isConstantOnly())
        newCongruence = newCongruence.div(c);
      return new IntervalRange(newInterval, newCongruence);
    }

    @Override public int lowerZeroBits (int noOfBits) {
      if (isConstant()) {
        long val = interval.getConstant().longValue();
        return Math.min(noOfBits, Long.numberOfTrailingZeros(val));

      }
      return Math.min(noOfBits, congruence.lowerZeroBits());
    }

    @Override public int upperZeroBits (int noOfBits) {
      if (interval.hasNegativeValues())
        return 0;
      if (noOfBits < 1)
        return 0;
      int res = 0;
      Interval enclosing = Interval.of(Bound.ZERO, BigInt.powerOfTwo(noOfBits - 1).sub(1));
      while (enclosing.contains(interval) && res < noOfBits) {
        res++;
        enclosing = enclosing.divRoundInvards(Bound.TWO);
      }
      return res;
    }

    @Override public int upperOneBits (int noOfBits) {
      // only be precise for unsigned ranges
      if (interval.hasNegativeValues())
        return 0;
      if (noOfBits < 1)
        return 0;
      int res = -1;
      BigInt one = BigInt.powerOfTwo(noOfBits);
      BigInt upper = one.sub(1);
      BigInt lower = Bound.ZERO;
      do {
        one = one.shr(1);
        lower = lower.add(one);
        res++;
      } while (Interval.of(lower, upper).contains(interval) && one.isPositive());
      return res;
    }

    @Override public BigInt numberOfDiscreteValues () {
      if (congruence.getScale().isZero())
        return Bound.ONE;
      return getMax().sub(getMin()).div(congruence.getScale()).add(Bound.ONE);
    }

    @Override public boolean isNonConvexApproximation () {
      // XXX bm: needs a more sophisticated logic or then we should just try to use intervalsets everywhere
      BigInt r = numberOfDiscreteValues();
      return r.isLessThan(BigInt.of(3));
    }

    @Override public boolean isFinite () {
      return interval.low().isFinite() && interval.high().isFinite();
    }

    @Override public BigInt getMin () {
      return interval.low().asInteger();
    }

    @Override public BigInt getMax () {
      return interval.high().asInteger();
    }

    @Override public Range ensureCongruent (Congruence c) {
      if (c.isConstantOnly()) {
        if (!interval.contains(c.getOffset()))
          throw new IllegalStateException();
        return new IntervalRange(Interval.of(c.getOffset()), c);
      }
      return new IntervalRange(interval.applyCongruence(c), c);
    }

    @Override public Iterator<BigInt> iterator () {
      if (!isFinite())
        throw new UnsupportedOperationException("Can not interate over infinite intervals");
      return interval.iteratorWithStride(congruence.getScale());
    }

    @Override public String toString () {
      return interval.toString();
    }

  }

  private static final class IntervalSetRange extends Range {
    private final IntervalSet intervalSet;

    private IntervalSetRange (IntervalSet intervalSet) {
      this.intervalSet = intervalSet;
      this.congruence = Congruence.ONE;
    }

    private IntervalSetRange (IntervalSet intervalSet, Congruence congruence) {
      this.intervalSet = intervalSet;
      this.congruence = congruence;
    }

    @Override public Interval convexHull () {
      return intervalSet.convexHull();
    }

    @Override public IntervalSet asSet () {
      return intervalSet;
    }

    @Override public boolean contains (BigInt value) {
      return intervalSet.contains(value);
    }

    @Override public Range union (Range other) {
      if (other instanceof IntervalSetRange) {
        Range unionRange;
        unionRange = from(this.intervalSet.join(((IntervalSetRange) other).intervalSet));
        unionRange.ensureCongruent(this.congruence.join(other.getCongruence()));
        return unionRange;
      } else {
        return other.union(this);
      }
    }

    @Override public Range add (Range other) {
      if (other instanceof IntervalSetRange) {
        Range unionRange;
        unionRange = from(this.intervalSet.add(((IntervalSetRange) other).intervalSet));
        unionRange.ensureCongruent(this.congruence.add(other.getCongruence()));
        return unionRange;
      } else {
        return other.add(this);
      }
    }

    @Override public Range mul (Range other) {
      if (other instanceof IntervalSetRange) {
        Range unionRange;
        unionRange = from(this.intervalSet.mul(((IntervalSetRange) other).intervalSet));
        unionRange.ensureCongruent(this.congruence.mul(other.getCongruence()));
        return unionRange;
      } else {
        return other.mul(this);
      }
    }

    @Override public Range mul (BigInt c) {
      // needs the congruence adjusted
      Congruence newCongruence = getCongruence().mul(c);
      return new IntervalSetRange(intervalSet.mul(c), newCongruence);
    }

    @Override public Range sub (BigInt c) {
      // needs the offset of the congruence adjusted
      Congruence newCongruence = getCongruence().sub(c);
      return new IntervalSetRange(intervalSet.sub(c), newCongruence);
    }

    @Override public Range divRoundInvards (BigInt c) {
      Congruence newCongruence = getCongruence();
      IntervalSet newIntervalSet = intervalSet.divRoundInvards(c);
      if (newIntervalSet == null)
        return null;
      if (!newCongruence.isConstantOnly())
        newCongruence = newCongruence.div(c);
      return new IntervalSetRange(newIntervalSet, newCongruence);
    }

    @Override public int lowerZeroBits (int noOfBits) {
      int res = noOfBits;
      for (Interval inv : intervalSet) {
        if (inv.isConstant()) {
          long val = inv.getConstant().longValue();
          int zeros = Long.numberOfTrailingZeros(val);
          if (zeros < res)
            res = zeros;
        } else {
          // for a convex set with more than one value, we don't know of any zero trailing bits
          res = 0;
        }
        if (res == 0)
          break;
      }
      if (res > 0)
        return res;
      return Math.min(noOfBits, congruence.lowerZeroBits());
    }

    @Override public int upperZeroBits (int noOfBits) {
      if (noOfBits < 1)
        return 0;
      int res = noOfBits;
      Interval enclosing = Interval.of(Bound.ZERO, BigInt.powerOfTwo(noOfBits - 1).sub(1));
      for (Interval interval : intervalSet) {
        if (interval.hasNegativeValues())
          return 0;
        int resLocal = -1;
        while (enclosing.contains(interval) && resLocal < noOfBits) {
          resLocal++;
          enclosing = enclosing.divRoundInvards(Bound.TWO);
        }
        if (resLocal <= 0)
          return 0;
        if (resLocal < res)
          res = resLocal;
      }
      return res;
    }

    @Override public int upperOneBits (int noOfBits) {
      if (noOfBits < 1)
        return 0;
      int res = noOfBits;
      for (Interval interval : intervalSet) {
        if (interval.hasNegativeValues())
          return 0;
        int resLocal = -1;
        BigInt one = BigInt.powerOfTwo(noOfBits - 1);
        BigInt upper = one.sub(1);
        BigInt lower = Bound.ZERO;
        do {
          one = one.shr(1);
          lower = lower.add(one);
          resLocal++;
        } while (Interval.of(lower, upper).contains(interval) && one.isPositive());
        if (resLocal < res)
          res = resLocal;
        if (res == 0)
          break;
      }
      return res;
    }

    @Override public BigInt numberOfDiscreteValues () {
      if (!intervalSet.low().isFinite() || !intervalSet.high().isFinite())
        throw new Error("requested bound of unbounded value");
      if (congruence.getScale().isZero())
        return Bound.ONE;

      // Sum the value counts for every Interval in the set
      BigInt result = Bound.ZERO;
      for (Interval interval : intervalSet) {
        BigInt valueCount = new IntervalRange(interval, getCongruence()).numberOfDiscreteValues();
        result = result.add(valueCount);
      }
      return result;
    }

    @Override public boolean isNonConvexApproximation () {
      for (Interval interval : intervalSet) {
        IntervalRange subRange = new IntervalRange(interval, getCongruence());
        if (!subRange.isNonConvexApproximation())
          return false;
      }
      return true;
    }

    @Override public boolean isFinite () {
      return intervalSet.isFinite();
    }

    @Override public BigInt getMax () {
      if (!intervalSet.high().isFinite())
        throw new Error("requested bound of unbounded value");
      return intervalSet.high().asInteger();
    }

    @Override public BigInt getMin () {
      if (!intervalSet.low().isFinite())
        throw new Error("requested bound of unbounded value");
      return intervalSet.low().asInteger();
    }

    @Override public Range ensureCongruent (Congruence c) {
      // Constant
      if (c.isConstantOnly()) {
        if (intervalSet.meet(IntervalSet.valueOf(c.getOffset())).isNone()) {
          throw new IllegalStateException();
        }
        return new IntervalSetRange(intervalSet, c);
      }
      // Adjust finite interval bounds to satisfy the congruence (for every interval)
      LinkedList<Interval> result = new LinkedList<Interval>();
      for (Interval interval : intervalSet) {
        result.add(interval.applyCongruence(c));
      }
      return new IntervalSetRange(new IntervalSet(result), c);
    }

    @Override public Iterator<BigInt> iterator () {
      if (!isFinite())
        throw new UnsupportedOperationException("Can not interate over infinite intervals");
      return intervalSet.iteratorIntWithStride(congruence.getScale());
    }

    @Override public String toString () {
      return intervalSet.toString();
    }
  }

}
