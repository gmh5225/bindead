package bindead.domains.undef;

import static bindead.TestsHelper.lines;

import java.io.IOException;

import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.debug.Benchmark;
import bindead.debug.DebugHelper;
import binparse.Binary;

public class PaperBenchmarks extends Benchmark {
  private final static AnalysisFactory analyzer = new AnalysisFactory().enableDomains("Undef");
  private final static AnalysisFactory heapAnalyzer = new AnalysisFactory().enableDomains("Undef").enableDomains("Heap");

  boolean benchmark = false;

  /**
   * This fails due to the heap domain summarizing memory regions and
   * triggering an assertion that compares the arguments of widening with the result.
   */
  @Test public void testLoop () throws Throwable {
    // int n = rnd(0, 100);
    // for (int i = 0; i < n; i++) {
    //   if (p == NULL)
    //     p = malloc (8);
    //   *p = i;
    //   *(p + 4) = i;
    //  }
    // if (p != NULL) {
    //   x = *p;
    //   y = *(p + 4);
    //   t = exit_label;
    //   t = t + x - y;          // without Undef domain x != y and the jump target is invalid
    //   goto t;
    // }
    // exit_label:
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov p, 0",
        "mov n, [0, 100]",      // int n = rnd(0, 100);
        "mov i, 0",
        "doloop:",
        "  cmpeq Z, i, n",
        "  brc Z, endloop:",
        "  cmpeq Z, p, 0",
        "  brc Z, alloc:",
        "    br end_alloc:",
        "  alloc:",
        "    mov size, 8",
        "    prim (p) = malloc(size)",
        "  end_alloc:",
        "  store p, i",
        "  add p1, p, 4",
        "  store p1, i",
        "  add i, i, 1",
        "  br doloop:",
        "endloop:",
        "cmpeq Z, 0, p",
        "brc Z, endif2:",
          "load x, p",
          "add p1, p, 4",
          "load y, p1",
          "mov t, endif2:",
          "add t, t, x",
          "sub t, t, y",
          "br t", // this crashes as unknown target without the Undef domain as x != y and thus t unbounded
        "endif2:",
        "halt"
      );
    AnalysisDebugHooks debugOutput = null;
    if (!benchmark) {
      DebugHelper.analysisKnobs.enableCommon();
      // DebugHelper.analysisKnobs.printCompactDomain();
      debugOutput = DebugHelper.printers.domainDump();
    }
    heapAnalyzer.runAnalysis(assembly, debugOutput);
//    analyzer.runAnalysis(assembly, debugOutput);
  }

  @Test public void testMalloc () throws Throwable {
    // if (?) {
    //   x = 4;
    //   p = malloc(x);
    //   *p = exit_label;
    // } else {
    //   p = NULL;
    // }
    // if (p != NULL) {
    //   x = *p;
    //   goto x;                // without Undef domain the jump target is TOP
    // }
    // exit_label:
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "cmpeq Z, 0, rnd",
        "brc Z, else:",
          "mov x, 4",
          "prim (p) = malloc(x)",
          "store p, endif2:",
          "br endif1:",
        "else:",
          "mov p, 0",
        "endif1:",
        "nop",
        "cmpeq Y, 0, p",
        "brc Y, endif2:",
          "load target, p",
          "br target",
          "nop",
        "endif2:",
        "halt"
      );
    AnalysisDebugHooks debugOutput = null;
    if (!benchmark) {
      DebugHelper.analysisKnobs.enableCommon();
      // DebugHelper.analysisKnobs.printCompactDomain();
      debugOutput = DebugHelper.printers.domainDump();
    }
    heapAnalyzer.runAnalysis(assembly, debugOutput);
  }

  /**
   * Test the merging of different function call paths using the undef domain.
   * Consists of 2 call-paths that end up in the same function.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns01 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 2 call-paths that end up in the same function.
   * This example uses a longer call-path.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns02 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple2-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 2 call-paths that end up in the same function.
   * Uses the passed in parameter values and return values.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns02_variation () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple3-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 3 call-paths that end up in the same function.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns03 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple4-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 2 call-paths that end up in the same function.
   * This example uses pointers to pass the variables on the stack of main to the called functions.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns04 () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple-pointers-O0-fpless");
    analyzer.runAnalysis(binary);
  }

  /**
   * Consists of 2 call-paths that end up in the same function.
   * This example uses pointers to pass the variables on the stack of main to the called functions.
   * This example is compiled to not use the framepointer in functions.
   */
  @Test public void functionReturns05() throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("undef-function-calls-simple2-pointers-O0-fpless");
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printNumVarOnly();
    DebugHelper.analysisKnobs.printCompactDomain();
    analyzer.runAnalysis(binary, DebugHelper.printers.domainDump());
  }

  @Override public void benchmark () {
    benchmark = true;
    try {
      functionReturns05();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static void main (String[] args) {
    new PaperBenchmarks().run();
  }
}
