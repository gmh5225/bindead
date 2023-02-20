package bindead.domains;

import static javalx.data.Option.some;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.numeric.Interval;

import org.junit.Ignore;
import org.junit.Test;

import rreil.lang.RReilAddr;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.analyses.algorithms.data.CallString;
import bindead.domains.affine.AffineProperties;
import bindead.domains.intervals.IntervalProperties;
import bindead.domains.syntacticstripes.StripeProperties;

@Ignore("The Arrays domain is not in a functional state. (That is, it doesn't exist.)")
// The tests here are kept for reference when a new arrays domain is to be implemented
public class Arrays {
  private final static AnalysisFactory analyzer = new AnalysisFactory(
      "Root Arrays Fields Predicate Undef PointsTo Wrapping DelayedWidening ThresholdsWidening Affine Intervals");

  public Arrays () {
    StripeProperties.INSTANCE.debugTests.setValue(true);
    StripeProperties.INSTANCE.debugAssignments.setValue(true);
    StripeProperties.INSTANCE.debugBinaryOperations.setValue(false);
    StripeProperties.INSTANCE.debugSubsetOrEqual.setValue(true);
    StripeProperties.INSTANCE.debugWidening.setValue(true);
    StripeProperties.INSTANCE.debugOther.setValue(true);
    AffineProperties.INSTANCE.debugSubsetOrEqual.setValue(true);
    IntervalProperties.INSTANCE.debugSubsetOrEqual.setValue(true);
    AnalysisProperties.INSTANCE.debugAssignments.setValue(true);
  }

  @Test public void test001 () {
    String assembly =
        "mov.d i, [1, 2] " +
        "mov.d p, [1, 2] " +
        "cmpeq.d f, i, p " +
        "brc.d f, offset: " +
        "halt\n" +
        "offset: sub.d o, i, p " +
        "halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(6), "o", 32, 0), is(some(Interval.of(0))));
    assertThat(analysis.query(RReilAddr.valueOf(6), "i", 32, 0), is(some(Interval.of(1, 2))));
    assertThat(analysis.query(RReilAddr.valueOf(6), "p", 32, 0), is(some(Interval.of(1, 2))));
  }

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
        "  xorb.1 neq, eq, 1" +
        "  brc.d neq, loop:" +
        "exit: halt";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(0x07), "t1", 32, 0), is(some(Interval.of(4, 20))));
  }

  @Test public void test013b () {
    String assembly =
        "mov.d q, sp " +
        "mov.d n, 6 " +
        "mov.d i, 0 " +
        "loop: " +
        "  add.d t, q, i " +
        "  store.d.b t, 0" +
        "  add.d i, i, 1 " +
        "  cmpeq.d eq, i, n" +
        "  xorb.1 neq, eq, 1" +
        "  brc.d neq, loop:" +
        "exit: halt";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    //assertThat(analysis.query(RReilAddr.valueOf(0x07), "t1", 32, 0), is(some(Interval.valueOf(4, 20))));
    System.out.println(analysis.getState(CallString.root(), RReilAddr.valueOf(4)));
    System.out.println(analysis.getState(CallString.root(), RReilAddr.valueOf(8)));
    System.out.println(analysis.getState(CallString.root(), RReilAddr.valueOf(9)));
  }

  @Test public void test013b_no_increment () {
    String assembly =
        "mov.d q, sp " +
        "mov.d n, 6 " +
        "mov.d i, 0 " +
        "loop: " +
        "  add.d t, q, i " +
        "  store.d.b t, 0" +
        "  cmpeq.d eq, i, n" +
        "  xorb.1 neq, eq, 1" +
        "  brc.d neq, loop:" +
        "exit: halt";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    //assertThat(analysis.query(RReilAddr.valueOf(0x07), "t1", 32, 0), is(some(Interval.valueOf(4, 20))));
    System.out.println(analysis.getState(CallString.root(), RReilAddr.valueOf(3)));
  }
}
