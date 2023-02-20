package rreil.lang;

public enum BinOp implements Reconstructable {
  /** 
   * Todo: Remove, not contained in LinBinOp
   */
  Add("add", "+"), Sub("sub", "-"),
  /**
   * Todo: end
   */
  Mul("mul", "*"), Divu("div", "/u"), Divs(
      "divs", "/s"), Mod("mod", "%"), Mods("mods", "%s"), Shl("shl", "<<"), Shr(
      "shr", ">>u"), Shrs("shrs", ">>s"), Xor("xor", "xor"), Or("or", "or"), And(
      "and", "and");
  private final String asInfix;
  private final String asPrefix;

  BinOp (final String asPrefix, final String asInfix) {
    this.asInfix = asInfix;
    this.asPrefix = asPrefix;
  }

  public String asInfix () {
    return asInfix;
  }

  public String asPrefix () {
    return asPrefix;
  }

  public SignednessHint signedness () {
    switch (this) {
    case Divs:
    case Shrs:
      return SignednessHint.ForceSigned;
    case Divu:
    case Shr:
      return SignednessHint.ForceUnsigned;
    default:
      return SignednessHint.DontCare;
    }
  }

  @Override public String reconstructCode () {
    return "BinOp." + toString();
  }
}
