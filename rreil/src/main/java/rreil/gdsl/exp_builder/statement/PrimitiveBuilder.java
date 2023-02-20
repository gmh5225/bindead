package rreil.gdsl.exp_builder.statement;

import java.util.List;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.VariableListBuilder;
import rreil.lang.Lhs;
import rreil.lang.RReil;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RReilFactory;

public class PrimitiveBuilder extends StatementBuilder {
  String op;
  VariableListBuilder lhs;
  VariableListBuilder rhs;

  public PrimitiveBuilder(BuildingStateManager manager, String op,
      VariableListBuilder lhs, VariableListBuilder rhs) {
    super(manager);
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override
  public BuildResult<? extends RReil> build() {
    List<Lhs> lhs = VariableListBuilder.asLhsList(this.lhs.build().getResult());
    List<Rval> rhs = VariableListBuilder.asRvalList(this.rhs.build().getResult());

    RReil prim = RReilFactory.instance.primOp(manager.nextAddress(), op, lhs, rhs);

    return result(prim);
  }
}
