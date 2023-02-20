package rreil.gdsl.builder.linear;


import gdsl.rreil.linear.ILinearExpression;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.RhsBuilder;
import rreil.lang.Rhs;

public abstract class LinearBuilder extends RhsBuilder implements ILinearExpression {
  public LinearBuilder (BuildingStateManager manager) {
    super(manager);
  }
  
  public abstract BuildResult<? extends Rhs.Lin> build();

  public abstract LinearBuilder size (int size);

  public abstract int getSize ();
}
