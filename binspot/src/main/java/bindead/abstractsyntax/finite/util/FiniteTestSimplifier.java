package bindead.abstractsyntax.finite.util;

import javalx.numeric.BigInt;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.exceptions.Unreachable;

/**
 * Simplifies tests syntactically and sees if they hold or not, if possible.
 *
 * @author Bogdan Mihaila
 */
public class FiniteTestSimplifier {
  private static final FiniteFactory finite = FiniteFactory.getInstance();
  private final static Test trueTest = finite.equalTo(1, Linear.ONE, Linear.ONE);
  private final static Test falseTest = finite.equalTo(1, Linear.ZERO, Linear.ONE);

  public static Test normalizeTautology (Test test) {
    Linear lhs = test.getLeftExpr();
    Linear rhs = test.getRightExpr();
    if (lhs.equals(rhs)) {
      return hasEqualSides(test);
    } else {
      return test;
    }
  }

  private static Test hasEqualSides (Test test) {
    switch (test.getOperator()) {
    case Equal:
      return trueTest;
    case NotEqual:
      return falseTest;
    case SignedLessThan:
      return falseTest;
    case SignedLessThanOrEqual:
      return trueTest;
    case UnsignedLessThan:
      return falseTest;
    case UnsignedLessThanOrEqual:
      return trueTest;
    default:
      throw new IllegalArgumentException();
    }
  }

  /**
   * Simplify a test and see if it satisfiable or unsatisfiable using the constants in the test only.
   *
   * @param test the test statement
   * @return {@code true} if the test is tautologous or {@code false} if nothing could be simplified.
   */
  public static boolean isTautology (Test test) {
    try {
      return isTautologyReportUnreachable(test);
    } catch (Unreachable e) {
      return true;
    }
  }

  /**
   * Simplify a test and see if it satisfiable or unsatisfiable using the constants in the test only.
   *
   * @param test the test statement
   * @return {@code true} if the test is tautologous or {@code false} if nothing could be simplified.
   * @throws {@link Unreachable} if the test is trivially unsatisfiable
   */
  public static boolean isTautologyReportUnreachable (Test test) throws Unreachable {
    Linear lhs = test.getLeftExpr();
    Linear rhs = test.getRightExpr();
    if (lhs.isConstantOnly() && rhs.isConstantOnly()) {
      BigInt lhsValue = lhs.getConstant();
      BigInt rhsValue = rhs.getConstant();
      switch (test.getOperator()) {
      case Equal:
        if (lhsValue.isEqualTo(rhsValue))
          return true;
        else
          throw new Unreachable();
      case NotEqual:
        if (!lhsValue.isEqualTo(rhsValue))
          return true;
        else
          throw new Unreachable();
      // the remaining cases would need the value to be wrapped, so we ignore them
      case SignedLessThan:
      case SignedLessThanOrEqual:
      case UnsignedLessThan:
      case UnsignedLessThanOrEqual:
      default:
        return false;
      }
    }
    return false;
  }
}
