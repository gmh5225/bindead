package bindead.domains.affine;

import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.data.Linear;
import bindead.data.Linear.Divisor;

public class InliningVisitor extends ZenoRhsVisitorSkeleton<Rhs, AffineState> {
  private static final ZenoFactory zeno = ZenoFactory.getInstance();
  private static final InliningVisitor visitor = new InliningVisitor();

  public static Assign run (Assign ass, AffineState sys) {
    Rhs newRhs = ass.getRhs().accept(visitor, sys);
    if (newRhs != ass.getRhs())
      return zeno.assign(ass.getLhs(), newRhs);
    return ass;
  }

  public static Test run (Test test, AffineState sys) {
    Linear le = test.getExpr();
    Linear res = sys.inlineIntoLinear(le, Divisor.one());  // divisor is always one for tests
    if (le != res)
      return zeno.comparison((res), test.getOperator());
    return test;
  }

  @Override public Rhs visit (RangeRhs expr, AffineState sys) {
    return expr;
  }

  @Override public Rhs visit (Bin expr, AffineState sys) {
    Rlin l = (Rlin) expr.getLeft().accept(this, sys);
    Rlin r = (Rlin) expr.getRight().accept(this, sys);
    if (l == expr.getLeft() && r == expr.getRight())
      return expr;
    return zeno.binary(l, expr.getOp(), r);
  }

  @Override public Rhs visit (Rlin expr, AffineState sys) {
    Linear le = expr.getLinearTerm();
    Linear.Divisor d = new Linear.Divisor(expr.getDivisor());
    Linear res = sys.inlineIntoLinear(le, d);
    if (le != res)
      return zeno.linear(res, d.get());
    return expr;
  }
}
