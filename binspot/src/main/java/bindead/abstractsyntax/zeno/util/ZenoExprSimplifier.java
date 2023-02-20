package bindead.abstractsyntax.zeno.util;

import static bindead.data.Linear.linear;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.data.Linear;
import bindead.domainnetwork.channels.QueryChannel;

/**
 * Simplifies an expression by replacing binary operations with constant arguments by their result. The visitor will
 * report invalid arithmetic operations as warnings.
 */
public class ZenoExprSimplifier extends ZenoRhsVisitorSkeleton<Rhs, QueryChannel> {
  private static final ZenoExprSimplifier visitor = new ZenoExprSimplifier();

  /**
   * Simplify an assignment statement.
   *
   * @param stmt the statement
   * @param channel channels to report errors
   * @return the simplified assignment or the input assignment in case nothing could be simplified
   */
  public static Assign run (Assign stmt, QueryChannel channel) {
    Rhs newRhs = stmt.getRhs().accept(visitor, channel);
    if (newRhs == null)
      newRhs = new Rlin(linear(Bound.ONE));
    if (newRhs != stmt.getRhs())
      return new Assign(stmt.getLhs(), newRhs);
    return stmt;
  }

  @Override public Rhs visit (Bin expr, QueryChannel channel) {
    BigInt l = expr.getLeft().getConstantOrNull();
    BigInt r = expr.getRight().getConstantOrNull();
    if (l != null && r != null)
      switch (expr.getOp()) {
      case Div:
        if (r.isZero()) // ARM semantics can return 0 when dividing by 0
          return new Rlin(linear(Bound.ZERO));
        else
          return new Rlin(linear(l.div(r)));
      case Shl:
        return new Rlin(linear(l.shl(r)));
      case Shr:
        return new Rlin(linear(l.shr(r)));
      case Mod:
        return new Rlin(linear(l.mod(r)));
      default:
        break;
      }
    if (l != null || r != null) {
      BigInt constVal;
      Linear linear;
      BigInt divisor;
      if (l != null) {
        constVal = l;
        linear = expr.getRight().getLinearTerm();
        divisor = expr.getRight().getDivisor();
      } else {
        constVal = r;
        linear = expr.getLeft().getLinearTerm();
        divisor = expr.getLeft().getDivisor();
      }
      switch (expr.getOp()) {
      case Mul:
        return new Rlin(linear.smul(constVal), divisor);
      default:
        break;
      }
    }
    return expr;
  }

  @Override public Rhs visit (Rlin expr, QueryChannel channel) {
    return expr;
  }

  @Override public Rhs visit (RangeRhs expr, QueryChannel channel) {
    if (expr.getRange().isConstant())
      return new Rlin(linear(expr.getRange().convexHull().low().asInteger()));
    return expr;
  }
}
