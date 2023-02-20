package rreil.lang.util;

import rreil.lang.RReil;

public interface RReilVisitor<R, T> {
  public R visit (RReil.Assign stmt, T data);

  public R visit (RReil.Load stmt, T data);

  public R visit (RReil.Store stmt, T data);

  public R visit (RReil.BranchToNative stmt, T data);

  public R visit (RReil.BranchToRReil stmt, T data);

  public R visit (RReil.Nop stmt, T data);

  public R visit (RReil.Assertion stmt, T data);

  public R visit (RReil.Branch stmt, T data);
  
  public R visit (RReil.PrimOp stmt, T data);

  public R visit (RReil.Native stmt, T data);
  
  public R visit (RReil.Throw stmt, T data);
  
  public R visit (RReil.Flop stmt, T data);
}
