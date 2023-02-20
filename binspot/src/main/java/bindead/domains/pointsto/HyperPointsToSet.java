package bindead.domains.pointsto;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.persistentcollections.AVLMap;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.data.NumVar.AddrVar;

class HyperPointsToSet implements Iterable<P2<AddrVar, Linear>> {
  private final static FiniteFactory fin = FiniteFactory.getInstance();

  // TODO AVLMap
  AVLMap<AddrVar, Linear> coefficients;

  Rhs offset;

  Linear sumOfFlags;

  private final boolean DEBUG = PointsToProperties.INSTANCE.debugOther.isTrue();

  @SuppressWarnings("unused") private void msg (String s) {
    if (DEBUG) {
      System.out.println("\nHyperPointsToSet: " + s);
    }
  }

  HyperPointsToSet (int size) {
    this(fin.literal(size, Bound.ZERO));
  }

  HyperPointsToSet (Rhs offset) {
    coefficients = AVLMap.<AddrVar, Linear>empty();
    this.offset = offset;
    this.sumOfFlags = Linear.ZERO;
  }

  private HyperPointsToSet (AVLMap<AddrVar, Linear> cm, Rlin o, Linear s) {
    coefficients = cm;
    offset = o;
    sumOfFlags = s;
  }

  void addCoefficient (AddrVar adr, Linear c) {
    Option<Linear> y = coefficients.get(adr);
    if (y.isSome()) {
      c = c.add(y.get());
    }
    coefficients = coefficients.bind(adr, c);
  }

  void addSumOfFlags (Linear c) {
    sumOfFlags = sumOfFlags.add(c);
  }

  void addOffset (Rlin o) {
    offset = getLinearOffset().add(o);
  }

  @Override public String toString () {
    String s = "";
    for (P2<AddrVar, Linear> e : coefficients) {
      s = s + "(" + e._1() + " -> " + e._2() + ") + ";
    }
    return s + "offset(" + offset + ") sumOfFlags(" + sumOfFlags + ")";
  }

  void add (HyperPointsToSet other) {
    for (P2<AddrVar, Linear> x : other.coefficients) {
      Linear myl = coefficients.get(x._1()).getOrNull();
      if (myl == null) {
        myl = x._2();
      } else {
        myl = myl.add(x._2());
      }
      coefficients = coefficients.bind(x._1(), myl);
    }
    offset = getLinearOffset().add(other.getLinearOffset());
    sumOfFlags = sumOfFlags.add(other.sumOfFlags);
  }

  void sub (HyperPointsToSet other) {
    for (P2<AddrVar, Linear> x : other.coefficients) {
      Linear myl = coefficients.get(x._1()).getOrNull();
      if (myl == null) {
        myl = x._2().negate();
      } else {
        myl = myl.sub(x._2());
      }
      coefficients = coefficients.bind(x._1(), myl);
    }
    offset = getLinearOffset().sub(other.getLinearOffset());
    sumOfFlags = sumOfFlags.sub(other.sumOfFlags);
  }

  Rlin getLinearOffset () {
    return (Rlin) offset;
  }

  boolean isScalar () {
    boolean isScalar = coefficients.isEmpty();
    // assert !isScalar || sumOfFlags.isZero(); // for scalars, sum must be zero
    return isScalar;
  }


  void setToTop () {
    coefficients = AVLMap.empty();
    offset = fin.range(0, Interval.TOP);
    sumOfFlags = Linear.ZERO;
  }

  AVLMap<AddrVar, Linear> getTerms () {
    return coefficients;
  }

  void addOffset (int size, BigInt o) {
    addOffset(fin.linear(size, Linear.linear(o)));
  }

  public HyperPointsToSet mul (BigInt coeff) {
    AVLMap<AddrVar, Linear> result = AVLMap.empty();
    for (P2<AddrVar, Linear> e : coefficients)
      result = result.bind(e._1(), e._2().smul(coeff));
    Rlin o = ((Rlin) offset).smul(coeff);
    Linear s = sumOfFlags.smul(coeff);
    return new HyperPointsToSet(result, o, s);
  }

  @Override public Iterator<P2<AddrVar, Linear>> iterator () {
    return coefficients.iterator();
  }
}
