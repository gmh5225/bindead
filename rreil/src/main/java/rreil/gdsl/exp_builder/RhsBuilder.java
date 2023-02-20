package rreil.gdsl.exp_builder;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rval;

public abstract class RhsBuilder extends Builder<Rhs> {
  public RhsBuilder(BuildingStateManager manager) {
    super(manager);
    // TODO Auto-generated constructor stub
  }
  
  public BuildResult<? extends Rval> buildRval() {
    BuildResult<? extends Rhs> myResult = build();
    SortedMap<RReilAddr, RReil> stmts = myResult.getStatements();
    
    BuildResult<? extends Rval> tempR = buildRval(myResult.getResult());
    stmts.putAll(tempR.getStatements());

    return result(tempR.getResult(), stmts);
  }
}
