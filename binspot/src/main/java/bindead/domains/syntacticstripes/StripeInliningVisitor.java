package bindead.domains.syntacticstripes;

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

class StripeInliningVisitor extends ZenoRhsVisitorSkeleton<Rhs, StripeState> {
  private static final ZenoFactory zeno = ZenoFactory.getInstance();

  public static Assign run (Assign assign, StripeState sys) {
    StripeInliningVisitor visitor = new StripeInliningVisitor();
    Rhs newRhs = assign.getRhs().accept(visitor, sys);
    if (newRhs != assign.getRhs())
      return zeno.assign(assign.getLhs(), newRhs);
    return assign;
  }

  public static Test run (Test test, StripeState sys) {
    Linear le = test.getExpr();
    Linear res = sys.inlineSyntactically(le);
    if (le != res)
      return zeno.comparison(res, test.getOperator());
    return test;
  }

  @Override public Rhs visit (RangeRhs expr, StripeState sys) {
    return expr;
  }

  @Override public Rhs visit (Bin expr, StripeState sys) {
    Rlin l = (Rlin) expr.getLeft().accept(this, sys);
    Rlin r = (Rlin) expr.getRight().accept(this, sys);
    if (l == expr.getLeft() && r == expr.getRight())
      return expr;
    return zeno.binary(l, expr.getOp(), r);
  }

  @Override public Rhs visit (Rlin expr, StripeState sys) {
    Linear le = expr.getLinearTerm();
    // FIXME: Check divisor usage here!
    Divisor d = new Divisor(expr.getDivisor());
    Linear res = sys.inlineSyntactically(le);
    if (le != res)
      return zeno.linear(res, d.get());
    return expr;
  }
}
