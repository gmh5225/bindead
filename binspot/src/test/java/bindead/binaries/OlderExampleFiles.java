package bindead.binaries;

import static bindead.TestsHelper.assertHasWarnings;
import static bindead.TestsHelper.assertNoWarnings;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import bindead.BenchmarkTool;
import bindead.BenchmarkTool.Benchmark;
import bindead.BenchmarkTool.BenchmarkResult;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.debug.DebugHelper;
import bindead.exceptions.AnalysisException;
import binparse.Binary;

/**
 * Some old files that were used as tests and benchmarks
 * at the beginning of the framework.
 *
 * @author Bogdan Mihaila
 */
public class OlderExampleFiles {
  private final static AnalysisFactory analyzer = new AnalysisFactory();
  private final static String oldExamplesDirectoryPath = "/bindead/old/";

  private static final AnalysisDebugHooks debugger =
    DebugHelper.combine(DebugHelper.printers.instructionEffect(), DebugHelper.printers.domainDumpBoth());

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
//    DebugHelper.analysisKnobs.disableAll();
//    AnalysisProperties.INSTANCE.useGDSLDisassembler.setValue(true);
//    AnalysisProperties.INSTANCE.disassembleBlockWise.setValue(true);
  }

//  @Before
  public void enableDebugger () {
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();
  }

  /**
   * Runs a time benchmark on all the tests in this file.
   *
   * @param args
   */
  public static void mainD (String[] args) {
    OlderExampleFiles tests = new OlderExampleFiles();
    DebugHelper.analysisKnobs.disableAll();
    for (BenchmarkResult result : BenchmarkTool.benchmarkMethods(tests)) {
      System.out.println(String.format("%s: %,d ms, %,d mb; took %,d ms to benchmark doing %,d repetitions.",
          result.getName(), result.getAverageRuntime(), result.getMemoryUsage(), result.getTotalRuntime(),
          result.getRepetitions()));
    }
  }

  @Benchmark @Test(expected = AnalysisException.class) public void barber () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("barber-O0");
    Analysis<?> analysis = analyzer.runAnalysis(binary);
    assertHasWarnings(55, analysis); // currently not reached because of exception
  }

  @Benchmark @Test public void array_int_x32 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("array-sum-int-x32");
    Analysis<?> analysis = analyzer.runAnalysis(binary);
    assertHasWarnings(1, analysis);
  }

  @Benchmark @Test public void globalVariablesRead () throws IOException {
    for (int i = 1; i <= 3; i++) {
      String fileName = String.format("global-vars-read-%03d", i);
      Binary binary = TestsHelper.get32bitExamplesBinary(fileName);
      Analysis<?> analysis = analyzer.runAnalysis(binary);
      assertNoWarnings(analysis);
    }
  }

  @Benchmark @Test public void globalVariablesWrite () throws IOException {
    for (int i = 1; i <= 4; i++) {
      String fileName = String.format("global-vars-write-%03d", i);
      Binary binary = TestsHelper.get32bitExamplesBinary(fileName);
      Analysis<?> analysis = analyzer.runAnalysis(binary);
      switch (i) {
      case 1:
        assertNoWarnings(analysis);
        break;
      case 2:
      case 3:
      case 4:
        assertHasWarnings(1, analysis);
        break;
      default:
        throw new IllegalStateException();
      }
    }
  }

  @Benchmark @Test public void binsearch () throws IOException {
    Binary binary = TestsHelper.getExamplesBinary(oldExamplesDirectoryPath + "binsearch.O2");
    Analysis<?> analysis = analyzer.runAnalysis(binary);
    assertHasWarnings(3, analysis);
  }

  @Benchmark @Test public void binsearchSymbolicBound () throws IOException {
    Binary binary = TestsHelper.getExamplesBinary(oldExamplesDirectoryPath + "binsearch-symbolic-bound.x32.O2");
    Analysis<?> analysis = analyzer.runAnalysis(binary);
    assertHasWarnings(2, analysis);
  }

  @Benchmark @Test public void triangularO1 () throws IOException {
    Binary binary = TestsHelper.getExamplesBinary(oldExamplesDirectoryPath + "triangular.x32.O1");
    enableDebugger();
    Analysis<?> analysis = analyzer.runAnalysis(binary);
    assertHasWarnings(1, analysis);
  }

  @Benchmark @Test public void triangularO2 () throws IOException {
    Binary binary = TestsHelper.getExamplesBinary(oldExamplesDirectoryPath + "triangular.x32.O2");
    Analysis<?> analysis = analyzer.runAnalysis(binary);
    assertHasWarnings(1, analysis);
  }
}
