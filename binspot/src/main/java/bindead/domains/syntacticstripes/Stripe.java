package bindead.domains.syntacticstripes;

import javalx.numeric.BigInt;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.data.Linear;
import bindead.data.Linear.Divisor;
import bindead.data.NumVar;

class Stripe {
  private static final ZenoFactory zeno = ZenoFactory.getInstance();
  final NumVar special;

  public Stripe (NumVar special) {
    this.special = special;
  }

  @Override public String toString () {
    return special.toString();
  }

  Linear asLinear () {
    return Linear.linear(special);
  }

  Linear asLinear (Divisor gcd, BigInt c) {
    assert gcd.get().isPositive();
    return Linear.linear(c, Linear.term(gcd.get(), special));
  }

  Linear asNegatedLinear (Divisor gcd, BigInt c) {
    assert gcd.get().isPositive();
    return Linear.linear(c, Linear.term(gcd.get().negate(), special));
  }

  Test asEquality (Linear lin) {
    Linear eq = lin.add(asLinear());
    return zeno.comparison(eq, ZenoTestOp.EqualToZero);
  }

  Test upperBoundPredicate (Linear lin, BigInt u) {
    Linear eq = lin.add(asLinear()).add(u);
    return zeno.comparison(eq, ZenoTestOp.LessThanOrEqualToZero);
  }

  Test lowerBoundPredicate (Linear lin, BigInt u) {
    Linear eq = asLinear().add(u).sub(lin);
    return zeno.comparison(zeno.linear(eq), ZenoTestOp.LessThanOrEqualToZero);
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + (special == null ? 0 : special.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Stripe other = (Stripe) obj;
    if (special == null) {
      if (other.special != null)
        return false;
    } else if (!special.equalTo(other.special))
      return false;
    return true;
  }
}
