package bindead.abstractsyntax.zeno.util;

import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.Linear;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.exceptions.Unreachable;

/**
 * Collection of helper methods dealing with Tests in Zeno.
 */
public class ZenoTestHelper {

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
    if (test.getExpr().isConstantOnly()) {
      BigInt exprValue = test.getExpr().getConstant();
      switch (test.getOperator()) {
      case EqualToZero:
        if (exprValue.isZero())
          return true;
        else {
          if (AnalysisProperties.INSTANCE.debugTests.isTrue())
            System.out.println("Tautology unreachable: " + test);
          throw new Unreachable();
        }
      case NotEqualToZero:
        if (!exprValue.isZero())
          return true;
        else {
          if (AnalysisProperties.INSTANCE.debugTests.isTrue())
            System.out.println("Tautology unreachable: " + test);
          throw new Unreachable();
        }
      case LessThanOrEqualToZero:
        if (!exprValue.isPositive())
          return true;
        else {
          if (AnalysisProperties.INSTANCE.debugTests.isTrue())
            System.out.println("Tautology unreachable: " + test);
          throw new Unreachable();
        }
      }
    }
    return false;
  }

  /**
   * Evaluate a test on the state and return if it is redundant, i.e. always satisfiable or not.
   */
  public static <D extends ZenoDomain<D>> boolean isAlwaysSatisfiable (Test test, D state) {
    return satisfiabilityWithQuery(test, state);
  }

  /**
   * Return the satisfiability of a test by evaluating the negated test in the given state.
   */
  private static <D extends ZenoDomain<D>> boolean satisfiabilityWithNegatedEval (Test test, D state) {
    Test opposingTest = test.not();
    try {
      state.eval(opposingTest);
    } catch (Unreachable _) {
      // the opposing test is unsatisfiable, thus, the original test is redundant
      return true;
    }
    return false;
  }

  /**
   * Return the satisfiability of a test by evaluating it in the given state and see if it modified the state.
   */
  private static <D extends ZenoDomain<D>> boolean satisfiabilityWithEval (Test test, D state) {
    try {
      D newState = state.eval(test);
      // if the test did not modify the state (make it smaller) then the original state is still subset-equal to it
      boolean redundant = state.subsetOrEqual(newState);
      return redundant;
    } catch (Unreachable _) {
      return false;
    }
  }

  /**
   * Return the satisfiability of a test by querying the values of the tested variables in the given state.
   */
  private static <D extends ZenoDomain<D>> boolean satisfiabilityWithQuery (Test test, D state) {
    Linear expr = test.getExpr();
    Interval exprValue = state.queryRange(expr).convexHull();
    switch (test.getOperator()) {
    case EqualToZero:
      return exprValue.isEqualTo(Interval.ZERO);
    case LessThanOrEqualToZero:
      return exprValue.isLessThanZero() || exprValue.isEqualTo(Interval.ZERO);
    case NotEqualToZero:
      return !exprValue.isEqualTo(Interval.ZERO);
    default:
      throw new IllegalStateException();
    }
  }

  /**
   * Return a set containing the tests in the passed in set but negated.
   */
  public static AVLSet<Test> negateTests (AVLSet<Test> tests) {
    AVLSet<Test> result = AVLSet.empty();
    for (Test test : tests) {
      result = result.add(test.not());
    }
    return result;
  }

  /**
   * Performs the syntactic predicate entailment test as described in the predicates paper.
   * @return {@code true} if {@code byTest} → {@code test}
   */
  public static boolean isSyntacticallyEntailedBy (Test test, Test byTest) {
    if (byTest.equals(test))
      return true;
    Linear testLinear = test.getExpr();
    if (!testLinear.isSingleTerm()) // only tests about one single variable are of interest
      return false;
    Linear byTestLinear = byTest.getExpr();
    if (!byTestLinear.isSingleTerm()) // only tests about one single variable are of interest
      return false;
    if (!testLinear.dropConstant().equals(byTestLinear.dropConstant())) // must be about the same variables and signs
      return false;
    BigInt testConstant = testLinear.getConstant().negate();
    BigInt byTestConstant = byTestLinear.getConstant().negate();
    switch (test.getOperator()) {
    case EqualToZero:
      return false; // only the case where test == byTest is useful here and that is caught above in the first line
    case NotEqualToZero:
      if (isEqual(byTest.getOperator()) && !byTestConstant.isEqualTo(testConstant))
        return true;
      if (isLessThanOrEqual(byTest.getOperator()) && byTestConstant.isLessThan(testConstant))
        return true;
      return false;
    case LessThanOrEqualToZero:
      if (isEqual(byTest.getOperator()) && byTestConstant.isLessThanOrEqualTo(testConstant))
        return true;
      if (isLessThanOrEqual(byTest.getOperator()) && byTestConstant.isLessThanOrEqualTo(testConstant))
        return true;
      return false;
    default:
      return false;
    }
  }

  private static boolean isEqual (ZenoTestOp operator) {
    return operator.equals(ZenoTestOp.EqualToZero);
  }

  private static boolean isLessThanOrEqual (ZenoTestOp operator) {
    return operator.equals(ZenoTestOp.LessThanOrEqualToZero);
  }

}