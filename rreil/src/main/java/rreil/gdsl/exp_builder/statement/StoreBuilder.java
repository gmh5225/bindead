package rreil.gdsl.exp_builder.statement;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.exp_builder.AddressBuilder;
import rreil.gdsl.exp_builder.linear.LinearBuilder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rval;
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
    BuildResult<? extends Rval> addressR = address.build();
    BuildResult<? extends Rval> rhsR = rhs.buildRval();

    SortedMap<RReilAddr, RReil> stmts = addressR.before(rhsR);

    return result(
        RReilFactory.instance.store(manager.nextAddress(), addressR.getResult(),
            rhsR.getResult()), stmts);
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
