package rreil.gdsl.exp_builder.linear;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.lang.LinBinOp;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RhsFactory;

public class BinopBuilder extends LinearBuilder {
  private LinearBuilder lhs;
  private LinearBuilder rhs;
  private LinBinOp op;

  public BinopBuilder(BuildingStateManager manager, LinBinOp op,
      LinearBuilder lhs, LinearBuilder rhs) {
    super(manager);
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override
  public LinearBuilder size(int size) {
    lhs.size(size);
    rhs.size(size);
    return this;
  }

  @Override
  public BuildResult<? extends Rhs> build() {
    BuildResult<? extends Rval> lhsR = lhs.buildRval();
    BuildResult<? extends Rval> rhsR = rhs.buildRval();

    SortedMap<RReilAddr, RReil> stmts = lhsR.before(rhsR);

    return result(
        RhsFactory.getInstance().binary(lhsR.getResult(), op, rhsR.getResult()),
        stmts);
  }

  @Override
  public int getSize() {
    return lhs.getSize();
  }
}
