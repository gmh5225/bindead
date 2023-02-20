package bindead.domains.widening;

import static bindead.TestsHelper.lines;

import org.junit.Before;
import org.junit.Test;

import bindead.BenchmarkTool.Benchmark;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;

/**
 * Some examples taken from "N. Halbwachs - When the decreasing sequence fails" and
 * "Lakhdar-Chaouch - Widening with thresholds for programs with complex control graphs" and other papers.
 * The remaining examples are tests for discovered bugs and interesting case studies.
 */
public class CollectedExamples {
  private final static AnalysisFactory analyzer = new AnalysisFactory();
  private static final AnalysisFactory phasedAnalyzer = new AnalysisFactory().enableDomains("Phased");
  private final static AnalysisFactory undefAnalyzer = new AnalysisFactory().enableDomains("Undef");
  private final static AnalysisFactory octagonsAnalyzer =
    new AnalysisFactory().disableDomains("Intervals").enableDomains("Apron(Octagons)");
  private final static AnalysisFactory polyhedraAnalyzer =
    new AnalysisFactory().disableDomains("Intervals").enableDomains("Apron(Polyhedra)");

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
  }

  public Analysis<?> evaluateAssertions (String assembly) {
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
    return analysis;
  }

  @Test public void nestedLoopsEqual () {
    // int i = 0;
    // int j = 0;
    // while (i < 10) {         ** i,j = [0, 10]
    //  while (j < 10) {        ** j = [0, 10], i = 0
    //    j++;
    //    i++;
    //  }                       ** j = 10
    // }                        ** i = 10
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop_i:",
        "assert i = [0, 10]",
        "assert j = [0, 10]",
        "cmples LES, 10, i",
        "brc LES, exit_loop_i:",
        "assert i = 0",
        "loop_j:",
        "assert j = [0, 10]",
        "cmples LES, 10, j",
        "brc LES, exit_loop_j:",
        "assert j = [0, 9]",
        "assert i = [0, 9]",
        "add j, j, 1",
        "add i, i, 1",
        "nop",
        "br loop_j:",
        "exit_loop_j:",
        "assert j = 10",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 10",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void nestedLoopsBigger () {
    // int i = 0;
    // int j = 0;
    // while (i < 10) {         ** i,j = [0, 5]
    //  while (j < 5) {         ** i,j = [0, 5]
    //    j++;                  ** i,j = [0, 4]
    //    i++;
    //  }                       ** i,j = 5
    // }                        ** unreachable!
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop_i:",
        "cmples LES, 10, i",
        "assert i = [0, 5]",
        "brc LES, exit_loop_i:",
        "loop_j:",
        "cmples LES, 5, j",
        "assert j = [0, 5]",
        "brc LES, exit_loop_j:",
        "assert j = [0, 4]",
        "assert i = [0, 4]",
        "add j, j, 1",
        "add i, i, 1",
        "nop",
        "br loop_j:",
        "exit_loop_j:",
        "assert i = 5",
        "assert j = 5",
        "br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Some weird combinations of nested loops.
   * NOTE: it does not work anymore with the newer thresholds widening as we infer more thresholds
   * and do thus converge slower to the fixpoint which leads to missing the stable state for i as at some point
   * we do not allow thresholds application anymore. If we allow some more time the application of thresholds
   * then this example can be analyzed.
   */
  @Test public void nestedLoopsSmaller () {
    // int i = 0;
    // int j = 0;
    // while (i < 5) {          ** i,j = [0, 10]
    //  while (j < 10) {        ** i,j = [0, 9]
    //    j++;
    //    i++;
    //  }
    // }                        ** i,j = 10
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 0",
        "loop_i:",
        "cmples LES, 5, i",
        "assert i = [0, 10]",         // here the threshold from j is used to narrow as i < 5 is not redundant anymore
        "brc LES, exit_loop_i:",
        "assert i = 0",
        "loop_j:",
        "cmples LES, 10, j",
        "assert j = [0, 10]",
        "brc LES, exit_loop_j:",
        "add j, j, 1",
        "add i, i, 1",
        "nop",
        "br loop_j:",
        "exit_loop_j:",
        "assert i = 10",
        "assert j = 10",
        "br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests a case where there should be no widening on y as there are only constant assignments.
   * Analyzing the loop again with x = y and y != 1 inside the loop will narrow y and x and the loop will be stable.
   */
  @Test public void shouldNotWidenEasy () {
    // int x = 0;
    // int y = 0;
    // while (y != 1) {
    //  x++;
    //  y = 1;
    // }                        ** x = 0; y = 1
    String assembly = lines(
        "mov.d x, 0",
        "mov.d y, 0",
        "loop:",
        "cmpeq.d ZF, y, 1",
        "brc.d ZF, exit:",
        "assert.d x = 0",
        "assert.d y = 0",
        "add.d x, x, 1",
        "mov.d y, 1",
        "br.d loop:",
        "exit:",
        "assert.d x = 1",
        "assert.d y = 1",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests a case where there should be no widening on y as there are only constant assignments.
   */
  @Test public void shouldNotWidenMedium () {
    // int x = 0;
    // int y = 0;
    // while (y != 1) {         ** x = 0, y = 0
    //  x = x + 4;
    //  y = 1;
    // }                        ** x = 4, y = 1
    String assembly = lines(
        "mov.d x, 0",
        "mov.d y, 0",
        "loop:",
        "cmpeq.d ZF, y, 1",
        "brc.d ZF, exit:",
        "assert.d x = 0",
        "assert.d y = 0",
        "add.d x, x, 4",
        "mov.d y, 1",
        "br.d loop:",
        "exit:",
        "assert.d x = 4",
        "assert.d y = 1",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests a case where there should be no widening on y as there are only constant assignments.
   */
  @Test public void shouldNotWidenHard () {
    // int x = 3;
    // int y = 2;
    // while (y != 1) {         ** x = 3, y = 2
    //  x = x + 4;
    //  y = 1;
    // }                        ** x = 7, y = 1
    String assembly = lines(
        "mov.d x, 3",
        "mov.d y, 2",
        "loop:",
        "cmpeq.d ZF, y, 1",
        "brc.d ZF, exit:",
        "assert.d x = 3",
        "assert.d y = 2",
        "add.d x, x, 4",
        "mov.d y, 1",
        "br.d loop:",
        "exit:",
        "assert.d x = 7",
        "assert.d y = 1",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests a case where there should be no widening on y as there are constant assignments.
   */
  @Test public void shouldNotWidenEasy2 () {
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
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests a case where there should be no widening on y as there are only constant assignments.
   * Does not work correctly if the if-else join is a widening point.
   * It is because of left-widening losing the bounds because of iteration order.
   */
  @Test public void shouldNotWidenMedium2 () {
    // NOTE: using "x == 0" as the condition the affine relation between x and y
    //          makes the example work even without delayed widening domain

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
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests a case where there should be no widening on y as there are only constant assignments.
   * This would not work with widening-on-edges either.
   */
  @Test public void shouldNotWidenMedium3 () {
    // NOTE: if one would use "x == 0" as the condition then the affine relation between x and y would
    //          make the example work even without delayed widening domain

    //  int x = 0;
    //  int y = 0;
    //  while (x < 10) {        ** x = [0, 10]  y = [0, 1]
    //   if (x == 1)
    //     y = 1;
    //   x++;
    //  }                       ** x = 10  y = [0, 1]
    String assembly = lines(
        "mov.b x, 0",
        "mov.b y, 0",
        "loop:",
        "cmples.b LES, 10, x",
        "assert.b x = [0, 10]",
        "assert.b y = [0, 1]",
        "brc.b LES, exit:",
        "assert.b x = [0, 9]",
        "assert.b y = [0, 1]",
        "cmpeq.b EQ, x, 1",
        "xor.1 NEQ, EQ, 1",
        "brc.b NEQ, first_end:",
        "mov.b y, 1",
        "first_end:",
        "add.b x, x, 1",
        "br.d loop:",
        "exit:",
        "assert.b x = 10",
        "assert.b y = [0, 1]",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Test if the delayed widening domain will not perform a widening on y because of the constant assignment.
   */
  @Test public void syntacticConstantsWidening () {
    //  int x = 0;
    //  int y = 0;
    //  while (x < 100) {       ** x = [0, 100]  y = [0, 1]
    //    x++;                  ** x = [0, 99]   y = [0, 1]
    //    y = 1;
    //  }                       ** x = 100  y = 1               ! this can only be inferred with the Predicates(Z)
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 0",
        "loop:",
        "  cmples LES, 100, x",
        "  assert x = [0, 100]",
        "  assert y = [0, 1]",
        "  brc LES, exit:",
        "  assert x = [0, 99]",
        "  assert y = [0, 1]",
        "  add x, x, 1",
        "  mov y, 1",
        "  br loop:",
        "exit:",
        "assert x = 100",
        "assert y = 1",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Test if the delayed widening domain will not perform a widening on y because of the constant assignment.
   */
  @Test public void semanticConstantsWidening () {
    //  int x = 0;
    //  int y = 0;
    //  int z = 1;
    //  while (x < 100) {       ** x = [0, 100]  y = [0, 1]
    //    x++;                  ** x = [0, 99]   y = [0, 1]
    //    y = z;
    //  }                       ** x = 100  y = 1               ! this can only be inferred with the Predicates(Z)
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 0",
        "mov z, 1",
        "loop:",
        "  cmples LES, 100, x",
        "  assert x = [0, 100]",
        "  assert y = [0, 1]",
        "  brc LES, exit:",
        "  assert x = [0, 99]",
        "  assert y = [0, 1]",
        "  add x, x, 1",
        "  mov y, z",
        "  br loop:",
        "exit:",
        "assert x = 100",
        "assert y = 1",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Test if the delayed widening domain will not perform a widening on x the first round because
   * of a constant assignment. Example taken from "McMillan - Interpolation and Widening (slides)"
   */
  @Test public void semanticConstantsWidening2 () {
    //  int x = 0;
    //  int y = 0;
    //  while (?) {
    //    x = 1 - x;            ** x = [0, 1]  y = [0, +oo]
    //    y++;
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov y, 0",
        "loop:",
        "  assert x = [0, 1]",
        "  assert y = [0, +oo]",
        "  sub x, 1, x",
        "  add y, y, 1",
        "  brc RND, loop:",
        "exit:",
        "assert x = [0, 1]",
        "assert y = [1, +oo]",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Test if the delayed widening domain will not perform a widening on y because of the constant assignment.
   */
  @Test public void shouldNotWidenImmediately () {
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
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests if the delayed widening will not delay the widening because of the initialization of the variables.
   */
  @Test public void shouldWidenImmediately () {
    //  int x = 0;
    //  int y = 0;
    //  while (x < 100) {       ** x = [0, 100]  y = [0, 100]
    //    x++;                  ** x = [0, 99]   y = [0, 99]
    //    y++;
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
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * If the widening points are inferred "too simple" a join from an if-else could be marked as a widening point.
   * This example should test if the narrowing still works in such cases. Note that the if-else join needs to be
   * made a widening point artificially for that.
   */
  @Test public void ifElseJoinWiden () {
    // int x = 0;
    // int y = 0;
    // while (x < 10) {         ** x = [0, 10]
    //  if (?) {
    //   x++;
    //   y = 1;
    //  } else {
    //   y = 2;
    //  }
    // }                        ** x = 10; y = [0, 2]
    String assembly = lines(
        "mov.d x, 0",
        "mov.d y, 0",
        "loop:",
        "cmples.d LES, 10, x",
        "assert.d x = [0, 10]",
        "brc.d LES, exit:",
        "brc.d RND, else:",
        "if:",
        "mov.d y, 1",
        "add.d x, x, 1",
        "br.d if_else_join:",
        "else:",
        "mov.d y, 2",
        "if_else_join:",
        "br.d loop:",
        "exit:",
        "assert.d x = 10",
        "assert.d y = [0, 2]",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void ifElseJoinWiden1 () {
    // int x = 0;
    // while (x < 10) {         ** x = [0, 10]
    //  if (?)
    //   x++;
    // }                        ** x = 10
    String assembly = lines(
        "mov.d x, 0",
        "loop:",
        "cmples.d LES, 10, x",
        "assert.d x = [0, 10]",
        "brc.d LES, exit:",
        "brc.d RND, if_end:",
        "if:",
        "add.d x, x, 1",
        "if_end:",
        "nop",
        "br.d loop:",
        "exit:",
        "assert.d x = 10",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void ifElseJoinWiden2 () {
    // int i = 0;
    // while (i < 10) {
    //  i++;
    //  if (?)
    //    ;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "cmples LES, 10, i",
        "assert i = [0, 10]",
        "brc LES, exit_loop_i:",
        "add i, i, 1",
        "brc RND, rnd_end:",
        "nop",
        "rnd_end:",
        "nop",
        "br loop_i:",
        "exit_loop_i:",
        "assert i = 10",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void ifElseJoinWiden3 () {
    // int x = 0;
    // while (x < 10) {         ** x = [0, 10]
    //  if (?) {
    //   x++;
    //  } else {
    //   ;
    //  }
    // }                        ** x = 10
    String assembly = lines(
        "mov.d x, 0",
        "loop:",
        "cmples.d LES, 10, x",
        "assert.d x = [0, 10]",
        "brc.d LES, exit:",
        "brc.d RND, else:",
        "if:",
        "add.d x, x, 1",
        "br.d if_else_join:",
        "else:",
        "nop",
        "if_else_join:",
        "br.d loop:",
        "exit:",
        "assert.d x = 10",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a constant bound and the counter is reset to the outer counter value.
   */
  @Test public void ChaouchNestedLoops () {
    // int L = 10;
    // int M = 10;
    // int N = 10;
    // int i = 0;
    // int j = 0;
    // int k = 0;
    // while (i < L) {          ** i = [0, 11]
    //   j = 0;
    //   while (j < M) {        ** j = [0, 10]
    //     j++;
    //     k = i;
    //     while (k < N) {      ** k = [0, 10]
    //       k++;
    //     }
    //     i = k;
    //   }
    //   i++;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov L, 10",
        "mov M, 10",
        "mov N, 10",
        "mov i, 0",
        "loop_i:",
        "  assert i = [0, 11]",
        "  cmples LES, L, i",
        "  brc LES, exit_loop_i:",
        "  mov j, 0",
        "  loop_j:",
        "    assert j = [0, 10]",
        "    cmples LES, M, j",
        "    brc LES, exit_loop_j:",
        "    add j, j, 1",
        "    mov k, i",
        "    loop_k:",
        "      assert k = [0, 10]",
        "      cmples LES, N, k",
        "      brc LES, exit_loop_k:",
        "      add k, k, 1",
        "      br loop_k:",
        "    exit_loop_k:",
        "    mov i, k",
        "    br loop_j:",
        "  exit_loop_j:",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset to the outer counter value.
   * Here we use the enclosing counter as the bound.
   */
  @Test public void ChaouchNestedLoopsModified () {
    // int i = 0;
    // int j = 0;
    // int k = 0;
    // while (i < 10) {          ** i = [0, 10]
    //   j = 0;
    //   while (j < i) {        ** j = [0, 9]
    //     j++;
    //     k = i;
    //     while (k < j) {      ** k = [1, 9]   because the loop is only executed after one i++
    //       k++;
    //     }
    //     i = k;
    //   }
    //   i++;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  assert i = [0, 10]",
        "  cmples LES, 10, i",
        "  brc LES, exit_loop_i:",
        "  mov j, 0",
        "  loop_j:",
        "    assert j = [0, 9]",
        "    cmples LES, i, j",
        "    brc LES, exit_loop_j:",
        "    add j, j, 1",
        "    mov k, i",
        "    loop_k:",
        "      assert k = [1, 9]",
        "      cmples LES, j, k",
        "      brc LES, exit_loop_k:",
        "      add k, k, 1",
        "      br loop_k:",
        "    exit_loop_k:",
        "    mov i, k",
        "    br loop_j:",
        "  exit_loop_j:",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset. Similar to matrix computations.
   */
  @Benchmark @Test public void twoNestedLoops () {
    // int i = 0;
    // int j = 0;
    // while (i < 100) {        ** i = [0, 100]
    //  j = 0;                  ** i = [0, 99]
    //  while (j <= i) {        ** j = [0, 100]
    //    j++;                  ** j = [0, 99]
    //  }                       ** j = [1, 100]
    //  i++;
    // }                        ** i = 100
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
//        "mov j, 0",           // leads to an affine equality i=j and thus adds another iteration till stable
        "nop",
        "loop_i:",
        "  cmples LES, 100, i",
        "  assert i = [0, 100]",
        "  brc LES, exit_loop_i:",
        "  assert i = [0, 99]",
        "  mov j, 0",
        "  loop_j:",
        "    cmplts LTS, i, j",
        "    assert j = [0, 100]",
        "    brc LTS, exit_loop_j:",
        "    assert j = [0, 99]",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  assert j = [1, 100]",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "assert i = 100",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly, DebugHelper.printers.domainDump());
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset. Similar to matrix computations.
   * Because of the strict inequality the inner loop is not executed the first time. This leads to the normal
   * thresholds not being able to maintain the threshold as it needs to join the phase of loop being executed j < i
   * with the one where the loop is not executed i >= j. When doing this we cannot keep the threshold j < i. Hence, this
   * example needs the phased domain with which it works.
   */
  @Benchmark @Test public void twoNestedLoopsStrict () {
    // int i = 0;
    // int j = 0;
    // while (i < 100) {        ** i = [0, 100]
    //  j = 0;                  ** i = [0, 99]
    //  while (j < i) {         ** j = [0, 99]
    //    j++;                  ** j = [0, 98]
    //  }                       ** j = [0, 99]
    //  i++;
    // }                        ** i = 100
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  cmples LES, 100, i",
        "  assert i = [0, 100]",
        "  brc LES, exit_loop_i:",
        "  assert i = [0, 99]",
        "  mov j, 0",
        "  loop_j:",
        "    cmples LES, i, j",
        "    assert j = [0, 99]",
        "    brc LES, exit_loop_j:",
        "    assert j = [0, 98]",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  assert j = [0, 99]",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "assert i = 100",
        "halt");
    Analysis<?> analysis = phasedAnalyzer.runAnalysis(assembly, DebugHelper.printers.domainDumpBoth());
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset. Similar to matrix computations.
   */
  @Benchmark @Test public void twoNestedLoopsDiagonal () {
    // int i = 0;
    // int j = 0;
    // while (i < 100) {        ** i = [0, 100]
    //  j = i;                  ** i = [0, 99]
    //  while (j < 100) {       ** j = [0, 100]
    //    j++;                  ** j = [0, 99]
    //  }                       ** j = 100
    //  i++;
    // }                        ** i = 100
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  cmples LES, 100, i",
        "  assert i = [0, 100]",
        "  brc LES, exit_loop_i:",
        "  assert i = [0, 99]",
        "  mov j, i",
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
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset. Similar to matrix computations.
   */
  @Test public void threeNestedLoops () {
    // int i = 0;
    // int j = 0;
    // int k = 0;
    // while (i <= 10) {          ** i = [0, 11]
    //   j = 0;
    //   while (j <= i) {        ** j = [0, 11]
    //     k = 0;
    //     while (k <= j) {      ** k = [0, 11]
    //       k++;
    //     }
    //     j++;
    //   }
    //   i++;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  assert i = [0, 11]",
        "  cmplts LTS, 10, i",
        "  brc LTS, exit_loop_i:",
        "  mov j, 0",
        "  loop_j:",
        "    assert j = [0, 11]",
        "    cmplts LTS, i, j",
        "    brc LTS, exit_loop_j:",
        "    mov k, 0",
        "    loop_k:",
        "      assert k = [0, 11]",
        "      cmplts LTS, j, k",
        "      brc LTS, exit_loop_k:",
        "      add k, k, 1",
        "      br loop_k:",
        "    exit_loop_k:",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset. Similar to matrix computations.
   * This version uses strict inequalities and thus does not execute the inner loops for the first iteration.
   */
  @Test public void threeNestedLoopsStrict () {
    // int i = 0;
    // int j = 0;
    // int k = 0;
    // while (i < 10) {          ** i = [0, 10]
    //   j = 0;
    //   while (j < i) {         ** j = [0, 9]
    //     k = 0;
    //     while (k < j) {       ** k = [1, 9]   because the loop is only executed after one i++
    //       k++;
    //     }
    //     j++;
    //   }
    //   i++;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  assert i = [0, 10]",
        "  cmples LES, 10, i",
        "  brc LES, exit_loop_i:",
        "  mov j, 0",
        "  loop_j:",
        //        "    assert j = [0, 9]",
        "    cmples LES, i, j",
        "    brc LES, exit_loop_j:",
        "    mov k, 0",
        "    loop_k:",
        //        "      assert k = [1, 9]",
        "      cmples LES, j, k",
        "      brc LES, exit_loop_k:",
        "      add k, k, 1",
        "      br loop_k:",
        "    exit_loop_k:",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = phasedAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset. Similar to matrix computations.
   */
  @Test public void threeNestedLoopsDiagonal () {
    // int i = 0;
    // int j = 0;
    // int k = 0;
    // while (i <= 10) {          ** i = [0, 11]
    //   j = i;
    //   while (j <= 10) {        ** j = [0, 11]
    //     k = j;
    //     while (k <= 10) {      ** k = [0, 11]
    //       k++;
    //     }
    //     j++;
    //   }
    //   i++;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  assert i = [0, 11]",
        "  cmplts LTS, 10, i",
        "  brc LTS, exit_loop_i:",
        "  mov j, i",
        "  loop_j:",
        "    assert j = [0, 11]",
        "    cmplts LTS, 10, j",
        "    brc LTS, exit_loop_j:",
        "    mov k, j",
        "    loop_k:",
        "      assert k = [0, 11]",
        "      cmplts LTS, 10, k",
        "      brc LTS, exit_loop_k:",
        "      add k, k, 1",
        "      br loop_k:",
        "    exit_loop_k:",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset. Similar to matrix computations.
   */
  @Test public void fourNestedLoops () {
    // int i = 0;
    // int j = 0;
    // int k = 0;
    // int l = 0;
    // while (i <= 10) {          ** i = [0, 11]
    //   j = 0;
    //   while (j <= i) {         ** j = [0, 11]
    //     k = 0;
    //     while (k <= j) {       ** k = [0, 11]
    //       l = 0;
    //       while (l <= k) {     ** l = [0, 11]
    //         l++;
    //       }
    //       k++;
    //     }
    //     j++;
    //   }
    //   i++;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  assert i = [0, 11]",
        "  cmplts LTS, 10, i",
        "  brc LTS, exit_loop_i:",
        "  mov j, 0",
        "  loop_j:",
        "    assert j = [0, 11]",
        "    cmplts LTS, i, j",
        "    brc LTS, exit_loop_j:",
        "    mov k, 0",
        "    loop_k:",
        "      assert k = [0, 11]",
        "      cmplts LTS, j, k",
        "      brc LTS, exit_loop_k:",
        "      mov l, 0",
        "      loop_l:",
        "        assert l = [0, 11]",
        "        cmplts LTS, k, l",
        "        brc LTS, exit_loop_l:",
        "        add l, l, 1",
        "        br loop_l:",
        "      exit_loop_l:",
        "      add k, k, 1",
        "      br loop_k:",
        "    exit_loop_k:",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with a non-constant bound and the counter is reset. Similar to matrix computations.
   */
  @Test public void fiveNestedLoops () {
    // int i = 0;
    // int j = 0;
    // int k = 0;
    // int l = 0;
    // int m = 0;
    // while (i <= 10) {          ** i = [0, 11]
    //   j = 0;
    //   while (j <= i) {         ** j = [0, 11]
    //     k = 0;
    //     while (k <= j) {       ** k = [0, 11]
    //       l = 0;
    //       while (l <= k) {     ** l = [0, 11]
    //         m = 0;
    //         while (m <= l) {   ** m = [0, 11]
    //           m++;
    //         }
    //         l++;
    //       }
    //       k++;
    //     }
    //     j++;
    //   }
    //   i++;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  assert i = [0, 11]",
        "  cmplts LTS, 10, i",
        "  brc LTS, exit_loop_i:",
        "  mov j, 0",
        "  loop_j:",
        "    assert j = [0, 11]",
        "    cmplts LTS, i, j",
        "    brc LTS, exit_loop_j:",
        "    mov k, 0",
        "    loop_k:",
        "      assert k = [0, 11]",
        "      cmplts LTS, j, k",
        "      brc LTS, exit_loop_k:",
        "      mov l, 0",
        "      loop_l:",
        "        assert l = [0, 11]",
        "        cmplts LTS, k, l",
        "        brc LTS, exit_loop_l:",
        "        mov m, 0",
        "        loop_m:",
        "          assert m = [0, 11]",
        "          cmplts LTS, l, m",
        "          brc LTS, exit_loop_m:",
        "          add m, m, 1",
        "          br loop_m:",
        "        exit_loop_m:",
        "        add l, l, 1",
        "        br loop_l:",
        "      exit_loop_l:",
        "      add k, k, 1",
        "      br loop_k:",
        "    exit_loop_k:",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  add i, i, 1",
        "  br loop_i:",
        "exit_loop_i:",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Nested loops with two different counters.
   * Our issue here is that we need to use a threshold twice in the inner loop because i is not stable
   * due to the outer loop. However, with polyhedra, octagons it works as they capture the triangle
   * relation between i and j.
   */
  @Benchmark @Test public void BygdeFig43 () {
    // int i = 0;
    // int j = 0;
    // while (i < 100) {        ** i = [0, 100]
    //  j = 1;                  ** i = [0, 98]
    //  while (j <= i) {        ** j = [0, 99]
    //    j++;                  ** j = [0, 98]
    //  }                       ** j = [0, 98]
    //  i = i + 2;
    // }                        ** i = 100
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop_i:",
        "  cmples LES, 100, i",
        "  assert i = [0, 100]",
        "  brc LES, exit_loop_i:",
        "  assert i = [0, 98]",
        "  mov j, 0",
        "  loop_j:",
        "    cmplts LTS, i, j",
        "    assert j = [0, 99]",
        "    brc LTS, exit_loop_j:",
        "    assert j = [0, 98]",
        "    add j, j, 1",
        "    br loop_j:",
        "  exit_loop_j:",
        "  assert j = [1, 99]",
        "  add i, i, 2",
        "  br loop_i:",
        "exit_loop_i:",
        "assert i = 100",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Adds new thresholds inside of the loop while incrementing a variable.
   * Because of affine equalities between the threshold and the loop variable
   * we get to apply the new threshold each time at the widening point and
   * thus would not terminate.
   *
   * Does terminate after fixing the affine domain to reduce its child and thus
   * breaking up the equality between variables in the next iteration.
   * See below for a more robust version.
   */
  @Test public void infiniteThresholdsInLoop1 () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx, 0",
        "mov eax, 16",
        "mov rnd, ?",
        "loop:",
        "  prim (ebx) = malloc(eax)",
        "  store ebx, 1",
        "  cmpeq ZF, 0, rnd",
        "  brc ZF, loop:",
        "halt"
      );
    undefAnalyzer.runAnalysis(assembly);
  }

  /**
   * Adds new thresholds inside of the loop while incrementing a variable.
   * Because of affine equalities between the threshold and the loop variable
   * we get to apply the new threshold each time at the widening point and
   * thus would not terminate.
   *
   * This variant is more reliable than the one above and does not terminate if the
   * thresholds widening assumes a finite amount of thresholds per program point.
   *
   * Not working anymore after affine was fixed even better.
   */
  @Test public void infiniteThresholdsInLoop2 () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov aip, 0",
        "mov tmp, 4",
        "mov ebx, 0",
        "mov eax, 16",
        "mov rnd, ?",
        "loop:",
        "  prim (ebx) = malloc(eax)",
        "  store ebx, 1",               // adds a new threshold by testing the sum of the pointer flags
        // the 4 next lines are there to establish the equality between aip and the threshold variable
        "  sub ipOffset, tmp, aip",
        "  add aip, aip, ipOffset",
        "  add tmp, tmp, 1",
        "  add aip, aip, 4",
        // randomly exit or continue the loop
        "  cmpeq ZF, 0, rnd",
        "  brc ZF, loop:",
        "halt"
      );
    undefAnalyzer.runAnalysis(assembly);
  }

  /**
   * Test the widening when a threshold on a different variable as the loop counter
   * comes from a bound inside the loop given by a branch condition.
   */
  @Test public void thresholdFromIf1 () {
    // int x = 0;
    // int y = 0;
    // while (x <= 100) {       ** x = [0, 100], y = [0, 11]
    //  x = x + 1;
    //  if (y <= 10)
    //    y = y + 1;
    // }                        ** x = 101, y = [2, 11]                ! this can only be inferred with the Predicates(Z)
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 0",
        "loop:",
        "  cmpltu LTx, 100, x",
        "  brc LTx, exit:",
        "  assert x = [0, 100]",
        "  assert y = [0, 11]",
        "  add x, x, 1",
        "  cmpltu LTy, 10, y",
        "  brc LTy, else:",
        "    add y, y, 1",
        "  else:",
        "  br loop:",
        "exit:",
        "assert x = 101",
        "assert y = [3, 11]", // predicates synthesize the 3 from precision loss on y after joins
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Same as above but uses the same flag for the comparison on x and y.
   */
  @Test public void thresholdFromIf2 () {
    // int x = 0;
    // int y = 0;
    // while (x <= 100) {       ** x = [0, 100], y = [0, 11]
    //  x = x + 1;
    //  if (y <= 10)
    //    y = y + 1;
    // }                        ** x = 101, y = [2, 11]                 ! this can only be inferred with the Predicates(Z)
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 0",
        "loop:",
        "  cmpltu LT, 100, x",
        "  brc LT, exit:",
        "  assert x = [0, 100]",
        "  assert y = [0, 11]",
        "  add x, x, 1",
        "  cmpltu LT, 10, y",
        "  brc LT, else:",
        "    add y, y, 1",
        "  else:",
        "  br loop:",
        "exit:",
        "assert x = 101",
        "assert y = [3, 11]", // predicates synthesize the 3 from precision loss on y after joins
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Same as above but uses a non-terminating loop.
   */
  @Test public void thresholdFromIf3 () {
    // int y = 0;
    // while (true) {           ** y = [0, 11]
    //  if (y <= 10)
    //    y = y + 1;
    // }                        ** unreachable
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov y, 0",
        "loop:",
        "  assert y = [0, 11]",
        "  cmpltu LT, 10, y",
        "  brc LT, else:",
        "    add y, y, 1",
        "  else:",
        "  br loop:",
        "exit:",
        "assert UNREACHABLE",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Same as above but uses a non-terminating loop and a variable as upper bound.
   */
  @Test public void thresholdFromIf4 () {
    // int y = 0;
    // int u = 10;
    // while (true) {           ** y = [0, 11]
    //  if (y <= u)
    //    y = y + 1;
    // }                        ** unreachable
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov y, 0",
        "mov u, 10",
        "loop:",
        "  assert y = [0, 11]",
        "  cmpltu LT, u, y",
        "  brc LT, else:",
        "    add y, y, 1",
        "  else:",
        "  br loop:",
        "exit:",
        "assert UNREACHABLE",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Same as above but uses a non-terminating loop and a variable as upper bound.
   */
  @Test public void thresholdFromIf5 () {
    // int y = 0;
    // int u = [9, 10];
    // while (true) {           ** y = [0, 11]
    //  if (y <= u)
    //    y = y + 1;
    // }                        ** unreachable
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov y, 0",
        "mov u, [9, 10]",
        "loop:",
        "  assert y = [0, 11]",
        "  cmpltu LT, u, y",
        "  brc LT, else:",
        "    add y, y, 1",
        "  else:",
        "  br loop:",
        "exit:",
        "assert UNREACHABLE",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests the substitution of a threshold to another variable,
   * given an equality relation.
   */
  @Test public void thresholdSubstitution () {
    // int x = [0, 1];
    // while (x <= 10) {        ** x = [0, 11]
    //    tmp = x;
    //    x = x + 1;
    //    tmp = ?
    // }                        ** x = 11
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 1]",                // a non-constant value is used to infer the affine equality between the variables
        "loop:",
        "  assert x = [0, 11]",
        "  mov tmp, x",
        "  cmpltu LT, 10, tmp",
        "  brc LT, exit:",
        "  add x, x, 1",
        "  mov tmp, ?",
        "  br loop:",
        "exit:",
        "assert x = 11",
        "assert tmp = 11",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Tests the join of thresholds. A particular problematic case is when
   * through substitution a threshold is maintained on a variable that
   * does only exist inside the loop (e.g. a temporary variable). At the
   * loop entry join point the threshold is tested for redundancy and as the variable
   * does not exist outside the loop, it is introduced with TOP. Hence, the threshold
   * on this variable can never be redundant. This can be unfortunate when
   * the temporary variable is just chosen due to an equality relation with
   * another variable (e.g. the loop counter). Having the threshold on the loop counter
   * would indeed show that it is redundant and thus can be used for narrowing.
   */
  @Test public void thresholdJoin () {
    // int x = [0, 1];
    // while (x <= 10) {        ** y = [0, 11]
    //    tmp = x;
    //    x = x + 1;
    // }                        ** x = 11, tmp = 11
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 1]",                // a non-constant value is used to infer the affine equality between the variables
        "loop:",
        "  assert x = [0, 11]",
        "  mov tmp, x",
        "  cmpltu LT, 10, tmp",
        "  brc LT, exit:",
        "  add x, x, 1",
        "  br loop:",
        "exit:",
        "assert x = 11",
        "assert tmp = 11",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Minimal example to show why we need to remove thresholds that have already been applied for narrowing.
   * Here we generate a threshold inside the loop and transform and apply it each time widening is performed.
   * This leads to the value of i increasing by one each time.
   */
  @Test public void nonTerminationExample1 () {
    //  int i = 0;
    //  while (true) {
    //    if (i < 50)
    //      ...
    //    i++;
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov i, 0",
        "loop:",
        "  assert i = [-oo, +oo]",
        "  cmples LES, 50, i",
        "  brc LES, if_end:",
        "    nop",
        "  if_end:",
        "  add i, i, 1",
        "  br loop:",
        "exit:",
        "assert UNREACHABLE",
        "halt"
      );
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Should demonstrate non-termination by using substitution to perform symbolic
   * incrementation of a threshold. Does terminate though as we increment the value of
   * the upper bound variable in the threshold.
   */
  @Test public void nonTerminationExample2 () {
    //    int i = 0;
    //    int j = 50;
    //    int tmp;
    //    while (true) {
    //      if (i < j) {
    //        tmp = j + 1;        // move threshold to t1 and increment it
    //        j = tmp;            // move threshold back
    //      }                     // on join we now have the incremented and non-incremented threshold
    //      i++;
    //    }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 50",
        "loop:",
        "  assert i = [-oo, +oo]",
        "  assert j = [-oo, +oo]",
        "  cmples LES, j, i",
        "  brc LES, if_end:",
        "  add tmp, j, 1",
        "  mov j, tmp",
        "  if_end:",
        "  add i, i, 1",
        "  br loop:",
        "exit:",
        "assert UNREACHABLE",
        "halt"
      );
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Should demonstrate non-termination by using substitution to perform symbolic
   * incrementation of a threshold. Does terminate though as we always apply the test
   * and thus restrict the value of i in the if-branch so it cannot grow indefinitely.
   */
  @Test public void nonTerminationExample3 () {
    //    int i = 0;
    //    int j = 50;
    //    int t1, t2;
    //    while (true) {
    //      if (i < j) {
    //        t1 = i;             // move threshold to t1
    //        t2 = t1 + 1;        // move threshold to t2 increment it
    //        i = t2;             // move threshold back
    //      }                     // on join we now have the incremented and non-incremented threshold
    //    }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 50",
        "loop:",
        "  assert i = [0, 50]",
        "  cmples LES, j, i",
        "  brc LES, if_end:",
        "  mov t1, i",
        "  add t2, t1, 1",
        "  mov i, t2",
        "  mov t2, 0",
        "  if_end:",
        "  br loop:",
        "exit:",
        "assert UNREACHABLE",
        "halt"
      );
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Should demonstrate non-termination by using substitution to perform symbolic
   * incrementation of a threshold.
   */
  @Test public void nonTerminationExample4 () {
    //    int i = [0, 1];
    //    int j = 50;
    //    int t1, t2;
    //    if (i < j) {
    //      while (true) {
    //        t1 = i;             // move threshold to t1
    //        t2 = t1 + 1;        // move threshold to t2 and increment it
    //        i = t2;             // move threshold back
    //      }                     // on join we now have the incremented and non-incremented threshold
    //    }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "mov j, 50",
        "cmples LES, j, i",
        "brc LES, if_end:",
        "  loop:",
        "    mov t1, i",
        "    add t2, t1, 1",
        "    mov i, t2",
        "    mov t2, 0",
        "  br loop:",
        "if_end:",
        "exit:",
        "assert UNREACHABLE",
        "halt"
      );
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Terminates.
   */
  @Test public void increaseTwoVarsAlternating () {
    // int x = 0;
    // int y = 1;
    // while (x != y) {
    //   if (x < y)
    //     x += 2
    //   else
    //     y += 2
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov y, 1",
        "loop:",
        "  cmpeq EQ, x, y",
        "  xor.1 NEQ, EQ, 1",
        "  brc NEQ, notequal:",
        "  br exit:",
        "  notequal:",
        "    cmpltu LT, x, y",
        "    brc LT, lower:",
        "    br greater:",
        "    lower:",
        "     add x, x, 2",
        "    br if_else_end:",
        "    greater:",
        "      add y, y, 2",
        "    if_else_end:",
        "  br loop:",
        "exit:",
        "assert x = [1, 255]",
        "assert y = [1, 255]",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Same as above but without loop condition.
   */
  @Test public void increaseTwoVarsAlternating2 () {
    // int x = 0;
    // int y = 1;
    // while (true) {
    //   if (x < y)
    //     x += 2
    //   else
    //     y += 2
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov y, 1",
        "loop:",
        "  assert x = [1, +oo]",
        "  assert y = [1, +oo]",
        "    cmpltu LT, x, y",
        "    brc LT, lower:",
        "    br greater:",
        "    lower:",
        "     add x, x, 2",
        "    br if_else_end:",
        "    greater:",
        "      add y, y, 2",
        "    if_else_end:",
        "  br loop:",
        "exit:",
        "assert UNREACHABLE",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Same as above but tests the else branch condition again to force the addition of a redundant threshold.
   * Does not terminate as we have now both thresholds tracked and play the ping pong with one variable
   * being incremented at a time inside the loop. Due to the test only one loop branch is evaluated for each iteration
   * as the other one is stable.
   */
  @Test public void increaseTwoVarsAlternating2NonTerm () {
    // int x = 0;
    // int y = 1;
    // while (true) {
    //   if (x < y)
    //     x += 2
    //   else
    //     y += 2
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov y, 1",
        "loop:",
        "  assert x = [1, +oo]",
        "  assert y = [1, +oo]",
        "    cmpltu LT, x, y",
        "    brc LT, lower:",
        "    br greater:",
        "    lower:",
        "     add x, x, 2",
        "    br if_else_end:",
        "    greater:",
        "      cmpltu LT, x, y",          // re-add threshold of if test by testing it now again when it is redundant
        "      brc LT, exit:",
        "      add y, y, 2",
        "    if_else_end:",
        " mov LT, ?",
        "  br loop:",
        "exit:",                        // reachable because of re-add thresholds jump hack
        "assert x = [2, 252]",          // congruences make this not go till 255
        "assert y = [3, 253]",          // congruences make this not go till 255
        "halt ");
    // NOTE: when analyzed with the RedundantAffine and Octagons this does not terminate as we generated an infinite
    // ascending chain in Octagons through the closure operation provided by the RedundantAffine. It terminates, though,
    // when the normal Affine is used with Octagons.
    evaluateAssertions(assembly);
  }

  /**
   * Same as above but adds two thresholds outside of the loop.
   * This does not terminate as we increase each variable at a time
   * and are thus able to restrict the other using the redundant thresholds from outside the loop
   * (they could also be generated inside the loop, does not matter).
   * It would terminate if we always would use the joined states of the if-else branches as then
   * x and y are both incremented at the widening point and cannot restrict each other as both are widened.
   */
  @Test public void increaseTwoVarsAlternating3 () {
    // NOTE: does not terminate if the iteration order is depth-first as
    // the two variables are then increased and widened alternating
//    FixpointAnalysisProperties.INSTANCE.processAddressesInOrder.setValue(false);
    // int x = [0, 1];          // necessary to force threshold substitution to other variables
    // int y = [1, 2];          // otherwise the affine domain would inline the constant into the threshold
    // if (x <= y + 1)
    //   ...
    // if (y <= x + 2)          // now we track both if-conditions as thresholds
    //   ...
    // while (true) {
    //   if (x < y)
    //     x += 2
    //   else
    //     y += 2
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 1]",
        "mov y, [1, 2]",
        "add t, y, 1",
        "cmpltu LT, t, x",
        "brc LT, exit:",
        "mov t, ?",
        "add t, x, 2",
        "cmpltu LT, t, y",
        "brc LT, exit:",
        "mov t, ?",
        "loop:",
        "  assert x = [1, +oo]",
        "  assert y = [1, +oo]",
        "    cmpltu LT, x, y",
        "    brc LT, lower:",
        "    br greater:",
        "    lower:",
        "     add x, x, 2",
        "    br if_else_end:",
        "    greater:",
        "      add y, y, 2",
        "    if_else_end:",
        "  br loop:",
        "exit:",
        "assert UNREACHABLE",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Same as above but with different start values to force one branch as stable in each iteration.
   * This does not terminate as we increase each variable at a time
   * and are thus able to restrict the other using the redundant thresholds from outside the loop
   * (they could also be generated inside the loop, does not matter).
   * It does not terminate as we manage to have one part of the if-then-else as stable each time
   * thus at the loop header only one variable is bigger each time and can be restricted by
   * a threshold on the other variable.
   */
  @Test public void increaseTwoVarsAlternating3NonTerm () {
    // int x = [0, 1];          // necessary to force threshold substitution to other variables
    // int y = [2, 3];          // otherwise the affine domain would inline the constant into the threshold
    // if (x <= y + 1)
    //   ...
    // if (y <= x + 2)          // now we track both if-conditions as thresholds
    //   ...
    // while (true) {
    //   if (x < y)
    //     x += 2
    //   else
    //     y += 2
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 1]",
        "mov y, [2, 3]",
        "add t, y, 1",
        "cmpltu LT, t, x",
        "brc LT, exit:",
        "mov t, ?",
        "add t, x, 2",
        "cmpltu LT, t, y",
        "brc LT, exit:",
        "mov t, ?",
        "loop:",
        "  assert x = [1, +oo]",
        "  assert y = [1, +oo]",
        "    cmpltu LT, x, y",
        "    brc LT, lower:",
        "    br greater:",
        "    lower:",
        "     add x, x, 2",
        "    br if_else_end:",
        "    greater:",
        "      add y, y, 2",
        "    if_else_end:",
        "  br loop:",
        "exit:",
        "assert x = 0",         // coming from the pre-loop tests
        "assert y = 3",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Same as above but adds two thresholds inside of the loop.
   */
  @Test public void increaseTwoVarsAlternating4 () {
    // NOTE: does not terminate if the iteration order is depth-first as
    // the two variables are then increased and widened alternating
//    FixpointAnalysisProperties.INSTANCE.processAddressesInOrder.setValue(false);
    // int x = [0, 1];          // necessary to force threshold substitution to other variables
    // int y = [1, 2];          // otherwise the affine domain would inline the constant into the threshold
    // while (true) {
    //   if (x <= y + 1)
    //     ...
    //   if (y <= x + 2)          // now we track both if-conditions as thresholds
    //     ...
    //   if (x < y)
    //     x += 2
    //   else
    //     y += 2
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, [0, 1]",
        "mov y, [1, 2]",
        "loop:",
        "  assert x = [1, +oo]",
        "  assert y = [1, +oo]",
        "  add t, y, 1",
        "  cmpltu LT, t, x",
        "  brc LT, exit:",
        "  mov t, ?",
        "  add t, x, 2",
        "  cmpltu LT, t, y",
        "  brc LT, exit:",
        "  mov t, ?",
        "    cmpltu LT, x, y",
        "    brc LT, lower:",
        "    br greater:",
        "    lower:",
        "     add x, x, 2",
        "    br if_else_end:",
        "    greater:",
        "      add y, y, 2",
        "    if_else_end:",
        "  br loop:",
        "exit:",
        "assert x = [0, +oo]",          // reachable because of the thresholds adding hack
        "assert y = [0, +oo]",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Same as above but adds two thresholds inside of the loop and
   * uses another loops inside to simulate the alternating incrementation.
   * Does terminate though because of the join after the incrementation
   * and the widening at the outer loop.
   */
  @Test public void increaseTwoVarsAlternating5 () {
    // NOTE: does not terminate if the iteration order is depth-first as
    // the two variables are then increased and widened alternating
//    FixpointAnalysisProperties.INSTANCE.processAddressesInOrder.setValue(false);
    // int x = [0, 1];          // necessary to force threshold substitution to other variables
    // int y = [1, 2];          // otherwise the affine domain would inline the constant into the threshold
    // while (true) {
    //   if (x <= y + 1)
    //     ...
    //   if (y <= x + 2)          // now we track both if-conditions as thresholds
    //     ...
    //   if (x < y)
    //     while (x < y)
    //         x += 2
    //   else
    //     while (y <= x)
    //         y += 2
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, [0, 1]",
        "mov y, [1, 2]",
        "loop:",
        "  add t, y, 1",
        "  cmpltu LT, t, x",
        "  brc LT, exit:",
        "  mov t, ?",
        "  add t, x, 2",
        "  cmpltu LT, t, y",
        "  brc LT, exit:",
        "  mov t, ?",
        "    cmpltu LT, x, y",
        "    brc LT, lower:",
        "    br greater:",
        "    lower:",
        "    innerloopx:",
        "    cmpleu LE, y, x",
        "    brc LE, if_else_end:",
        "      add x, x, 2",
        "      br innerloopx:",
        "    br if_else_end:",
        "    greater:",
        "    innerloopy:",
        "    cmpltu LT, x, y",
        "    brc LT, if_else_end:",
        "      add y, y, 2",
        "      br innerloopy:",
        "    if_else_end:",
        "  br loop:",
        "exit:",
        "assert x = [0, +oo]",          // reachable because of the thresholds adding hack
        "assert y = [0, +oo]",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Same as above but without if-else inside the loop.
   * This terminates because x and y are widened at the same time and thus cannot
   * restrict each other using the thresholds.
   */
  @Test public void increaseTwoVarsAlternating7 () {
    // int x = 0;
    // int y = 1;
    //   if (x <= y + 1)
    //     ...
    //   if (y <= x + 2)          // now we track both if-conditions as thresholds
    //     ...
    // while (true) {
    //   x += 2;
    //   y += 2;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 1]",
        "mov y, [1, 2]",
        "add t, y, 1",
        "cmpltu LT, t, x",
        "brc LT, exit:",
        "mov t, ?",
        "add t, x, 2",
        "cmpltu LT, t, y",
        "brc LT, exit:",
        "mov t, ?",
        "loop:",
        "     add x, x, 2",
        "     add y, y, 2",
        "  br loop:",
        "exit:",
        "assert x = [1, +oo]",          // reachable because of the thresholds adding hack
        "assert y = [1, +oo]",
        "halt ");
    evaluateAssertions(assembly);
  }

}
