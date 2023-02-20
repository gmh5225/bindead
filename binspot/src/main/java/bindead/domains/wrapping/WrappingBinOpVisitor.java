package bindead.domains.wrapping;

import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.FiniteBinOpVisitor;
import bindead.abstractsyntax.zeno.Zeno.ZenoBinOp;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.domains.wrapping.WrappingStateBuilder.WrapInfo;

class WrappingBinOpVisitor extends FiniteBinOpVisitor<Linear> {
  private final WrappingStateBuilder builder;

  public WrappingBinOpVisitor (WrappingStateBuilder builder) {
    this.builder = builder;
  }

  @Override protected Linear visitAdd (Rlin left, Rlin right) {
    Linear l = left.accept(builder, WrapInfo.noWrap);
    Linear r = right.accept(builder, WrapInfo.noWrap);
    return l.add(r);
  }

  @Override protected Linear visitAnd (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.unsigned(left.getSize());
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, wi);
    NumVar res = builder.getTemp();
    builder.getChildOps().addAnd(res, l, r, left.getSize());
    return Linear.linear(res);
  }

  @Override protected Linear visitDiv (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.unsigned(left.getSize());
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, wi);
    return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.binary(WrappingStateBuilder.zeno.linear(l),
        ZenoBinOp.Div, WrappingStateBuilder.zeno.linear(r))));
  }

  @Override protected Linear visitDivs (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.signed(left.getSize());
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, wi);
    return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.binary(WrappingStateBuilder.zeno.linear(l),
        ZenoBinOp.Div, WrappingStateBuilder.zeno.linear(r))));
  }

  @Override protected Linear visitMod (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.unsigned(left.getSize());
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, wi);
    return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.binary(WrappingStateBuilder.zeno.linear(l),
        ZenoBinOp.Mod, WrappingStateBuilder.zeno.linear(r))));
  }

  @Override protected Linear visitMul (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.unsigned(left.getSize());
    Linear r = right.accept(builder, wi);
    if (r.isConstantOnly())
      return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.linear(left.getLinearTerm().smul(
          r.getConstant()))));
    Linear l = left.accept(builder, wi);
    if (l.isConstantOnly())
      return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.linear(right.getLinearTerm().smul(
          l.getConstant()))));
    return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.binary(WrappingStateBuilder.zeno.linear(l),
        ZenoBinOp.Mul, WrappingStateBuilder.zeno.linear(r))));
  }

  @Override protected Linear visitOr (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.unsigned(left.getSize());
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, wi);
    NumVar res = builder.getTemp();
    builder.getChildOps().addOr(res, l, r, left.getSize());
    return Linear.linear(res);
  }

  @Override protected Linear visitShl (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.unsigned(left.getSize());
    WrapInfo satWi = WrapInfo.unsigned(true, Integer.highestOneBit(right.getSize()));
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, satWi);
    return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.binary(WrappingStateBuilder.zeno.linear(l),
        ZenoBinOp.Shl, WrappingStateBuilder.zeno.linear(r))));
  }

  @Override protected Linear visitShr (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.unsigned(left.getSize());
    WrapInfo satWi = WrapInfo.unsigned(true, Integer.highestOneBit(right.getSize()));
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, satWi);
    return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.binary(WrappingStateBuilder.zeno.linear(l),
        ZenoBinOp.Shr, WrappingStateBuilder.zeno.linear(r))));
  }

  @Override protected Linear visitShrs (Rlin left, Rlin right) {
    WrapInfo wi = WrapInfo.signed(left.getSize());
    WrapInfo satWi = WrapInfo.unsigned(true, Integer.highestOneBit(right.getSize()));
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, satWi);
    return Linear.linear(builder.assignToTemp(WrappingStateBuilder.zeno.binary(WrappingStateBuilder.zeno.linear(l),
        ZenoBinOp.Shr, WrappingStateBuilder.zeno.linear(r))));
  }

  @Override protected Linear visitSub (Rlin left, Rlin right) {
    Linear l = left.accept(builder, WrapInfo.noWrap);
    Linear r = right.accept(builder, WrapInfo.noWrap);
    return l.sub(r);
  }

  @Override protected Linear visitXor (Rlin left, Rlin right) {
    if (left.getLinearTerm().equals(right.getLinearTerm())) /* x xorb x == 0 */
      return Linear.ZERO;
    WrapInfo wi = WrapInfo.unsigned(left.getSize());
    Linear l = left.accept(builder, wi);
    Linear r = right.accept(builder, wi);
    NumVar res = builder.getTemp();
    builder.getChildOps().addXOr(res, l, r, left.getSize());
    return Linear.linear(res);
  }
}