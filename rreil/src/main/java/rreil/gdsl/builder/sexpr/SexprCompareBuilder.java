package rreil.gdsl.builder.sexpr;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.CompareBuilder;
import rreil.lang.Rhs;
import rreil.lang.Rhs.SimpleExpression;

public class SexprCompareBuilder extends SexprBuilder {
  CompareBuilder me;
//  int size;
  
  public SexprCompareBuilder(BuildingStateManager manager, int size, CompareBuilder me) {
    super(manager);
    this.me = me;
    me.size(size);
  }

  @Override
  public BuildResult<? extends SimpleExpression> build() {
    return me.build();
  }

  @Override
  public SexprCompareBuilder size(int size) {
    return this;
  }

  @Override
  public int getSize() {
    return me.getSize();
  }
}
