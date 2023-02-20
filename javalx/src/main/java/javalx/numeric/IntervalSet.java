package javalx.numeric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import javalx.data.ConcatIterator;
import javalx.data.Option;
import javalx.fn.Fn2;

public class IntervalSet implements Comparable<IntervalSet>, Iterable<Interval> {
  /**
   * Per definition ordered from low to high, where single intervals are disjoint and not adjacent, and infinite values
   * are only allowed at the outer bounds.
   */
  private final LinkedList<Interval> intervals;

  public static final IntervalSet ZERO = new IntervalSet(Interval.ZERO);
  public static final IntervalSet MINUSONE = new IntervalSet(Interval.MINUSONE);
  public static final IntervalSet TOP = new IntervalSet(Interval.TOP);
  public static final IntervalSet BOOLEANTOP = new IntervalSet(Interval.BOOLEANTOP);
  public static final IntervalSet GREATER_THAN_OR_EQUAL_TO_ZERO = new IntervalSet(Interval.GREATER_THAN_OR_EQUAL_TO_ZERO);


  private static final Comparator<Interval> COMPARATOR_LOWER_BOUND = new Comparator<Interval>() {
    @Override public int compare (Interval t1, Interval t2) {
      final Bound t1Low = t1.low();
      final Bound t2Low = t2.low();
      return t1Low.compareTo(t2Low);
    }
  };

  public static final Fn2<IntervalSet, IntervalSet, IntervalSet> join =
    new Fn2<IntervalSet, IntervalSet, IntervalSet>() {
      @Override public IntervalSet apply (IntervalSet a, IntervalSet b) {
        return a.join(b);
      }
    };
  public static final Fn2<IntervalSet, IntervalSet, IntervalSet> widen =
    new Fn2<IntervalSet, IntervalSet, IntervalSet>() {
      @Override public IntervalSet apply (IntervalSet a, IntervalSet b) {
        return a.widen(b);
      }
    };

  /**
   * @see #norm(List)
   * @param base
   */
  public IntervalSet (Interval... base) {
    this.intervals = norm(Arrays.asList(base));
  }

  /**
   * @see #norm(List)
   * @param base
   */
  public IntervalSet (List<Interval> base) {
    this.intervals = norm(new ArrayList<Interval>(base));
  }

  /**
   * Side-effect: sorts given list!<br/>
   * 1. Sorts input by lower bound<br/>
   * 2. Iterates over input and checks for possibilities to join (overlap/adjacence)<br/>
   *
   * @param input
   * @return Returns normalized lists of intervals
   */
  private static LinkedList<Interval> norm (List<Interval> input) {
    if (input.isEmpty())
      throw new IllegalArgumentException("Given list of intervals must not be emtpy!");
    // 1. Sort by lower bound
    Collections.sort(input, COMPARATOR_LOWER_BOUND);
    // 2. Normalize, low to high
    final Iterator<Interval> it = input.iterator();
    final LinkedList<Interval> result = new LinkedList<Interval>();

    Interval prev = it.next();
    result.add(prev);
    while (it.hasNext()) {
      Interval cur = it.next();
      // Look if there's something to drop/join
      if (prev.contains(cur)) {
        continue; // Drop cur interval
      } else if (cur.overlaps(prev) || prev.isAdjacent(cur)) {
        // Replace prev with joined interval
        cur = prev.join(cur);
        result.removeLast();
      }
      result.add(cur);
      prev = cur;
    }
    return result;
  }

  /**
   * @param bigInts
   * @return An {@link IntervalSet} containing all intervals defined by the bounds of the given {@link BigInt}s.
   */
  public static IntervalSet valueOf (BigInt... bigInts) {
    final ArrayList<Interval> intervals = new ArrayList<Interval>(bigInts.length);
    for (BigInt bigInt : bigInts) {
      final Interval boundedInt = Interval.of(bigInt);
      intervals.add(boundedInt);
    }
    return new IntervalSet(intervals);
  }

  /**
   * @param intervals
   * @return An {@link Option} containing an {@link IntervalSet} with all given intervals. {@link Option#none()} if they
   *         are not wellformed (or intervals.isEmpty())
   */
  public static IntervalSet valueOf (Interval... intervals) {
    return new IntervalSet(intervals);
  }

