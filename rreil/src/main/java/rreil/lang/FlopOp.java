package rreil.lang;

public enum FlopOp implements Reconstructable {
  Fadd("fadd", "f+"),
  Fsub("fsub", "f-"),
  Fmul("fmul", "f*");
  private final String asInfix;
  private final String asPrefix;

  FlopOp (final String asPrefix, final String asInfix) {
    this.asInfix = asInfix;
    this.asPrefix = asPrefix;
  }

  public String asInfix () {
    return asInfix;
  }

  public String asPrefix () {
    return asPrefix;
  }

  @Override public String reconstructCode () {
    return "FlopOp." + toString();
  }
}
