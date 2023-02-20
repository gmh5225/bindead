package bindead.domains.affine;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.num;
import static bindead.data.Linear.term;
import static bindead.debug.DebugHelper.logln;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import bindead.data.NumVar;
import bindead.domains.affine.Equations.AffineHullResult;

public class EquationsTest {
  private static HashMap<Integer, NumVar> intToVar = new HashMap<>();
  private static final NumVar s = NumVar.fresh("s");
  private static final NumVar t = NumVar.fresh("t");
  private static final NumVar u = NumVar.fresh("u");
  private static final Equations p1 = new Equations(intToVar,
    linear(num(-25), term(num(5), s)).toEquality(),
    linear(num(-7), term(num(1), t)).toEquality(),
    linear(num(-3), term(num(1), u)).toEquality());
  private static final Equations p2 = new Equations(intToVar,
    linear(num(-3), term(num(1), s)).toEquality(),
    linear(num(-5), term(num(1), t)).toEquality(),
    linear(num(-8), term(num(1), u)).toEquality());
  private static final Equations p2_ = new Equations(intToVar,
    linear(num(-3), term(num(1), s)).toEquality(),
    linear(num(-7), term(num(1), t)).toEquality(),
    linear(num(-8), term(num(1), u)).toEquality());
  private static final Equations p12Res = new Equations(intToVar,
    linear(num(-31), term(num(5), s), term(num(2), u)).toEquality(),
    linear(num(-41), term(num(5), t), term(num(2), u)).toEquality());
  private static final Equations p12_Res = new Equations(intToVar,
    linear(num(-31), term(num(5), s), term(num(2), u)).toEquality(),
    linear(num(-7), term(num(1), t)).toEquality());
  private static final Equations p1Res = new Equations(intToVar,
    linear(num(-3), term(num(1), u)).toEquality());
  private static final Equations p2Res = new Equations(intToVar,
    linear(num(-8), term(num(1), u)).toEquality());

  @Before public void precondition () {
    // enable or disable the debug output
//    DebugHelper.analysisKnobs.printLogging();
  }

  /**
   * Tests the affine hull of different points.
   */
  @Test public void testAffineHull001 () {
    logln("calculating affine hull of\n" + p1 + "and\n" + p2);
    AffineHullResult res = Equations.affineHull(p1, p2);
    logln("result:\n" + res.toString());
    assertThat(res.common, is(p12Res));
    assertThat(res.onlyFst, is(p1Res));
    assertThat(res.onlySnd, is(p2Res));
  }

  /**
   * Tests the affine hull of different points.
   */
  @Test public void testAffineHull002 () {
    logln("calculating affine hull of\n" + p1 + "and\n" + p2_);
    AffineHullResult res = Equations.affineHull(p1, p2_);
    logln("result:\n" + res.toString());
    assertThat(res.common, is(p12_Res));
    assertThat(res.onlyFst, is(p1Res));
    assertThat(res.onlySnd, is(p2Res));
  }

  /**
   * Tests the affine hull of two lines.
   */
  @Test public void testAffineHull003 () {
    Equations l1 = new Equations(intToVar,
        linear(num(0), term(num(1), s), term(num(-1), t)).toEquality());
    Equations l2 = new Equations(intToVar,
        linear(num(0), term(num(1), s), term(num(1), t)).toEquality());
    Equations empty = new Equations(0);
    logln("calculating affine hull of\n" + l1 + "and\n" + l2);
    AffineHullResult res = Equations.affineHull(l1, l2);
    logln("result:\n" + res.toString());
    assertThat(res.common, is(empty));
    assertThat(res.onlyFst, is(l1));
    assertThat(res.onlySnd, is(l2));
  }

  /**
   * Tests the affine hull of two lines.
   */
  @Test public void testAffineHull004 () {
    Equations l1 = new Equations(intToVar,
        linear(num(0), term(num(1), s), term(num(-1), t)).toEquality());
    Equations l2 = new Equations(intToVar,
        linear(num(0), term(num(1), s), term(num(-1), t), term(num(1), u)).toEquality());
    Equations empty = new Equations(0);
    logln("calculating affine hull of\n" + l1 + "and\n" + l2);
    AffineHullResult res = Equations.affineHull(l1, l2);
    logln("result:\n" + res.toString());
    assertThat(res.common, is(empty));
    assertThat(res.onlyFst, is(l1));
    assertThat(res.onlySnd, is(l2));
  }

  /**
   * Tests the affine hull of different points.
   */
  @Test public void testAffineHull005 () {
    Equations p1 = new Equations(intToVar,
        linear(num(-5), term(num(1), s)).toEquality(),
        linear(num(-10), term(num(1), t)).toEquality());
    Equations p2 = new Equations(intToVar,
        linear(num(-1), term(num(1), s)).toEquality(),
        linear(num(-9), term(num(1), t)).toEquality());
    Equations line = new Equations(intToVar,
        linear(num(35), term(num(1), s), term(num(-4), t)).toEquality());
    // TODO: not sure where this is coming from?
    Equations p1_only = new Equations(intToVar, linear(num(-10), term(num(1), t)).toEquality());
    Equations p2_only = new Equations(intToVar, linear(num(-9), term(num(1), t)).toEquality());
    logln("calculating affine hull of\n" + p1 + "and\n" + p2);
    AffineHullResult res = Equations.affineHull(p1, p2);
    logln("result:\n" + res.toString());
    assertThat(res.common, is(line));
    assertThat(res.onlyFst, is(p1_only));
    assertThat(res.onlySnd, is(p2_only));
  }

}