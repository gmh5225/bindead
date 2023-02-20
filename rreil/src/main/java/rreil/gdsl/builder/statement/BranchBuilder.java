package rreil.gdsl.builder.statement;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.AddressBuilder;
import rreil.gdsl.builder.BranchHintBuilder;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.lang.RReil;
import rreil.lang.Rhs.Lin;
import rreil.lang.util.RReilFactory;

public class BranchBuilder extends StatementBuilder {
  private AddressBuilder address;
  private BranchHintBuilder hint;

  public BranchBuilder (BuildingStateManager manager, BranchHintBuilder hint, AddressBuilder address) {
    super(manager);
    this.hint = hint;
    this.address = address;
  }

  @Override public BuildResult<? extends RReil> build () {
    BuildResult<? extends Lin> rhsR = address.build();
    return result(RReilFactory.instance.branchNative(manager.nextAddress(),
        rhsR.getResult(), hint.build().getResult()), rhsR.getStatements());
  }

  @Override public Builder<RReil> size (int size) {
    return this;
  }

  @Override public int getSize () {
    throw new RuntimeException("No size field");
  }

}
