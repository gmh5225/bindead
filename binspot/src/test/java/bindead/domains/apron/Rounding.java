package bindead.domains.apron;

import static apron.Texpr1BinNode.OP_DIV;
import static apron.Texpr1BinNode.OP_POW;
import static bindead.debug.DebugHelper.logln;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import javalx.data.products.P3;

import org.junit.Before;
import org.junit.Test;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import apron.Interval;
import apron.Linexpr1;
import apron.Linterm1;
import apron.Manager;
import apron.MpqScalar;
import apron.Polka;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import apron.Texpr1Node;
import apron.Texpr1VarNode;

/**
 * Tests to know more about the rounding type depending on global parameters in Apron.
 * For now the tests are tailored to the polyhedra domain of Apron as other numeric domains
 * have different rounding behavior.
 *
 * @author Bogdan Mihaila
 */
public class Rounding {
  private static boolean rounding = false;
  private static int roundingDirection = Texpr1BinNode.RDIR_ZERO;

  /**
   * Depending on the domain taken to represent the values rounding for division
   * behaves differently.
   */
  private static Manager getTop () {
    Manager top = new Polka(false);
    tweakFlags(top);
    return top;
  }

  private static void tweakFlags (Manager top) {
    top.setPreferedScalarType(Manager.SCALAR_MPQ);
    int algoBoostValue = 100;
    top.setAlgorithm(Manager.FUNID_BOUND_LINEXPR, algoBoostValue);
    top.setAlgorithm(Manager.FUNID_BOUND_TEXPR, algoBoostValue);
    top.setAlgorithm(Manager.FUNID_BOUND_DIMENSION, algoBoostValue);
    top.setAlgorithm(Manager.FUNID_APPROXIMATE, algoBoostValue);
    top.setAlgorithm(Manager.FUNID_IS_BOTTOM, algoBoostValue);
    top.setAlgorithm(Manager.FUNID_IS_TOP, algoBoostValue);

    top.setFlagBestWanted(Manager.FUNID_BOUND_LINEXPR, true);
    top.setFlagBestWanted(Manager.FUNID_BOUND_TEXPR, true);
    top.setFlagBestWanted(Manager.FUNID_BOUND_DIMENSION, true);
    top.setFlagExactWanted(Manager.FUNID_APPROXIMATE, true);
  }

  @Before public void precondition () {
    // ignore Tests if the preconditions (native library installed) are not satisfied
    assumeTrue(NativeLibsLoading.haveApronNativeLibraries());
//    DebugHelper.analysisKnobs.printLogging();
  }

  @Test public void power () throws Exception {
    String x = "x";
    String a = "a";
    String b = "b";
    Environment env = new Environment(new String[] {x, a, b}, new String[] {""});
    Abstract1 state = new Abstract1(getTop(), env);
    // first with constants:
    // 20 / (2 ^_i,0 3)
    Texpr1BinNode shiftAmount =
      new Texpr1BinNode(OP_POW, Texpr1BinNode.RTYPE_INT, Texpr1BinNode.RDIR_ZERO, expr(2), expr(3));
    Texpr1BinNode division =
      new Texpr1BinNode(OP_DIV, Texpr1BinNode.RTYPE_INT, Texpr1BinNode.RDIR_ZERO, expr(20), shiftAmount);
    Texpr1Intern expr = new Texpr1Intern(state.getEnvironment(), division);
    logln("evaluating: x = " + expr);
    Abstract1 resultState = state.assignCopy(state.getCreationManager(), x, expr, null);
    logln(resultState);
    assertThat(resultState.isBottom(state.getCreationManager()), is(false));
    logln(resultState.getBound(state.getCreationManager(), x));
    assertThat(resultState.getBound(state.getCreationManager(), x), is(new Interval(2, 2)));
    logln();

    // now with variables
    // 1 * a(v12)=20 / (2 ^_i,0 (1 * b(v13)=3))
    Texpr1Intern introA = new Texpr1Intern(env, new Texpr1CstNode(num(20)));
    resultState = state.assignCopy(state.getCreationManager(), a, introA, null);
    Texpr1Intern introB = new Texpr1Intern(env, new Texpr1CstNode(num(3)));
    resultState = resultState.assignCopy(state.getCreationManager(), b, introB, null);
    shiftAmount = new Texpr1BinNode(OP_POW, Texpr1BinNode.RTYPE_INT, Texpr1BinNode.RDIR_ZERO, expr(2), expr(b));
    division = new Texpr1BinNode(OP_DIV, Texpr1BinNode.RTYPE_INT, Texpr1BinNode.RDIR_ZERO, expr(a), shiftAmount);
    expr = new Texpr1Intern(state.getEnvironment(), division);
    logln("evaluating: x = " + expr);
    logln("in state: " + resultState);
    resultState = resultState.assignCopy(state.getCreationManager(), x, expr, null);
    logln(resultState);
    assertThat(resultState.isBottom(state.getCreationManager()), is(false));
    logln(resultState.getBound(state.getCreationManager(), x));
    assertThat(resultState.getBound(state.getCreationManager(), x), is(new Interval(2, 2)));
    logln();
  }

