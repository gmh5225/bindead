package rreil.gdsl.exp_builder;

import gdsl.rreil.expression.ICompare;

import java.util.SortedMap;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.exp_builder.linear.LinearBuilder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.ComparisonOp;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.RhsFactory;

public class CompareBuilder extends Builder<Rhs> implements ICompare {
  private LinearBuilder lhs;
  private LinearBuilder rhs;
  private ComparisonOp op;

  public CompareBuilder (BuildingStateManager manager, ComparisonOp op,
      LinearBuilder lhs, LinearBuilder rhs) {
    super(manager);
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override public CompareBuilder size (int size) {
    lhs.size(size);
    rhs.size(size);
    return this;
  }

  @Override public BuildResult<? extends Rhs> build () {
    BuildResult<? extends Rval> lhsR = lhs.buildRval();
    BuildResult<? extends Rval> rhsR = rhs.buildRval();

    SortedMap<RReilAddr, RReil> stmts = lhsR.before(rhsR);

    return result(
        RhsFactory.getInstance().comparison(lhsR.getResult(), op, rhsR.getResult()),
        stmts);
  }

  public int getSize () {
    return 1;
  }
}
