package rreil.gdsl.exp_builder.statement;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.exp_builder.AddressBuilder;
import rreil.gdsl.exp_builder.BranchHintBuilder;
import rreil.lang.RReil;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RReilFactory;

public class BranchBuilder extends StatementBuilder {
  private AddressBuilder address;
  private BranchHintBuilder hint;

  public BranchBuilder(BuildingStateManager manager, BranchHintBuilder hint,
      AddressBuilder address) {
    super(manager);
    this.hint = hint;
    this.address = address;
  }

  @Override
  public BuildResult<? extends RReil> build() {
    BuildResult<? extends Rval> rhsR = address.build();

    return result(RReilFactory.instance.branchNative(manager.nextAddress(),
        rhsR.getResult(), hint.build().getResult()), rhsR.getStatements());
  }

  @Override
  public Builder<RReil> size(int size) {
    return this;
  }

  @Override
  public int getSize() {
    throw new RuntimeException("No size field");
  }

}
