package bindead.abstractsyntax.finite;

import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import rreil.lang.BinOp;
import rreil.lang.ComparisonOp;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Bin;
import bindead.abstractsyntax.finite.Finite.Cmp;
import bindead.abstractsyntax.finite.Finite.Lhs;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.Finite.TestOp;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.domains.fields.VariableCtx;

public class FiniteFactory {
  private static final FiniteFactory INSTANCE = new FiniteFactory();

  private FiniteFactory () {
  }

  public static FiniteFactory getInstance () {
    return INSTANCE;
  }

  /* REFACTOR hsi: why is there a binary operation Add for Rlins when there is a
   * better RLin.add(Rlin) method? Same for Sub.
   * */
  public Bin binary (Finite.Rlin left, BinOp op, Finite.Rlin right) {
    return new Finite.Bin(left, op, right);
  }

  public Finite.Cmp comparison (Finite.Rlin left, ComparisonOp op, Finite.Rlin right) {
    return new Finite.Cmp(left, op, right);
  }

  public Finite.SignExtend signExtend (Finite.Rlin rhs) {
    return new Finite.SignExtend(rhs);
  }

  public Finite.Convert convert (Finite.Rlin rhs) {
    return new Finite.Convert(rhs);
  }

  public Finite.FiniteRangeRhs range (int size, Interval range) {
    return new Finite.FiniteRangeRhs(size, range);
  }

  public Assign assign (Lhs lhs, Rhs rhs) {
    return new Assign(lhs, rhs);
  }

  public Rlin linear (int size, NumVar id) {
    return new Rlin(size, Linear.linear(id));
  }

  public Rlin linear (int size, Linear linear) {
    return new Rlin(size, linear);
  }

  public Rlin linear (VariableCtx ctx) {
    return linear(ctx.getSize(), ctx.getVariable());
  }

  public Rlin literal (int size, BigInt value) {
    return new Rlin(size, Linear.linear(value));
  }

  public Lhs variable (int size, NumVar id) {
    return new Lhs(size, id);
  }

  public Test equalToZero (int size, NumVar id) {
    return equalTo(size, Linear.linear(id), Linear.ZERO);
  }

  public Test equalToZero (Rlin rhs) {
    return equalTo(rhs.getSize(), rhs.getLinearTerm(), Linear.ZERO);
  }

  public Test notEqualTo (int size, Linear left, Linear right) {
    return new Test(size, left, TestOp.NotEqual, right);
  }

  public Test notEqualToZero (int size, NumVar id) {
    return notEqualTo(size, Linear.linear(id), Linear.ZERO);
  }

  public Test notEqualToZero (Rlin rhs) {
    return notEqualTo(rhs.getSize(), rhs.getLinearTerm(), Linear.ZERO);
  }

  public Test equalToOne (int size, NumVar id) {
    return equalTo(size, Linear.linear(id), Linear.ONE);
  }

  public Test equalTo (int size, Linear left, Linear right) {
    return new Test(size, left, TestOp.Equal, right);
  }

  public Test equalTo (int size, NumVar left, NumVar right) {
    return new Test(size, Linear.linear(left), TestOp.Equal, Linear.linear(right));
  }

  public Test signedLessThanOrEqualTo (int size, Linear left, Linear right) {
    return new Test(size, left, TestOp.SignedLessThanOrEqual, right);
  }

  public Test signedLessThan (int size, Linear left, Linear right) {
    return new Test(size, left, TestOp.SignedLessThan, right);
  }

  public Test unsignedLessThanOrEqualTo (int size, Linear left, Linear right) {
    return new Test(size, left, TestOp.UnsignedLessThanOrEqual, right);
  }

  public Test unsignedLessThan (int size, Linear left, Linear right) {
    return new Test(size, left, TestOp.UnsignedLessThan, right);
  }

  public Test test (Cmp cmp) {
    return cmp.asTest();
  }

}
