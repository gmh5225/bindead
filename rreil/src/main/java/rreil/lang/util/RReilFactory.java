package rreil.lang.util;

import java.util.List;

import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import rreil.lang.AssertionOp;
import rreil.lang.BinOp;
import rreil.lang.ComparisonOp;
import rreil.lang.FlopOp;
import rreil.lang.Lhs;
import rreil.lang.LinBinOp;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Native;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Throw;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.LinBin;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Test;

public class RReilFactory {
  private static final RhsFactory rhs = RhsFactory.getInstance();
  public static final RReilFactory instance = new RReilFactory();

  public RReil.Assertion assertionReachable (RReilAddr address) {
    return new RReil.Assertion.AssertionReachable(address);
  }

  public RReil.Assertion assertionUnreachable (RReilAddr address) {
    return new RReil.Assertion.AssertionUnreachable(address);
  }

  public RReil.Assertion assertionWarnings (RReilAddr address, int numberOfWarnings) {
    return new RReil.Assertion.AssertionWarnings(address, numberOfWarnings);
  }

  public RReil.Assertion assertion (RReilAddr address, Rhs lhs, AssertionOp operator, Rhs rhs, int size) {
    return new RReil.Assertion.AssertionCompare(address, lhs, operator, rhs, size);
  }

  public Test test (Rhs.Cmp comparison) {
    return new Test(comparison);
  }

  public Test testIfZero (Rhs.Lin value) {
    Cmp cmp = new Cmp(value, ComparisonOp.Cmpeq, rhs.falseLin());
    return new Test(cmp);
  }

  public Test testIfNonZero (Rhs.Lin value) {
    Cmp cmp = new Cmp(value, ComparisonOp.Cmpneq, rhs.falseLin());
    return new Test(cmp);
  }

  public Test testIfZero (Rhs.Rval value) {
    Cmp cmp = new Cmp(new LinRval(value), ComparisonOp.Cmpeq, rhs.falseLin());
    return new Test(cmp);
  }

  public Test testIfNonZero (Rhs.Rval value) {
    Cmp cmp = new Cmp(new LinRval(value), ComparisonOp.Cmpneq, rhs.falseLin());
    return new Test(cmp);
  }

  public RReil.Assign assign (RReilAddr address, Lhs lhs, Rhs rhs) {
    return new RReil.Assign(address, lhs, rhs);
  }

  public RReil.Load load (RReilAddr address, Lhs lhs, Rhs.Rval rhs) {
    return new RReil.Load(address, lhs, new LinRval(rhs));
  }

  public RReil.Load load (RReilAddr address, Lhs lhs, Rhs.Lin rhs) {
    return new RReil.Load(address, lhs, rhs);
  }

  public RReil.Store store (RReilAddr address, Rhs.Rval lhs, Rhs.Rval rhs) {
    return new RReil.Store(address, new LinRval(lhs), new LinRval(rhs));
  }

  public RReil.Store store (RReilAddr address, Rhs.Lin lhs, Rhs.Lin rhs) {
    return new RReil.Store(address, lhs, rhs);
  }

  public RReil.BranchToNative branchNative (RReilAddr address, Rhs.Rval cond, Rhs.Rval target) {
    return new RReil.BranchToNative(address, new LinRval(cond), new LinRval(target));
  }

  public RReil.BranchToNative branchNative (RReilAddr address, Rhs.SimpleExpression cond, Rhs.Lin target) {
    return new RReil.BranchToNative(address, cond, target);
  }

  /**
   * TODO: remove when intra-RREIL jumps are replaced by the newer if-then-else and while constructs
   */
  public RReil.BranchToRReil branch (RReilAddr address, Rhs.Rval cond, Rhs.Address target) {
    return new RReil.BranchToRReil(address, new LinRval(cond), target);
  }
  
  public RReil.BranchToRReil branch (RReilAddr address, Rhs.SimpleExpression cond, Rhs.Address target) {
    return new RReil.BranchToRReil(address, cond, target);
  }

  public RReil.Branch branchNative (RReilAddr address, Rhs.Rval target, RReil.Branch.BranchTypeHint hint) {
    return new RReil.Branch(address, new LinRval(target), hint);
  }

  public RReil.Branch branchNative (RReilAddr address, Rhs.Lin target, RReil.Branch.BranchTypeHint hint) {
    return new RReil.Branch(address, target, hint);
  }

  public RangeRhs range (int size, Interval range) {
    return rhs.range(size, range);
  }

  public Rhs.Rvar variable (int size, int offset, MemVar id) {
    return rhs.variable(size, offset, id);
  }

  public Address rreilAddress (int size, RReilAddr address) {
    return rhs.rreilAddress(size, address);
  }

  public Rhs.Rlit literal (int size, BigInt value) {
    return rhs.literal(size, value);
  }

  public Rhs.Cmp comparision (Rhs.Rval left, ComparisonOp op, Rhs.Rval right) {
    return rhs.comparison(left, op, right);
  }

  public Rhs.Bin binary (Rhs.Rval left, BinOp op, Rhs.Rval right) {
    return rhs.binary(left, op, right);
  }

  public LinBin binary (Rhs.Rval left, LinBinOp op, Rhs.Rval right) {
    return rhs.binary(left, op, right);
  }

  public Rhs.SignExtend castSx (Rhs.Rval rhs) {
    return RReilFactory.rhs.castSx(rhs);
  }

  public Rhs.Convert castZx (Rhs.Rval rhs) {
    return RReilFactory.rhs.castZx(rhs);
  }

  public RReil.Nop nop (RReilAddr address) {
    return new RReil.Nop(address);
  }

  public Native nativeInsn (RReilAddr address, String name, Rhs.Rlit opnd) {
    return new Native(address, name, opnd);
  }

  public PrimOp primOp (RReilAddr address, String name, List<Lhs> outArgs, List<Rhs.Rval> inArgs) {
    return new PrimOp(address, name, outArgs, inArgs);
  }

  public RReil.Flop flop (RReilAddr address, FlopOp flop, Rvar lhs,
      List<Rvar> rhs, Rvar flags) {
    return new RReil.Flop(address, flop, lhs, rhs, flags);
  }

  public Throw throw_ (RReilAddr address, String exception) {
    return new Throw(address, exception);
  }

}
