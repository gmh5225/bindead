package bindead.domains.wrapping;

import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Cmp;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.FiniteExprVisitor;
import bindead.abstractsyntax.zeno.Zeno;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.combinators.ZenoStateBuilder;
import bindead.exceptions.DomainStateException.UnimplementedMethodException;

public class WrappingStateBuilder extends ZenoStateBuilder implements
    FiniteExprVisitor<Linear, WrappingStateBuilder.WrapInfo> {
  static final ZenoFactory zeno = ZenoFactory.getInstance();
  private final QueryChannel state;
  private final NumVar[] tempVars = {NumVar.getSingleton("wrapTmpOne"), NumVar.getSingleton("wrapTmpTwo")};
  private int lastTemp = 0;

  WrappingStateBuilder (QueryChannel state) {
    this.state = state;
  }

  NumVar getTemp () {
    assert lastTemp < tempVars.length;
    return tempVars[lastTemp++];
  }

  private void killTemps () {
    while (lastTemp > 0)
      getChildOps().addKill(tempVars[--lastTemp]);
  }

  public NumVar assignToTemp (Zeno.Rhs rhs) {
    NumVar var = getTemp();
    getChildOps().addAssignment(zeno.assign(zeno.variable(var), rhs));
    return var;
  }

  private void doPointTest (Finite.Test test, ZenoTestOp op) {
    int size = test.getSize();
    if (size == 0) {
      Linear expr = test.toLinear();
      getChildOps().addTest(zeno.comparison(expr, op));
      return;
    }
    Interval uTop = Interval.unsignedTop(size);
    Interval sTop = Interval.signedTop(size);

    Linear lLin = test.getLeftExpr();
    Linear rLin = test.getRightExpr();

    Interval lRange = state.queryRange(lLin).convexHull();
    Interval rRange = state.queryRange(rLin).convexHull();

    BigInt lSpan = lRange.getSpan();
    BigInt rSpan = rRange.getSpan();
    BigInt width = uTop.getSpan();

    // set the left hand side to a signed or unsigned top value if it encompasses more values that the top value
    if (lSpan == null || width.isLessThan(lSpan)) {
      lSpan = width;
      // TODO: we could improve guessing sTop or uTop if we check if rRange can be shifted to fit into sTop
      if (sTop.contains(rRange) && rRange.hasNegativeValues())
        lRange = sTop;
      else
        lRange = uTop;
      NumVar var = linearToVar(lLin);
      getChildOps().addAssignment(zeno.assign(zeno.variable(var), zeno.range(Range.from(lRange))));
      lLin = Linear.linear(var);
    }

    if (rSpan == null || width.isLessThan(rSpan)) {
      rSpan = width;
      // TODO: see above
      if (sTop.contains(lRange) && lRange.hasNegativeValues())
        rRange = sTop;
      else
        rRange = uTop;
      NumVar var = linearToVar(lLin);
      getChildOps().addAssignment(zeno.assign(zeno.variable(var), zeno.range(Range.from(rRange))));
      lLin = Linear.linear(var);
    }

    // ensure that the lower bounds lie within the same quadrant
    BigInt diff = lRange.low().asInteger().sub(rRange.low().asInteger());
    BigInt shift = diff.abs().divRoundDown(width).mul(width).mul(diff.sign());
    rRange = rRange.add(shift);
    rLin = rLin.add(shift);

    // the range with the smaller lower l overlaps in two quadrants with the other range iff the other range's upper
    // bound u is greater than l+width; in this case, over-approximate the result by not applying the test
    boolean applicable;
    if (lRange.low().isLessThan(rRange.low()))
      applicable = rRange.high().asInteger().isLessThan(lRange.low().asInteger().add(width));
    else
      applicable = lRange.high().asInteger().isLessThan(rRange.low().asInteger().add(width));
    if (!applicable)
      return;

    Linear t = lLin.sub(rLin);
    getChildOps().addTest(zeno.comparison(t, op));
    getChildOps().hoist();
    return;
  }

  private void doRelationalTest (Finite.Test test, WrapInfo wi, BigInt slack, ZenoTestOp op) {
    Linear lLin = wrapLinear(test.getLeftExpr(), wi);
    Linear rLin = wrapLinear(test.getRightExpr(), wi);

    Linear t = lLin.sub(rLin).add(slack);
    getChildOps().addTest(zeno.comparison(t, op));
    getChildOps().hoist();
  }

  void runTest (Finite.Test test) {
    switch (test.getOperator()) {
    case Equal:
      doPointTest(test, ZenoTestOp.EqualToZero);
      break;
    case NotEqual:
      doPointTest(test, ZenoTestOp.NotEqualToZero);
      break;
    case SignedLessThan:
      doRelationalTest(test, WrapInfo.signed(test.getSize()), Bound.ONE, ZenoTestOp.LessThanOrEqualToZero);
      break;
    case SignedLessThanOrEqual:
      doRelationalTest(test, WrapInfo.signed(test.getSize()), Bound.ZERO, ZenoTestOp.LessThanOrEqualToZero);
      break;
    case UnsignedLessThan:
      doRelationalTest(test, WrapInfo.unsigned(test.getSize()), Bound.ONE, ZenoTestOp.LessThanOrEqualToZero);
      break;
    case UnsignedLessThanOrEqual:
      doRelationalTest(test, WrapInfo.unsigned(test.getSize()), Bound.ZERO, ZenoTestOp.LessThanOrEqualToZero);
      break;
    default:
      break;
    }
    killTemps();
  }

  void runAssign (Finite.Assign stmt, WrappingStateBuilder builder) {
    if (stmt.getRhs() instanceof Cmp) { // over-approximate the test result with top
      // TODO: it would be possible to execute the test and assign 0, 1 or [0, 1] depending on the feasibility
      RangeRhs top = zeno.range(Interval.BOOLEANTOP);
      Zeno.Assign assign = zeno.assign(zeno.variable(stmt.getLhs().getId()), top);
      builder.getChildOps().addAssignment(assign);
    } else {
      Linear rhs = stmt.getRhs().accept(this, WrapInfo.noWrap);
      NumVar resVar = rhs.getSingleVarOrNull();
      if (resVar == null || !resVar.equalTo(stmt.getLhs().getId())) {
        Zeno.Assign assign = zeno.assign(zeno.variable(stmt.getLhs().getId()), zeno.linear(rhs));
        builder.getChildOps().addAssignment(assign);
      }
    }
    builder.getChildOps().hoist();
    killTemps();
  }

  @Override public Linear visit (Finite.Bin expr, WrapInfo wi) {
    WrappingBinOpVisitor binOpVisitor = new WrappingBinOpVisitor(this);
    return binOpVisitor.visit(expr);
  }

  @Override public Linear visit (Finite.SignExtend expr, WrapInfo wi) {
    int rhsSize = expr.getExpr().getSize();
    return expr.getExpr().accept(this, WrapInfo.signed(rhsSize));
  }

  @Override public Linear visit (Finite.Convert expr, WrapInfo wi) {
    int rhsSize = expr.getExpr().getSize();
    if (wi.getSize() > rhsSize)
      return expr.getExpr().accept(this, WrapInfo.unsigned(rhsSize));
    else
      return expr.getExpr().accept(this, WrapInfo.noWrap);
  }

  @Override public Linear visit (Finite.FiniteRangeRhs expr, WrapInfo wi) {
    if (expr.getRange().isConstant())
      return Linear.linear(expr.getRange().getConstant());
    NumVar var = assignToTemp(zeno.range(expr.getRange()));
    if (wi.useZenoSemantics() || wi.getInterval().contains(expr.getRange()))
      return Linear.linear(var);
    // although the constant range could be wrapped manually, we use the wrap child op since it already implements this
    // logic; if the result fits into a single quadrant, we apply the same number of child ops anyway
    getChildOps().addWrap(var, wi.getInterval(), wi.saturated);
    return Linear.linear(var);
  }

  private Linear wrapLinear (Linear linear, WrapInfo wi) {
    if (wi.useZenoSemantics())
      return linear;
    Interval wrappingRange = wi.getInterval();
    BigInt con = linear.isConstantOnly() ? linear.getConstant() : null;
    if (con != null) {
      if (!wrappingRange.contains(con)) {
        BigInt diff = wrappingRange.high().asInteger().sub(con);
        BigInt width = wrappingRange.getSpan();
        BigInt shift = diff.div(width).mul(width);
        linear = linear.add(shift);
      }
    } else {
      Interval value = state.queryRange(linear).convexHull();
      if (!wrappingRange.contains(value)) {
        NumVar var = linearToVar(linear);
        getChildOps().addWrap(var, wrappingRange, wi.saturated);
        linear = Linear.linear(var);
      }
    }
    return linear;
  }

  private NumVar linearToVar (Linear lin) {
    NumVar var = lin.getSingleVarOrNull();
    if (var == null) {
      var = assignToTemp(zeno.linear(lin));
    }
    return var;
  }

  @Override public Linear visit (Rlin expr, WrapInfo wi) {
    return wrapLinear(expr.getLinearTerm(), wi);
  }

  @Override public Linear visit (Cmp expr, WrapInfo data) {
    // compares should not appear on this level anymore
    // usually the Finite Predicates Domain translates them away.
    // So if this is reached then the domain is missing.
    throw new UnimplementedMethodException("finite comparison must be caught in the assignment");
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Childops: " + getChildOps());
    return builder.toString();
  }

  public static class WrapInfo {
    boolean saturated = false;
    int size = 0;
    public static WrapInfo noWrap = unsigned(0);

    private WrapInfo (boolean saturated, int size) {
      this.saturated = saturated;
      this.size = size;
    }

    public static WrapInfo unsigned (boolean saturated, int size) {
      return new WrapInfo(saturated, size);
    }

    public static WrapInfo unsigned (int size) {
      return new WrapInfo(false, size);
    }

    public static WrapInfo signed (boolean saturated, int size) {
      return new WrapInfo(saturated, -size);
    }

    public static WrapInfo signed (int size) {
      return new WrapInfo(false, -size);
    }

    public boolean useZenoSemantics () {
      return size == 0;
    }

    public Interval getInterval () {
      if (size < 0)
        return Interval.signedTop(-size);
      else if (size > 0)
        return Interval.unsignedTop(size);
      else
        return null;
    }

    public int getSize () {
      return Math.abs(size);
    }
  }
}