  public static IntervalSet valueOf (String str) {
    str = removeBraces(str);
    List<Interval> intervalList = new LinkedList<Interval>();
    // "," not followed by 0 or more whitespaces AND (1 or more digits with sign) OR (+oo) AND "]"
    String[] intervalStrs = str.split(",(?!([\\s]*((\\+|-)?[\\d]+|(\\+oo))\\]))");
    for (String intervalStr : intervalStrs) {
      intervalStr = intervalStr.trim();
      intervalList.add(Interval.of(intervalStr.trim()));
    }
    return new IntervalSet(intervalList);
  }

  private static String removeBraces (String str) {
    boolean startsWithBrace = str.startsWith("{");
    boolean endsWithBrace = str.endsWith("}");
    if (startsWithBrace != endsWithBrace)
      throw new NumberFormatException("IntervalSet misses a opening or closing brace!");
    if (startsWithBrace)
      str = str.substring(1, str.length() - 1);
    return str;
  }

  /**
   * @see Interval#signedTop(int)
   */
  public static IntervalSet signedTop (int size) {
    return IntervalSet.valueOf(Interval.signedTop(size));
  }

  /**
   * @see Interval#unsignedTop(int)
   */
  public static IntervalSet unsignedTop (int size) {
    return IntervalSet.valueOf(Interval.unsignedTop(size));
  }

  /**
   * Compares the IntervalSets by the sequence of their intervals lower bounds
   */
  @Override public int compareTo (IntervalSet other) {
    final Iterator<Interval> thisIt = this.iterator();
    final Iterator<Interval> otherIt = other.iterator();

    while (thisIt.hasNext() && otherIt.hasNext()) {
      final int compResult = COMPARATOR_LOWER_BOUND.compare(thisIt.next(), otherIt.next());
      if (compResult != 0) {
        return compResult;
      }
    }
    if (thisIt.hasNext()) {
      return 1; // Implies that !other.hasNext()
    } else {
      if (otherIt.hasNext())
        return -1;
      else
        return 0;
    }
  }

  /**
   * Puts all intervals of both sets together and {@link #norm(List)}s them.
   *
   * @param other
   * @return a new {@link IntervalSet} with the result
   */
  public IntervalSet join (IntervalSet other) {
    final ArrayList<Interval> newIntervalsBase = new ArrayList<Interval>(other.intervals.size() + intervals.size());
    newIntervalsBase.addAll(other.intervals);
    newIntervalsBase.addAll(intervals);
    return new IntervalSet(newIntervalsBase);
  }

