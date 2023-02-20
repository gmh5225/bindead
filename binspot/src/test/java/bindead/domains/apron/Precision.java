package bindead.domains.apron;

import static bindead.TestsHelper.lines;
import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.exceptions.DomainStateException;

/**
 * Some tests that show the powers and limitation of Apron and its various domains
 * alone or when combined with our domains.
 * TODO: add more tests, especially comparisons to our domains
 *
 * @author Bogdan Mihaila
 */
public class Precision {
  @SuppressWarnings("unused")
  private final static AnalysisFactory bindeadAnalyzer = new AnalysisFactory(); // use default domain hierarchy
  private static AnalysisFactory apronAnalyzer;
  static {
    if (NativeLibsLoading.haveApronNativeLibraries())
      apronAnalyzer =
        new AnalysisFactory().disableDomains("Undef", "Congruences", "Intervals").enableDomains("Apron(Intervals)");
  }

  @Before public void precondition () {
    // ignore Tests if the preconditions (native library installed) are not satisfied
    assumeTrue(NativeLibsLoading.haveApronNativeLibraries());
  }

  /**
   * With the old Affine domain and our Intervals this test does not work.
   */
  @Test public void canonical3Variables () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x1, [1, 2]",
        "mov x2, [0, 100]",
        "add x3, x1, x2",
        "assert x1 = [1, 2]",
        "assert x2 = [0, 100]",
        "assert x3 = [1, 102]",
        "halt");
    Analysis<?> analysis = apronAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Division by a constant of value zero should result in zero as dictated by the ARM semantics.
   */
  @Test public void divisionByZero1 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x1, [10, 11]",
        "mov x2, 0",
        "divs x3, x1, x2",
        "assert x3 = 0",
        "halt");
    Analysis<?> analysis = apronAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Division by an interval containing zero is not yet implemented in Apron.
   */
  @Test(expected = DomainStateException.UnimplementedMethodException.class)
  public void divisionByZero2 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x1, 10",
        "mov x2, [-1, 1]",
        "divs x3, x1, x2",
        "assert x3 = [-10, 10]",
        "halt");
    Analysis<?> analysis = apronAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

}
