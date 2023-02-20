package rreil.gdsl.builder;

import gdsl.rreil.IRReilCollection;
import gdsl.rreil.statement.IStatement;

import java.util.ArrayList;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.StatementCollection;
import rreil.gdsl.builder.statement.StatementBuilder;
import rreil.lang.RReil;
import rreil.lang.util.RReilFactory;

public class StatementCollectionBuilder extends Builder<StatementCollection> implements IRReilCollection<IStatement> {
  private ArrayList<StatementBuilder> statements = new ArrayList<StatementBuilder>();
  
  public StatementCollectionBuilder(BuildingStateManager manager) {
    super(manager);
  }
  
  @Override
  public void add(IStatement statement) {
    statements.add((StatementBuilder)statement);
    
  }
  
  @Override
  public IStatement get(int i) {
    return statements.get(i);
  }

  @Override
  public BuildResult<StatementCollection> build() {
    StatementCollection statements = new StatementCollection();
    
    for (StatementBuilder statement : this.statements) {
      BuildResult<? extends RReil> bR = statement.build();
      statements.addAll(bR.getStatements());
      statements.add(bR.getResult());
    }
    
    if(manager.hasStagedAddress()) {
      RReil nop = RReilFactory.instance.nop(manager.nextAddress());
      statements.add(nop);
    }

    return result(statements);
  }

  @Override
  public StatementCollectionBuilder size(int size) {
    throw new RuntimeException("Not supported");
  }
  
  @Override
  public int size() {
    throw new RuntimeException("Not supported");
  }

  @Override
  public int getSize() {
    throw new RuntimeException("Not supported");
  }

}
