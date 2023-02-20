package bindead.domains.affine;

import static bindead.TestsHelper.evaluateAssertions;
import static bindead.TestsHelper.lines;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;

import org.junit.Test;

import rreil.lang.RReilAddr;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.data.Linear;

/**
 * Perform some simple operations on the affine domain.
 */
public class AffineTest {
  private final static AnalysisFactory analyzer = new AnalysisFactory();

  @Test public void test001 () {
    String assembly =
      "mov.d u, 1\n" +
        "mov.d v, 3\n" +
        "mov.1 SomeFlag, ?\n" +
        "brc.d SomeFlag, noSet:\n" +
        "add.d u, u, 5\n" +
        "mov.d v, 7\n" +
        "noSet:" +
        "mov.d y, u\n" +
        "exit:" +
        "mov.d r0, ?\n" +
        "halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(8), "y", 32, 0), is(Option.some(Interval.of(1, 6))));
    assertThat(analysis.query(RReilAddr.valueOf(8), "v", 32, 0), is(Option.some(Interval.of(3, 7))));
  }

  @Test public void test007 () {
    String assembly =
      "mov.d x1, [1, 2]\n" +
        "mov.d x2, [0, 100]\n" +
        "add.d x3, x1, x2\n" +
        "exit:\n" +
        "mov.d ret, ?\n" +
        "halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(4), "x3", 32, 0), is(Option.some(Interval.of(1, 102))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "x2", 32, 0), is(Option.some(Interval.of(0, 100))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "x1", 32, 0), is(Option.some(Interval.of(1, 2))));
  }

  @Test public void test008 () {
    String assembly =
      "mov.d x2, [1, 2]\n" +
        "mov.d x1, [0, 100]\n" +
        "add.d x3, x1, x2\n" +
        "exit:\n" +
        "mov.d ret, ?\n" +
        "halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(4), "x3", 32, 0), is(Option.some(Interval.of(1, 102))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "x1", 32, 0), is(Option.some(Interval.of(0, 100))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "x2", 32, 0), is(Option.some(Interval.of(1, 2))));
  }


  @Test public void test009 () {
    String assembly =
      "mov.d x1, [1, 2]\n" +
        "mov.d x2, [2, 3]\n" +
        "mov.d x2, x1\n" +
        "exit:\n" +
        "mov.d ret, ?\n" +
        "halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(4), "x2", 32, 0), is(Option.some(Interval.of(1, 2))));
    assertThat(analysis.query(RReilAddr.valueOf(4), "x1", 32, 0), is(Option.some(Interval.of(1, 2))));
  }

  /**
   * Test the join of variables with exchanged values.
   */
  @Test public void testJoinWithRedundantIntervals () {
    String assembly =
      "brc.d FLAG, branch2: " + // FLAG unknown
        "branch1: " +
        "mov.d x1, 0 " +
        "mov.d x2, 1 " +
        "brc.d 1, join: " +
        "branch2: " +
        "mov.d x1, 1 " +
        "mov.d x2, 0 " +
        "brc.d 1, join: " +
        "nop " +
        "join: " +
        "halt";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(8), "x1", 32, 0), is(Option.some(Interval.of(0, 1))));
  }

  @Test public void linearMulAddTest () {
    ZenoFactory zeno = ZenoFactory.getInstance();
    Rlin minusOne = zeno.literal(Bound.MINUSONE);
    Rlin ffffffff = zeno.literal(BigInt.of(0xffffffffl)); // all ones set in 32bit = +4 294 967 295 or -1 if signed
    Linear res = ffffffff.add(minusOne).getLinearTerm();
    assertThat(res.getConstant(), is(BigInt.of(0xfffffffel)));
  }

  /**
   * Tests the subtraction of equal variables from each other where the variable values are TOP.
   */
  @Test public void testSubtraction () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, ?",
        "mov y, x",
        "sub x, y, x",
        "assert x = 0",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Bug in RedundandAffine, x = f after widening but was not reduced in intervals
   * as the transitive closure on test application was missing.
   */
  @Test public void affineReduction () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov counter, 0",
        "mov x, 0",
        "mov f, 0",
        "loop1:",
        "assert counter = [0, 2]",
        "assert x = [0, 2]",
        "assert f = [0, 2]",
        "cmpltu LT, counter, 2",
        "xor.1 GE, LT, 1",
        "brc GE, exit_loop1:",
        "  add counter, counter, 1",
        "  add x, x, 1",
        "  add f, f, 1",
        "br loop1:",
        "exit_loop1:",
        "assert counter = 2",
        "assert x = 2",
        "assert f = 2",
        "exit:",
        "halt ");
    evaluateAssertions(assembly);
  }

}