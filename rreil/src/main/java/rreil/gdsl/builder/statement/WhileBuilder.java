package rreil.gdsl.builder.statement;

import java.util.SortedMap;

import rreil.gdsl.Globals;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.StatementCollection;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.StatementCollectionBuilder;
import rreil.gdsl.builder.sexpr.SexprBuilder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.SimpleExpression;
import rreil.lang.util.RReilFactory;
import rreil.lang.util.RhsFactory;

public class WhileBuilder extends StatementBuilder {
  private SexprBuilder cond;
  private StatementCollectionBuilder body;

  public WhileBuilder (BuildingStateManager manager, SexprBuilder cond,
      StatementCollectionBuilder body) {
    super(manager);
    this.cond = cond.size(1);
    this.body = body;
  }

  @Override public BuildResult<? extends RReil> build () {
    BuildResult<? extends SimpleExpression> condR = cond.build();

    RReilAddr branchCondAddr = manager.nextAddress();
    RReilAddr branchBehindAddress = manager.nextAddress();

    RReilAddr bodyAddr = manager.stageAddress();

    RReil branchCondRReil = RReilFactory.instance.branch(branchCondAddr, condR.getResult(),
        RhsFactory.getInstance().rreilAddress(Globals.defaultAddressSize, bodyAddr));

    BuildResult<StatementCollection> bodyR = body.build();

    RReil branchBackRReil = RReilFactory.instance.branch(manager.nextAddress(), RhsFactory.getInstance().trueLin(),
        RhsFactory.getInstance().rreilAddress(Globals.defaultAddressSize, branchCondAddr));

    RReilAddr behindAddr = manager.stageAddress();
    RReil branchBehindRReil = RReilFactory.instance.branch(branchBehindAddress, RhsFactory.getInstance().trueLin(),
        RhsFactory.getInstance().rreilAddress(Globals.defaultAddressSize, behindAddr));

    SortedMap<RReilAddr, RReil> stmts = BuildResult.emptyStatements();
    stmts.put(branchCondAddr, branchCondRReil);
    stmts.put(branchBehindAddress, branchBehindRReil);
    stmts.putAll(bodyR.getResult().getInstructions());
    stmts.put(branchBackRReil.getRReilAddress(), branchBackRReil);

    return result(branchBackRReil, stmts);
  }
}
