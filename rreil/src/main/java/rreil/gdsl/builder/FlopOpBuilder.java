package rreil.gdsl.builder;

import rreil.gdsl.BuildingStateManager;
import rreil.lang.FlopOp;
import gdsl.rreil.IFlop;

public class FlopOpBuilder extends Builder<FlopOp> implements IFlop {
  private FlopOp flop;
  
  public FlopOpBuilder (BuildingStateManager manager, FlopOp flop) {
    super(manager);
    this.flop = flop;
  }

  @Override public BuildResult<? extends FlopOp> build () {
    return result(flop);
  }
}
