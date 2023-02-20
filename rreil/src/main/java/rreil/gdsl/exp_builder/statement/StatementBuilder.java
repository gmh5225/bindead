package rreil.gdsl.exp_builder.statement;

import gdsl.rreil.statement.IStatement;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.Builder;
import rreil.lang.RReil;

public abstract class StatementBuilder extends Builder<RReil> implements IStatement {
  public StatementBuilder(BuildingStateManager manager) {
    super(manager);
  }
}
