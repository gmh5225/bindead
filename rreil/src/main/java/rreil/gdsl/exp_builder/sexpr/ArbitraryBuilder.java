package rreil.gdsl.exp_builder.sexpr;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Component;
import rreil.lang.Rhs;
import rreil.lang.util.RhsFactory;

public class ArbitraryBuilder extends SexprBuilder {
  private Component<Integer> size = new Component<Integer>();
  
  public ArbitraryBuilder(BuildingStateManager manager) {
    super(manager);
  }

  @Override
  public BuildResult<? extends Rhs> build() {
    return result(RhsFactory.getInstance().arbitrary(size.get()));
  }

  @Override
  public ArbitraryBuilder size(int size) {
    this.size.set(size);
    return this;
  }

  @Override
  public int getSize() {
    return size.get();
  }
}
