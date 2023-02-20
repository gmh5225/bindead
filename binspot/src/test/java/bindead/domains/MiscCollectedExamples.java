package bindead.domains;

import static bindead.TestsHelper.lines;

import org.junit.Before;
import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.debug.DebugHelper;
import bindead.domains.affine.Affine;
import bindead.domains.congruences.Congruences;
import bindead.domains.intervals.Intervals;
import bindead.domains.predicates.finite.Predicates;
import bindead.domains.widening.oldthresholds.ThresholdsWidening;

/**
 * A place to gather unit tests for newly discovered bugs until they will get fixed.
 * Please move the unit test after fixing the bug to a separate file where it fits thematically.
 */
public class MiscCollectedExamples {
  private final static AnalysisFactory defaultAnalyzer = new AnalysisFactory(); // default domain stack
  @SuppressWarnings("unused")
  private static String[] interestingDomains =
  {Predicates.NAME, ThresholdsWidening.NAME, Affine.NAME, Congruences.NAME, Intervals.NAME};
//  private static AnalysisDebugHooks debugger = DebugHelper.printers.domainDumpFiltered(interestingDomains);
  @SuppressWarnings("unused")
  private static AnalysisDebugHooks debugger = DebugHelper.combine(
      DebugHelper.printers.instructionEffect(),
      DebugHelper.printers.domainDumpBoth());

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  public static void evaluateAssertions (String assembly) {
    evaluateAssertions(assembly, defaultAnalyzer);
  }

