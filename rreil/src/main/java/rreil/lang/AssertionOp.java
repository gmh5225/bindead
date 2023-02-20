package rreil.lang;

import java.util.HashMap;
import java.util.Map;

import rreil.lang.SignednessHint;

/**
 * Comparison operators. Currently only used in the assertion instructions.
 *
 * @author Bogdan Mihaila
 */
public enum AssertionOp implements Reconstructable {
  AffineEquality("*+="), // by affine equality
  Equal("="), // by value
  NotEqual("!="),
  UnsignedLessThanOrEqual("<=u"),
  SignedLessThanOrEqual("<="),
  UnsignedLessThan("<u"),
  SignedLessThan("<");

  private final String opString;
  private static final Map<String, AssertionOp> mappings = new HashMap<String, AssertionOp>();
  static {
    for (AssertionOp operator : AssertionOp.values()) {
      mappings.put(operator.toString(), operator);
    }
  }

  AssertionOp (String opString) {
    this.opString = opString;
  }

  public static AssertionOp from (String string) {
    return mappings.get(string);
  }

  public ComparisonOp toComparison () {
    switch (this) {
    case Equal:
      return ComparisonOp.Cmpeq;
    case AffineEquality:
      return ComparisonOp.Cmpeq;
    case NotEqual:
      return ComparisonOp.Cmpneq;
    case SignedLessThan:
      return ComparisonOp.Cmplts;
    case SignedLessThanOrEqual:
      return ComparisonOp.Cmples;
    case UnsignedLessThan:
      return ComparisonOp.Cmpltu;
    case UnsignedLessThanOrEqual:
      return ComparisonOp.Cmpleu;
    default:
      throw new IllegalStateException();
    }
  }

  @Override public String toString () {
    return opString;
  }

  public SignednessHint signedness () {
    switch (this) {
      case SignedLessThan:
      case SignedLessThanOrEqual:
        return SignednessHint.ForceSigned;
      case UnsignedLessThan:
      case UnsignedLessThanOrEqual:
        return SignednessHint.ForceUnsigned;
      default:
        return SignednessHint.DontCare;
    }
  }

  @Override public String reconstructCode () {
    return "AssertionOp." + super.toString();
  }

}
