package satDomain;

class AffineEntry {
  int innerVar;
  final public int outerVar;
  boolean polarity;

  AffineEntry (AffineEntry ae) {
    outerVar = ae.outerVar;
    innerVar = ae.innerVar;
    polarity = ae.polarity;
  }

  AffineEntry (int o, int i, boolean p) {
    outerVar = o;
    innerVar = i;
    polarity = p;
  }

  @Override public int hashCode () {
    return outerVar;
  }

  @Override public String toString () {
    return "<o:" + outerVar + ", i:" + innerVar + ", p:" + polarity + ">";
  }
}