  public static void evaluateAssertions (String assembly, AnalysisFactory analyzer) {
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  public static void evaluateAssertionsDebug (String assembly) {
    evaluateAssertionsDebug(assembly, defaultAnalyzer);
  }

  public static void evaluateAssertionsDebug (String assembly, AnalysisFactory analyzer) {
//    PredicatesProperties.INSTANCE.debugBinaryOperations.setValue(true);
//    FixpointAnalysisProperties.INSTANCE.debugBinaryOperations.setValue(true);
//    FixpointAnalysisProperties.INSTANCE.debugSubsetOrEqual.setValue(true);
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();
    Analysis<?> analysis = analyzer.runAnalysis(assembly, debugger);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * Taken from "L. Chien - An Abstract Domain to Discover Interval Linear Equalities".
   * The problem here lies in joining two different affine equalities from the two branches.
   * Using interval coefficients for affine equalities the join can be expressed.
   * However, using congruences we can express the join, too.
   */
  @Test public void intervalLinearEqualities () {
    //  x, y = ?;
    //  if (?)
    //    x = y + 1;
    //  else
    //    x = 2 * y + 1;
    //  ...
    //  if (x == 0)
    //    y = 1 / y + 1
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [-oo, +oo]",
        "mov y, [-oo, +oo]",
        "brc RND, else:",
        "then:",
        "  add x, y, 1",        // x = y-1
        "  br if_end:",
        "else:",
        "  mul y, y, 2",        // y % 2
        "  add x, y, 1",        // x = y-1 with x % 2 + 1
        "if_end:",
        "assert x *+= y",       // hence, we can keep the affine equality between the variables, with congruences of x, y becoming % 1 + 0 on join
        "cmpneq NZF, x, 0",
        "brc NZF, exit:",
        "zero:",
        "  assert x = 0",
        "  assert y != 0",    // we lose the affine equality between the variables on the zero test (wrapping issue)
        "  div y, 1, y",
        "  add y, y, 1",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Taken from "A. Miné - The Octagon Abstract Domain"
   */
  @Test public void randomWalkOctagons () {
    //    int m = 100;
    //    int a = 0;
    //    int i = 1;
    //    while (i <= m) {
    //      if (?)
    //        a++;
    //      else
    //        a--;
    //      i++;
    //    }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov m, 100",
        "mov a, 0",
        "mov i, 1",
        "loop:",
        "  assert i = [1, 101]",
        "  assert a = [-100, 100]",
        "  cmplts LTS, m, i",
        "  brc LTS, loop_end:",
        "  brc RND, else:",
        "  then:",
        "    add a, a, 1",
        "    br if_end:",
        "  else:",
        "    sub a, a, 1",
        "  if_end:",
        "  add i, i, 1",
        "  br loop:",
        "loop_end:",
        "assert i = 101",
        "assert a = [-100, 100]",
        "assert a <= m",
        "mul mm, m, -1",
        "assert mm <= a",
        "halt");
    AnalysisFactory analyzer = new AnalysisFactory()
      .disableDomains("Heap", "PointsTo", "Predicates(Z)", "Congruences", "Intervals")
      .enableDomains("Apron(Octagons)");
    evaluateAssertions(assembly, analyzer);
  }

  /**
   * Taken from "A. Miné - The Octagon Abstract Domain"
   * Using the Phased and ConstantsWideningThresholds domains we can analyze this if the
   * randomness source is modified (see comments in the code below).
   * The Phased domain separates the invariants (affine equalities) for the different
   * stages of the loop and the second domain is needed to insert the right stage separators
   * into the Phased domain.
   */
  @Test public void randomWalkEqualities () {
    //    int m = 100;
    //    int a = 0;
    //    int i = 1;
    //    int RND = ?;
    //    while (i <= m) {
    //      if (RND)
    //        a++;
    //      else
    //        a--;
    //      i++;
    //    }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov m, 100",
        "mov a, 0",
        "mov i, 1",
        // Line below is needed. RND has to exist and be the same field (bit-size) that is introduced later
        // on at the comparison. It being boolean leads to one of the states from the other branch to be bottom
        // thus the ++ or -- not being applied to it. We then can keep the equality between a and i in both branches.
        // If one of the branches is not bottom then it spills over and we get a common non-separable state
        // where a is later on unbounded after widening. This comes from the fact that we cannot maintain the affine
        // equality between a and i anymore and thus widening does not restrict a due to a threshold on i.
        // If RND is not defined outside the loop it gets introduced inside the loop and then at the join of the loop
        // header we join it with TOP, thus having an unbounded value for it. When we later test it in the if-branch it
        // will get wrapped and thus set to boolean TOP=[0, 1]. This renders the one state in the phase domain not to be bottom
        // anymore when entering one the if-branches which leads to the spill-over because of the non-separable states.
        // NOTE: Having the variable defined as below we actually have a slightly different program than the one in the paper as seen when
        // compared to the other test with octagons.
        // It moves the RND variable out of the loop thus changing the semantics slightly. The loop now executes always the same if-branch
        // although not sure which. Out analyzer can find the invariant for this as it separates the different
        // if-branches inside the loop completely.
        "mov.1 RND, [0, 1]",
        "loop:",
        "  assert i = [1, 101]",
        "  assert a = [-100, 100]",
        "  cmplts LTS, m, i",
        "  brc LTS, loop_end:",
        "  brc RND, else:",
        "  then:",
        "    add a, a, 1",
        "    br if_end:",
        "  else:",
        "    sub a, a, 1",
        "  if_end:",
        "  add i, i, 1",
        "  br loop:",
        "loop_end:",
        "assert i = 101",
        "assert a = [-100, 100]",
        "assert a <= m",
        "mul mm, m, -1",
        "assert mm <= a",
        "halt");
    AnalysisFactory analyzer = new AnalysisFactory()
      .disableDomains("Heap", "DelayedWidening")
      .enableDomains("DelayedWidening(Thresholds)", "Phased");
    evaluateAssertionsDebug(assembly, analyzer);
  }

  @Test public void randomWalkUnbounded () {
    //    int m = ?;
    //    int a = 0;
    //    int i = 1;
    //    while (i <= m) {
    //      if (?)
    //        a++;
    //      else
    //        a--;
    //      i++;
    //    }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov m, ?",
        "mov a, 0",
        "mov i, 1",
        // Line below is not needed with octagons. See comments in test above for why this line is need when not using the octagons.
//        "mov.1 RND, [0, 1]",
        "loop:",
        // using 64 bits here to avoid wrapping for the unbounded variable "m"; otherwise the invariant "i <= m+1" cannot be verified
        "  convert.q.d mq, m",
        "  convert.q.d iq, i",
        "  convert.q.d aq, a",
        "  add.q minc, mq, 1",
        "  assert.q iq <= minc",
        // assert a = [-i+1, i-1] NOTE: for some reason we lose this invariant at about the 8th iteration during widening
//        "  assert.q aq < iq",
//        "  mul.q miq, iq, -1",
//        "  assert.q iq < aq",
        "  cmplts LTS, m, i",
        "  brc LTS, loop_end:",
        "  brc RND, else:",
        "  then:",
        "    add a, a, 1",
        "    br if_end:",
        "  else:",
        "    sub a, a, 1",
        "  if_end:",
        "  add i, i, 1",
        "  br loop:",
        "loop_end:",
//        "assert i = 101",
//        "assert a = [-100, 100]",
//        "assert a <= m",
//        "mul mm, m, -1",
//        "assert mm <= a",
        "halt");
    AnalysisFactory analyzer = new AnalysisFactory()
    .disableDomains("Heap")
    .disableDomains("PointsTo") // with PointsTo but without Congruences it does not terminate!??!
    .disableDomains("Predicates(Z)")
    .disableDomains("RedundantAffine")
    .disableDomains("Congruences")
    .disableDomains("Intervals")
    .enableDomains("Affine")
    .enableDomains("Apron(Octagons)");
    evaluateAssertionsDebug(assembly, analyzer);
  }

