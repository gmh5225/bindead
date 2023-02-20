package bindead.domains.segments.heap;

import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Range;

public class PathString {

  final BigInt prefix; //  corresponds to p(?)* with address p

  public PathString (BigInt p) {
    prefix = p;
  }

  @Override public String toString () {
    return "[" + prefix + ", ..]";
  }

  public int compareTo (PathString other) {
    return prefix.compareTo(other.prefix);
  }

  @Override public boolean equals (Object other) {
    return other instanceof PathString && prefix.equals(((PathString) other).prefix);
  }

  @Override @Deprecated public int hashCode () {
    throw new UnimplementedException();
  }

  /**
   * currently we only allow single step prefixes as pathStrings, so a transitive pathString is just the pathstring of
   * its first component.
   */
  PathString plus (PathString pathString2) {
    return this;
  }

  public boolean is (Range ofs) {
    assert prefix.isFinite();
    return ofs.contains(prefix.asInteger());
  }

  public boolean isZeroOffset () {
    return prefix.isZero();
  }

  public BigInt getPrefix () {
    return prefix;
  }

  public boolean is (PathString pathString2) {
    return prefix.isEqualTo(pathString2.prefix);
  }
}
