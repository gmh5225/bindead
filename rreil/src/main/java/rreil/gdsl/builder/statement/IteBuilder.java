package rreil.gdsl.builder.statement;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.Globals;
import rreil.gdsl.StatementCollection;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.builder.StatementCollectionBuilder;
import rreil.gdsl.builder.sexpr.SexprBuilder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.SimpleExpression;
import rreil.lang.util.RReilFactory;
import rreil.lang.util.RhsFactory;

public class IteBuilder extends StatementBuilder {
  private SexprBuilder cond;
  private StatementCollectionBuilder then_branch;
  private StatementCollectionBuilder else_branch;

  public IteBuilder (BuildingStateManager manager, SexprBuilder cond,
      StatementCollectionBuilder then_branch,
      StatementCollectionBuilder else_branch) {
    super(manager);
    this.cond = cond.size(1);
    this.then_branch = then_branch;
    this.else_branch = else_branch;
  }

  @Override public BuildResult<? extends RReil> build () {
    BuildResult<? extends SimpleExpression> condR = cond.build();

    RReilAddr branchThenAddress = manager.nextAddress();

    BuildResult<StatementCollection> elseR = else_branch.build();

    RReilAddr branchEndAddr = manager.nextAddress();

    RReilAddr staged = manager.stageAddress();
    RReil branchThenRReil = RReilFactory.instance.branch(branchThenAddress,
        condR.getResult(),
        RhsFactory.getInstance().rreilAddress(Globals.defaultAddressSize, staged));

    BuildResult<StatementCollection> thenR = then_branch.build();

    RReilAddr behindAddr = manager.stageAddress();
    // RReil nopRReil = Factory.instance.nop(nopAddr);
    RReil branchEndRReil =
      RReilFactory.instance.branch(branchEndAddr, RhsFactory.getInstance().trueLin(),
          RhsFactory.getInstance().rreilAddress(Globals.defaultAddressSize, behindAddr));

    SortedMap<RReilAddr, RReil> stmts = condR.getStatements();
    stmts.putAll(elseR.getResult().getInstructions());
    stmts.putAll(thenR.getResult().getInstructions());
    stmts.put(branchEndRReil.getRReilAddress(), branchEndRReil);

    return result(branchThenRReil, stmts);
  }

  @Override public Builder<RReil> size (int size) {
    throw new RuntimeException("No size field");
  }

  @Override public int getSize () {
    throw new RuntimeException("No size field");
  }

}
