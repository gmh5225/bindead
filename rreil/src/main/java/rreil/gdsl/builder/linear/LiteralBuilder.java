package rreil.gdsl.builder.linear;

import javalx.numeric.BigInt;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Component;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RhsFactory;

public class LiteralBuilder extends LinearBuilder {
  protected Component<Integer> size;
  protected BigInt imm;

  public LiteralBuilder(BuildingStateManager manager, BigInt imm) {
    super(manager);
    this.imm = imm;
    size = new Component<Integer>();
  }

  public LiteralBuilder size(int size) {
    this.size.set(size);
    return this;
  }

  public BuildResult<LinRval> build() {
    Rval rv = RhsFactory.getInstance().literal(size.get(), imm);
    return result(RhsFactory.getInstance().linRval(rv));
  }

  @Override
  public BuildResult<? extends Rval> buildRval() {
    return result(RhsFactory.getInstance().literal(size.get(), imm));
  }

  @Override
  public int getSize() {
    return size.get();
  }
}
