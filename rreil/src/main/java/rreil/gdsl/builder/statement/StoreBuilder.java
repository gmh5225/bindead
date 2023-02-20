package rreil.gdsl.builder.statement;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.AddressBuilder;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.builder.linear.LinearBuilder;
import rreil.lang.RReil;
import rreil.lang.Rhs.Lin;
import rreil.lang.util.RReilFactory;

public class StoreBuilder extends StatementBuilder {
  private LinearBuilder rhs;
  private AddressBuilder address;

  public StoreBuilder(BuildingStateManager manager, int size,
      AddressBuilder address, LinearBuilder rhs) {
    super(manager);
    this.rhs = rhs.size(size);
    this.address = address;
  }

  @Override
  public BuildResult<? extends RReil> build() {
    BuildResult<? extends Lin> addressR = address.build();
    BuildResult<? extends Lin> rhsR = rhs.build();

    return result(
        RReilFactory.instance.store(manager.nextAddress(), addressR.getResult(),
            rhsR.getResult()));
  }

  @Override
  public Builder<RReil> size(int size) {
    return this;
  }

  @Override
  public int getSize() {
    return rhs.getSize();
  }

}
