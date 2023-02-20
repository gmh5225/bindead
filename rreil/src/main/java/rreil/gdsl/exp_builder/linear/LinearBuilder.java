package rreil.gdsl.exp_builder.linear;

import gdsl.rreil.linear.ILinearExpression;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.exp_builder.RhsBuilder;

public abstract class LinearBuilder extends RhsBuilder implements ILinearExpression {
	public LinearBuilder(BuildingStateManager manager) {
    super(manager);
  }

  public abstract LinearBuilder size(int size);
	
	public abstract int getSize();
}