  /**
   * Divide constants using different rounding modes for the division.
   */
  @Test public void divisionWithConstants () throws Exception {
    // 20 / 8 = 5/2 = 2.5
    rounding = false;
    P3<Texpr1Intern, Abstract1, Interval> result1 = div(20, 8);
    logln("evaluating: x = " + result1._1());
    logln("result: " + result1._2());
    logln("x = " + result1._3());
    assertThat(result1._3(), is(new Interval(5, 2, 5, 2)));
    logln();

    // 20 /_i,0 8 = 2
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_ZERO;
    P3<Texpr1Intern, Abstract1, Interval> result2 = div(20, 8);
    logln("evaluating: x = " + result2._1());
    logln("result: " + result2._2());
    logln("x = " + result2._3());
    assertThat(result2._3(), is(new Interval(2, 2)));
    logln();

    // 20 /_i,n 8 = [2, 3]
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_NEAREST;
    P3<Texpr1Intern, Abstract1, Interval> result3 = div(20, 8);
    logln("evaluating: x = " + result3._1());
    logln("result: " + result3._2());
    logln("x = " + result3._3());
    assertThat(result3._3(), is(new Interval(2, 3)));
    logln();

    // 20 /_i,-oo 8 = 2
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_DOWN;
    P3<Texpr1Intern, Abstract1, Interval> result4 = div(20, 8);
    logln("evaluating: x = " + result4._1());
    logln("result: " + result4._2());
    logln("x = " + result4._3());
    assertThat(result4._3(), is(new Interval(2, 2)));
    logln();

    // 20 /_i,+oo 8 = 3
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_UP;
    P3<Texpr1Intern, Abstract1, Interval> result5 = div(20, 8);
    logln("evaluating: x = " + result5._1());
    logln("result: " + result5._2());
    logln("x = " + result5._3());
    assertThat(result5._3(), is(new Interval(3, 3)));
    logln();
  }

  private static P3<Texpr1Intern, Abstract1, Interval> div (int dividend, int divisor) throws ApronException {
    int roundingType = rounding ? Texpr1BinNode.RTYPE_INT : Texpr1BinNode.RTYPE_REAL;
    String x = "x";
    Environment env = new Environment(new String[] {x}, new String[] {""});
    Abstract1 state = new Abstract1(getTop(), env);
    Texpr1BinNode division1 = new Texpr1BinNode(OP_DIV, roundingType, roundingDirection, expr(dividend), expr(divisor));
    Texpr1Intern expr = new Texpr1Intern(state.getEnvironment(), division1);
    Abstract1 newState = state.assignCopy(state.getCreationManager(), x, expr, null);
    Interval bound = newState.getBound(state.getCreationManager(), x);
    return new P3<Texpr1Intern, Abstract1, Interval>(expr, newState, bound);
  }

