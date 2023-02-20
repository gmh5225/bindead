package javalx.numeric;

import javalx.xml.XmlPrintable;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents a fixed, finite range of bits, bytes, etc. to describe the access range in regions.
 * Note that this class cannot describe an empty range of bits as it relies on well-formed intervals.
 */
public class FiniteRange implements Comparable<FiniteRange>, XmlPrintable {

  private final BigInt low;
  private final BigInt high;

  private FiniteRange (BigInt lo, BigInt up) {
    this.low = lo;
    this.high = up;
    assert this.low.isLessThanOrEqualTo(this.high) : "Range is not well-formed: [" + lo + ", " + up + "]";
  }

  /**
   * Build a new FiniteRange with the lower and the upper bound set to {@code c},
   * that is, an interval with span 1.
   */
  public static FiniteRange of (long c) {
    BigInt c1 = BigInt.of(c);
    return new FiniteRange(c1, c1);
  }

  /**
   * Build a new FiniteRange with {@code lo} as lower bound and {@code up} as upper bound.
   */
  public static FiniteRange of (long lo, long up) {
    return new FiniteRange(BigInt.of(lo), BigInt.of(up));
  }

  public static FiniteRange of (BigInt lo, long span) {
    return new FiniteRange(lo, lo.add(BigInt.of(span - 1)));
  }

  public BigInt low () {
    return low;
  }

  public BigInt high () {
    return high;
  }

  /**
   * Not a "semantically correct" ordering, but just the lexicographic order on (lower, upper), so that we can store
   * FiniteRanges in collections.
   * */
  @Override public int compareTo (FiniteRange b) {
    int cmp1 = low.compareTo(b.low);
    return cmp1 == 0 ? high.compareTo(b.high) : cmp1;
  }

  public boolean overlaps (Interval other) {
    return low.isLessThanOrEqualTo(other.high) && other.low.isLessThanOrEqualTo(high);
  }

  public FiniteRange join (FiniteRange other) {
    return new FiniteRange(low.min(other.low), high.max(other.high));
  }

  public boolean isEqualTo (FiniteRange other) {
    return low.isEqualTo(other.low) && high.isEqualTo(other.high);
  }

  @Override public boolean equals (Object o) {
    return o instanceof FiniteRange && this.isEqualTo((FiniteRange) o);
  }

  @Override public int hashCode () {
    int hash = 5;
    hash = 23 * hash + this.low.hashCode();
    hash = 23 * hash + this.high.hashCode();
    return hash;
  }

  /**
   * Return the "span" of this FiniteRange that is hi - lo + 1, if this FiniteRange is finite or null if this is
   * FiniteRange has an infinite span.
   *
   * @return The span of this FiniteRange or null for infinite span FiniteRanges.
   */
  public BigInt getSpan () {
    return high.sub(low).add(Bound.ONE);
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    return builder.e("FiniteRange").t(toString()).up();
  }

  @Override public String toString () {
    String fmt = "[%s:%s]";
    return String.format(fmt, low, getSpan());
  }

  public Interval toInterval () {
    return Interval.of(low, high);
  }
}
