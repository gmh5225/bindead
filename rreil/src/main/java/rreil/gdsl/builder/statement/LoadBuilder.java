package rreil.gdsl.builder.statement;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.AddressBuilder;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.builder.VariableBuilder;
import rreil.lang.RReil;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RReilFactory;

public class LoadBuilder extends StatementBuilder {
  private VariableBuilder lhs;
  private AddressBuilder address;

  public LoadBuilder(BuildingStateManager manager, int size, VariableBuilder lhs,
      AddressBuilder address) {
    super(manager);
    this.lhs = lhs.size(size);
    this.address = address;
  }

  @Override
  public BuildResult<? extends RReil> build() {
    BuildResult<Rvar> lhsR = lhs.build();
    BuildResult<? extends Lin> rhsR = address.build();

    return result(
        RReilFactory.instance.load(manager.nextAddress(), lhsR.getResult().asLhs(),
            rhsR.getResult()), lhsR.getStatements());
  }

  @Override
  public Builder<RReil> size(int size) {
    return this;
  }

  @Override
  public int getSize() {
    return lhs.getSize();
  }

}
