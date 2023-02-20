package bindead.domains.widening;

import static bindead.TestsHelper.evaluateAssertions;
import static bindead.TestsHelper.lines;

import org.junit.Before;
import org.junit.Test;

import bindead.BenchmarkTool;
import bindead.BenchmarkTool.Benchmark;
import bindead.BenchmarkTool.BenchmarkResult;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.debug.DebugHelper;
import bindead.domains.affine.Affine;
import bindead.domains.congruences.Congruences;
import bindead.domains.intervals.Intervals;
import bindead.domains.phased.Phased;
import bindead.domains.predicates.finite.Predicates;
import bindead.domains.widening.delayed.DelayedWideningProperties;
import bindead.domains.widening.oldthresholds.ThresholdsWidening;
import bindead.domains.widening.thresholds.ThresholdsWideningProperties;

public class PaperBenchmarks {
  private static final AnalysisFactory normalAnalyzer = new AnalysisFactory();
  private static final AnalysisFactory phasedAnalyzer = new AnalysisFactory().enableDomains("Phased");
  private static final AnalysisFactory octagonsAnalyzer = new AnalysisFactory().disableDomains("Intervals")
      .enableDomains("Apron(Octagons)");
  private static final AnalysisFactory polyhedraAnalyzer = new AnalysisFactory().disableDomains("Intervals")
      .enableDomains("Apron(Polyhedra)");

  private static String[] interestingDomains =
  {Predicates.NAME, ThresholdsWidening.NAME, Phased.NAME, Affine.NAME, Congruences.NAME, Intervals.NAME};
  @SuppressWarnings("unused")
  private static AnalysisDebugHooks debugger = DebugHelper.printers.domainDumpFiltered(interestingDomains);


  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

//  @Before
  public void debug () {
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();
    AnalysisProperties.INSTANCE.debugWidening.setValue(true);
    ThresholdsWideningProperties.INSTANCE.debugWidening.setValue(true);
    DelayedWideningProperties.INSTANCE.debugWidening.setValue(true);
  }

  /**
   * Runs a time benchmark on all the tests in this file.
   *
   * @param args
   */
  public static void main (String[] args) {
    debugger = null;
    PaperBenchmarks tests = new PaperBenchmarks();
    tests.silence();
    for (BenchmarkResult result : BenchmarkTool.benchmarkMethods(tests)) {
      System.out.println(result);
    }
  }

  @Benchmark @Test public void simpleLoopFig1 () {
    //  int x = 0;
    //  int y = 0;
    //  while (x < 100) {       ** x = [0, 100]  y = [0, 100]
    //    x = x + 1;            ** x = [0, 99]   y = [0, 99]
    //    y = y + 1;
    //  }                       ** x = 100  y = 100
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov y, 0",
        "loop:",
        "cmples LES, 100, x",
        "assert x = [0, 100]",
        "assert y = [0, 100]",
        "brc LES, exit:",
        "assert x = [0, 99]",
        "assert y = [0, 99]",
        "add x, x, 1",
        "add y, y, 1",
        "br loop:",
        "exit:",
        "assert x = 100",
        "assert y = 100",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    evaluateAssertions(analysis);
  }

