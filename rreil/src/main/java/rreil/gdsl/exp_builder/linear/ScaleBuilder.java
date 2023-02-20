package rreil.gdsl.exp_builder.linear;

import javalx.numeric.BigInt;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.lang.BinOp;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RhsFactory;

public class ScaleBuilder extends LinearBuilder {
  private int scale;
  private LinearBuilder opnd;

  public ScaleBuilder(BuildingStateManager manager, int scale,
      LinearBuilder opnd) {
    super(manager);
    this.scale = scale;
    this.opnd = opnd;
  }

  @Override
  public LinearBuilder size(int size) {
    opnd.size(size);
    return this;
  }

  private Rhs buildRhs(BuildResult<? extends Rval> opndR) {
    return RhsFactory.getInstance().binary(
        RhsFactory.getInstance().literal(getSize(), BigInt.of(scale)),
        BinOp.Mul, opndR.getResult());
  }

  @Override
  public BuildResult<? extends Rhs> build() {
    BuildResult<? extends Rval> opndR = opnd.buildRval();
    return result(buildRhs(opndR), opndR.getStatements());
  }  
  
  @Override
  public int getSize() {
    return opnd.getSize();
  }

}
