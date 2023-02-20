package rreil.gdsl.exp_builder.linear;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.VariableBuilder;
import rreil.lang.Rhs.Rval;

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
  public BuildResult<? extends Rval> build() {
    return vb.build();
  }

  @Override
  public BuildResult<? extends Rval> buildRval() {
    return build();
  }

  @Override
  public int getSize() {
    return vb.getSize();
  }
}
