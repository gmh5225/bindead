package bindead.abstractsyntax.finite;

import bindead.abstractsyntax.finite.Finite.Rlin;

/**
 * Visitor base class for binary operations in the finite domain.
 */
public abstract class FiniteBinOpVisitor<R> {
  public R visit (Finite.Bin stmt) {
    Rlin l = stmt.getLeft();
    Rlin r = stmt.getRight();
    switch (stmt.getOp()) {
      case Add:
        return visitAdd(l, r);
      case And:
        return visitAnd(l, r);
      case Divu:
        return visitDiv(l, r);
      case Divs:
        return visitDivs(l, r);
      case Mod:
        return visitMod(l, r);
      case Mul:
        return visitMul(l, r);
      case Or:
        return visitOr(l, r);
      case Shl:
        return visitShl(l, r);
      case Shr:
        return visitShr(l, r);
      case Shrs:
        return visitShrs(l, r);
      case Sub:
        return visitSub(l, r);
      case Xor:
        return visitXor(l, r);
      default:
        throw new UnsupportedOperationException("Impossible");
    }
  }

  protected abstract R visitAdd (Rlin l, Rlin r);

  protected abstract R visitAnd (Rlin l, Rlin r);

  protected abstract R visitDiv (Rlin l, Rlin r);

  protected abstract R visitDivs (Rlin l, Rlin r);

  protected abstract R visitMod (Rlin l, Rlin r);

  protected abstract R visitMul (Rlin l, Rlin r);

  protected abstract R visitOr (Rlin l, Rlin r);

  protected abstract R visitShl (Rlin l, Rlin r);

  protected abstract R visitShr (Rlin l, Rlin r);

  protected abstract R visitShrs (Rlin l, Rlin r);

  protected abstract R visitSub (Rlin l, Rlin r);

  protected abstract R visitXor (Rlin l, Rlin r);
}
