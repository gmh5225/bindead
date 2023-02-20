package rreil.gdsl.exp_builder.statement;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.exp_builder.AddressBuilder;
import rreil.gdsl.exp_builder.sexpr.SexprBuilder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.RReil.Branch.BranchTypeHint;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RReilFactory;

public class ConditionalBranchBuilder extends StatementBuilder {
  private SexprBuilder condition;
  private AddressBuilder target_true;
  private AddressBuilder target_false;

  public ConditionalBranchBuilder(BuildingStateManager manager,
      SexprBuilder condition, AddressBuilder target_true,
      AddressBuilder target_false) {
    super(manager);
    this.condition = condition.size(1);
    this.target_true = target_true;
    this.target_false = target_false;
  }

  @Override
  public BuildResult<? extends RReil> build() {
    BuildResult<? extends Rval> condR = condition.buildRval();
    BuildResult<? extends Rval> target_trueR = target_true.build();
    BuildResult<? extends Rval> target_falseR = target_false.build();

    SortedMap<RReilAddr, RReil> stmts = condR.before(target_trueR);
    stmts.putAll(target_falseR.getStatements());

    RReil cbranch = RReilFactory.instance.branchNative(manager.nextAddress(), condR.getResult(),
        target_trueR.getResult());
    stmts.put(cbranch.getRReilAddress(), cbranch);

    return result(RReilFactory.instance.branchNative(manager.nextAddress(),
        target_falseR.getResult(), BranchTypeHint.Jump), stmts);
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