  /**
   * Division of variables containing only constant values using different rounding modes for the division.
   */
  @Test public void divisionWithVariables () throws Exception {
    String x = "x";
    String a = "a";
    String b = "b";
    Environment env = new Environment(new String[] {x, a, b}, new String[] {""});
    Abstract1 state = new Abstract1(getTop(), env);
    Texpr1Intern introA = new Texpr1Intern(env, new Texpr1CstNode(num(20)));
    state = state.assignCopy(state.getCreationManager(), a, introA, null);
    Texpr1Intern introB = new Texpr1Intern(env, new Texpr1CstNode(num(8)));
    state = state.assignCopy(state.getCreationManager(), b, introB, null);

    logln("state: " + state);
    logln();

    // 20 / 8 = 5/2 = 2.5
    rounding = false;
    P3<Texpr1Intern, Abstract1, Interval> result1 = div(state, x, a, b);
    logln("evaluating: x = " + result1._1());
    logln("result: " + result1._2());
    logln("x = " + result1._3());
    // division without rounding on variables yields bottom! This means Apron implements integer semantics.
    // instead of assertThat(result1._3(), is(new Interval(5, 2, 5, 2))); we have:
    assertTrue(result1._2().isBottom(state.getCreationManager()));
    logln();

    // 20 /_i,0 8 = 2
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_ZERO;
    P3<Texpr1Intern, Abstract1, Interval> result2 = div(state, x, a, b);
    logln("evaluating: x = " + result2._1());
    logln("result: " + result2._2());
    logln("x = " + result2._3());
    assertThat(result2._3(), is(new Interval(2, 2)));
    logln();

    // 20 /_i,n 8 = [2, 3]
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_NEAREST;
    P3<Texpr1Intern, Abstract1, Interval> result3 = div(state, x, a, b);
    logln("evaluating: x = " + result3._1());
    logln("result: " + result3._2());
    logln("x = " + result3._3());
    assertThat(result3._3(), is(new Interval(2, 3)));
    logln();

    // 20 /_i,-oo 8 = 2
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_DOWN;
    P3<Texpr1Intern, Abstract1, Interval> result4 = div(state, x, a, b);
    logln("evaluating: x = " + result4._1());
    logln("result: " + result4._2());
    logln("x = " + result4._3());
    assertThat(result4._3(), is(new Interval(2, 2)));
    logln();

    // 20 /_i,+oo 8 = 3
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_UP;
    P3<Texpr1Intern, Abstract1, Interval> result5 = div(state, x, a, b);
    logln("evaluating: x = " + result5._1());
    logln("result: " + result5._2());
    logln("x = " + result5._3());
    assertThat(result5._3(), is(new Interval(3, 3)));
    logln();
  }

  private static P3<Texpr1Intern, Abstract1, Interval> div (Abstract1 state, String resultVar, String dividend,
      String divisor) throws ApronException {
    int roundingType = rounding ? Texpr1BinNode.RTYPE_INT : Texpr1BinNode.RTYPE_REAL;
    Texpr1BinNode division1 = new Texpr1BinNode(OP_DIV, roundingType, roundingDirection, expr(dividend), expr(divisor));
    Texpr1Intern expr = new Texpr1Intern(state.getEnvironment(), division1);
    Abstract1 newState = state.assignCopy(state.getCreationManager(), resultVar, expr, null);
    Interval bound = newState.getBound(state.getCreationManager(), resultVar);
    return new P3<Texpr1Intern, Abstract1, Interval>(expr, newState, bound);
  }

