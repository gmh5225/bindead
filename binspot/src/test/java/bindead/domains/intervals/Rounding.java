package bindead.domains.intervals;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import javalx.numeric.BigInt;
import javalx.numeric.Interval;

import org.junit.Test;

import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.analyses.DomainFactory;
import bindead.data.NumVar;
import bindead.domainnetwork.interfaces.FiniteDomain;

/**
 * Test the rounding in division for the Intervals numeric domain and the Interval datatype.
 *
 * @author Bogdan Mihaila
 */
@SuppressWarnings("rawtypes")
public class Rounding {
  private static final FiniteDomain top = DomainFactory.parseFiniteDomain("Wrapping Affine Congruences Intervals");
  private static final NumVar x1 = NumVar.fresh("x1");
  private static final NumVar x2 = NumVar.fresh("x2");
  private static final NumVar x3 = NumVar.fresh("x3");

  /**
   * Return the TOP domain value for 32 bit variables.
   */
  private static RichFiniteDomain getTop () {
    return FiniteDomainHelper.for32bitVars(top);
  }

  private static Interval val (long value) {
    return Interval.of(value);
  }

  private static Interval val (long lower, long upper) {
    return Interval.of(lower, upper);
  }

  private static Interval divZero (Interval dividend, Interval divisor) {
    return dividend.divRoundZero(divisor);
  }

  private static Interval divInt (Interval dividend, int divisor) {
    return dividend.divRoundInvards(BigInt.of(divisor));
  }

  @Test public void divisionInDatatype () {
    // rounding to zero (C-semantics)
    // 10 / 3 = 3
    Interval result1 = divZero(val(10), val(3));
    assertThat(result1, is(val(3)));
    // [9, 10] / 3 = 3
    Interval result2 = divZero(val(9, 10), val(3));
    assertThat(result2, is(val(3)));
    // 10 / [2, 3] = [3, 5]
    Interval result3 = divZero(val(10), val(2, 3));
    assertThat(result3, is(val(3, 5)));
    // [9, 10] / [2, 3] = [3, 5]
    Interval result4 = divZero(val(9, 10), val(2, 3));
    assertThat(result4, is(val(3, 5)));

    // rounding inwards to integer solutions
    //  [10, 10] / 3 = [3.333, 3.333] -> null as there are no integer solutions in the interval
    Interval result5 = divInt(val(10), 3);
    assertThat(result5, is(nullValue()));
    //  [10, 11] / 3 = [3.333, 3.666] -> null as there are no integer solutions in the interval
    Interval result6 = divInt(val(10, 11), 3);
    assertThat(result6, is(nullValue()));
    // [9, 10] / 3 = 3
    Interval result7 = divInt(val(9, 10), 3);
    assertThat(result7, is(val(3)));
    // [9, 15] / 4 = [2.25, 3.75] -> [3, 3]
    Interval result8 = divInt(val(9, 15), 4);
    assertThat(result8, is(val(3)));
    // [9, 10] / 1 = [9, 10]
    Interval result9 = divInt(val(9, 10), 1);
    assertThat(result9, is(val(9, 10)));
    // [9, 10] / -1 = [-10, -9]
    Interval result10 = divInt(val(9, 10), -1);
    assertThat(result10, is(val(-10, -9)));
  }

  @Test public void divisionInDomain () {
    // 10 / 3 = 3
    RichFiniteDomain d1 = getTop();
    d1 = d1.introduce(x1, val(10));
    d1 = d1.introduce(x2, val(3));
    d1 = d1.assignDivision(x3, x1, x2);
    d1.assertValueIs(x3, val(3));
    // [9, 10] / 3 = 3
    RichFiniteDomain d2 = getTop();
    d2 = d2.introduce(x1, val(9, 10));
    d2 = d2.introduce(x2, val(3));
    d2 = d2.assignDivision(x3, x1, x2);
    d2.assertValueIs(x3, val(3));
    // 10 / [2, 3] = [3, 5]
    RichFiniteDomain d3 = getTop();
    d3 = d3.introduce(x1, val(10));
    d3 = d3.introduce(x2, val(2, 3));
    d3 = d3.assignDivision(x3, x1, x2);
    d3.assertValueIs(x3, val(3, 5));
    // [9, 10] / [2, 3] = [3, 5]
    RichFiniteDomain d4 = getTop();
    d4 = d4.introduce(x1, val(9, 10));
    d4 = d4.introduce(x2, val(2, 3));
    d4 = d4.assignDivision(x3, x1, x2);
    d4.assertValueIs(x3, val(3, 5));
  }

  @Test public void divisionByZeroInDatatype () {
    // in ARM semantics a division by zero can yield zero
    // 10 / 0 = 0
    Interval result1 = divZero(val(10), val(0));
    assertThat(result1, is(val(0)));
    // [9, 10] / 0 = 0
    Interval result2 = divZero(val(9, 10), val(0));
    assertThat(result2, is(val(0)));
    // 10 / [-2, 2] = [-10, 10]
    Interval result3 = divZero(val(10), val(-2, 2));
    assertThat(result3, is(val(-10, 10)));
    // [9, 10] / [-2, 2] = [-10, 10]
    Interval result4 = divZero(val(9, 10), val(-2, 2));
    assertThat(result4, is(val(-10, 10)));

    // 10 / 0 = 0
    Interval result5 = divInt(val(10), 0);
    assertThat(result5, is(val(0)));
    // [9, 10] / 0 = 0
    Interval result6 = divInt(val(9, 10), 0);
    assertThat(result6, is(val(0)));
  }

  @Test public void divisionByZeroInDomain () {
    // 10 / 0 = 0
    RichFiniteDomain d1 = getTop();
    d1 = d1.introduce(x1, val(10));
    d1 = d1.introduce(x2, val(0));
    d1 = d1.assignDivision(x3, x1, x2);
    d1.assertValueIs(x3, val(0));
    // [9, 10] / 0 = 0
    RichFiniteDomain d2 = getTop();
    d2 = d2.introduce(x1, val(9, 10));
    d2 = d2.introduce(x2, val(0));
    d2 = d2.assignDivision(x3, x1, x2);
    d2.assertValueIs(x3, val(0));
    // 10 / [-2, 2] = [-10, 10]
    RichFiniteDomain d3 = getTop();
    d3 = d3.introduce(x1, val(10));
    d3 = d3.introduce(x2, val(-2, 2));
    d3 = d3.assignDivision(x3, x1, x2);
    d3.assertValueIs(x3, val(-10, 10));
    // [9, 10] / [-2, 2] = [-10, 10]
    RichFiniteDomain d4 = getTop();
    d4 = d4.introduce(x1, val(9, 10));
    d4 = d4.introduce(x2, val(-2, 2));
    d4 = d4.assignDivision(x3, x1, x2);
    d4.assertValueIs(x3, val(-10, 10));
  }

}
