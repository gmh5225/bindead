package rreil.lang;


public enum ComparisonOp implements Reconstructable {
  Cmpeq("cmpeq", "=="),
  Cmpneq("cmpneq", "!="),
  Cmples("cmples", "<="),
  Cmpleu("cmpleu", "<=u"),
  Cmplts("cmplts", "<"),
  Cmpltu("cmpltu", "<u");
  private final String asPrefix;
  private final String asInfix;

  ComparisonOp (String asPrefix, String asInfix) {
    this.asPrefix = asPrefix;
    this.asInfix = asInfix;
  }

  public String asInfix () {
    return asInfix;
  }

  public String asPrefix () {
    return asPrefix;
  }

  public SignednessHint signedness () {
    switch (this) {
      case Cmples:
      case Cmplts:
        return SignednessHint.ForceSigned;
      case Cmpleu:
      case Cmpltu:
        return SignednessHint.ForceUnsigned;
      default:
        return SignednessHint.DontCare;
    }
  }

  @Override public String reconstructCode () {
    return "ComparisonOp." + toString();
  }
}