  /**
   * Widens this by the given {@link IntervalSet}. (Where this is assumed to be subsetOrEqual of other)
   *
   * @param other
   * @return a new {@link IntervalSet} with the result
   */
  public IntervalSet widen (IntervalSet other) {
    final Queue<Interval> thisIt = new LinkedList<Interval>(this.intervals);
    final Queue<Interval> otherIt = new LinkedList<Interval>(other.intervals);
    final LinkedList<Interval> result = new LinkedList<Interval>();

    // Preconditions: Both iterators values are normed as defined in #intervals
    Interval highestInterval = null;    // Needed because there's no guarantee that result.peekLast() is the highest!
    boolean isFirstIntervalInThis = true; // Needed to detect APPEAR outside neg
    while (!thisIt.isEmpty() && !otherIt.isEmpty()) {
      final Interval thisCur = thisIt.poll();
      Interval otherCur = otherIt.peek();
      boolean thisCurIsMeet = false;     // Flag whether thisCur is met by/the same as any interval from other

      // Handle everything which may interfere with thisCur:
      // o < t; o <= t; o >= t (where t = thisCur and o = otherCur Interval)
      while (otherCur.high().isLessThanOrEqualTo(thisCur.high()) || otherCur.low().isLessThanOrEqualTo(thisCur.high())) {
        if (otherCur.isEqualTo(thisCur)) {
          // Simply forget about otherCur
          thisCurIsMeet |= true; // Stayed the same, so it should be adopted to result
        } else {
          if (!otherCur.overlaps(thisCur)) {
            thisCurIsMeet |= false;      // thisCur might DISAPPEAR...
            // Easy case: OtherCur is strictly smaller then thisCur: (APPEAR)
            // Handle special case: APPEAR outside! -> [-oo,..]
            if (isFirstIntervalInThis)
            {
              final Interval grownInterval = Interval.downFrom(thisCur.high());
              highestInterval = grownInterval;
              result.add(grownInterval);
            } else {
              // APPEAR inside ???
              highestInterval = otherCur;
              result.add(otherCur);
            }
          } else {
            thisCurIsMeet |= true;
            // o and t overlap but are not equal, which leaves us with 6 possible situations in theory. But only 4 are
            // allowed!
            final int loMode = otherCur.low().compareTo(thisCur.low());
            final int hiMode = otherCur.high().compareTo(thisCur.high());

            // Handle 2 forbidden situations (where both bounds move)
            if (Math.abs(loMode + hiMode) == 2) {
              // MOVE: Not allowed!
              throw new IllegalArgumentException("Intervals must not MOVE!");
            }
            final Bound lo;
            if (loMode < 0) {
              // Grow lower bound
              if (highestInterval == null) {
                lo = Bound.NEGINF;
              } else {
                lo = highestInterval.high();       // This will join them when normed later on
              }
            } else {
              // Take "newer lower" bound.
              lo = otherCur.low();
            }

            final Bound hi;
            if (hiMode > 0) {
              // Grow higher bound
              // Check for stop case where grown interval already stops at the same bound as an existing one
              // E.g.: [0] [2,4] [6] widen [0,4] [6] => [0,4] [6] (where grown interval [0]->[0,4] ends at [2,4])
              final Iterator<Interval> thisNextIt = thisIt.iterator();
              Interval thisNext = null;
              while (thisNextIt.hasNext()) {
                // EAT next intervals..
                thisNext = thisNextIt.next();
                if (thisNext.high().isLessThan(otherCur.high())) {
                  thisNextIt.remove();  // EAT!
                } else if (otherCur.high().isEqualTo(thisNext.high())) {
                  thisNextIt.remove();  // EAT!
                  break;
                } else if (otherCur.high().isLessThan(thisNext.high())) {
                  break;
                }
              }
              if (thisNext != null && thisNext.high().isEqualTo(otherCur.high())) {
                hi = otherCur.high(); // Hurray, special case!
              } else {
                // Grow to next upper bound
                final Interval next;
                next = thisIt.peek();
                if (next == null) {
                  hi = Bound.POSINF;
                } else {
                  hi = next.high();
                }
              }
            } else {
              // Take "newer" lower bound.
              hi = otherCur.high();
            }
            // Check if we have to continue at all
            if (lo.isEqualTo(Bound.POSINF)) {
              // This is a special case, which may occur during (inner) widening. As all further intervals are joined
              // anyway, we quit here!
              return new IntervalSet(result);
            }
            // Construct widen'ed Interval
            final Interval widened = Interval.of(lo, hi);
            highestInterval = widened;
            result.add(widened);
          }
        }
        // Proceed in other
        otherIt.remove();
        if (otherIt.isEmpty()) {
          break;
        }
        otherCur = otherIt.peek();
      }

      if (!thisCurIsMeet) {
        // DISAPPEAR: Should not happen: All intervals from this should overlap at least one from other
        throw new IllegalArgumentException("Intervals must not DISAPPEAR!");
      }
      // Proceed in this
      if (highestInterval == null) {
        highestInterval = thisCur;
      } else {
        highestInterval = highestInterval.high().isLessThan(thisCur.high()) ? thisCur : highestInterval;
      }
      result.add(thisCur);
      isFirstIntervalInThis = false;
    }

    // Handle appendix
    if (!thisIt.isEmpty()) {
      for (Interval newInterval : thisIt)
      {
        if (!highestInterval.contains(newInterval)) {
          // DISAPPEAR: Should not happen!
          throw new IllegalArgumentException("Intervals must not DISAPPEAR!");
        }
      }
    }
    if (!otherIt.isEmpty()) {
      // APPEAR outside: Leads to unbounded growth -> POSINF
      final Interval last = result.pollLast();
      final Interval lastGrown = Interval.of(last.low(), Bound.POSINF);
      result.add(lastGrown);
    }
    return new IntervalSet(result);     // Gets norm(...)ed here
  }

