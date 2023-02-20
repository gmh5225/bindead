package rreil.gdsl.exp_builder.sexpr;

import gdsl.rreil.sexpression.ISimpleExpression;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.exp_builder.RhsBuilder;

public abstract class SexprBuilder extends RhsBuilder implements ISimpleExpression {
  public SexprBuilder(BuildingStateManager manager) {
    super(manager);
  }
  
  @Override
  public abstract SexprBuilder size(int size);
}
