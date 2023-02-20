package rreil.gdsl.builder.statement;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.VariableBuilder;
import rreil.gdsl.builder.expression.ExpressionBuilder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.util.RReilFactory;
import rreil.lang.Rhs.Rvar;

public class AssignBuilder extends StatementBuilder {
  private VariableBuilder lhsBuilder;
  private ExpressionBuilder rhsBuilder;

  public AssignBuilder (BuildingStateManager manager, int size, VariableBuilder lhsBuilder,
      ExpressionBuilder rhsBuilder) {
    super(manager);
    this.lhsBuilder = lhsBuilder;
    this.rhsBuilder = rhsBuilder;

    size(size);
  }

  public BuildResult<RReil.Assign> build () {
    BuildResult<? extends Rvar> lhs = lhsBuilder.build();
    BuildResult<? extends Rhs> rhs = rhsBuilder.build();

    SortedMap<RReilAddr, RReil> stmts = lhs.before(rhs);

    RReil.Assign stmt = RReilFactory.instance.assign(manager.nextAddress(), lhs
        .getResult().asLhs(), rhs.getResult());

    return result(stmt, stmts);
  }

  @Override public AssignBuilder size (int size) {
    rhsBuilder.size(size);
    lhsBuilder.size(rhsBuilder.getSize());
    return this;
  }

  @Override public int getSize () {
    return lhsBuilder.getSize();
  }

  // public IStatement sem_assign(long size, IVariable var, IExpression exp) {
  // VariableBuilder lhsBuilder = (VariableBuilder) var;
  // LinearBuilder rhsBuilder = (LinearBuilder) exp;

}
