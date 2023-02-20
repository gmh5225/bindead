package rreil.gdsl.builder.sexpr;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.linear.LinearBuilder;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rval;

public class SexprLinearBuilder extends SexprBuilder {
  LinearBuilder me;
  
  public SexprLinearBuilder(BuildingStateManager manager, LinearBuilder me) {
    super(manager);
    this.me = me;
  }

  @Override
  public BuildResult<? extends Lin> build() {
    return me.build();
  }

  @Override
  public SexprLinearBuilder size(int size) {
    me.size(size);
    return this;
  }

  @Override
  public int getSize() {
    return me.getSize();
  }
  
  public BuildResult<? extends Rval> buildRval() {
    return me.buildRval();
  }
}
