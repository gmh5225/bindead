package bindead.domains;

import static bindead.TestsHelper.lines;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;
import binparse.Binary;

/**
 * Test the new SegMem domain.
 */
public class SegmentedMemory {
  private final static AnalysisFactory analyzer = new AnalysisFactory().enableDomains("Undef");

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  @Test public void stackStoreLoadSimple () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "sub sp, sp, 40",
        "store sp, 42",
        "load r1, sp",
        "assert r1 = 42",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Test if one can read from the stack frame below after a call.
   */
  @Test public void stackStoreLoadWithCall () {
    AnalysisFactory newAnalyzer = new AnalysisFactory(
      "SegMem Processor Stack Data Fields SupportSet Predicates(F) " +
        "PointsTo Wrapping ThresholdsWidening Affine Congruences Intervals");
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "sub sp, sp, 40",
        "store sp, retPoint:",
        "call someFun:",
        "nop",
        "retPoint:",
        "someFun:",
        "load r1, sp",
        "assert r1 = retPoint:",
        "halt");
    Analysis<?> analysis = newAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void directCall () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "sub sp, sp, 40",
        "br f1:",
        "nop",
        "f1:",
        "store sp, 42",
        "sub sp, sp, 4",
        "store sp, ret_g:",
        "call g:",
        "ret_g:",
        "load r1, sp",
        "add sp, sp, 40",
        "assert r1 = 10",
        "prim halt",
        "g:",
        "sub sp, sp, 12",
        "br g1:",
        "nop",
        "g1:",
        "add si, sp, 16",
        "store si, 10",
        "add sp, sp, 12",
        "load addr, sp",
        "add sp, sp, 4",
        "br addr");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void indirectBranch () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov r1, 10",
        "sub r1, r1, 1",
        "cmpltu NZF, 0, r1",
        "mov r2, target:",
        "brc NZF, r2",
        "add r3, r1, 1",
        "target:",
        "add r4, r1, 1",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Ignore// this cannot work yet as it uses a variable sized array on the stack with different sizes from 2 callsites
  @Test public void tailCall () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("tailCall-simple-O0");
    analyzer.runAnalysis(binary);
  }

  @Ignore// this cannot work yet as joining frames of recursive functions is not yet implemented
  @Test public void recursion () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("recursion-O0");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 3 call-paths that end up in the same function and use a parameter passed through a pointer.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionsWithPointerParameter () throws IOException {
      Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple2-pointers-O0-fpless");
      analyzer.runAnalysis(binary);
    }

  /**
   * Test the merging of different function call paths using the undef domain.
   * Consists of 2 call-paths that end up in the same function.
   */
  @Test public void functionReturns01 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple-O0");
    analyzer.runAnalysis(binary);
  }

  /**
   * Test the merging of different function call paths using the undef domain.
   * Consists of 2 call-paths that end up in the same function.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns02 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 2 call-paths that end up in the same function.
   * This example uses a longer call-path.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns03 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple2-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 2 call-paths that end up in the same function.
   * Uses the passed in parameter values and return values.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns04 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple3-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 3 call-paths that end up in the same function.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns05 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple4-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * NOTE: this does not work because of an affine equality bug (a state is bottom because of an equality but it should not be)
   * if we iterate without the loops-first heuristic.
   *
   * Test the merging of different function call paths using the undef domain.
   * Uses call paths of different lengths and function performing operations on arrays.
   */
  @Test public void functions01 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-O0");
    analyzer.runAnalysis(binary);
  }

  /**
   * NOTE: this does not work because of an affine equality bug (a state is bottom because of an equality but it should not be)
   * if we iterate without the loops-first heuristic.
   *
   * Test the merging of different function call paths using the undef domain.
   * Uses call paths of different lengths and function performing operations on arrays.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functions02 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-O0-fpless");
    analyzer.runAnalysis(binary);
  }

}
