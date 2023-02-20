package rreil.gdsl.exp_builder.statement;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.builder.VariableBuilder;
import rreil.gdsl.exp_builder.AddressBuilder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rval;
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
    BuildResult<? extends Rval> rhsR = address.build();

    SortedMap<RReilAddr, RReil> stmts = lhsR.before(rhsR);

    return result(
        RReilFactory.instance.load(manager.nextAddress(), lhsR.getResult().asLhs(),
            rhsR.getResult()), stmts);
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
