package bindead.domains.gauge;

import static bindead.data.Linear.linear;
import static javalx.data.Option.some;
import static javalx.data.products.P2.tuple2;
import javalx.data.Option;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.persistentcollections.AVLMap;
import bindead.abstractsyntax.zeno.Zeno;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.NumVar;

/**
 * A visitor that evaluates expressions to wedge values,
 * over-approximating non-linear operations by [-oo,+oo]
 *
 * @author sergey
 */
public final class WedgeEvaluator extends ZenoRhsVisitorSkeleton<Wedge, AVLMap<NumVar, Wedge>> {
  public static final WedgeEvaluator INSTANCE = new WedgeEvaluator();

  @Override public Wedge visit (Zeno.Bin stmt, final AVLMap<NumVar, Wedge> wedges) {
    Wedge left = stmt.getLeft().accept(this, wedges);
    Wedge right = stmt.getRight().accept(this, wedges);
    Wedge approximation;
    switch (stmt.getOp()) {
    case Mul:
      approximation = left.multiply(right);
      break;
    case Div:
      approximation = Wedge.FULL;
      break;
    default:
      approximation = Wedge.FULL;
      break;
    }
    return approximation;
  }

  @Override public Wedge visit (Zeno.Rlin rlin, final AVLMap<NumVar, Wedge> wedges) {
    Linear lin = rlin.getLinearTerm();
    Wedge wedge = Wedge.singleton(lin.getConstant());
    for (Term t : lin.getTerms())
      if (!t.getCoeff().isEqualTo(Bound.ZERO))
        wedge = wedge.add(wedges.get(t.getId()).getOrElse(Wedge.FULL).multiply(Wedge.singleton(t.getCoeff())));
    return wedge;
  }

  @Override public Wedge visit (Zeno.RangeRhs range, final AVLMap<NumVar, Wedge> wedges) {
    Interval interval = range.getRange().convexHull();
    Bound lower = interval.low();
    Bound upper = interval.high();
    Option<Linear> l, u;
    if (lower.isFinite())
      l = some(linear(lower.asInteger()));
    else
      l = Option.<Linear>none();
    if (upper.isFinite())
      u = some(linear(upper.asInteger()));
    else
      u = Option.<Linear>none();
    return Wedge.wedge(tuple2(l, u));
  }
}