  /**
   * Assign a linear expression divided by a constant to a new variable using different rounding modes for the division.
   */
  @Test public void assignment () throws Exception {
    String a = "a";
    String b = "b";
    Environment env = new Environment(new String[] {a, b}, new String[] {""});
    Abstract1 state = new Abstract1(getTop(), env);
    Texpr1Intern introA = new Texpr1Intern(env, new Texpr1CstNode(num(6)));
    state = state.assignCopy(state.getCreationManager(), a, introA, null);
    // 1 * a + 0
    Texpr1Node varAsLinear =
      Texpr1Node.fromLinexpr1(new Linexpr1(env, new Linterm1[] {new Linterm1(a, num(1))}, num(0)));

    logln("state: " + state);
    logln();

    // b := 1 * a=6 / 1 = 6
    rounding = false;
    P3<Texpr1Intern, Abstract1, Interval> result1 = div(state, b, varAsLinear, 1);
    logln("evaluating: b = " + result1._1());
    logln("result: " + result1._2());
    logln("b = " + result1._3());
    assertThat(result1._3(), is(new Interval(6, 6)));
    logln();

    // b := 1 * a=6 /_i,0 1 = [5, 6]
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_ZERO;
    P3<Texpr1Intern, Abstract1, Interval> result2 = div(state, b, varAsLinear, 1);
    logln("evaluating: b = " + result2._1());
    logln("result: " + result2._2());
    logln("b = " + result2._3());
    // division with rounding yields an over-approximated value due to polyhedra using an interval to express rounding.
    assertThat(result2._3(), is(new Interval(5, 6)));
    logln();

    // b := 1 * a=6 /_i,n 1 = 6
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_NEAREST;
    P3<Texpr1Intern, Abstract1, Interval> result3 = div(state, b, varAsLinear, 1);
    logln("evaluating: b = " + result3._1());
    logln("result: " + result3._2());
    logln("b = " + result3._3());
    assertThat(result3._3(), is(new Interval(6, 6)));
    logln();

    // b := 1 * a=6 /_i,-oo 1 = [5, 6]
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_DOWN;
    P3<Texpr1Intern, Abstract1, Interval> result4 = div(state, b, varAsLinear, 1);
    logln("evaluating: b = " + result4._1());
    logln("result: " + result4._2());
    logln("b = " + result4._3());
    // division with rounding yields an over-approximated value due to polyhedra using an interval to express rounding.
    assertThat(result4._3(), is(new Interval(5, 6)));
    logln();

    // b := 1 * a=6 /_i,+oo 1 = [6, 7]
    rounding = true;
    roundingDirection = Texpr1BinNode.RDIR_UP;
    P3<Texpr1Intern, Abstract1, Interval> result5 = div(state, b, varAsLinear, 1);
    logln("evaluating: b = " + result5._1());
    logln("result: " + result5._2());
    logln("b = " + result5._3());
    // division with rounding yields an over-approximated value due to polyhedra using an interval to express rounding.
    assertThat(result5._3(), is(new Interval(6, 7)));
    logln();
  }

  private static P3<Texpr1Intern, Abstract1, Interval> div (Abstract1 state, String resultVar, Texpr1Node dividend,
      int divisor) throws ApronException {
    int roundingType = rounding ? Texpr1BinNode.RTYPE_INT : Texpr1BinNode.RTYPE_REAL;
    Texpr1BinNode division = new Texpr1BinNode(OP_DIV, roundingType, roundingDirection, dividend, expr(divisor));
    Texpr1Intern assign = new Texpr1Intern(state.getEnvironment(), division);
    Abstract1 newState = state.assignCopy(state.getCreationManager(), resultVar, assign, null);
    Interval bound = newState.getBound(state.getCreationManager(), resultVar);
    return new P3<Texpr1Intern, Abstract1, Interval>(assign, newState, bound);
  }

  private static Texpr1Node expr (String variableName) {
    return new Texpr1VarNode(variableName);
  }

  private static Texpr1Node expr (int value) {
    return new Texpr1CstNode(num(value));
  }

  private static MpqScalar num (int value) {
    return new MpqScalar(value);
  }

}
