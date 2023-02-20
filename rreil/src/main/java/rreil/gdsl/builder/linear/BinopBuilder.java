package rreil.gdsl.builder.linear;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.lang.LinBinOp;
import rreil.lang.Rhs;
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
  public BuildResult<? extends Rhs.Lin> build() {
    BuildResult<? extends Rhs.Lin> lhsR = lhs.build();
    BuildResult<? extends Rhs.Lin> rhsR = rhs.build();
    
    return result(RhsFactory.getInstance().binary(lhsR.getResult(), op, rhsR.getResult()));
  }

  @Override
  public int getSize() {
    return lhs.getSize();
  }
}
