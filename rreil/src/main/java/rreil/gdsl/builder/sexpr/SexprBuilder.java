package rreil.gdsl.builder.sexpr;

import gdsl.rreil.sexpression.ISimpleExpression;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.RhsBuilder;
import rreil.lang.Rhs.SimpleExpression;

public abstract class SexprBuilder extends RhsBuilder implements ISimpleExpression {
  public SexprBuilder(BuildingStateManager manager) {
    super(manager);
  }
  
  public abstract BuildResult<? extends SimpleExpression> build();
  
  @Override
  public abstract SexprBuilder size(int size);
}
