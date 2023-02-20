package rreil.lang;

/**
 * A binary operation in a linear, that is, either addition or subtraction.
 */
public enum LinBinOp implements Reconstructable {
  Add("add", "+"),
  Sub("sub", "-");

  private final String asInfix;
  private final String asPrefix;

  LinBinOp (final String asPrefix, final String asInfix) {
    this.asInfix = asInfix;
    this.asPrefix = asPrefix;
  }

  public String asInfix () {
    return asInfix;
  }

  public String asPrefix () {
    return asPrefix;
  }

  @SuppressWarnings("static-method")
  public SignednessHint signedness () {
    return SignednessHint.DontCare;
  }

  @Override public String reconstructCode () {
    return "LinBinOp." + toString();
  }
}