  /**
   * Taken from "F. Logozzo - Subpolyhedra: A (More) scalable approach to infer linear inequalities". Fig. 2
   */
  @Test public void subpolyTest1 () {
//    void Foo(int i, int j) {
//      int x = i, y = j;
//      if (x <= 0)
//        return;
//      while (x > 0) {
//        x--;
//        y--;
//      }
//      if (y == 0)
//        Assert(i == j);
//    }
  }


  /**
   * Taken from "F. Logozzo - Subpolyhedra: A (More) scalable approach to infer linear inequalities". Fig. 3
   */
  @Test public void subpolyTest2 () {
//  int x = 0, y = 0, w = 0, z = 0;
//  while (...) {
//    if (...) {
//      x++; y += 100;
//    } else if (...) {
//      if (x >= 4) {
//        x++; y++;
//      }
//    } else if (y > 10 * w && z >= 100 * x) {
//      y = -y;
//    }
//    w++; z += 10;
//  }
//  if (x >= 4 && y <= 2)
//    Assert(false);
    // Loop invariant: x ≤ y ≤ 100·x ∧ z = 10·w
    // TODO: we should probably not be able to prove this but lets see. The problem is if we can
    // relate vars with different induction strength. How about this: while(?) {i++; y+=100;}
    // can we infer the y ≤ 100·x invariant? Nope, probably but I should try an example with a bounded loop
    // and see what the congruences and affine domain can infer there after widening.
  }

  /**
   * Taken from "F. Logozzo - Subpolyhedra: A (More) scalable approach to infer linear inequalities". Fig. 5a
   */
  @Test public void subpolyTest3 () {
//  if (...) {
//    assume x == 3 * y;
//  } else {
//    x = 0;
//    y = 1;
//  }
  }

  /**
   * Taken from "F. Logozzo - Subpolyhedra: A (More) scalable approach to infer linear inequalities". Fig. 5b
   */
  @Test public void subpolyTest4 () {
//    i = k;
//    while(...)
//      i++;
//    assert i >= k;
  }

  /**
   * Taken from "F. Logozzo - Subpolyhedra: A (More) scalable approach to infer linear inequalities". Fig. 1
   */
  @Test public void subpolyTest5 () {
//    class StringBuilder {
//      int ChunkLen; char[] ChunkChars;
//      public void Append(int wb, int count) {
//        Contract.Requires(wb >= 2 * count);
//        if (count + ChunkLen > ChunkChars.Length)
//          CopyChars(wb, ChunkChars.Length - ChunkLen);
//        // ...
//      }
//      private void CopyChars(int wb, int len) {
//        Contract.Requires(wb >= 2 * len);
//        // ...
//      }
//    }
  }

