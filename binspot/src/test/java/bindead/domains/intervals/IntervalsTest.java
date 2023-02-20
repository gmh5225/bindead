package bindead.domains.intervals;

import static bindead.debug.DebugHelper.logln;
import javalx.numeric.Interval;

import org.junit.Test;

import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.analyses.DomainFactory;
import bindead.data.NumVar;
import bindead.domainnetwork.interfaces.FiniteDomain;

@SuppressWarnings({"rawtypes"})
public class IntervalsTest {
  private static final FiniteDomain top = DomainFactory.parseFiniteDomain("Wrapping Affine Congruences Intervals");
  private static final NumVar x1 = NumVar.fresh("x1");
  private static final NumVar x2 = NumVar.fresh("x2");

  /**
   * Return the TOP domain value for 32 bit variables.
   */
  private static RichFiniteDomain getTop () {
    return FiniteDomainHelper.for32bitVars(top);
  }

  @Test public void lowerEqual1 () {
    RichFiniteDomain d = getTop();
    d = d.introduce(x1);
    d = d.introduce(x2);
    d = d.assign(x1, Interval.of(1, 10));
    d = d.assign(x2, 9);
    logln("Initially:\n" + d);
    logln();
    logln("x1 <= x2");
    d = d.lessOrEqualTo(x1, x2);
    logln();
    logln(d);
    d.assertValueIs(x1, Interval.of(1, 9));
    d.assertValueIs(x2, 9);
  }

  @Test public void lowerEqual2 () {
    RichFiniteDomain d = getTop();
    d = d.introduce(x1);
    d = d.introduce(x2);
    d = d.assign(x1, Interval.of(1, 10));
    d = d.assign(x2, Interval.of(9, 10));
    logln("Initially:\n" + d);
    logln();
    logln("x1 <= x2");
    d = d.lessOrEqualTo(x1, x2);
    logln();
    logln(d);
    d.assertValueIs(x1, Interval.of(1, 10));
    d.assertValueIs(x2, Interval.of(9, 10));
  }

  @Test public void lowerEqual3 () {
    RichFiniteDomain d = getTop();
    d = d.introduce(x1);
    d = d.introduce(x2);
    d = d.assign(x1, Interval.of(-2147483648, -1));
    d = d.assign(x2, Interval.of(-2147483648, 2147483647));
    logln("Initially:\n" + d);
    logln();
    logln("x1 <= x2");
    d = d.lessOrEqualTo(x1, x2);
    logln();
    logln(d);
    d = d.assign(x1, Interval.of(-2147483648, -1));
    d = d.assign(x2, Interval.of(-2147483648, 2147483647));
  }

}
