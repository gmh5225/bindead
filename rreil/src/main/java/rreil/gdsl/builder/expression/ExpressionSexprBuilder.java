package rreil.gdsl.builder.expression;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.builder.sexpr.SexprBuilder;
import rreil.lang.Rhs;

public class ExpressionSexprBuilder extends ExpressionBuilder {
  private SexprBuilder me;
  
  public ExpressionSexprBuilder(BuildingStateManager manager, SexprBuilder me) {
    super(manager);
    this.me = me;
  }

  @Override
  public BuildResult<? extends Rhs> build() {
    return me.build();
  }

  @Override
  public Builder<Rhs> size(int size) {
    me.size(size);
    return this;
  }

  @Override
  public int getSize() {
    return me.getSize();
  }

}