  /**
   * Taken from "F. Logozzo - Subpolyhedra: A (More) scalable approach to infer linear inequalities". Fig. 4a
   */
  @Test public void subpolyTest6 () {
//    if (...) {
//      assume x - y <= 0;
//    } else {
//      assume x - y <= 5;
//    }
//    assert x - y <= 5;
    // NOTE: we could prove this invariant with our predicates(flags) domain if we would either
    // a) be lucky and have both assumptions tests be assigned to the same flag, e.g. "f = 1 -> x -y ..."
    // then we would test if there is a syntactic entailment at the join and keep the weaker fact "x - y <= 5"
    // or
    // b) we would reduce the predicates domain after each test and thus keep "true -> x -y ..."
    // in both branches (this would keep the true facts always) which on join would then always
    // try the syntactic/semantic entailment and not lose true facts only because we join a flag variable
    // which is defined as true/false with a branch where this variable is not defined. On such a join we lose
    // the fact as it is now only true as soon as we test the flag again.
  }

  /**
   * Taken from "F. Logozzo - Subpolyhedra: A (More) scalable approach to infer linear inequalities". Fig. 4b
   */
  @Test public void subpolyTest7 () {
//    if(...) {
//      assume x == y;
//      assume y <= z;
//    } else {
//      assume x <= y;
//      assume y == z;
//    }
//    assert x <= y;
//    assert y <= z;
    // NOTE: see first the comments for the 4a testcase above. Here the luckily assign things to the same flag
    // will be even less likely to work. So we need the same idea to track facts in the predicate domain as
    // true -> bla
  }

  /**
   * Taken from "M. Karr - Affine Relationships Among Variables of a Program". Fig. 4
   */
  @Test public void affineTest1 () {
//    if (?) {
//      m = n + 1;
//      :join1
//      m = m - 2;
//      goto join2;
//    } else {
//      n = m + 1;
//      :join2
//      n = n - 2;
//      goto join1;
//    }
  }

  /**
   * Taken from "M. Karr - Affine Relationships Among Variables of a Program". Fig. 5
   */
  @Test public void affineTest2 () {
//    i = 2;
//    j = k + 5;
//    while (?) {
//      i = i + 1;                // j = k + 3i - 1
//      j = j + 3;                // j = k + 3i + 2
//    }
  }

  /**
   * Taken from "A. Miné - Symbolic Methods to Enhance the Precision of Numerical Abstract Domains". Fig. 1
   */
  @Test public void affineTest3 () {
//    X ←[−10, 20];
//    Y ←X;
//    if (Y ≤ 0)
//      Y ←−X;
//    // here, Y ∈ [0, 20]
  }

  /**
   * Taken from "Simon - Widening Polyhedra with Landmarks". Fig. 4
   */
  @Test public void landmarksPolyhedra () {
    //  int i = 1;
    //  for (int y = 1; y < 8; y = y * 2)
    //    i++;
    // TODO: try with polyhedra
  }

  /**
   * Taken from "Amato - Localizing widening and narrowing". Fig. 3
   */
  @Test public void localizedHybrid () {
    //  int i = 0;
    //  while (TRUE) {
    //    i = i + 1;
    //    int j = 0;
    //    while (j < 10) { // Inv: 0 ≤ i ≤ 10
    //      j = j + 1;
    //    }
    //    if (i > 9)
    //      i = 0;
    //  }
    // TODO: try with polyhedra
  }

  /**
   * Taken from "Amato - Localizing widening and narrowing". Fig. 3
   */
  @Test public void localizedHH () {
    //  int i = 0;
    //  while (i < 4) {
    //    int j = 0;
    //    while (j < 4) { // Inv: i ≤ j + 3
    //      i = i + 1;
    //      j = j + 1;
    //    }
    //    i = i - j + 1;
    //  }
    // TODO: try with polyhedra
  }

  /**
   * Taken from "Amato - Localizing widening and narrowing". Fig. 3
   */
  @Test public void localizedNested2 () {
    //  int i = 0;
    //  while (TRUE) { // Inv: i ≥ 0
    //    int j = 0;
    //    while (j < 10) {
    //      j = j + 1;
    //    }
    //    i = i + 11 - j;
    //  }
    // TODO: try with polyhedra
  }

  /**
   * Taken from "Blanchet - Design and Implementation of a Special-Purpose Static Program Analyzer ...". Fig. 6.7
   */
  @Test public void relations () {
  //  boolean b = ?;
  //  int i = 0;
  //  int x = 0;
  //  while (i < 100) {
  //    x = x + 1;
  //    if (b)
  //      x = 0;
  //    i = i + 1;
  //  }
    // TODO: try with affine and then polyhedra
  }

}
