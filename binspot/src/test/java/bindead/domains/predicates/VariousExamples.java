package bindead.domains.predicates;

import static bindead.TestsHelper.lines;

import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;

/**
 * Tests for the predicates domain.
 */
public class VariousExamples {
  private final static AnalysisFactory analyzer = new AnalysisFactory();

  public Analysis<?> evaluateAssertionsDebug (String assembly) {
//    PredicatesProperties.INSTANCE.debugBinaryOperations.setValue(true);
//    FixpointAnalysisProperties.INSTANCE.debugBinaryOperations.setValue(true);
//    FixpointAnalysisProperties.INSTANCE.debugSubsetOrEqual.setValue(true);

    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();
    Analysis<?> analysis = analyzer.runAnalysis(assembly, DebugHelper.printers.domainDumpBoth());
    TestsHelper.evaluateAssertions(analysis);
    return analysis;
  }

  public Analysis<?> evaluateAssertions (String assembly) {
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
    return analysis;
  }

  @Test public void equality1 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 16",
        "mov y, 16",
        "cmpeq f, x, y",
        "assert.1 f = [0, 1]",
        "brc f, equal:",
        "assert UNREACHABLE",
        "equal:",
        "assert.1 f = 1",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void equality2 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, +oo]",
        "mov y, [0, 10]",
        "cmpeq f, x, y",
        "assert.1 f = [0, 1]",
        "brc f, equal:",
        "assert x = [0, +oo]",
        "assert y = [0, 10]",
        "br exit:",
        "equal:",
        "assert x = [0, 10]",
        "assert y = [0, 10]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void equality3 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 2",
        "loop:",
        "cmpeq ZF, y, 1",
        "assert y = [1, 2]",
        "brc ZF, exit:",
        "assert y = 2",
        "add x, x, 1",
        "mov y, 1",
        "assert y = 1",
        "br loop:",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void overwriteVariable1 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 1",
        "mov y, [2, 3]",
        "cmplts f, x, y",  // f -> {x < y}
        "assert.1 f = [0, 1]",
        "mov y, 0xdead", //  f -> {} no substitution as y not constant
        "brc f, lower:",
        "higher:",
        "assert.1 f = 0",
        "br exit:",
        "lower:",
        "assert.1 f = 1",
        "exit: ",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void overwriteVariable2 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 1",
        "mov y, 2",
        "cmplts f, x, y", // f -> {x < y}
        "assert.1 f = [0, 1]",
        "mov y, 0xdead", // f -> {x < 2} substitution of y
        "brc f, lower:",
        "higher:",
        "assert UNREACHABLE",
        "br exit:",
        "lower:",
        "assert.1 f = 1",
        "exit: ",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void overwriteVariable3 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [2, 3]",
        "mov y, x", // x == y in affine
        "cmpeq f, x, y", // f -> {x == y}
        "assert.1 f = [0, 1]",
        "mov y, 0xdead",  // f -> {x == x} through substitution
        "brc f, equal:",
        "notequal:",
        "assert UNREACHABLE",
        "br exit:",
        "equal:",
        "assert.1 f = 1",
        "exit: ",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void loopLowerEqual () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 16",
        "loop:",
        "assert x = [0, 16]",
        "add x, x, 1",
        "cmples f1, x, y",
        "brc f1, loop:",
        "assert x = 17",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void loopLower () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 16",
        "loop:",
        "assert x = [0, 15]",
        "add x, x, 1",
        "cmplts f1, x, y",
        "brc f1, loop:",
        "assert x = 16",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void loopEqual () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 16",
        "loop:",
        "assert x = [0, 15]",
        "add x, x, 1",
        "cmpeq f1, x, y",
        "brc f1, loop_exit:",
        "br loop:",
        "loop_exit:",
        "assert x = 16",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void loopNotEqual () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov y, 16",
        "loop:",
        "assert x = [0, 15]",
        "add x, x, 1",
        "cmpeq f1, x, y",
        "xor.1 f1, f1, 1", // disequality expressed through xor
        "brc f1, loop:",
        "assert x = 16",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void lowerEqual () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [-1, 42]",
        "cmples LE, x, 1",
        "brc LE, isLE:",
        "assert x = [2, 42]",
        "br exit:",
        "isLE:",
        "assert x = [-1, 1]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void lowerEqualWithWrapping1 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [-1, +oo]",
        "cmples LE, x, 1",
        "brc LE, isLE:",
        "assert x = [2, 2147483647]",
        "br exit:",
        "isLE:",
        "assert x = [-2147483648, 1]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void lowerEqualWithWrapping2 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, [0, 255]",
        "mov y, x",
        "cmples LES, x, 0",
        "brc LES, exit_les:",
        "assert x = [1, 127]",
        "assert y = [1, 127]",
        "br exit:",
        "exit_les:",
        "assert x = [-128, 0]",
        "exit:",
        "assert x = [-128, 127]",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void greaterThanWithWrapping () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [-1, +oo]",
        "cmples LE, x, 1",
        "xor.1 GT, LE, 1",
        "brc GT, isGT:",
        "assert x = [-2147483648, 1]",
        "br exit:",
        "isGT:",
        "assert x = [2, 2147483647]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void topCutout () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, +oo]",
        "cmplts LT, x, -1",
        "brc LT, isLT:",
        //        "assert x = [-1, +oo]", // value gets wrapped and cut to range, so below is right
        "assert x = [-1, 2147483647]",
        "cmples LE, x, 1",
        "xor.1 GT, LE, 1",
        "brc GT, isGT:",
        "assert x = [-1, 1]",
        "br exit:",
        "isLT:",
        "assert x = [-2147483648, -2]",
        "br exit:",
        "isGT:",
        "assert x = [2, 2147483647]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void substitution () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 10]",
        "mov z, x", // x == z in affine
        "mov y, x", // y == z in affine; actually y = z and y = x here
        "cmpeq f, z, y",
        "assert.1 f = [0, 1]",
        "mov z, 12345", // predicate on z should be substituted to x; actually it results in y == y
        "brc f, equal:",
        "assert UNREACHABLE",
        "equal:",
        "assert.1 f = 1",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void transformPredicate () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [1, 2]",
        "mov y, 3",
        "cmpltu f, x, y",
        "assert.1 f = [0, 1]",
        "add x, x, 1",
        "brc f, lower:",
        "higher:",
        "assert UNREACHABLE",
        "lower:",
        "assert.1 f = 1",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Test the introduction and manipulation of boolean variables.
   */
  @Test public void testBoleanVarIntroduction () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov a, 3",
        "mov.1 F1, 3",
        "assert.1 F1 = 3", // boolean flag can have a bigger numeric value in domain
        "mov.1 F2, 1",
        "cmpeq F1, a, 1",
        "assert.1 F1 = [0, 1]", // flag is set to unknown in comparison above
        "brc F1, exit:",
        "assert.1 F1 = 0", // flag is now known
        "add.1 F2, F2, 1",
        "assert.1 F2 = 2", // boolean flag can have a bigger numeric value in domain
        "mov.1 F1, 0", // association of flag with comparison should be killed here
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void predicateGenerationOnJoin () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "nop",
        "brc RND, right:",
        "left:",
        "mov x, [0, 5]",
        "mov.1 f, 0",
        "br join:",
        "right:",
        "mov x, [10, 15]",
        "mov.1 f, 1",
        "join:",
        "nop",
        "assert x = [0, 15]", // convex approximation
        "brc f, true:",
        "false:",
        "assert x = [0, 5]", // inferred through predicates on f
        "br exit:",
        "true:",
        "assert x = [10, 15]", // inferred through predicates on f
        "exit:",
        "assert x = [0, 15]", // convex approximation
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Tests how the predicates domain helps to maintain the invariant that x != 0
   * if that has been tested by a branching before.
   */
  @Test public void divisionByZero () {
    // if (x != 0) {
    //  assert x != 0;
    //  y = 10 / x;
    // }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [-1, 1]",
        "mov y, 1",
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
   * Example from: Heizmann - Software Model Checking for People Who Love Automata
   * Works only if setting the flag for the if foo p = 0 to false outside the loop as
   * that allows us to keep the predicate in the join on the loop-back edge.
   */
  @Test public void traceSeparation () {
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
//        "assert n = [-1, 5]",
        "cmplts ZF, n, 0",
        "brc ZF, loop_exit:",
//        "assert n = [0, 5]",
        "cmpeq fa, p, 0",
        "brc fa, assertion_fail:",
//        "assert p = [1, 100]",
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
   * Tests a bug in PredicateStateBuilder.remove(lhs) where a predicate with a projected out variable would
   * be inlined again due to inlining equalities for flags which in term removed the flags first.
   */
  @Test public void flagEqualitiesInline () {
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
   * With substitution predicates bug that introduced unsound implications
   * this did not terminate.
   */
  @Test public void shouldNotWidenImmediately () {
    //  int x = 0;
    //  int y = 0;
    //  while (x < 100) {       ** x = [0, 100]  y = [0, 1]
    //    x++;                  ** x = [0, 99]   y = [0, 1]
    //    y = 0;
    //    y++;
    //  }                       ** x = 100  y = 1       ! this can only be inferred with the Predicates(Z)
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
   * Example from: Fisher - Joining Dataflow with Predicates
   */
  @Test public void pathSensitiveState () {
    //    x = 1;
    //    f = [0, 1]
    //    assert(x == 1);
    //    x = 0;
    //    if (f)
    //      x = 1;
    //    if (f)
    //      assert(x == 1);
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov f, [0, 1]",
        "assert x = 0",
        "cmpeq EQ, f, 0",
        "brc EQ, zero:",
        "assert.1 EQ = 0",
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
   * Show how reducing a variable that has an equality, also reduces the
   * other and if the equality is still valid afterwards.
   */
  @Test public void equalitiesReduction () {
    //    x = [0, 1]
    //    y = x
    //    if (x < 1)
    //      ...
    //    x = [0, 2]
    //    y = x
    //    if (x < 2)
    //      ...
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "mov f, [0, 1]",
        "assert x = 0",
        "cmpeq EQ, f, 0",
        "brc EQ, zero:",
        "assert.1 EQ = 0",
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
   * An example where we keep an implication alive on the backedge and transform it inside the loop body.
   * As the transformed implication is also valid we keep adding implications and are never stable.
   * Note that this does not terminate as our fixpoint enters the loop before going
   * the else path of the if. Thus f = 0 when entering the loop and every implication with f = 1 as premise
   * is kept because of false->anything is kept in the join.
   */
  @Test public void nonTerminationDueToTransformation1 () {
    //  x = [0, 2]
    //  if (x < 1)
    //    ...
    //  //here we have f -> x < 1  and !f -> x >= 1
    //  while (x <= 5) {
    //    //here we have g -> x <= 5 and !g -> x > 5
    //    x++;
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 2]",
        "cmpltu f, x, 1",
        "brc f, lower:",
        "greater_equal:",
        "  assert x = [1, 2]",
        "  assert.1 f = 0",
        "  br if_else_end:",
        "lower:",
        "  assert x = 0",
        "  assert.1 f = 1",
        "if_else_end:",
        "assert x = [0, 2]",
        "assert.1 f = [0, 1]",
        "loop:",
        "cmpltu LT, 5, x",
        "brc LT, exit:",
        "  add x, x, 1",
        "  br loop:",
        "exit:",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Similar to first example above but now we explicitly set the value of f = 1
   * before entering the loop.
   */
  @Test public void nonTerminationDueToTransformation2 () {
    //  x = [0, 2]
    //  if (x < 1)
    //    ...
    //  //here we have f -> x < 1  and !f -> x >= 1
    //  if (f) {
    //    while (x <= 5) {
    //      //here we have g -> x <= 5 and !g -> x > 5
    //      x++;
    //    }
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 2]",
        "cmpltu f, x, 1",
        "brc f, lower:",
        "greater_equal:",
        "  assert x = [1, 2]",
        "  assert.1 f = 0",
        "  br if_else_end:",
        "lower:",
        "  assert x = 0",
        "  assert.1 f = 1",
        "if_else_end:",
        "assert x = [0, 2]",
        "assert.1 f = [0, 1]",
        "brc f, exit:",
        " loop:",
        " cmpltu LT, 5, x",
        " brc LT, exit:",
        "   add x, x, 1",
        "   br loop:",
        "exit:",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Similar to the two examples above but now we do not rely on a predicate on a flag as a premise
   * in the implication but synthesize implications through a join. These synthesized implications
   * from outside the loop are kept while iterating and are transformed forever.
   *
   * Note that this actually terminates because we enter the loop before going the else path in the first if
   * and thus get a fixpoint inside the loop. As we then go the else path we are not bigger although
   * we now have the synthesized predicates and do not enter the loop again. Thus, we cannot
   * propagate the synthesized predicates and transform them inside the loop and cause a similar
   * non-termination condition as in the above examples.
   */
  @Test public void nonTerminationDueToTransformation3 () {
    //  x = [0, 2]
    //  if (x < 1)
    //    f = 1
    //  else
    //    f = 0
    //  //here we have f = 1 -> x < 1  and f = 0 -> x >= 1
    //  if (f = 1) {
    //    while (x <= 5) {
    //      //here we have LE -> x <= 5 and !LE -> x > 5
    //      x++;
    //    }
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 2]",
        "cmpltu LT, x, 1",
        "brc LT, lower:",
        "greater_equal:",
        "  assert x = [1, 2]",
        "  mov f, 1",
        "  br if_else_end:",
        "lower:",
        "  assert x = 0",
        "  mov f, 0",
        "if_else_end:",
        "assert x = [0, 2]",
        "assert.1 LT = [0, 1]",
        "assert f = [0, 1]",
        "cmpeq EQ, f, 1",
        "xor.1 NEQ, EQ, 1",
        "brc NEQ, exit:",
        " loop:",
        " cmpltu LT1, 5, x",
        " brc LT1, exit:",
        "   add x, x, 1",
        "   br loop:",
        "exit:",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Similar to above example but now we force the reevaluation of the loop after analyzing the
   * else path by changing the state in the else path to become bigger. We use a new variable
   * that changes its value on the if-else paths.
   *
   * Note that it terminates. It is not enough of a hack as the
   * loop is analyzed once and then the predicates at the loop header are all entailed in
   * the new state so we are stable.
   */
  @Test public void nonTerminationDueToTransformation4 () {
    //  x = [0, 2]
    //  if (x < 1) {
    //    f = 1
    //    crowbar = 2
    //  } else {
    //    f = 0
    //    crowbar = [1, 3]
    //    // trick!: the value above needs to be an interval containing the value of crowbar from the other path
    //    // only then we cannot separate them again when entering the if-branch testing f below
    //    // otherwise we would not be bigger and thus not enter the loop again
    //  }
    //  //here we have f = 1 -> x < 1  and f = 0 -> x >= 1
    //  if (f = 1) {
    //    while (x <= 5) {
    //      //here we have LE -> x <= 5 and !LE -> x > 5
    //      x++;
    //    }
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 2]",
        "cmpltu LT, x, 1",
        "brc LT, lower:",
        "greater_equal:",
        "  assert x = [1, 2]",
        "  mov f, 1",
        "  mov crowbar, 2",
        "  br if_else_end:",
        "lower:",
        "  assert x = 0",
        "  mov f, 0",
        "  mov crowbar, [1, 3]",
        "if_else_end:",
        "assert x = [0, 2]",
        "assert.1 LT = [0, 1]",
        "assert f = [0, 1]",
        "cmpeq EQ, f, 1",
        "xor.1 NEQ, EQ, 1",
        "brc NEQ, exit:",
        " loop:",
        " cmpltu LT, 5, x",
        " brc LT, exit:",
        "   add x, x, 1",
        "   br loop:",
        "exit:",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Trying to synthesize implications using the join/widening in a loop as
   * a loop is analyzed completely by our fixpoint before continuing.
   *
   * Note that it terminates. Unfortunately it is not trivial to exit the loop
   * generating the implications and having the over-approximated state.
   * The latter is reduced by the predicates such that it describes precisely
   * the state when leaving the loop and not the join with the state before the loop.
   */
  @Test public void nonTerminationDueToTransformation5 () {
    //  counter = 0
    //  x = 0
    //  f = 0
    //  if (counter < 1) {
    //    counter++
    //    x++
    //    f++
    //  }
    //  if (f = 1) {
    //    while (x <= 5) {
    //      //here we have LE -> x <= 5 and !LE -> x > 5
    //      x++;
    //    }
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov counter, 0",
        "mov x, 0",
        "mov f, 0",
        "loop1:",
        "cmpltu LT, counter, 1",
        "xor.1 GE, LT, 1",
        "brc GE, exit_loop1:",
        "  add counter, counter, 1",
        "  add x, x, 1",
        "  add f, f, 1",
        "br loop1:",
        "exit_loop1:",
        "assert counter = 1",
        "assert x = 1",
        "assert f = 1",
        "cmpeq EQ, f, 1",
        "xor.1 NEQ, EQ, 1",
        "brc NEQ, exit:",
        " loop:",
        " cmpltu LT, 5, x",
        " brc LT, exit:",
        "   add x, x, 1",
        "   br loop:",
        "exit:",
        "halt ");
    evaluateAssertions(assembly);
  }

  /**
   * Finally convinced/hacked the analyzer!
   * Using a goto it is possible to force the complete evaluation of an if-else before
   * entering the loop. Thus we synthesize the required implications in the if-else
   * that will be transformed forever in the loop.
   */
  @Test public void nonTerminationDueToTransformation6 () {
    //  x = [0, 2]
    //  wideningBlocker = [0, 1]
    //  if (x < 1) {
    //    ...
    //  } else {
    //    else_branch:
    //    ...
    //  }
    //  if (?) {
    //    wideningBlocker = 1234567             // delays widening when jumping back
    //    goto else_branch:
    //  }
    //  //here we have LT = 1 -> x < 1  and LT = 0 -> x >= 1
    //  if (f = 1) {
    //    while (x <= 5) {
    //      //here we have LE -> x <= 5 and !LE -> x > 5
    //      x++;
    //    }
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov RND, ?",
        "mov x, [0, 2]",
        "mov wideningBlocker, [0, 1]",
        "cmpltu LT, x, 1",
        "brc LT, lower:",
        "greater_equal:",
        "  assert x = [1, 2]",
        "  br if_else_end:",
        "lower:",
        "  assert x = [0, 2]",
        "if_else_end:",
        "assert x = [0, 2]",
        "assert.1 LT = [0, 1]", // here we get an implication between LT and x that survives
        "cmpeq EQ, RND, 1",
        "brc EQ, jump_over:",
        "  mov wideningBlocker, 1234567",
        "  br lower:",
        "jump_over:",
        "cmpeq EQ, f, 2",
        "xor.1 NEQ, EQ, 1",
        "brc NEQ, exit:",
        " loop:",
        " cmpleu LE, x, 0",
        " brc LE, exit:",
        "   sub x, x, 1",
        "   br loop:",
        "exit:",
        "halt ");
    evaluateAssertions(assembly);
  }

  @Test public void nonTerminationDueToTransformation7 () {
    //  x = 0
    //  have: f = 1 -> x < 1
    //  f = 1
    //  while (x < 10) {
    //    x++
    //    if (f == 1)
    //      ....
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [0, 2]",
        "cmpltu f, 1, x",
        "brc f, lower:",
        "greater_equal:",
        "  assert x = [0, 1]",
        "  assert.1 f = 0",
        "  br if_else_end:",
        "lower:",
        "  assert x = 2",
        "  assert.1 f = 1",
        "if_else_end:",
        "assert x = [0, 2]",
        "assert.1 f = [0, 1]",
        "brc f, exit:",
        "  assert.1 f = 0",
        "  loop:",
        "  cmpltu LT, 10, x",
        "  brc LT, exit:",
        "    add x, x, 1",
        "    brc f, exit:",
        "    br loop:",
        "exit:",
        "halt ");
    evaluateAssertions(assembly);
  }

}
