package rreil.gdsl.builder;

import rreil.gdsl.BuildingStateManager;
import rreil.lang.RReil.Branch.BranchTypeHint;
import gdsl.rreil.IBranchHint;

/**
 * This class is used to build a {@link BranchTypeHint} object that represents
 * a Gdsl RReil branch hint AST node.
 * 
 * @author Julian Kranz
 */
public class BranchHintBuilder extends Builder<BranchTypeHint> implements IBranchHint {
  private BranchTypeHint hint;
  
  public BranchHintBuilder (BuildingStateManager manager, BranchTypeHint hint) {
    super(manager);
    this.hint = hint;
  }

  @Override public BuildResult<? extends BranchTypeHint> build () {
    return result(hint);
  }
}
