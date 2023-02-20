package rreil.gdsl.exp_builder.statement;

import java.util.List;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.Globals;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.VariableBuilder;
import rreil.gdsl.builder.VariableListBuilder;
import rreil.gdsl.exp_builder.FlopOpBuilder;
import rreil.lang.RReil;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RReilFactory;

public class FlopBuilder extends StatementBuilder {
  FlopOpBuilder flop;
  VariableBuilder lhs;
  VariableListBuilder rhs;
  VariableBuilder flags;

  public FlopBuilder(BuildingStateManager manager, FlopOpBuilder flop,
      VariableBuilder flags, VariableBuilder lhs, VariableListBuilder rhs) {
    super(manager);
    this.flop = flop;
    this.lhs = lhs;
    this.rhs = rhs;
    this.flags = flags.size(Globals.defaultFloatingFlagsSize);
  }

  @Override
  public BuildResult<? extends RReil> build() {
    Rvar lhs = this.lhs.build().getResult();
    List<Rvar> rhs = this.rhs.build().getResult();
    Rvar flags = this.flags.build().getResult();

    RReil flop = RReilFactory.instance.flop(manager.nextAddress(), this.flop.build()
        .getResult(), lhs, rhs, flags);

    return result(flop);
  }
}
