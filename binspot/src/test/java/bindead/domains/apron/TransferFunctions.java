package bindead.domains.apron;

import static bindead.TestsHelper.lines;
import static bindead.debug.DebugHelper.logln;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javalx.numeric.Interval;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.DomainFactory;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.DebugHelper;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.apron.TransferFunctions.CustomizedJunitTestSuite;
import bindead.exceptions.Unreachable;

/**
 * Test the transfer functions of the Apron domain. This class runs the tests for the transfer functions
 * that are collected in a subclass first for a numeric domain stack of Bindead and then one with Apron.
 * The subtle differences in the results are tested for and annotated in the source code.
 *
 * @author Bogdan Mihaila
 */
// Use custom Runner implementation to run tests with different parameters
@RunWith(CustomizedJunitTestSuite.class)
@SuppressWarnings({"rawtypes", "unused"})
public class TransferFunctions {
  /**
   * Used to pass either a domain + analyzer config with Apron or with our domains.
   */
  private static class AnalyzerConfig {
    private static final FiniteDomain bindeadTop = DomainFactory
        .parseFiniteDomain("Wrapping Affine Congruences Intervals -IntervalSets");
    private static FiniteDomain apronTop;

    private final static AnalysisFactory bindeadAnalyzer = new AnalysisFactory(); // use default domain hierarchy
    private static AnalysisFactory apronAnalyzer;

    static {
      if (NativeLibsLoading.haveApronNativeLibraries()) {
        apronTop = DomainFactory.parseFiniteDomain("Wrapping -Congruences Apron(Polyhedra)");
        apronAnalyzer = new AnalysisFactory(
          "SegMem Processor Stack Data Fields Undef SupportSet Predicates(F) PointsTo " +
            "Wrapping -ThresholdsWidening -Congruences Apron(Polyhedra)");
      }
    }

    private final boolean usesApronNumerics;

    @Before public void precondition () {
      // ignore Tests if the preconditions (native library installed) are not satisfied
      assumeTrue(NativeLibsLoading.haveApronNativeLibraries());
    }

    public AnalyzerConfig (boolean useApronNumerics) {
      this.usesApronNumerics = useApronNumerics;
    }

    public RichFiniteDomain getInitialDomain () {
      if (usesApron())
        return FiniteDomainHelper.for32bitVars(apronTop);
      else
        return FiniteDomainHelper.for32bitVars(bindeadTop);
    }

    public AnalysisFactory getAnalyzer () {
      if (usesApron())
        return apronAnalyzer;
      else
        return bindeadAnalyzer;
    }

    public boolean usesApron () {
      return usesApronNumerics;
    }
  }

  private static final NumVar x1 = NumVar.fresh("x1");
  private static final NumVar x2 = NumVar.fresh("x2");
  private static final NumVar x3 = NumVar.fresh("x3");
  private static final NumVar x4 = NumVar.fresh("x4");
  private static final NumVar x5 = NumVar.fresh("x5");

  private final AnalyzerConfig config;

  public TransferFunctions (AnalyzerConfig config) {
    this.config = config;
  }