  @Benchmark @Test public void nestedLoopsRandom () {
    // int i = 0;
    // int j = 0;
    // while (i < 10) {         ** i,j = [0, 10]
    //  while (j < 10) {        ** j = [0, 10], i = 0           ! with congruences we can infer this
    //   if (?) {               ** i, j = [0, 9]
    //    j++;
    //    i++;
    //   }
    //  }                       ** j = 10
    // }                        ** i = 10
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop_i:",
        "assert i = [0, 10]",
        "cmples LES, 10, i",
        "brc LES, exit_loop_i:",
        "assert i = 0",
        "loop_j:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "assert i = [0, 9]",
        "brc RND, inc_end:",
        "add j, j, 1",
        "add i, i, 1",
        "inc_end:",
        "nop",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 10",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 10",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void nestedLoopsRandomModified () {
    // int i = 0;
    // int j = 0;
    // while (i < 10) {         ** i = [0, 10]
    //  j = 0;                  ** i = 0                ! with congruences we can infer this
    //  while (j < 10) {        ** j = [0, 10]
    //   if (?) {               ** i, j = [0, 9]
    //    j++;
    //    i++;
    //   }
    //  }                       ** j = 10
    // }                        ** i = 10
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "assert i = [0, 10]",
        "cmples LES, 10, i",
        "brc LES, exit_loop_i:",
        "assert i = 0",
        "mov j, 0",
        "loop_j:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "assert i = [0, 9]",
        "brc RND, inc_end:",
        "add j, j, 1",
        "add i, i, 1",
        "inc_end:",
        "nop",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 10",
        "assert i = 10",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 10",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void nestedLoopsMedium () {
    // int i = 0;
    // int j = 0;
    // while (i < 10) {         ** i = [0, 10]
    //  j = 0;                  ** i = 0                ! with congruences we can infer this
    //  while (j < 10) {        ** j = [0, 10]
    //    j++;                  ** i, j = [0, 9]
    //    i++;
    //  }                       ** j = 10
    // }                        ** i = 10
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "assert i = [0, 10]",
        "cmples LES, 10, i",
        "brc LES, exit_loop_i:",
        "assert i = 0",
        "mov j, 0",
        "loop_j:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "assert i = [0, 9]",
        "add j, j, 1",
        "add i, i, 1",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 10",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 10",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests widening inside a nested loop. The narrowing predicates are transformed in the inner loop such that applying
   * them again at the inner loop header would let the value for i grow steady, leading to veeery slow termination.
   */
  @Benchmark @Test public void nestedLoopsHard () {
    // int i = 0;
    // int j = 0;
    // while (i < 10) {         ** i = [0, 11]
    //  j = 0;                  ** i = 0                ! with congruences we can infer this
    //  while (j < 10) {        ** j = [0, 10]
    //    j++;                  ** i, j = [0, 9]
    //    i++;
    //  }                       ** j = 10
    //  i++;
    // }                        ** i = 11               ! with congruences we can infer this
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "assert i = [0, 11]",
        "cmples LES, 10, i",
        "brc LES, exit_loop_i:",
        "assert i = 0",
        "mov j, 0",
        "loop_j:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "assert i = [0, 9]",
        "add j, j, 1",
        "add i, i, 1",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 10",
        "add i, i, 1",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 11",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void nestedLoopsHardModified1 () {
    // int i = 0;
    // int j = 0;
    // while (i < 2) {         ** i = [0, 11]
    //  j = 0;                  ** i = 0                ! with congruences we can infer this
    //  while (j < 10) {        ** j = [0, 10]
    //    j++;                  ** i, j = [0, 9]
    //    i++;
    //  }                       ** j = 10
    //  i++;
    // }                        ** i = 11               ! with congruences we can infer this
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "assert i = [0, 11]",
        "cmples LES, 2, i",
        "brc LES, exit_loop_i:",
        "assert i = 0",
        "mov j, 0",
        "loop_j:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "assert i = [0, 9]",
        "add j, j, 1",
        "add i, i, 1",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 10",
        "add i, i, 1",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 11",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  // we cannot find the invariants for i here thus assertions commented out below
  @Benchmark @Test public void nestedLoopsHardModified2 () {
    // int i = 0;
    // int j = 0;
    // while (i < 15) {         ** i = [0, 22]
    //  j = 0;                  ** i = [0, 14]
    //  while (j < 10) {        ** j = [0, 10]
    //    j++;                  ** j = [0, 9]
    //    i++;                  ** i = [0, 20]
    //  }                       ** j = 10
    //  i++;                    ** i = [0, 21]
    // }                        ** i = 22
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "assert i = [-oo, +oo]",      // the broken values
        "cmples LES, 15, i",
        "brc LES, exit_loop_i:",
        //          "assert i = [0, 14]",
        "mov j, 0",
        "loop_j:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        //            "assert i = [0, 9]",
        "add j, j, 1",
        "add i, i, 1",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 10",
        "add i, i, 1",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = [15, 127]",         // the broken values
//        "assert i = 11",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void HalbwachsFig1a () {
    // int i = 0;
    // while (i < 100) {        ** i = [0, 100]
    //  i++;                    ** i = [0, 99]
    // }                        ** i = 100
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop:",
        "cmples LES, 100, i",
        "assert i = [0, 100]",
        "brc LES, exit_loop:",
        "assert i = [0, 99]",
        "add i, i, 1",
        "br loop:",
        "exit_loop:",
        "assert i = 100",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with two different counters.
   */
  @Benchmark @Test public void HalbwachsFig1b () {
    // int i = 0;
    // int j = 0;
    // while (i < 100) {        ** i = [0, 100]
    //  j = 0;                  ** i = [0, 99]
    //  while (j < 100) {       ** j = [0, 100]
    //    j++;                  ** j = [0, 99]
    //  }                       ** j = 100
    //  i++;
    // }                        ** i = 100
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "cmples LES, 100, i",
        "assert i = [0, 100]",
        "brc LES, exit_loop_i:",
        "assert i = [0, 99]",
        "mov j, 0",
        "loop_j:",
        "cmples LES, 100, j",
        "assert j = [0, 100]",
        "brc LES, exit_loop_j:",
        "assert j = [0, 99]",
        "add j, j, 1",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 100",
        "add i, i, 1",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 100",
        "halt");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with two different counters. Taken from Halbwachs Figure 1b with the initialization of j outside of
   * the inner loop to not have the constant domain delay widening.
   */
  @Benchmark @Test public void HalbwachsFig1bModified () {
    // int i = 0;
    // int j = 0;
    // while (i < 100) {        ** i = [0, 100]
    //  while (j < 100) {       ** j = [0, 100]
    //    j++;                  ** j = [0, 99]
    //  }                       ** j = 100
    //  i++;
    // }                        ** i = 100
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop_i:",
        "  cmples LES, 100, i",
        "  assert i = [0, 100]",
        "  brc LES, exit_loop_i:",
        "  assert i = [0, 99]",
        "  loop_j:",
        "    cmples LES, 100, j",
        "    assert j = [0, 100]",
        "    brc LES, exit_loop_j:",
        "    assert j = [0, 99]",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  assert j = 100",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "assert i = 100",
        "halt");
//    TODO: does not work with polyhedra. See why!
//    Analysis<?> analysis = polyhedraAnalyzer.runAnalysis(assembly);
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }


  /**
   * Needs the constant assignments widening domain to not widen to -oo on the else path.
   */
  @Benchmark @Test public void HalbwachsFig2a () {
    // int n = 0;
    // while (true) {           ** n = [0, 60]
    //  if (n < 60)
    //   n++;
    //  else                    ** n = 60
    //   n = 0;
    // }                        ** never reached!
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov n, 0",
        "loop:",
        "cmples LES, 60, n",
        "assert n = [0, 60]",
        "brc LES, reset:",
        "count:",
        "add n, n, 1",
        "br join:",
        "reset:",
        "assert n = 60",
        "mov n, 0",
        "join:",
        "br loop:",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void HalbwachsFig2b () {
    // int n = 0;
    // while (true) {
    //  if (?) {                ** n = [0, 60]
    //   if (n < 60)
    //    n++;
    //   else
    //    n = 0;
    //  }                       ** n = [0, 60]
    // }                        ** never reached!
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov n, 0",
        "loop:",
        "brc RND, join_rnd:",
        "cmples LES, 60, n",
        "assert n = [0, 60]",
        "brc LES, reset:",
        "count:",
        "add n, n, 1",
        "br join_if:",
        "reset:",
        "mov n, 0",
        "join_if:",
        "assert n = [0, 60]",
        "join_rnd:",
        "assert n = [0, 60]",
        "br loop:",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Needs constant assignments widening to not widen on the else path.
   */
  @Benchmark @Test public void HalbwachsFig2bModified () {
    // int n = 0;
    // while (true) {
    //  if (?) {                ** n = [0, 70]
    //   if (n < 60)
    //    n++;
    //   else
    //    n = 0;                ** n = [0, 60]
    //  } else {
    //   n = 70;
    //  }                       ** n = [0, 70]
    // }                        ** never reached!
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov n, 0",
        "loop:",
        "brc RND, assign:",
        "assert n = [0, 70]",
        "cmples LES, 60, n",
        "brc LES, reset:",
        "count:",
        "add n, n, 1",
        "br join_if:",
        "reset:",
        "mov n, 0",
        "join_if:",
        "assert n = [0, 60]",
        "br join_rnd:",
        "assign:",
        "mov n, 70",
        "join_rnd:",
        "assert n = [0, 70]",
        "br loop:",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  // we cannot find the invariants for i here thus assertions commented out below
  @Benchmark @Test public void HalbwachsFig4 () {
    // int i = 0;
    // int j = 0;
    // while (i < 4) {
    //  j = 0;
    //  while (j < 4) {
    //   i++;
    //   j++;
    //  }
    //  i = i - j + 1;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        //          "assert i = [0, 4]",
        "cmples LES, 4, i",
        "brc LES, exit_loop_i:",
        "mov j, 0",
        "loop_j:",
        "assert j = [0, 4]",
        "cmples LES, 4, j",
        "brc LES, exit_loop_j:",
        "add i, i, 1",
        "add j, j, 1",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 4",
        //          "assert i = [0, 7]",
        "add j, j, 1",
        "sub i, i, j",
        "br loop_i:",
        "exit_loop_i:",
        //        "assert i = 4",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Needs the domain state separation to infer two invariants for i < 50 and 50 <= i < 100
   * From "Gopan - Guided Static Analysis"
   */
  @Benchmark @Test public void GopanFig1a () {
    // int i = 0;
    // int j = 0;
    // while (true) {
    //   if (i <= 50)
    //     j++;
    //   else
    //     j--;
    //   if (j < 0)
    //     break;
    //   i++;
    // }
    //
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop:",
        "assert i = [0, 102]",
        "assert j = [0, 51]",
        "cmples LES, i, 50",
        "brc  LES, else:",
        "assert j = [0, 51]",
        "sub j, j, 1",
        "br  if_j_exit:",
        "else:",
        "assert j = [0, 50]",
        "add j, j, 1",
        "if_j_exit:",
        "cmplts LES, j, 0",
        "brc  LES, exit:",
        "add i, i, 1",
        "br  loop:",
        "exit:",
        "assert i = 102",
        "assert j = -1",
        "halt ");
    Analysis<?> analysis = phasedAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void GopanFig1aModified () {
    // int i = 0;
    // int j = 0;
    // while (i < 100) {
    //  if (i < 50) {
    //    j++;
    //  } else {
    //    j--;
    //  }
    //  i++;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop_i:",
        "assert i = [0, 100]",
        "assert j = [0, 50]",
        "cmples LES, 100, i",
        "brc LES, exit_loop_i:",
        "cmples LES, 50, i",
        "brc  LES, decrement_j:",
        "assert j = [0, 49]",
        "add j, j, 1",
        "br end_if:",
        "decrement_j:",
        "assert j = [1, 50]",
        "sub j, j, 1",
        "end_if:",
        "add i, i, 1",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 100",
        "assert j = 0",
        "halt ");
    Analysis<?> analysis = phasedAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Test the example from "Lakhdar-Chaouch - Widening with thresholds for programs with complex control graphs" Figure
   * 2.
   */
  @Benchmark @Test public void ChaouchFig2 () {
    // int i = 0;
    // int j = 10;
    // while (i <= j) {
    //   i = i + 2;             ** i = [0, 8]
    //   j = j - 1;             ** j = [6, 10]
    // }                        ** i = 8; j = 6
    //
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 10",
        "loop:",
        "assert i = [0, 8]",
        "assert j = [6, 10]",
        "cmplts LTS, j, i",
        "brc LTS, exit:",
        "add i, i, 2",
        "sub j, j, 1",
        "br loop:",
        "exit:",
        "assert i = 8",
        "assert j = 6",
        "halt");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void ChaouchFig3 () {
    // int i = 0;
    // int j = 0;
    // while (true) {
    //   if (?) {
    //     if (i < 10)
    //      i++;
    //   } else {
    //     if (j < 10)
    //      j++;
    //   }
    //   if (i > 9 && j > 9)
    //     break;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop:",
        "brc RND, else:",
        "assert i = [0, 10]",
        "cmples LES, 10, i",
        "brc LES, exit_loop_i:",
        "assert i = [0, 9]",
        "add i, i, 1",
        "exit_loop_i:",
        "br exit_if_else:",
        "else:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "add j, j, 1",
        "exit_loop_j:",
        "exit_if_else:",
        "cmples LES, i, 9",
        "brc LES, continue:",
        "cmples LES, j, 9",
        "brc LES, continue:",
        "br exit:",
        "continue:",
        "br loop:",
        "exit:",
        "assert j = 10",
        "assert i = 10",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void ChaouchFig3Modified () {
    // int i = 0;
    // int j = 0;
    // while (true) {
    //   if (?) {
    //     while (i < 10)
    //      i++;
    //   } else {
    //     while (j < 10)
    //      j++;
    //   }
    //   if (i > 9 && j > 9)
    //     break;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop:",
        "brc RND, else:",
        "loop_i:",
        "assert i = [0, 10]",
        "cmples LES, 10, i",
        "brc LES, exit_loop_i:",
        "assert i = [0, 9]",
        "add i, i, 1",
        "br loop_i:",
        "exit_loop_i:",
        "br exit_if_else:",
        "else:",
        "loop_j:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "add j, j, 1",
        "br loop_j:",
        "exit_loop_j:",
        "exit_if_else:",
        "cmples LES, i, 9",
        "brc LES, continue:",
        "cmples LES, j, 9",
        "brc LES, continue:",
        "br exit:",
        "continue:",
        "br loop:",
        "exit:",
        "assert j = 10",
        "assert i = 10",
        "halt ");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Benchmark @Test public void ChaouchFig4 () {
    // int i = 0;
    // while (true) {
    //   if (?) {
    //     i++;
    //     if (i >= 100)
    //       i = 0;
    //   }
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop:",
        "assert i = [0, 99]",
        "brc RND, rnd_end:",
        "add i, i, 1",
        "cmples LES, i, 99",
        "brc LES, if_end:",
        "mov i, 0",
        "if_end:",
        "rnd_end:",
        "assert i = [0, 99]",
        "br loop:",
        "exit:",
        "halt");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with two different counters. Nearly same as Halbwachs Fig. 1b.
   */
  @Benchmark @Test public void ChaouchFig5 () {
    // int i = 0;
    // int j = 0;
    // while (i < 10) {         ** i = [0, 10]
    //  j = 0;                  ** i = [0, 9]
    //  while (j < 10) {        ** j = [0, 10]
    //    j++;                  ** j = [0, 9]
    //  }                       ** j = 10
    //  i++;
    // }                        ** i = 10
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "cmples LES, 10, i",
        "assert i = [0, 10]",
        "brc LES, exit_loop_i:",
        "assert i = [0, 9]",
        "mov j, 0",
        "loop_j:",
        "cmples LES, 10, j",
        "assert j = [0, 10]",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "add j, j, 1",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 10",
        "add i, i, 1",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 10",
        "halt");
    Analysis<?> analysis = normalAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Needs the domain state separation to infer two invariants for i < 50 and 50 <= i < 100
   * Same as "Gopan - Guided Static Analysis"
   */
  @Benchmark @Test public void ChaouchFig6 () {
    // int i = 0;
    // int j = 0;
    // while (true) {
    //   if (i <= 50)
    //     j++;
    //   else
    //     j--;
    //   if (j < 0)
    //     break;
    //   i++;
    // }
    //
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop:",
        "cmples LES, i, 50",
        "brc  LES, else:",
        "sub j, j, 1",
        "br  if_j_exit:",
        "else:",
        "add j, j, 1",
        "if_j_exit:",
        "cmplts LES, j, 0",
        "brc  LES, exit:",
        "add i, i, 1",
        "br  loop:",
        "exit:",
        "assert i = 102",
        "assert j = -1",
        "halt ");
    Analysis<?> analysis = phasedAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

}
