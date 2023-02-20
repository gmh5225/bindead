package rreil.lang.util;

import rreil.lang.Rhs;

public interface RhsVisitor<R, T> {
  public R visit (Rhs.Bin expr, T data);
  
  public R visit (Rhs.LinBin expr, T data);

  public R visit (Rhs.LinScale expr, T data);
  
  public R visit (Rhs.LinRval expr, T data);

  public R visit (Rhs.Cmp expr, T data);

  public R visit (Rhs.SignExtend expr, T data);

  public R visit (Rhs.Convert expr, T data);

  public R visit (Rhs.Rvar expr, T data);

  public R visit (Rhs.Rlit expr, T data);

  public R visit (Rhs.Address expr, T data);

  public R visit (Rhs.RangeRhs expr, T data);
}
