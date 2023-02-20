package bindead.binaries;

import static bindead.TestsHelper.assertHasWarnings;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bindead.BenchmarkTool;
import bindead.BenchmarkTool.BenchmarkResult;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.debug.DebugHelper;
import bindead.domains.affine.Affine;
import bindead.domains.apron.Apron;
import bindead.domains.intervals.IntervalSets;
import bindead.domains.intervals.Intervals;
import bindead.domains.predicates.finite.Predicates;
import bindead.domains.widening.oldthresholds.ThresholdsWidening;
import bindead.exceptions.AnalysisException;
import binparse.Binary;

public class FlagsBenchmarks {
  // NOTE: using the Undef domain unfortunately decreases precision as it introduces more
  // variables in Affine and the reduction is less precise
  private final static AnalysisFactory analyzer = new AnalysisFactory().disableDomains("Heap", "Undef");

  private static String[] interestingDomains = {Predicates.NAME, ThresholdsWidening.NAME,
    bindead.domains.widening.thresholds.ThresholdsWidening.NAME,
    Affine.NAME, Intervals.NAME, IntervalSets.NAME, Apron.NAME};
//  private static AnalysisDebugHooks debugger = null;
//  private static AnalysisDebugHooks debugger = DebugHelper.printers.domainDumpFiltered(interestingDomains);
  private static AnalysisDebugHooks debugger =
    DebugHelper.combine(DebugHelper.printers.instructionEffect(),
        DebugHelper.printers.domainDumpBoth());

  @BeforeClass public static void init () {
    DebugHelper.analysisKnobs.useGDSLasFrontendAndOptimize();
//    analyzer.disableDomains("Predicates(F)");
  }

  @AfterClass public static void teardown () {
    DebugHelper.analysisKnobs.useLegacyFrontend();
  }

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before
  public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  @Before
  public void enableDebugger () {
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();
  }

  @Test public void crackaddrGoodOversimplifiedNoData () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-good-oversimplified-nodata");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  /**
   * Results in an exception as the return address is overwritten and we cannot return to main safely.
   * Warnings are similar to above but writes to local buffer go over the buffer bounds and write to the whole stack.
   */
  @Test(expected = AnalysisException.class) public void crackaddrBadOversimplifiedNoData () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-bad-oversimplified-nodata");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis); // currently not reached because of the exception
  }

  /**
   * Warnings are:
   * - read from global data segment with non-constant offset. That is the reading of the string literal.
   * - write to local buffer inside loop with non-constant offset but still bounded to buffer, thus benign
   * - 2 writes to local buffer with non-constant offset after the loop. This should be more precise. TODO: Investigate!
   */
  @Test public void crackaddrGoodOversimplified () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-good-oversimplified");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  /**
   * Results in an exception as the return address is overwritten and we cannot return to main safely.
   * Warnings are similar to above but writes to local buffer go over the buffer bounds and write to the whole stack.
   */
  @Test(expected = AnalysisException.class) public void crackaddrBadOversimplified () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-bad-oversimplified");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis); // currently not reached because of the exception
  }

  @Test public void crackaddrGoodOversimplifiedLonger () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-good-oversimplified-longer-input");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBadOversimplifiedLonger () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-bad-oversimplified-longer-input");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis);  // currently not reached because of the exception
  }

  @Test public void crackaddrGoodNoLength () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-good-nolength");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBadNoLength () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-bad-nolength");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis); // currently not reached because of the exception
  }

  @Test public void crackaddrGood () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-good");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBad () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-bad");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis); // currently not reached because of the exception
  }

  /**
   * Does not work because of a not happening threshold substitution because of the affine domain
   * not storing variables equalities but constants for constant variables.
   */
  @Test public void crackaddrGoodPointersWithData () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-good-pointers-with-data");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBadPointersWithData () throws IOException {
//    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/undef-function-calls-simple4");
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-bad-pointers-with-data");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(16, analysis); // currently not reached because of the exception
  }

  @Test public void crackaddrGoodPointers () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-good-pointers");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBadPointers () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/crackaddr-bad-pointers");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(16, analysis); // currently not reached because of the exception
  }

  @Test public void array () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/array-sum-int");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test public void funcalls1 () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/undef-function-calls");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  public static void main (String[] args) {
    debugger = null;
    FlagsBenchmarks tests = new FlagsBenchmarks();
    tests.silence();
    for (BenchmarkResult result : BenchmarkTool.benchmarkMethods(tests)) {
      System.out.println(result);
    }
  }

  @Test public void funcalls2 () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/tailCall-simple");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test public void barber () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("/x32examples/barber");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

}
