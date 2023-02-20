package rreil.gdsl.builder;

import gdsl.rreil.expression.ICompare;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.linear.LinearBuilder;
import rreil.lang.ComparisonOp;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.Lin;
import rreil.lang.util.RhsFactory;

public class CompareBuilder extends Builder<Cmp> implements ICompare {
  private LinearBuilder lhs;
  private LinearBuilder rhs;
  private ComparisonOp op;

  public CompareBuilder (BuildingStateManager manager, ComparisonOp op,
      LinearBuilder lhs, LinearBuilder rhs) {
    super(manager);
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override public CompareBuilder size (int size) {
    lhs.size(size);
    rhs.size(size);
    return this;
  }

  @Override public BuildResult<? extends Cmp> build () {
    Lin lhsLin = lhs.build().getResult();
    Lin rhsLin = rhs.build().getResult();
    return result(RhsFactory.getInstance().comparison(lhsLin, op, rhsLin));
  }

  public int getSize () {
    return 1;
  }
}
