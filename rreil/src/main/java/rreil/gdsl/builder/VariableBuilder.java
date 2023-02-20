package rreil.gdsl.builder;

import gdsl.rreil.ILimitedVariable;
import gdsl.rreil.IVariable;
import rreil.gdsl.BuildingStateManager;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RhsFactory;

public class VariableBuilder extends Builder<Rvar> implements IVariable, ILimitedVariable {
  protected Component<Integer> size;
  protected int offset;
  protected IdBuilder var;

  public VariableBuilder (BuildingStateManager manager, IdBuilder var, int offset) {
    super(manager);
    this.offset = offset;
    this.var = var;
    size = new Component<Integer>();
  }

  @Override public VariableBuilder size (int size) {
    this.size.set(size);
    return this;
  }

  @Override public BuildResult<Rvar> build () {
    return result(RhsFactory.getInstance().variable(size.get(), offset, var.build().getResult()));
  }

  @Override public int getSize () {
    return size.get();
  }
}
