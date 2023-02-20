package bindead.domains;

import static javalx.data.Option.some;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.numeric.Interval;


import org.junit.Test;

import rreil.lang.RReilAddr;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;

/**
 * Some test for overlapping fields in the memory domain.
 */
public class OverlappingFields {
  private final static AnalysisFactory analyzer = new AnalysisFactory();

  @Test public void test001 () {
    String assembly =
        "mov.q rax, 0\n" +
        "mov.d rax/0, [0, 32]\n" +
        "mov.d rax/32, 0\n" +
        "mov.q rbx, rax\n" +
        "exit: halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(4), "rbx", 64, 0), is(some(Interval.of(0, 32))));
  }

  @Test public void test002 () {
    String assembly =
        "mov.q rax, 0\n" +
        "mov.d rax/32, [0, 32]\n" +
        "mov.d rax/0, 0\n" +
        "mov.q rbx, rax\n" +
        "exit: halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(4), "rbx", 64, 0), is(some(Interval.of(0, 137438953472l))));
  }

  @Test public void test003 () {
    String assembly =
        "mov.q rax, [0, 32]\n" +
        "mov.d rbx, rax\n" +
        "exit: halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(2), "rbx", 32, 0), is(some(Interval.of(0, 32))));
  }

  @Test public void test004 () {
    String assembly =
        "mov.d rax/32, [0, 32]\n" +
        "mov.d rax/0, [32, 64]\n" +
        "mov.w bx, rax\n" +
        "exit: halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertThat(analysis.query(RReilAddr.valueOf(3), "bx", 16, 0), is(some(Interval.of(32, 64))));
  }
}
