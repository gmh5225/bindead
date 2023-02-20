package bindead.domains.affine;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.num;
import static bindead.data.Linear.term;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;


import org.junit.Test;

import bindead.data.Linear;
import bindead.data.Linear.Divisor;
import bindead.data.NumVar;
import bindead.domainnetwork.combinators.ZenoChildOp;

public class AffineStateBuilderTest {
  private static final NumVar x0 = NumVar.fresh("x0");
  private static final NumVar x1 = NumVar.fresh("x1");
  private static final NumVar x2 = NumVar.fresh("x2");
  private static final NumVar x3 = NumVar.fresh("x3");
  private static final NumVar x4 = NumVar.fresh("x4");
  private static final NumVar x5 = NumVar.fresh("x5");
  private static final AffineState system;

  static {
    AffineStateBuilder sys = new AffineStateBuilder(AffineState.EMPTY);
    sys.insertLinear(linear(Bound.ONE, term(num(3), x0), term(num(3), x3), term(num(1), x4)).toEquality());
    sys.insertLinear(linear(Bound.ONE, term(num(1), x1), term(num(2), x3), term(num(1), x4)).toEquality());
    sys.insertLinear(linear(Bound.ONE, term(num(2), x2), term(num(3), x3)).toEquality());
    system = sys.build();
  }

  /**
   * Test of inlineIntoLinear method, of class AffineCtxBuilder.
   */
  @Test public void testInlineIntoLinear () {
    System.out.println("original system:\n" + system);
    Linear orig = linear(num(4), term(num(6), x0), term(num(2), x1), term(num(4), x2), term(num(16), x3), term(num(4), x4));
    System.out.println("original equality: " + orig);
    Divisor d = Divisor.one();
    Linear inlined = system.inlineIntoLinear(orig, d);
    System.out.println("inlining system yields: (" + inlined + ")/" + d);
    assertThat(inlined.iterator().hasNext(), is(false));
    assertThat(inlined.getConstant(), is(BigInt.of(-2)));
    assertThat(d.get(), is(Bound.ONE));
  }

  /**
   * Test of inlineLinear method, of class AffineCtxBuilder.
   */
  @Test public void testInlineLinear () {
    AffineStateBuilder sys = new AffineStateBuilder(system);
    // state that x3 = x4-1
    Linear con = linear(num(1), term(num(-1), x4), term(num(3), x3));
    // simulate an intersection
    sys.inlineLinear(con, x4);
    System.out.println("equality system after intersecting with " + con + ":\n" + sys.build());
  }

  /**
   * Test of removeLhsVar method, of class AffineCtxBuilder.
   */
  @Test public void testRemoveLhsVar () {
    AffineStateBuilder sys = new AffineStateBuilder(system);
    sys.removeLhsVar(x1);
    sys.sane();
  }

  /**
   * Test of removeRhsVar method, of class AffineCtxBuilder.
   */
  @Test public void testRemoveRhsVar () {
    AffineStateBuilder sys = new AffineStateBuilder(system);
    sys.removeRhsVar(x3, true);
    sys.sane();

    System.out.println("removing x3 yields:\n" + sys.build() + "operations on child:");
    for (ZenoChildOp t : sys.getChildOps()) {
      System.out.println(t.toString());
    }
  }

  @Test public void testInvertibleToNewKey () {
    AffineStateBuilder sys = new AffineStateBuilder(AffineState.EMPTY);
    sys.insertLinear(linear(Bound.ZERO, term(num(1), x0), term(num(1), x1), term(num(1), x2), term(num(1), x5)).toEquality());
    sys.insertLinear(linear(Bound.ZERO, term(num(1), x3), term(num(1), x5)).toEquality());
    sys.insertLinear(linear(Bound.ZERO, term(num(1), x4), term(num(1), x5)).toEquality());
    System.out.println("input system:\n"+sys.build());
    sys.affineTrans(Divisor.one(), x5, linear(Bound.ZERO, term(num(-1), x1), term(num(-1), x2), term(num(1), x5)));

    assert(sys.getChildOps().length()>1);
    System.out.println(sys.getChildOps());
    AffineState result = sys.build();

    AffineStateBuilder resultSys = new AffineStateBuilder(AffineState.EMPTY);
    resultSys.insertLinear(linear(Bound.ZERO, term(num(1), x0), term(num(-2), x4), term(num(-1), x5)).toEquality());
    resultSys.insertLinear(linear(Bound.ZERO, term(num(1), x1), term(num(1), x2), term(num(1), x4), term(num(1), x5)).toEquality());
    resultSys.insertLinear(linear(Bound.ZERO, term(num(1), x3), term(num(-1), x4)).toEquality());
    AffineState expectedResult = resultSys.build();
    assertThat(result, is(expectedResult));
  }
}