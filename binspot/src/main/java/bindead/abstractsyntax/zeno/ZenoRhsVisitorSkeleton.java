package bindead.abstractsyntax.zeno;

import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.Lhs;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.exceptions.DomainStateException.UnimplementedMethodException;

/**
 * This visitor traverses only right-hand sides of a Z expression.
 *
 * @param <R> the synthesized result attribute
 * @param <T> the inherited attribute
 */
public abstract class ZenoRhsVisitorSkeleton<R, T> implements ZenoVisitor<R, T> {
  @Override public R visit (Bin expr, T data) {
    expr.getLeft().accept(this, data);
    expr.getRight().accept(this, data);
    return null;
  }

  @Override public R visit (Rlin expr, T data) {
    return null;
  }

  @Override public R visit (RangeRhs expr, T data) {
    return null;
  }

  @Override public final R visit (Lhs expr, T data) {
    throw new UnimplementedMethodException();
  }

  @Override public final R visit (Assign stmt, T data) {
    throw new UnimplementedMethodException();
  }
}