  // OPT Optimization possible here!!! Based on the ordering of the interval lists we don't have to meet every
  // Interval..
  public Option<IntervalSet> meet (IntervalSet other) {
    return createCartesianOpt(other, new Fn2<Interval, Interval, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, Interval b) {
        return a.meet(b);
      }
    });
  }

  /**
   * Implemented using join, i.e. {@code a <= b iff a v b = b}
   */
  public boolean subsetOrEqual (IntervalSet other) {
    IntervalSet joined = this.join(other);
    return joined.isEqualTo(other);
  }

  @Override public Iterator<Interval> iterator () {
    return intervals.iterator();
  }

  public Iterator<BigInt> iteratorInt () {
    return new ConcatIterator<BigInt>(intervals);
  }

  public Iterator<BigInt> iteratorIntWithStride (BigInt stride) {
    return new StridedIterator(this, stride);
  }


  public Bound low () {
    final Interval lowestInterval = intervals.getFirst();
    return lowestInterval.low();
  }

  public Bound high () {
    final Interval highestInterval = intervals.getLast();
    return highestInterval.high();
  }

  /**
   * Returns {@code true} if this range contains a given value.
   */
  public boolean contains (BigInt value) {
    for (Interval interval : intervals) {
      if (interval.contains(value))
        return true;
    }
    return false;
  }

  public boolean isEqualTo (IntervalSet other) {
    final Iterator<Interval> otherIt = other.iterator();
    final Iterator<Interval> thisIt = this.iterator();
    while (otherIt.hasNext() && thisIt.hasNext()) {
      Interval thisInterval = thisIt.next();
      Interval otherInterval = otherIt.next();
      if (!otherInterval.isEqualTo(thisInterval))
        return false;
    }
    return !otherIt.hasNext() && !thisIt.hasNext();
  }

  /**
   * @return <code>true</code> iff this {@link IntervalSet} {@link #isConsistent()} and its {@link Interval}
   *         {@link Interval#isConstant()}
   */
  public boolean isConstant () {
    if (intervals.size() == 1) {
      return intervals.getFirst().isConstant();
    }
    return false;
  }

  /**
   * @see #intervals
   * @return <code>true</code> iff both bounds low and high are bounded
   */
  public boolean isFinite () {
    return low().isFinite() && high().isFinite();
  }

  /**
   * Return the constant if this {@link IntervalSet} {@link #isConstant()} (e.g [c, c]) or <code>null</code>.
   *
   * @return The constant denoted by this {@link IntervalSet} or <code>null</code>.
   */
  public BigInt getConstantOrNull () {
    if (isConstant()) {
      return low().asInteger();
    }
    return null;
  }

  /**
   * Reduces this {@link IntervalSet} to a simple {@link Interval}.
   *
   * @return [low(), high()]
   */
  public Interval convexHull () {
    return Interval.of(low(), high());
  }

  /**
   * @return NEW list around {@link #intervals}
   */
  List<Interval> getIntervals () {
    return new ArrayList<Interval>(intervals);
  }

  public IntervalSet add (IntervalSet right) {
    return createCartesian(right, new Fn2<Interval, Interval, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, Interval b) {
        return Option.some(a.add(b));
      }
    });
  }

  public IntervalSet add (BigInt scalar) {
    return createCartesian(scalar, new Fn2<Interval, BigInt, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, BigInt scalar) {
        return Option.some(a.add(scalar));
      }
    });
  }

  public IntervalSet sub (IntervalSet right) {
    return createCartesian(right, new Fn2<Interval, Interval, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, Interval b) {
        return Option.some(a.sub(b));
      }
    });
  }

  public IntervalSet sub (BigInt scalar) {
    return createCartesian(scalar, new Fn2<Interval, BigInt, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, BigInt scalar) {
        return Option.some(a.sub(scalar));
      }
    });
  }

  public IntervalSet mul (IntervalSet right) {
    return createCartesian(right, new Fn2<Interval, Interval, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, Interval b) {
        return Option.some(a.mul(b));
      }
    });
  }

  public IntervalSet mul (BigInt right) {
    return createCartesian(right, new Fn2<Interval, BigInt, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, BigInt b) {
        return Option.some(a.mul(b));
      }
    });
  }

  public IntervalSet divRoundInvards (BigInt right) {
    return createCartesian(right, new Fn2<Interval, BigInt, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, BigInt b) {
        return Option.some(a.divRoundInvards(b));
      }
    });
  }

  public IntervalSet divRoundZero (IntervalSet right) {
    return createCartesian(right, new Fn2<Interval, Interval, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, Interval b) {
        return Option.fromNullable(a.divRoundZero(b));
      }
    });
  }

  public IntervalSet shl (IntervalSet right) {
    return createCartesian(right, new Fn2<Interval, Interval, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, Interval b) {
        return Option.some(a.shl(b));
      }
    });
  }

  public IntervalSet shr (IntervalSet right) {
    return createCartesian(right, new Fn2<Interval, Interval, Option<Interval>>() {
      @Override public Option<Interval> apply (Interval a, Interval b) {
        return Option.some(a.shr(b));
      }
    });
  }

  public IntervalSet negate () {
    return this.mul(MINUSONE);
  }

  /**
   * Calls fn on every element of 'this x other'. The resulting list of {@link Interval}s is returned as
   * normed {@link IntervalSet}. If result {@link Option#isNone()} it's omitted.
   *
   * @param other
   * @param fn
   * @return The newly created {@link IntervalSet} filled with the normed results of fn
   */
  public IntervalSet createCartesian (IntervalSet other, Fn2<Interval, Interval, Option<Interval>> fn) {
    Option<IntervalSet> result = createCartesianOpt(other, fn);
    return result.get();
  }

  /**
   * Calls {@code fn} on every element of 'this x other'. The resulting list of {@link Interval}s is returned as
   * normed {@link IntervalSet}. If fn's result {@link Option#isNone()} it's omitted.
   *
   * @param other
   * @param fn  The function to apply on the product of elements from this and other.
   * @return The newly created {@link IntervalSet} filled with the normed results of {@code fn}. If results are empty,
   *         {@link Option#none()} is returned
   */
  public Option<IntervalSet> createCartesianOpt (IntervalSet other, Fn2<Interval, Interval, Option<Interval>> fn) {
    LinkedList<Interval> result = new LinkedList<Interval>();
    for (Interval thisInt : this.getIntervals()) {
      for (Interval otherInt : other.getIntervals()) {
        Option<Interval> resultOption = fn.apply(thisInt, otherInt);
        if (resultOption.isSome())
          result.add(resultOption.get());
      }
    }
    if (result.isEmpty())
      return Option.none();
    else
      return Option.some(new IntervalSet(result));
  }

  /**
   * Calls fn on every element of 'this'. The resulting list of {@link Interval}s is returned as normed
   * {@link IntervalSet}. If result {@link Option#isNone()} it's omitted.
   *
   * @param fn
   * @return The newly created {@link IntervalSet} filled with the normed results of fn
   */
  public <T> IntervalSet createCartesian (T arg, Fn2<Interval, T, Option<Interval>> fn) {
    final LinkedList<Interval> result = new LinkedList<Interval>();
    for (Interval thisInt : this.getIntervals()) {
      final Option<Interval> resultOption = fn.apply(thisInt, arg);
      if (resultOption.isSome())
        result.add(resultOption.get());
    }
    return new IntervalSet(result);
  }

  @Override public boolean equals (Object o) {
    return o instanceof IntervalSet && this.isEqualTo((IntervalSet) o);
  }

  @Override public int hashCode () {
    int hash = 5;
    for (Interval interval : intervals) {
      hash = 7 * hash + interval.hashCode();
    }
    return hash;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    if (intervals.size() > 1)
      builder.append("{ ");
    boolean first = true;
    for (Interval interval : intervals) {
      if (first) {
        builder.append(interval.toString());
        first = false;
      } else {
        builder.append(", ").append(interval.toString());
      }
    }
    if (intervals.size() > 1)
      builder.append(" }");
    return builder.toString();
  }

  private static class StridedIterator implements Iterator<BigInt> {
    private final Iterator<Interval> intervalIterator;
    private final BigInt stride;
    private Interval curInterval;
    private BigInt ptr;

    private StridedIterator (IntervalSet set, BigInt stride) {
      this.intervalIterator = set.iterator();
      this.stride = stride;
      this.curInterval = intervalIterator.next();
      this.ptr = curInterval.low().asInteger();
    }

    private StridedIterator (IntervalSet set) {
      this(set, Bound.ONE);
    }

    @Override public boolean hasNext () {
      final boolean strideInCurrentInterval =
        stride.isPositive() && !curInterval.high().asInteger().isLessThan(ptr);
      if (strideInCurrentInterval) {
        return true;
      } else {
        boolean result = false;
        while (intervalIterator.hasNext())
        {
          curInterval = intervalIterator.next();

          if (curInterval.contains(ptr)) {
            result = true;
            break;
          }

          final BigInt timesStride = curInterval.low().asInteger().sub(ptr).div(stride);
          ptr = ptr.add(stride.mul(timesStride.add(Bound.ONE)));

          if (curInterval.contains(ptr)) {
            result = true;
            break;
          }
        }

        return result;
      }
    }

    @Override public BigInt next () {
      if (!hasNext())
        throw new NoSuchElementException();

      BigInt next = ptr;
      ptr = ptr.add(stride);
      return next;
    }

    @Override public void remove () {
      throw new UnsupportedOperationException();
    }
  }

}
