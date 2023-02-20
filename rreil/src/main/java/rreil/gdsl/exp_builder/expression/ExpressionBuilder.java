package rreil.gdsl.exp_builder.expression;

import gdsl.rreil.expression.IExpression;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.Builder;
import rreil.lang.Rhs;

public abstract class ExpressionBuilder extends Builder<Rhs> implements IExpression {
  public ExpressionBuilder(BuildingStateManager manager) {
    super(manager);
  }
}
