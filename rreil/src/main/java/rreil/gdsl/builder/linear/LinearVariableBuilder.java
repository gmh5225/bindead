package rreil.gdsl.builder.linear;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.VariableBuilder;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RhsFactory;

public class LinearVariableBuilder extends LinearBuilder {
  private VariableBuilder vb;

  public LinearVariableBuilder(BuildingStateManager manager, VariableBuilder vb) {
    super(manager);
    this.vb = vb;
  }

  @Override
  public LinearBuilder size(int size) {
    vb.size(size);
    return this;
  }

  @Override
  public BuildResult<? extends Rhs.LinRval> build() {
    return result(RhsFactory.getInstance().linRval(vb.build().getResult()));
  }

  @Override
  public BuildResult<? extends Rval> buildRval() {
    return vb.build();
  }

  @Override
  public int getSize() {
    return vb.getSize();
  }
}
