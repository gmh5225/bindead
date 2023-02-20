package rreil.gdsl.exp_builder.statement;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.Globals;
import rreil.gdsl.StatementCollection;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.exp_builder.StatementCollectionBuilder;
import rreil.gdsl.exp_builder.sexpr.SexprBuilder;
import rreil.lang.BinOp;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RReilFactory;
import rreil.lang.util.RhsFactory;

public class WhileBuilder extends StatementBuilder {
  private SexprBuilder cond;
  private StatementCollectionBuilder body;

  public WhileBuilder(BuildingStateManager manager, SexprBuilder cond,
      StatementCollectionBuilder body) {
    super(manager);
    this.cond = cond.size(1);
    this.body = body;
  }

  @Override
  public BuildResult<? extends RReil> build() {
    RReilAddr startAddr = manager.stageAddress();

    BuildResult<? extends Rval> condR = cond.buildRval();
    Rvar temp = RhsFactory.getInstance()
        .variable(1, 0, manager.nextTemporary());
    RReil negateRReil = RReilFactory.instance.assign(manager.nextAddress(), temp.asLhs(),
        RReilFactory.instance.binary(condR.getResult(), BinOp.Xor, Rlit.true_));

    RReilAddr branchEndAddress = manager.nextAddress();

    BuildResult<StatementCollection> bodyR = body.build();

    RReil branchBackRReil = RReilFactory.instance.branch(manager.nextAddress(), Rlit.true_,
        RhsFactory.getInstance().rreilAddress(Globals.defaultAddressSize, startAddr));

    RReilAddr behindAddr = manager.stageAddress();
    RReil branchEndRReil = RReilFactory.instance.branch(branchEndAddress, temp,
        RhsFactory.getInstance().rreilAddress(Globals.defaultAddressSize, behindAddr));

    SortedMap<RReilAddr, RReil> stmts = condR.getStatements();
    stmts.put(negateRReil.getRReilAddress(), negateRReil);
    stmts.putAll(bodyR.getResult().getInstructions());
    stmts.put(branchEndRReil.getRReilAddress(), branchEndRReil);

    return result(branchBackRReil, stmts);
  }
}