  /**
   * Silence any debug output that was enabled by previous tests. Comment out to see messages.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  @Before public void precondition () {
    // ignore Tests if the preconditions (native library installed) are not satisfied
    assumeTrue(NativeLibsLoading.haveApronNativeLibraries());
//    DebugHelper.analysisKnobs.printLogging();
  }

  @Test public void introduction () throws Exception {
    RichFiniteDomain d = config.getInitialDomain();
    logln("TOP: " + d);
    d = d.introduce(x1);
    d = d.introduce(x2, 1);
    logln("introducing x1 = T, x2 = 1:\n" + d);
    d.assertValueIs(x1, Interval.TOP);
    d.assertValueIs(x2, 1);
  }

  @Test public void projection () throws Exception {
    RichFiniteDomain d = config.getInitialDomain();
    logln("TOP: " + d);
    d = d.introduce(x1);
    d = d.introduce(x2, 1);
    logln("d: " + d);
    d = d.project(x1);
    d = d.project(x2);
    logln("projecting x1, x2:\n" + d);
    d.assertValueIs(x1, Interval.TOP);
    d.assertValueIs(x2, Interval.TOP);
  }

  @Test public void substitution () throws Exception {
    RichFiniteDomain d = config.getInitialDomain();
    d = d.introduce(x1, 1);
    logln("d: " + d);
    d = d.substitute(x1, x2);
    logln("substituting x1 by x2:\n" + d);
    d.assertValueIs(x1, Interval.TOP);
    d.assertValueIs(x2, 1);
  }

  @Test public void join () throws Exception {
    RichFiniteDomain d1 = config.getInitialDomain();
    RichFiniteDomain d2 = config.getInitialDomain();
    d1 = d1.introduce(x1, 0);
    d1 = d1.introduce(x2, 1);
    logln("d1: " + d1);
    d2 = d2.introduce(x1, 1);
    d2 = d2.introduce(x2, 2);
    logln("d2: " + d2);
    RichFiniteDomain join = d1.join(d2);
    logln("joining both: " + join);
    join.assertValueIs(x1, Interval.of(0, 1));
    join.assertValueIs(x2, Interval.of(1, 2));
  }

  @Test public void widen () throws Exception {
    RichFiniteDomain d1 = config.getInitialDomain();
    RichFiniteDomain d2 = config.getInitialDomain();
    d1 = d1.introduce(x1, 0);
    d1 = d1.introduce(x2, 1);
    logln("d1: " + d1);
    d2 = d2.introduce(x1, 1);
    d2 = d2.introduce(x2, 2);
    logln("d2: " + d2);
    RichFiniteDomain widened1 = d1.widen(d1.join(d2));
    logln("widening d1 with d1vd2: " + widened1);
    widened1.assertValueIs(x1, Interval.upFrom(0));
    widened1.assertValueIs(x2, Interval.upFrom(1));
    RichFiniteDomain widened2 = d1.join(d2).widen(d2);
    logln("widening d1vd2 with d2: " + widened2);
    if (config.usesApron())
      widened2.assertValueIs(x1, 1); // weird but true in Apron(Polyhedra), as it expects d1 leq d2, Octagons seem to work
    else
      widened2.assertValueIs(x1, Interval.of(0, 1)); // our numeric domains widen here to [0, 1]
    if (config.usesApron()) // same as above, difference between our numeric domains and Apron
      widened2.assertValueIs(x2, 2);
    else
      widened2.assertValueIs(x2, Interval.of(1, 2));
    RichFiniteDomain widened3 = d1.widen(d2);
    logln("widening d1 with d2: " + widened3);
    if (config.usesApron()) // same as above, difference between our numeric domains and Apron
      widened3.assertValueIs(x1, Interval.TOP);
    else
      widened3.assertValueIs(x1, Interval.upFrom(0));
    if (config.usesApron()) // same as above, difference between our numeric domains and Apron
      widened3.assertValueIs(x2, Interval.TOP);
    else
      widened3.assertValueIs(x2, Interval.upFrom(1));
    RichFiniteDomain widened4 = d2.widen(d1);
    logln("widening d2 with d1: " + widened4);
    if (config.usesApron()) // same as above, difference between our numeric domains and Apron
      widened4.assertValueIs(x1, Interval.TOP);
    else
      widened4.assertValueIs(x1, Interval.downFrom(1));
    if (config.usesApron()) // same as above, difference between our numeric domains and Apron
      widened4.assertValueIs(x2, Interval.TOP);
    else
      widened4.assertValueIs(x2, Interval.downFrom(2));
  }

  @Test public void subset () throws Exception {
    RichFiniteDomain d1 = config.getInitialDomain();
    RichFiniteDomain d2 = config.getInitialDomain();
    d1 = d1.introduce(x1, 0);
    d1 = d1.introduce(x2, 1);
    logln("d1: " + d1);
    d2 = d2.introduce(x1, 1);
    d2 = d2.introduce(x2, 2);
    logln("d2: " + d2);
    RichFiniteDomain join = d1.join(d2);
    logln("joining both: " + join);
    join.assertValueIs(x1, Interval.of(0, 1));
    join.assertValueIs(x2, Interval.of(1, 2));
    assertTrue(join.subsetOrEqual(join));
    assertTrue(d1.subsetOrEqual(join));
    assertTrue(d2.subsetOrEqual(join));
  }

  @Test public void assign () throws Exception {
    RichFiniteDomain d = config.getInitialDomain();
    d = d.introduce(x1, 11);
    d = d.introduce(x2);
    d = d.introduce(x3, 3);
    d = d.assign(x2, 22);
    d = d.assign(x3, 33);
    logln("d: " + d);
    d.assertValueIs(x1, 11);
    d.assertValueIs(x2, 22);
    d.assertValueIs(x3, 33);
    d = d.assign(x2, x1);
    logln("assigning x2 = x1:\n" + d);
    d.assertValueIs(x2, 11);
    d = d.assign(x2, 2, x1);
    logln("assigning x2 = 2 * x1:\n" + d);
    d.assertValueIs(x2, 22);
    d = d.assign(x2, x1, x3);
    logln("assigning x2 = x1 + x3:\n" + d);
    d.assertValueIs(x2, 44);
  }

  @Test public void tests () throws Exception {
    RichFiniteDomain d = config.getInitialDomain();
    d = d.introduce(x1);
    d = d.introduce(x2);
    d = d.assign(x1, Interval.of(0, 5));
    d = d.assign(x2, Interval.of(4, 25));
    logln("d:\n" + d);
    d.assertValueIs(x1, Interval.of(0, 5));
    d.assertValueIs(x2, Interval.of(4, 25));
    // remove a middle element with disequality
    RichFiniteDomain d1 = d.notEqualTo(x1, 3);
    logln("testing x1 != 3:\n" + d1); // with our numeric domains, IntervalSets would cut the 3 out
    d1.assertValueIs(x1, Interval.of(0, 5)); // over-approximation
    // remove a right-most element with disequality
    RichFiniteDomain d2 = d.notEqualTo(x1, 5);
    logln("testing x1 != 5:\n" + d2);
    d2.assertValueIs(x1, Interval.of(0, 4));
    // remove a right-most element with lower test
    RichFiniteDomain d3 = d.lessOrEqualTo(x1, 4);
    logln("testing x1 <= 4:\n" + d3);
    d3.assertValueIs(x1, Interval.of(0, 4));
    // remove a left-most element with lower test
    RichFiniteDomain d4 = d.lessOrEqualTo(1, x1);
    logln("testing 1 <= x1:\n" + d4);
    d4.assertValueIs(x1, Interval.of(1, 5));
    // remove all elements besides the middle element with lower tests
    RichFiniteDomain d5 = d.lessOrEqualTo(3, x1).lessOrEqualTo(x1, 4);
    logln("testing 3 <= x1 <= 4:\n" + d5);
    d5.assertValueIs(x1, Interval.of(3, 4));
    // remove a middle element with lower tests and join
    RichFiniteDomain d6 = d.lessOrEqualTo(x1, 2).join(d.lessOrEqualTo(4, x1));
    logln("testing x1 <= 2 v 4 <= x1:\n" + d6);
    d6.assertValueIs(x1, Interval.of(0, 5));  // over-approximation
    // pick one element with equality
    RichFiniteDomain d7 = d.equalTo(x1, 3);
    logln("testing x1 == 3:\n" + d7);
    d7.assertValueIs(x1, 3);
    // bottom on wrong equality
    try {
      RichFiniteDomain d8 = d.equalTo(x1, 6);
      fail();
    } catch (Unreachable _) {
      logln("testing x1 == 6:\n" + "<bottom>");
      assertTrue(true);
    }
    // bottom on wrong disequality
    try {
      RichFiniteDomain d9 = d.assign(x1, 6);
      d9 = d9.notEqualTo(x1, 6); // our domains would infer bottom here
      if (config.usesApron())
        assertTrue(true); // Apron disequalities seem to be very weak, so will only infer bottom at the next test below
      else
        fail();
      d9 = d9.lessOrEqualTo(x1, 5);
      fail();
    } catch (Unreachable _) {
      logln("testing x1 := 6; x1 != 6:\n" + "<bottom>");
      assertTrue(true);
    }
    // overlapping intervals with lower test
    RichFiniteDomain d10 = d.lessOrEqualTo(x1, x2);
    logln("testing x1 <= x2:\n" + d10);
    // nothing again because of over-approximation
    d10.assertValueIs(x1, Interval.of(0, 5));
    d10.assertValueIs(x2, Interval.of(4, 25));
  }

  @Test public void copyAndPaste () throws Exception {
    VarSet copyVars = VarSet.of(x1, x2);

    RichFiniteDomain d1 = config.getInitialDomain();
    d1 = d1.introduce(x1, 5);
    d1 = d1.introduce(x2, 10);
    d1 = d1.introduce(x3, Interval.of(1, 2));
    logln("d1:\n" + d1);
    d1.assertValueIs(x1, 5);
    d1.assertValueIs(x2, 10);
    d1.assertValueIs(x3, Interval.of(1, 2));

    RichFiniteDomain dest = config.getInitialDomain();
    dest = dest.introduce(x3, Interval.of(1, 2));
    logln("dest:\n" + dest);

    RichFiniteDomain dest1 = dest.copyAndPaste(copyVars, d1);
    logln("c&p x1, x2:\n" + dest1);
    dest1.assertValueIs(x1, 5);
    dest1.assertValueIs(x2, 10);

    RichFiniteDomain d2 = config.getInitialDomain();
    d2 = d2.introduce(x1, Interval.of(0, 5));
    d2 = d2.introduce(x2, Interval.of(4, 25));
    d2 = d2.introduce(x3, Interval.of(1, 2));
    logln("d2:\n" + d2);
    d2.assertValueIs(x1, Interval.of(0, 5));
    d2.assertValueIs(x2, Interval.of(4, 25));
    d2.assertValueIs(x3, Interval.of(1, 2));

    logln("dest:\n" + dest);

    RichFiniteDomain dest2 = dest.copyAndPaste(copyVars, d2);
    logln("c&p x1, x2:\n" + dest2);
    dest2.assertValueIs(x1, Interval.of(0, 5));
    dest2.assertValueIs(x2, Interval.of(4, 25));

    RichFiniteDomain d3 = config.getInitialDomain();
    d3 = d3.introduce(x1, 5);
    d3 = d3.introduce(x2, 10);
    d3 = d3.introduce(x3, Interval.of(1, 2));

    RichFiniteDomain d4 = config.getInitialDomain();
    d4 = d4.introduce(x1, 10);
    d4 = d4.introduce(x2, 5);
    d4 = d4.introduce(x3, Interval.of(1, 2));

    RichFiniteDomain d5 = d3.join(d4); // x1 + x2 = 15 should be inferred
    logln("d5:\n" + d5);
    d5.assertEqualityRelationExists(x1, x2);

    RichFiniteDomain dest3 = dest.copyAndPaste(copyVars, d5);
    logln("c&p x1, x2:\n" + dest3);
    dest3.assertValueIs(x1, Interval.of(5, 10));
    dest3.assertValueIs(x2, Interval.of(5, 10));
    dest3.assertEqualityRelationExists(x1, x2);
  }

  @Test public void foldExpand () throws Exception {
    // a = 5, b = 10 fold a, b -> a = [5, 10]; simple numeric fold
    RichFiniteDomain d1 = config.getInitialDomain();
    d1 = d1.introduce(x1, 5);
    d1 = d1.introduce(x2, 10);
    d1 = d1.introduce(x3, 10);
    d1 = d1.introduce(x5, Interval.of(1, 2));
    logln("d1:\n" + d1);
    d1.assertValueIs(x1, 5);
    d1.assertValueIs(x2, 10);
    d1.assertValueIs(x5, Interval.of(1, 2));
    ListVarPair foldVars1 = ListVarPair.singleton(x1, x2);
    RichFiniteDomain d1Result = d1.fold(foldVars1);
    logln("fold x1 <- x2 :\n" + d1Result);
    d1Result.assertValueIs(x1, Interval.of(5, 10));
    // now do an expand
    d1Result = d1Result.expandNG(foldVars1);
    logln("expand x1 -> x2 :\n" + d1Result);
    d1Result.assertValueIs(x1, Interval.of(5, 10));
    d1Result.assertValueIs(x2, Interval.of(5, 10));
    logln();

    // maintain equality with other variable through fold
    // equalities with other variable, a = c; b = c; fold (a<-b) -> a = c
    RichFiniteDomain d2 = config.getInitialDomain();
    d2 = d2.introduce(x1, 10);
    d2 = d2.introduce(x2, 5);
    d2 = d2.introduce(x3, 5);
    d2 = d2.introduce(x5, Interval.of(1, 2));
    d2 = d2.join(d1);  // x1 + x2 = 15 and x1 + x3 = 15 should be inferred
    logln("d2:\n" + d2);
    d2.assertValueIs(x1, Interval.of(5, 10));
    d2.assertValueIs(x2, Interval.of(5, 10));
    d2.assertValueIs(x3, Interval.of(5, 10));
    d2.assertValueIs(x5, Interval.of(1, 2));
    // our Affine domain and Apron have no transitive closure over equalities, so need to test different equalities here
    d2.assertEqualityRelationExists(x1, x3);
    if (config.usesApron()) {
      d2.assertEqualityRelationExists(x1, x2); // TODO: try Apron + old Affine for transitive equalities
    } else {
      d2.assertEqualityRelationExists(x2, x3);
    }
    ListVarPair foldVars2 = ListVarPair.singleton(x2, x3);
    RichFiniteDomain d2Result = d2.fold(foldVars2);
    logln("fold x2 <- x3 :\n" + d2Result);
    d2Result.assertValueIs(x1, Interval.of(5, 10));
    d2Result.assertValueIs(x2, Interval.of(5, 10));
    d2Result.assertEqualityRelationExists(x1, x2);
    // now do an expand that shows we maintained the above relations
    d2Result = d2Result.expandNG(foldVars2);
    logln("expand x2 -> x3 :\n" + d2Result);
    d2Result.assertValueIs(x1, Interval.of(5, 10));
    d2Result.assertValueIs(x2, Interval.of(5, 10));
    d2Result.assertValueIs(x3, Interval.of(5, 10));
    // our Affine domain and Apron have no transitive closure over equalities, so need to test different equalities here
    d2Result.assertEqualityRelationExists(x1, x3);
    if (config.usesApron()) {
      d2Result.assertEqualityRelationExists(x1, x2);
    } else {
      d2Result.assertEqualityRelationExists(x2, x3);
    }
    logln();

    // maintain equality between groups throughout fold
    // equalities within fold groups (multifold), a = b; c = d; fold (a<-c, b<-d) -> a = b
    // here we choose all the values to be the same a = b = c = d as we can only query
    // equality relations but not inequality ones through our channels.
    // But Apron does maintain also inequality relations throughout the fold. Just choose different values for c, d
    RichFiniteDomain d3 = config.getInitialDomain();
    d3 = d3.introduce(x1, 5);
    d3 = d3.introduce(x2, 10);
    RichFiniteDomain d4 = config.getInitialDomain();
    d4 = d4.introduce(x1, 10);
    d4 = d4.introduce(x2, 5);
    RichFiniteDomain d5 = d3.join(d4); // infers equalities between x1, x2
    d5.assertEqualityRelationExists(x1, x2);
    d3 = d5;
    d4 = d5;
    d3 = d3.introduce(x3, 5);
    d3 = d3.introduce(x4, 10);
    d4 = d4.introduce(x3, 10);
    d4 = d4.introduce(x4, 5);
    d5 = d3.join(d4); // infers equalities between x3, x4
    logln("d5:\n" + d5);
    d5.assertEqualityRelationExists(x3, x4);
    ListVarPair foldVars3 = new ListVarPair();
    foldVars3.add(x1, x3);
    foldVars3.add(x2, x4);
    RichFiniteDomain d5Result = d5.fold(foldVars3);
    logln("fold x1 <- x3, x2 <- x4 :\n" + d5Result);
    d5Result.assertValueIs(x1, Interval.of(5, 10));
    d5Result.assertValueIs(x2, Interval.of(5, 10));
    d5Result.assertEqualityRelationExists(x1, x2); // relations maintained throughout fold!
    d5Result = d5Result.expandNG(foldVars3);
    logln("expand x1 -> x3, x2 -> x4 :\n" + d5Result);
    d5Result.assertValueIs(x1, Interval.of(5, 10));
    d5Result.assertValueIs(x2, Interval.of(5, 10));
    d5Result.assertEqualityRelationExists(x1, x2); // and relations restated after expand
    d5Result.assertEqualityRelationExists(x3, x4);
  }

  @Test public void arithmetics () throws Exception {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov a, 20",
        "mov b, 3",
        "add sum, a, b",
        "assert sum = 23",
        "sub subtraction, a, b",
        "assert subtraction = 17",
        "mul prod, a, b",
        "assert prod = 60",
        "div division, a, b",
        "assert division = 6",
        "mod modulo, a, b",
        "assert modulo = 2",
        "shl leftshift, a, b",
        "assert leftshift = 160",
        "shr rightshift, a, b",
        "assert rightshift = 2",
        "halt ");
    Analysis<?> analysis = config.getAnalyzer().runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Used to pass a custom parameter to the TestCases class.
   *
   * @author Bogdan Mihaila
   */
  // cannot be made private as it won't be recognized in annotation above anymore
  protected static class CustomizedJunitTestSuite extends Suite {
    private final List<Runner> runners = new ArrayList<Runner>();

