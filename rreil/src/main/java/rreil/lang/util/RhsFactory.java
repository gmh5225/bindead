package rreil.lang.util;

import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import rreil.lang.BinOp;
import rreil.lang.ComparisonOp;
import rreil.lang.LinBinOp;
import rreil.lang.MemVar;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Bin;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.Convert;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.LinBin;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.LinScale;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Rhs.SignExtend;

public class RhsFactory {
  private static final RhsFactory instance = new RhsFactory();

  private RhsFactory () {
  }

  public static RhsFactory getInstance () {
    return instance;
  }

  public Rvar variable (int size, int offset, MemVar id) {
    return new Rhs.Rvar(size, offset, id);
  }

  public Address rreilAddress (int size, RReilAddr address) {
    return new Address(size, address);
  }
  
  public Rhs.LinRval rreilAddressLin (int size, RReilAddr address) {
    return new Rhs.LinRval(rreilAddress(size, address));
  }

  public Rlit literal (int size, BigInt value) {
    return new Rhs.Rlit(size, value);
  }

  public Cmp comparison (Rhs.Lin left, ComparisonOp op, Rhs.Lin right) {
    return new Rhs.Cmp(left, op, right);
  }

  @Deprecated public Cmp comparison (Rhs.Rval left, ComparisonOp op, Rhs.Rval right) {
    return new Rhs.Cmp(new Rhs.LinRval(left), op, new Rhs.LinRval(right));
  }

  public Bin binary (Rhs.Rval left, BinOp op, Rhs.Rval right) {
    return new Rhs.Bin(left, op, right);
  }

  public LinBin binary (Rhs.Lin left, LinBinOp op, Rhs.Lin right) {
    return new Rhs.LinBin(left, op, right);
  }

  @Deprecated public LinBin binary (Rhs.Rval left, LinBinOp op, Rhs.Rval right) {
    return new Rhs.LinBin(new Rhs.LinRval(left), op, new Rhs.LinRval(right));
  }

  public LinScale scale (Rhs.Lin opnd, BigInt _const) {
    return new Rhs.LinScale(opnd, _const);
  }

  public LinRval linRval (Rhs.Rval rval) {
    return new Rhs.LinRval(rval);
  }

  public SignExtend castSx (Rhs.Rval rhs) {
    return new Rhs.SignExtend(rhs);
  }

  public Convert castZx (Rhs.Rval rhs) {
    return new Rhs.Convert(rhs);
  }

  public RangeRhs arbitrary (int size) {
    return new Rhs.RangeRhs(size, Interval.unsignedTop(size));
  }

  public RangeRhs top (int size) {
    return new Rhs.RangeRhs(size, Interval.TOP);
  }

  public RangeRhs range (int size, Interval range) {
    return new Rhs.RangeRhs(size, range);
  }
  
  public Rhs.LinRval trueLin() {
    return new Rhs.LinRval(Rlit.true_);
  } 
  
  public Rhs.LinRval falseLin() {
    return new Rhs.LinRval(Rlit.false_);
  }
}
