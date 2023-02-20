package bindead.domains;

import static bindead.TestsHelper.evaluateAssertions;
import static bindead.TestsHelper.lines;
import static javalx.data.Option.some;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.data.Option;
import javalx.numeric.Interval;

import org.junit.Ignore;
import org.junit.Test;

import rreil.lang.RReilAddr;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;
import bindead.domains.widening.delayed.DelayedWideningProperties;
import bindead.domains.widening.thresholds.ThresholdsWideningProperties;

/**
 * Test for the now defunct and obsolete Stripes domain.
 * Still useful to have them around to test the numeric domains stack.
 */
public class Stripes {
  private final static AnalysisFactory analyzer = new AnalysisFactory();

  /**
   * With the old Affine domain and our Intervals this test does not work.
   * One variable would be projected out and is represented by the two others.
   * In the case of a sum one variable would be represented by subtracting the other
   * from the sum and thus being much less precise than its actual value due to the imprecise
   * interval arithmetic.
   * The new redundant Affine keeps the variables in the child so it does not lose precision here.
   */
  @Test public void canonicalThreeVariablesTest () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x1, [1, 2]",
        "mov x2, [0, 100]",
        "add x3, x1, x2",
        "assert x1 = [1, 2]",
        "assert x2 = [0, 100]",
        "assert x3 = [1, 102]",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void test001 () {
    String assembly = lines(
        "mov.d u, 1",
        "mov.d v, 3",
        "mov.1 SomeFlag, ?",
        "brc.d SomeFlag, noSet:",
        "add.d u, u, 5",
        "mov.d v, 7",
        "noSet:",
        "mov.d y, u",
        "halt ");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void test002 () {
    String assembly =
      "mov.d eax, 0 " +
        "loop:" +
        "mul.d t0, eax, 4 " +
        "add.d t1, sp, t0 " +
        "store.d.d t1, 0xf00 " +
        "add.d eax, eax, 1 " +
        "cmplts.d LT, eax, 10 " +
        "brc.d LT, loop: " +
        "halt ";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(7), "eax", 32, 0), is(Option.some(Interval.of(10))));
  }

  @Test public void test003 () {
    String assembly =
      "mov.d r1, 0x0 " +
        "mov.d r2, 0x10 " +
        "mov.d r3, 0x0 " +
        "loop: " +
        "add.d r3, r3, r1 " +
        "add.d r1, r1, 0x1 " +
        "cmples.d f1, r1, r2 " +
        "brc.d f1, loop: " +
        "exit: halt ";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(3), "r1", 32, 0), is(some(Interval.of(0, 16))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "r1", 32, 0), is(some(Interval.of(17, 17))));
  }

  @Test public void test003b () {
    String assembly =
      "mov.d x, 0x0 " +
        "mov.d y, 0x10 " +
        "mov.d z, 0x0 " +
        "loop: " +
        "add.d z, z, x " +
        "add.d x, x, 0x1 " +
        "cmples.d f, x, y " +
        "brc.d f, loop: " +
        "exit: halt ";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(3), "x", 32, 0), is(some(Interval.of(0, 16))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "x", 32, 0), is(some(Interval.of(17, 17))));
  }

  @Test public void test005a () {
    String assembly = lines(
        "mov.d eax, 0",
        "loop:",
        "assert.d eax = [0, 36]",
        "add.d t0, eax, sp",
        "add.d eax, eax, 4",
        "cmplts.d LT, eax, 40",
        "brc.d LT, loop:",
        "exit:",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void test005b () {
    String assembly = lines(
        "mov.d eax, 0",
        "mov.d ebx, 0",
        "loop:",
        "assert.d eax = [0, 36]",
        "add.d t0, eax, ebx",
        "add.d eax, eax, 4",
        "cmplts.d LT, eax, 40",
        "brc.d LT, loop:",
        "exit:",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void test009 () {
    String assembly =
      "mov.d x1, [1, 2] " +
        "mov.d x2, [2, 3] " +
        "mov.d x2, x1 " +
        "exit: " +
        "mov.d ret, ? " +
        "halt ";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(4), "x2", 32, 0), is(some(Interval.of(1, 2))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "x1", 32, 0), is(some(Interval.of(1, 2))));
  }

  @Test public void test010a () {
    String assembly =
      "mov.d x, [1, 2] " +
        "mov.d y, [0, 100] " +
        "add.d z, y, x " +
        "add.d x, x, y " +
        "exit:" +
        "halt ";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(4), "x", 32, 0), is(some(Interval.of(1, 102))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "y", 32, 0), is(some(Interval.of(0, 100))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "z", 32, 0), is(some(Interval.of(1, 102))));
  }

  @Test public void test010b () {
    String assembly =
      "mov.d x, [1, 2] " +
        "mov.d y, [0, 100] " +
        "add.d z, x, y " +
        "add.d y, y, x " +
        "exit:" +
        "halt ";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(4), "x", 32, 0), is(some(Interval.of(1, 2))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "y", 32, 0), is(some(Interval.of(1, 102))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "z", 32, 0), is(some(Interval.of(1, 102))));
  }

  @Test public void test010c () {
    String assembly =
      "mov.d u, [3, 4] " +
        "mov.d v, [5, 6] " +
        "mov.d x, [1, 2] " +
        "mov.d y, [0, 100] " +
        "add.d v, u, y " +
        "add.d z, x, y " +
        "add.d y, y, z " +
        "exit:" +
        "halt ";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(7), "u", 32, 0), is(some(Interval.of(3, 4))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "v", 32, 0), is(some(Interval.of(3, 104))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "x", 32, 0), is(some(Interval.of(1, 2))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "y", 32, 0), is(some(Interval.of(1, 202))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "z", 32, 0), is(some(Interval.of(1, 102))));
  }

  @Test public void test010d () {
    String assembly =
      "mov.d u, [3, 4] " +
        "mov.d v, [5, 6] " +
        "mov.d x, [1, 2] " +
        "mov.d y, [0, 100] " +
        "add.d v, u, y " +
        "add.d z, x, y " +
        "add.d z, z, y " +
        "exit:" +
        "halt ";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
//    System.out.println(analysis.getState(CallString.root(), RReilAddr.valueOf(7)));
    assertThat(analysis.query(RReilAddr.valueOf(7), "u", 32, 0), is(some(Interval.of(3, 4))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "v", 32, 0), is(some(Interval.of(3, 104))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "x", 32, 0), is(some(Interval.of(1, 2))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "y", 32, 0), is(some(Interval.of(0, 100))));
    assertThat(analysis.query(RReilAddr.valueOf(7), "z", 32, 0), is(some(Interval.of(1, 202))));
  }

  @Test public void test011 () {
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
        "assert y = [5, 6]",
        "assert x = 10",
        "assert z = [10, +oo]",         // ! this can only be inferred with the Predicates(Z)
        "exit:",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    evaluateAssertions(analysis);
  }

  @Ignore// FIXME: investigate why the redundant affine loses precision here
  @Test public void test012 () {
    String assembly =
      "mov.d ebx, sp " +
        "mov.d esi, 6 " +
        "mov.d ecx, 1 " +
        "mov.d edx, 1 " +
        "loop:" +
        "  mul.d t0, ecx, 4" +
        "  add.d t1, t0, ebx" +
        "  load.d.d t2, t1" +
        "  add.d t3, eax, t2" +
        "  mov.d eax, t3" +
        "  mul.d t0, ecx, 4" +
        "  add.d t1, t0, ebx" +
        "  add.d t2, t1, -4" +
        "  load.d.d t3, t2" +
        "  add.d t4, eax, t3" +
        "  mov.d eax, t4" +
        "  add.d t0, edx, 1" +
        "  mov.d edx, t0" +
        "  mov.d ecx, edx" +
        "  cmpeq.d eq, edx, esi" +
        "  xor.1 neq, eq, 1" +
        "  brc.d neq, loop:" +
        "exit: halt";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(0x07), "t1", 32, 0), is(some(Interval.of(4, 20))));
    assertThat(analysis.query(RReilAddr.valueOf(0x0d), "t2", 32, 0), is(some(Interval.of(0, 16))));
  }

  @Ignore// FIXME: investigate why the redundant affine loses precision here
  @Test public void test013a () {
    String assembly =
      "mov.d ebx, sp " +
        "mov.d esi, 6 " +
        "mov.d ecx, 1 " +
        "mov.d edx, 1 " +
        "loop:" +
        "  mul.d t0, ecx, 4" +
        "  add.d t1, t0, ebx" +
        "  load.d.d t2, t1" +
        "  add.d t3, eax, t2" +
        "  mov.d eax, t3" +
        "  add.d t0, edx, 1" +
        "  mov.d edx, t0" +
        "  mov.d ecx, edx" +
        "  cmpeq.d eq, edx, esi" +
        "  xor.1 neq, eq, 1" +
        "  brc.d neq, loop:" +
        "exit: halt";
    DebugHelper.analysisKnobs.printCodeListing();
    DebugHelper.analysisKnobs.printInstructions();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();
    ThresholdsWideningProperties.INSTANCE.debugWidening.setValue(true);
    DelayedWideningProperties.INSTANCE.debugWidening.setValue(true);
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(0x07), "t1", 32, 0), is(some(Interval.of(4, 20))));
  }

  @Ignore// FIXME: investigate why the redundant affine loses precision here
  @Test public void test013b () {
    String assembly =
      "mov.d ebx, sp " +
        "mov.d esi, 6 " +
        "mov.d ecx, 1 " +
        "mov.d edx, 1 " +
        "loop:" +
        "  mul.d t0, ecx, 4" +
        "  add.d t1, t0, ebx" +
        "  load.d.d t2, t1" +
        "  add.d t3, eax, t2" +
        "  mov.d eax, t3" +
        "  add.d t0, edx, 1" +
        "  sub.d t1, 4294967295, edx " +
        "  mov.d edx, t0" +
        "  mov.d ecx, edx" +
        "  cmpeq.d eq, edx, esi" +
        "  xor.1 neq, eq, 1" +
        "  brc.d neq, loop:" +
        "exit: halt";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(0x07), "t1", 32, 0), is(some(Interval.of(4, 20))));
  }

  @Test public void testXXX () {
    String assembly = lines(
        "mov.d n, 6",
        "mov.d i, 0",
        "mov.1 e, 0",
        "loop:",
        "  assert.d i = [0, 5]",
        "  add.d i, i, 1",
        "  mov.1 e, 1",
        "  cmpeq.d eq, i, n",
        "  xor.1 neq, eq, 1",
        "  brc.d neq, loop:",
        "exit:",
        "assert.d i = 6",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }
}