    /**
     * Only called reflectively. Do not use programmatically.
     */
    public CustomizedJunitTestSuite (Class<?> klass) throws Throwable {
      super(klass, Collections.<Runner>emptyList());
      runners.add(new CustomJunitRunner(klass, new AnalyzerConfig(false), "Bindead"));
      runners.add(new CustomJunitRunner(klass, new AnalyzerConfig(true), "Apron"));
    }

    @Override protected List<Runner> getChildren () {
      return runners;
    }

    /**
     * Used to pass a custom parameter to the TestCases class.
     *
     * @author Bogdan Mihaila
     */
    private static class CustomJunitRunner extends BlockJUnit4ClassRunner {
      private final Object parameter;
      private final String name;

      public CustomJunitRunner (Class<?> klass, Object parameter, String name) throws InitializationError {
        super(klass);
        this.parameter = parameter;
        this.name = name;
      }

      @Override protected Object createTest () throws Exception {
        return getTestClass().getOnlyConstructor().newInstance(parameter);
      }

      @Override protected void validateConstructor (List<Throwable> errors) {
        // no need to be picky!
      }

      @Override protected String getName () {
        return name + "Tests";
      }

      @Override protected String testName (FrameworkMethod method) {
        return String.format("[%s]_%s", name, method.getName());
      }

    }
  }
}
