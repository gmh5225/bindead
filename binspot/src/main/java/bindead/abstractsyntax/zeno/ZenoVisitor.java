package bindead.abstractsyntax.zeno;

public interface ZenoVisitor<R, T> {
  public R visit (Zeno.Assign stmt, T data);

  public R visit (Zeno.Bin expr, T data);

  public R visit (Zeno.Lhs variable, T data);

  public R visit (Zeno.Rlin lin, T data);

  public R visit (Zeno.RangeRhs range, T data);
}
