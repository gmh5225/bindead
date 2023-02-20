package bindead.abstractsyntax.finite;

import bindead.abstractsyntax.finite.Finite.Bin;
import bindead.abstractsyntax.finite.Finite.Cmp;
import bindead.abstractsyntax.finite.Finite.Convert;
import bindead.abstractsyntax.finite.Finite.FiniteRangeRhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.SignExtend;

public interface FiniteExprVisitor<R, T> {
  public R visit (Bin expr, T data);

  public R visit (Cmp expr, T data);

  public R visit (SignExtend expr, T data);

  public R visit (Convert expr, T data);

  public R visit (Rlin expr, T data);

  public R visit (FiniteRangeRhs expr, T data);

}
