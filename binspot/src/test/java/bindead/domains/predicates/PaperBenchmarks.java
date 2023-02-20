package bindead.domains.predicates;

import static bindead.TestsHelper.lines;

import org.junit.Test;

import bindead.BenchmarkTool;
import bindead.BenchmarkTool.Benchmark;
import bindead.BenchmarkTool.BenchmarkResult;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;

/**
 * A collection of examples that are less precise without the Predicates(Z) domain.
 *
 * @author Bogdan Mihaila
 */
public class PaperBenchmarks {
  private final static AnalysisFactory normalAnalyzer = new AnalysisFactory();

  public void evaluateAssertionsDebug (String assembly) {
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printMemVarOnly();
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly, DebugHelper.printers.domainDump());
    TestsHelper.evaluateAssertions(analysis);
  }

  public void evaluateAssertions (String assembly) {
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Runs a time benchmark on all the tests in this file.
   * @param args
   */
  public static void mainDisabled (String[] args) {
    PaperBenchmarks tests = new PaperBenchmarks();
    DebugHelper.analysisKnobs.disableAll();
    for (BenchmarkResult result : BenchmarkTool.benchmarkMethods(tests)) {
      System.out.println(result);
    }
  }

  /**
   * Tests how the predicates domain helps to maintain the invariant that x != 0
   * if that has been tested by a branching before.
   */
  @Benchmark @Test public void divisionByZero () {
    // if (x != 0) {
    //  assert x != 0;
    //  y = 10 / x;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [-1, 1]",
        "cmpeq ZF, x, 0",
        "brc ZF, zero:",
        "  non_zero:",
        "  cmpeq f, x, 0",
        "  brc f, assertion_fail:",
        "    div y, 10, x",
        "    br join:",
        "zero:",
        "  nop",
        "join:",
        "nop",
        "exit:",
        "halt",
        "assertion_fail:",
        "assert UNREACHABLE",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Example from Heizmann - Software Model Checking for People Who Love Automata
   * Works only if setting the flag for the if foo p = 0 to false outside the loop as
   * that allows us to keep the predicate in the join on the loop-back edge.
   */
  @Benchmark @Test public void traceSeparation () {
    // assume p != 0;
    // n = 5;
    // while (n >= 0) { // or while (n != 0)
    //  assert p != 0;
    //  if (n == 0)
    //   p = 0;
    //  n--;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov n, 5",
        "mov p, [1, 100]", // cannot set p to [1, +oo] as wrapping would not evaluate the assert p != 0 to true
        // set the fn flag to 0
        "cmpeq fn, 1, 0",
        "brc fn, exit:",
        "loop:",
        "assert n = [-1, 5]",
        "cmplts ZF, n, 0",
        "brc ZF, loop_exit:",
        "assert n = [0, 5]",
        "cmpeq fa, p, 0",
        "brc fa, assertion_fail:",
        "assert p = [1, 100]",
        "cmpeq fn, n, 0",
        "brc fn, then:",
        "br if_end:",
        "then:",
        "mov p, 0",
        "if_end:",
        "sub n, n, 1",
        "br loop:",
        "loop_exit:",
        "nop",
        "exit:",
        "halt",
        "assertion_fail:",
        "assert UNREACHABLE",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Example from: Fisher - Joining Dataflow with Predicates
   */
  @Benchmark @Test public void pathSensitiveState () {
    //    x = 1;
    //    f = [0, 1]
    //    assert(x == 1);
    //    x = 0;
    //    if (f)
    //      x = 1;
    //    assert(x == [0, 1]);
    //    if (f)
    //      assert(x == 1);
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov f, [0, 1]",
        "assert x = 0",
        "cmpeq EQ, f, 0",
        "brc EQ, zero:",
        "  non_zero1:",
        "  mov x, 1",
        "  zero:",
        "if_else_end1:",
        "assert x = [0, 1]",
        "cmpeq EQ, f, 0",
        "brc EQ, exit:",
        "  non_zero2:",
        "  assert x = 1",
        "exit:",
        "assert x = [0, 1]",
        "halt ");
    evaluateAssertions(assembly);
  }
  /**
   * With substitution predicates bug that introduced unsound implications
   * this did not terminate.
   */
  @Benchmark @Test public void shouldNotWidenImmediately () {
    //  int x = 0;
    //  int y = 0;
    //  while (x < 100) {       ** x = [0, 100]  y = [0, 1]
    //    x++;                  ** x = [0, 99]   y = [0, 1]
    //    y = 0;
    //    y++;
    //  }                       ** x = 100  y = 1               ! this can only be inferred with the Predicates(Z)
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov y, 0",
        "loop:",
        "cmples LES, 100, x",
        "assert x = [0, 100]",
        "assert y = [0, 1]",
        "brc LES, exit:",
        "assert x = [0, 99]",
        "assert y = [0, 1]",
        "add x, x, 1",
        "mov y, 0",
        "add y, y, 1",
        "br loop:",
        "exit:",
        "assert x = 100",
        "assert y = 1",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Tests a case where there should be no widening on y as there are only constant assignments.
   */
  @Benchmark @Test public void shouldNotWidenEasy1 () {
    //  int x = 0;
    //  int y = 0;
    //  while (x < 100) {       ** x = [0, 100]  y = [0, 1]
    //    y = 1;                ** x = [0, 96]  y = [0, 1]
    //    x = x + 4;
    //  }                       ** x = 100  y = 1               ! this can only be inferred with the Predicates(Z)
    String assembly = lines(
        "mov.b x, 0",
        "mov.b y, 0",
        "loop:",
          "cmples.b LES, 100, x",
          "assert.b x = [0, 100]",
          "assert.b y = [0, 1]",
          "brc.b LES, exit:",
          "assert.b x = [0, 96]",
          "assert.b y = [0, 1]",
          "mov.b y, 1",
          "add.b x, x, 4",
          "br.d loop:",
        "exit:",
        "assert.b x = 100",
        "assert.b y = 1",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Tests a case where there should be no widening on y as there are only constant assignments.
   */
  @Benchmark @Test public void shouldNotWidenMedium2 () {
    //  int x = 0;
    //  int y = 0;
    //  while (x < 10) {        ** x = [0, 12]  y = [0, 2]
    //   if (x == 0)
    //     y = 2;
    //    x = x + 4;
    //  }                       ** x = 12  y = 2                ! this can only be inferred with the Predicates(Z)
    String assembly = lines(
        "mov.b x, 0",
        "mov.b y, 0",
        "loop:",
          "cmples.b LES, 10, x",
          "assert.b x = [0, 12]",
          "assert.b y = [0, 2]",
          "brc.b LES, exit:",
          "assert.b x = [0, 8]",
          "assert.b y = [0, 2]",
          "cmpeq.b EQ, x, 0",
          "xor.1 NEQ, EQ, 1",
          "brc.b NEQ, first_end:",
            "mov.b y, 2",
          "first_end:",
          "add.b x, x, 4",
          "br.d loop:",
        "exit:",
        "assert.b x = 12",
        "assert.b y = 2",
        "halt ");
    evaluateAssertions(assembly);
  }


  /**
   * Tests a bug in PredicateStateBuilder.remove(lhs) where a predicate with a projected out variable would
   * be inlined again due to inlining equalities for flags which in term removed the flags first.
   */
  @Benchmark @Test public void flagEqualitiesInline () {
    //  int x = 0;
    //  int y = 0;
    //  while (x < 10) {        ** x = [0, 8]  y = [0, 1]
    //   if (x == 0)
    //     y = 1;
    //    x = x + 4;
    //  }                       ** x = 12  y = 1                ! this can only be inferred with the Predicates(Z)
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 0",
        "loop:",
        "cmplts LES, 10, x",
        "brc LES, exit:",
        "assert x = [0, 8]",
        "assert y = [0, 1]",
        "cmpeq EQ, x, 0",
        "xor.1 NEQ, EQ, 1",
        "brc NEQ, if_end:",
        "mov y, 1",
        "if_end:",
        "add x, x, 4",
        "br loop:",
        "exit:",
        "assert x = 12",
        "assert y = 1",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Separates the first loop iteration from the rest, where widening is applied.
   * Thus can improve the value of a variable inside the loop when exiting the loop.
   */
  @Benchmark @Test public void stripes011 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, [5, 6]",
        "mov z, 0",
        "loop:",
        "  assert x = [0, 9]",
        "  add z, z, y",
        "  add x, x, 1",
        "  cmplts LT, x, 10",
        "  brc LT, loop:",
        "exit:",
        "assert x = 10",
        "assert y = [5, 6]",
        "assert z = [10, +oo]",         // ! this can only be inferred with the Predicates(Z)
        "halt");
    evaluateAssertions(assembly);
  }

}
