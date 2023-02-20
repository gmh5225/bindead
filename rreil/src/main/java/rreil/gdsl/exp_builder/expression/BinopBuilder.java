package rreil.gdsl.exp_builder.expression;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.exp_builder.linear.LinearBuilder;
import rreil.lang.BinOp;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RhsFactory;

public class BinopBuilder extends ExpressionBuilder {
  private LinearBuilder lhs;
  private LinearBuilder rhs;
  private BinOp op;

  public BinopBuilder (BuildingStateManager manager, BinOp op,
      LinearBuilder lhs, LinearBuilder rhs) {
    super(manager);
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override public BinopBuilder size (int size) {
    lhs.size(size);
    rhs.size(size);
    return this;
  }

  @Override public BuildResult<? extends Rhs> build () {
    BuildResult<? extends Rval> lhsR = lhs.buildRval();
    BuildResult<? extends Rval> rhsR = rhs.buildRval();

    SortedMap<RReilAddr, RReil> stmts = lhsR.before(rhsR);

    return result(
        RhsFactory.getInstance().binary(lhsR.getResult(), op, rhsR.getResult()),
        stmts);
  }

  @Override public int getSize () {
    return lhs.getSize();
  }
}
