package rreil.gdsl.exp_builder.expression;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Component;
import rreil.gdsl.exp_builder.linear.LinearBuilder;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RhsFactory;

public class ExtensionBuilder extends ExpressionBuilder {
  Component<Integer> size = new Component<Integer>();
  private LinearBuilder opnd;
  private boolean signed;

  public ExtensionBuilder(BuildingStateManager manager, boolean signed,
      int fromSize, LinearBuilder opnd) {
    super(manager);
    this.signed = signed;
    this.opnd = opnd.size(fromSize);
  }

  @Override
  public ExtensionBuilder size(int size) {
    this.size.set(size);
    return this;
  }

  @Override
  public BuildResult<? extends Rhs> build() {
    BuildResult<? extends Rval> opndR = opnd.buildRval();

    if (signed)
      return result(RhsFactory.getInstance().castSx(opndR.getResult()),
          opndR.getStatements());
    else
      return result(RhsFactory.getInstance().castZx(opndR.getResult()),
          opndR.getStatements());
  }

  @Override
  public int getSize() {
    return size.get();
  }
}
