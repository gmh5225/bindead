package bindead.domains;
import static bindead.TestsHelper.evaluateAssertions;
import static bindead.TestsHelper.lines;
import static bindead.TestsHelper.AssertionsBuilder.assertResultOf;
import static bindead.data.Linear.linear;
import static bindead.data.Linear.num;
import static bindead.data.Linear.term;
import static bindead.debug.DebugHelper.log;
import static bindead.debug.DebugHelper.logln;
import static javalx.data.Option.some;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Congruence;
import javalx.numeric.Interval;

import org.junit.Test;

import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.DomainFactory;
import bindead.data.NumVar;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.exceptions.Unreachable;

@SuppressWarnings({"rawtypes"})
public class Congruences {
  private static final FiniteDomain top = DomainFactory.parseFiniteDomain("Wrapping Congruences Intervals");

  private static final NumVar x1 = NumVar.fresh("x1");
  private static final NumVar x2 = NumVar.fresh("x2");
  private static final NumVar x3 = NumVar.fresh("x3");
  private static final NumVar x4 = NumVar.fresh("x4");
  private static final NumVar x5 = NumVar.fresh("x5");
  private static final NumVar x6 = NumVar.fresh("x6");
  private static final NumVar x7 = NumVar.fresh("x7");
  private static final NumVar x8 = NumVar.fresh("x8");
  private static final NumVar x9 = NumVar.fresh("x9");
  private static final NumVar x10 = NumVar.fresh("x10");
  private static final NumVar x11 = NumVar.fresh("x11");


  private static RichFiniteDomain wrap (FiniteDomain domain) {
    return FiniteDomainHelper.for32bitVars(domain);
  }

  /**
   * Generate a domain with the following values:<br>
   * <pre>
   * x1 = 1
   * x2 = T
   * x3 = 3
   * x4 = T
   * x5 = T
   * x6 = 6
   * x7 = T
   * x8 = T
   * x9 = 9
   * x10 = T
   * </pre>
   */
  private static RichFiniteDomain genInitialSystem1 (FiniteDomain domain) {
    RichFiniteDomain d = wrap(domain);
    d = d.introduce(x1, 1);
    d = d.introduce(x2);
    d = d.introduce(x3, 3);
    d = d.introduce(x4);
    d = d.introduce(x5);
    d = d.introduce(x6, 6);
    d = d.introduce(x7);
    d = d.introduce(x8);
    d = d.introduce(x9, 9);
    d = d.introduce(x10);
    return d;
  }

  /**
   * Generate a domain with the following values:<br>
   * <pre>
   * x1 = 2
   * x2 = T
   * x3 = 3
   * x4 = T
   * x5 = 5
   * x6 = T
   * x7 = T
   * x8 = 8
   * x9 = T
   * x10 = T
   * </pre>
   */
  private static RichFiniteDomain genInitialSystem2 (FiniteDomain domain) {
    RichFiniteDomain d = wrap(domain);
    d = d.introduce(x1, 2);
    d = d.introduce(x2);
    d = d.introduce(x3, 3);
    d = d.introduce(x4);
    d = d.introduce(x5, 5);
    d = d.introduce(x6);
    d = d.introduce(x7);
    d = d.introduce(x8, 8);
    d = d.introduce(x9);
    d = d.introduce(x10);
    return d;
  }

  /**
   * Generate a domain with the following values:<br>
   *
   * <pre>
   * x1 = 2
   * x2 = 1
   * x3 = 5
   * x4 = T
   * x5 = 5
   * x6 = 6
   * x7 = T
   * x8 = T
   * x9 = T
   * x10 = T
   * </pre>
   */
  private static RichFiniteDomain genInitialSystem3 (FiniteDomain domain) {
    RichFiniteDomain d = wrap(domain);
    d = d.introduce(x1, 2);
    d = d.introduce(x2, 1);
    d = d.introduce(x3, 5);
    d = d.introduce(x4);
    d = d.introduce(x5, 5);
    d = d.introduce(x6, 6);
    d = d.introduce(x7);
    d = d.introduce(x8);
    d = d.introduce(x9);
    d = d.introduce(x10);
    return d;
  }

  /**
   * Generate a domain with the following values:<br>
   *
   * <pre>
   * x1 = 4
   * x2 = 4
   * x3 = 5
   * x4 = 1
   * x5 = 10
   * x6 = 9
   * x7 = T
   * x8 = T
   * x9 = T
   * x10 = T
   * </pre>
   */
  private static RichFiniteDomain genInitialSystem4 (FiniteDomain domain) {
    RichFiniteDomain d = wrap(domain);
    d = d.introduce(x1, 4);
    d = d.introduce(x2, 4);
    d = d.introduce(x3, 5);
    d = d.introduce(x4, 1);
    d = d.introduce(x5, 10);
    d = d.introduce(x6, 9);
    d = d.introduce(x7);
    d = d.introduce(x8);
    d = d.introduce(x9);
    d = d.introduce(x10);
    return d;
  }

  @Test public void testUndefSet () {
    RichFiniteDomain d = genInitialSystem1(top);
    logln("initialization:\n" + d.toString());
    d = d.assign(x5, linear(num(2), term(x3)));
    d.assertValueIs(x3, 3);
    d.assertValueIs(x5, 5);
    d.assertValueIs(x9, 9);
    d = d.assign(x6, x2, x6);
    d.assertValueIs(x6, Interval.TOP);
    d = d.project(x6);
    logln("\nproject(x6):\n" + d.toString());
  }

  @Test public void testJoinWithScaling () {
    RichFiniteDomain d1 = wrap(top);
    d1 = d1.introduce(x1, 10);
    d1 = d1.introduce(x2, 7);
    RichFiniteDomain d2 = wrap(top);
    d2 = d2.introduce(x1, 0);
    d2 = d2.introduce(x2, 5);
    RichFiniteDomain d12 = d1.join(d2);
    logln("d12:\n" + d12);
    d12.assertValueIs(x1, Interval.of(0, 10));
    d12.assertValueIsCongruent(x1, congr(10, 0));
    d12.assertValueIs(x2, Interval.of(5, 7));
    d12.assertValueIsCongruent(x2, congr(2, 1));
  }

  @Test public void testJoin () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    RichFiniteDomain d2 = genInitialSystem2(top);
    RichFiniteDomain d12 = d1.join(d2);
    logln("d1.join(d2):");
    logln("d1:\n" + d1.toString());
    logln("d2:\n" + d2.toString());
    logln("d12:\n" + d12.toString());
    // d12 = d12.substitute(x11, x3); // works, commented because of eval followed by subsetOrEqual
    // logln("\nsubstitute(x11,x3):\nd12:\n" + d12.toString());
    assertThat(d1.subsetOrEqual(d2), is(false));
    d12.assertValueIs(x6, Interval.TOP);
    RichFiniteDomain fst = d1.equalTo(x1, 1);
    fst = fst.substitute(x5, x11);
    fst = fst.substitute(x11, x5);
    logln("\nsetting x1 to 1 in d1:\n" + fst.toString());
    fst.assertValueIs(x6, 6);
    assertThat(fst.subsetOrEqual(d1), is(true));
    assertThat(d1.subsetOrEqual(fst), is(true));
    RichFiniteDomain snd = d2.equalTo(x1, 2);
    snd = snd.substitute(x5, x11);
    snd = snd.substitute(x11, x5);
    logln("\nsetting x1 to 2 in d2:\n" + snd.toString());
    snd.assertValueIs(x5, 5);
    assertThat(snd.subsetOrEqual(d2), is(true));
    assertThat(d2.subsetOrEqual(snd), is(true));
    RichFiniteDomain fstsnd = fst.join(snd);
    assertThat(fstsnd.subsetOrEqual(d12), is(true));
    assertThat(d12.subsetOrEqual(fstsnd), is(true));
    logln("\njoining both:\n" + fstsnd.toString());
  }

  @Test public void testWhackyJoin () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    RichFiniteDomain d2 = genInitialSystem2(top);
    RichFiniteDomain d12 = d1.join(d2);
    logln("d12:\n" + d12);
    d12.assertValueIs(x6, Interval.TOP);
    RichFiniteDomain fst = d1.equalTo(x1, 1);
    fst.assertValueIs(x6, 6);
    // create an additional top variable in the state where x6 was set
    fst = fst.project(x6);
    fst = fst.introduce(x6);
    logln("\nsetting x1 to 1 and then setting x6 to top in d1:\n" + fst.toString());
    RichFiniteDomain snd = d2.equalTo(x1, 2);
    snd.assertValueIs(x5, 5);
    // create an additional non-top variable in the state where x6 was undefined
    snd = snd.assign(x6, x5);
    logln("\nsetting x1 to 2 and then set x6:=x5+1 in d2:\n" + snd.toString());

    // create an additional non-top variable in the state where x4 was undefined
    snd = snd.assign(x4, linear(BigInt.of(3), term(x5), term(BigInt.of(2), x6)));
    logln("\nsetting x1 to 2 and then set x4:=x5 + 2*x6 + 3 in d2:\n" + snd.toString());

    RichFiniteDomain fstsnd = fst.join(snd);
    logln("\njoining both:\n" + fstsnd.toString());
    fstsnd.assertValueIs(x6, Interval.TOP);
    fstsnd = fstsnd.equalTo(x1, 2);
    logln("\nsecond state of join (x1=2):\n" + fstsnd.toString());
  }

  @Test public void testWhackierJoin () {
    RichFiniteDomain d3 = genInitialSystem3(top);
    RichFiniteDomain d4 = genInitialSystem4(top);
    RichFiniteDomain d34 = d3.join(d4);
    logln("d3:\n" + d3.toString());
    logln("d4:\n" + d4.toString());
    logln("d34:\n" + d34.toString());

    RichFiniteDomain fst = d34.assign(x4, x1, x3);
    logln("\nsetting x4:=x1+x3 in d34:\n" + fst.toString());

    RichFiniteDomain snd = d34.assign(x6, x5);
    logln("\nsetting x6:=x5 in d34:\n" + snd.toString());

    // Testing query range
    log("\nQuery range x1: ");
    for (BigInt item : d34.getWrappedDomain().queryRange(linear(term(x1))))
      log(item + " ");
    logln();

    log("Query range x1+x2: ");
    for (BigInt item : d34.getWrappedDomain().queryRange(linear(term(x1), term(x2))))
      log(item + " ");
    logln();
  }

  @Test public void testTests () {
    RichFiniteDomain d = genInitialSystem1(top);
    // first test constant congruences
    d.assertValueIs(x3, 3);
    d = d.assign(x1, linear(term(num(2), x3)));
    d.assertValueIsCongruent(x1, congr(0, 6));
    // then the non-constant ones
    d = d.assign(x1, Interval.of(1, 2));
    d.assertValueIs(x1, Interval.of(1, 2));
    d = d.assign(x1, linear(term(num(3), x1)));
    logln("x1 congruent *3:");
    logln(d);
    d.assertValueIsCongruent(x1, congr(3, 0));
    d = d.assign(x1, Interval.of(1, 2));
    d.assertValueIs(x1, Interval.of(1, 2));
    d = d.assign(x1, linear(num(2), term(num(3), x1)));
    logln();
    logln("x1 congruent *3+2:");
    logln(d);
    logln();
    logln("setting x1 = 5");
    d = d.equalTo(x1, 5);
    logln(d);
    d.assertValueIs(x1, 5);
    d.assertValueIsCongruent(x1, congr(0, 5));
  }

  @Test public void testConstantTests () {
    RichFiniteDomain d = genInitialSystem1(top);
    d.assertValueIs(x3, 3);
    logln(d);
    logln();
    logln("x3 == 3");
    d = d.equalTo(x3, 3);
    logln("x3 == 0");
    try {
      d = d.equalTo(x3, 0);
      assertTrue(false);
    } catch (Unreachable _) {
      assertTrue(true);
    }
    logln("3 == 3");
    d = d.equalTo(3, 3);
    logln("3 == 0");
    try {
      d = d.equalTo(3, 0);
      assertTrue(false);
    } catch (Unreachable _) {
      assertTrue(true);
    }
  }

  @Test public void testMeet () {
    assertThat(congr(4, 3).meet(congr(3, 2)), is(some(congr(12, 11))));
    assertThat(congr(0, -16).meet(congr(222, 206)), is(some(congr(0, -16))));
    assertThat(congr(3, 0).meet(congr(5, 0)), is(some(congr(15, 0))));
    assertThat(congr(2, 1).meet(congr(3, 0)), is(some(congr(6, 3))));
    assertThat(congr(2, 1).meet(congr(4, 1)), is(some(congr(4, 1))));
    assertThat(congr(2, 1).meet(congr(3, 1)), is(some(congr(6, 1))));
    assertThat(congr(2, 1).meet(congr(3, 2)), is(some(congr(6, 5))));
    assertThat(congr(4, 1).meet(congr(3, 0)), is(some(congr(12, 9))));
    assertThat(congr(1, 1).meet(congr(0, 1)), is(some(congr(0, 1))));
    assertThat(congr(3, 1).meet(congr(0, 1)), is(some(congr(0, 1))));
    assertThat(congr(3, 1).meet(congr(0, -1)), is(Option.<Congruence>none()));
    assertThat(congr(1, 0).meet(congr(0, 1)), is(some(congr(0, 1))));
    assertThat(congr(1, 0).meet(congr(0, -1)), is(some(congr(0, -1))));
    assertThat(congr(1, 0).meet(congr(0, -5)), is(some(congr(0, -5))));
    assertThat(congr(0, 1).meet(congr(0, 1)), is(some(congr(0, 1))));
    assertThat(congr(0, -5).meet(congr(0, -5)), is(some(congr(0, -5))));
    assertThat(congr(0, -1).meet(congr(0, -1)), is(some(congr(0, -1))));
    assertThat(congr(0, 1).meet(congr(0, 3)), is(Option.<Congruence>none()));
    assertThat(congr(3, 1).meet(congr(99, 3)), is(Option.<Congruence>none()));
    assertThat(congr(3, 2).meet(congr(6, 3)), is(Option.<Congruence>none()));
  }

  @Test public void testSubsetOrEqual () {
    assertThat(congr(0, 0).subsetOrEqual(congr(1, 0)), is(true));
  }

  @Test public void testRounding () {
    RichFiniteDomain d = wrap(top);
    d = d.introduce(x1);
    // x1 = [0, 1]
    d = d.assign(x1, Interval.of(0, 1));
    logln("Before assign:\n" + d);
    // x1 = x1 * 11 => congruence(x1) = 11 + 0
    d = d.assign(x1, linear(term(num(11), x1)));
    logln("Before assign:\n" + d);
    d.assertValueIs(x1, Interval.of(0, 11));
    // 10 <= x1 => x1 = [11]
    d = d.lessOrEqualTo(10, x1);
    logln("After test:\n" + d);
    d.assertValueIs(x1, Interval.of(11));
  }

  /**
   * Simple non-invertible (overwriting) assignment.
   */
  @Test public void testAssignments1 () {
    RichFiniteDomain d = wrap(top);
    d = d.introduce(x1);
    // x1 = [1, 2]
    d = d.assign(x1, Interval.of(1, 2));
    // x1 = x1 * 4 => congruence(x1) = 4 + 0
    d = d.assign(x1, linear(term(num(4), x1)));
    logln("Before assign:\n" + d);
    Congruence congr4 = new Congruence(4, 0);
    d.assertValueIs(x1, Interval.of(4, 8));
    d.assertValueIsCongruent(x1, congr4);
    // x1 = [3, 5] => congruence(x1) = 1 + 0
    d = d.assign(x1, Interval.of(3, 5));
    logln("After assign:\n" + d);
    d.assertValueIs(x1, Interval.of(3, 5));
    d.assertValueIsCongruent(x1, congr(1, 0));
  }

  /**
   * Simple invertible (lhs also on rhs) assignment with only one variable.
   */
  @Test public void testAssignments2 () {
    RichFiniteDomain d = wrap(top);
    d = d.introduce(x1);
    // x1 = [1, 2]
    d = d.assign(x1, Interval.of(1, 2));
    // x1 = x1 * 4 => congruence(x1) = 4 + 0
    d = d.assign(x1, linear(term(num(4), x1)));
    logln("Before assign:\n" + d);
    Congruence congr4 = new Congruence(4, 0);
    d.assertValueIs(x1, Interval.of(4, 8));
    d.assertValueIsCongruent(x1, congr4);
    // x1 = 8 * x1 - 1 => congruence(x1) = 32 - 1
    d = d.assign(x1, linear(num(-1), term(num(8), x1)));
    logln("After assign:\n" + d);
    Congruence congrBoth = new Congruence(32, -1);
    d.assertValueIs(x1, Interval.of(31, 63));
    d.assertValueIsCongruent(x1, congrBoth);
  }

  /**
   * Simple invertible (lhs also on rhs) assignment with only one variable.
   */
  @Test public void testAssignments3 () {
    RichFiniteDomain d = wrap(top);
    d = d.introduce(x1);
    // x1 = [1, 2]
    d = d.assign(x1, Interval.of(1, 2));
    // x1 = x1 * 3 + 2 => congruence(x1) = 3 + 2
    d = d.assign(x1, linear(num(2), term(num(3), x1)));
    logln("Before assign:\n" + d);
    Congruence congr3p2 = new Congruence(3, 2);
    d.assertValueIs(x1, Interval.of(5, 8));
    d.assertValueIsCongruent(x1, congr3p2);
    // x1 = 8 * x1 - 1 => congruence(x1) = 8 * (3 + 2) - 1 = 24 + 15
    d = d.assign(x1, linear(num(-1), term(num(8), x1)));
    logln("After assign:\n" + d);
    Congruence congrBoth = new Congruence(24, 15);
    d.assertValueIs(x1, Interval.of(39, 63));
    d.assertValueIsCongruent(x1, congrBoth);
  }

  /**
   * More complex invertible (lhs also on rhs) assignment with two variables.
   */
  @Test public void testAssignments4 () {
    RichFiniteDomain d = wrap(top);
    d = d.introduce(x1);
    // x1 = [1, 2]
    d = d.assign(x1, Interval.of(1, 2));
    // x1 = x1 * 3 + 2 => congruence(x1) = 3 + 2
    d = d.assign(x1, linear(num(2), term(num(3), x1)));
    logln("Before assign:\n" + d);
    Congruence congr3p2 = new Congruence(3, 2);
    d.assertValueIs(x1, Interval.of(5, 8));
    d.assertValueIsCongruent(x1, congr3p2);

    d = d.introduce(x2);
    // x2 = [3, 4]
    d = d.assign(x2, Interval.of(3, 4));
    // x2 = x2 * 2 + 1 => congruence(x2) = 2 + 1
    d = d.assign(x2, linear(num(1), term(num(2), x2)));
    Congruence congr2p1 = new Congruence(2, 1);
    d.assertValueIs(x2, Interval.of(7, 9));
    d.assertValueIsCongruent(x2, congr2p1);

    // x1 = 8 * x1 - 1 + 2 * x2 => congruence(x1) = 4 + 1
    d = d.assign(x1, linear(num(-1), term(num(8), x1), term(num(2), x2)));
    logln("After assign:\n" + d);
    Congruence congrBoth = new Congruence(4, 1);
    d.assertValueIs(x1, Interval.of(53, 81));
    d.assertValueIsCongruent(x1, congrBoth);
  }

  /**
   * More complex non-invertible (overwriting) assignment with two variables.
   */
  @Test public void testAssignments5 () {
    RichFiniteDomain d = wrap(top);
    d = d.introduce(x1);
    // x1 = [1, 2]
    d = d.assign(x1, Interval.of(1, 2));
    // x1 = x1 * 3 + 2 => congruence(x1) = 3 + 2
    d = d.assign(x1, linear(num(2), term(num(3), x1)));
    logln("Before assign:\n" + d);
    Congruence congr3p2 = new Congruence(3, 2);
    d.assertValueIs(x1, Interval.of(5, 8));
    d.assertValueIsCongruent(x1, congr3p2);

    d = d.introduce(x2);
    // x2 = [3, 4]
    d = d.assign(x2, Interval.of(3, 4));
    // x2 = x2 * 2 + 1 => congruence(x2) = 2 + 1
    d = d.assign(x2, linear(num(1), term(num(2), x2)));
    Congruence congr2p1 = new Congruence(2, 1);
    d.assertValueIs(x2, Interval.of(7, 9));
    d.assertValueIsCongruent(x2, congr2p1);

    d = d.introduce(x3);
    // x3 = [5, 7]
    d = d.assign(x3, Interval.of(5, 7));
    // x3 = x3 * 3 - 1 => congruence(x3) = 3 - 1
    d = d.assign(x3, linear(num(-1), term(num(3), x3)));
    Congruence congr3m1 = new Congruence(3, -1);
    d.assertValueIs(x3, Interval.of(14, 20));
    d.assertValueIsCongruent(x3, congr3m1);

    // x1 = 5 * x2 - 1 + 2 * x3 => congruence(x1) = 2
    d = d.assign(x1, linear(num(-1), term(num(5), x2), term(num(2), x3)));
    logln("After assign:\n" + d);
    Congruence congrBoth = new Congruence(2, 0);
    d.assertValueIs(x1, Interval.of(62, 84));
    d.assertValueIsCongruent(x1, congrBoth);
  }

  @Test public void testArithmetic1 () {
      String assembly = lines(
          "option DEFAULT_SIZE = 16",
          "mov r1, [2, 62]",
          "shr r2, r1, 2",
          "mul r2, r2, 4",
          "assert r2 = [0, 60]",
          "mov r1, [1, 15]",
          "mul r1, r1, 4",
          "assert r1 = [4, 60]",
          "sub r3, r1, r2",
          "assert r3 = [-56, 60]",
          "halt");
      evaluateAssertions(assembly);
    }

  @Test public void testArithmetic2 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov r1, [1, 3]",
        "mul r2, r1, 4",
        "assert r2 = [4, 12]",
        "div r3, r2, 8",
        "assert r3 = [0, 1]",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void testArithmetic3 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov r1, [1, 4]",
        "mul r2, r1, 4",
        "assert r2 = [4, 16]",
        "div r3, r2, 8",
        "assert r3 = [0, 2]",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void testArithmetic4 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov r1, [1, 4]",
        "mul r2, r1, 8",
        "assert r2 = [8, 32]",
        "div r3, r2, 3",
        "assert r3 = [2, 10]",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void testArithmetic5 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov x, [1, 4]",
        "mul x, x, 4",
        "assert x = [4, 16]",
        "add x, x, 2",
        "assert x = [6, 18]",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void testArithmetic6 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov x, [1, 4]",
        "mul x, x, 4",
        "assert x = [4, 16]",
        "mod x, x, 2",
// TODO: modulo not implemented on intervals
//        "assert x = [0, 1]",
        "assert x = [-oo, +oo]",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void testArithmetic7 () {
    assertThat(congr(2, 1).add(congr(4, 3)), is(congr(2, 0)));
    assertThat(congr(0, 1).add(congr(0, 3)), is(congr(0, 4)));
    assertThat(congr(2, 1).add(congr(0, 3)), is(congr(2, 0)));
    assertThat(congr(2, 1).add(congr(0, -3)), is(congr(2, 0)));
    assertThat(congr(0, 0).add(congr(0, -3)), is(congr(0, -3)));
    assertThat(congr(1, 0).add(congr(0, -3)), is(congr(1, 0)));
  }

  /**
   * Test taken from the motivation of the linear congruences domain:
   * Granger - Static Analysis of Linear Congruence Equalities Among Variables of a Program
   */
  @Test public void testLinearCongruences () {
//    int i = 0;
//    int j = 0;
//    int k = 0;
//    while (*) {
//      i = i + 4;
//      k = k + 4;
//      if (*)
//        j = j + 4;
//      else
//        j = j + 12;
//    }                                 ** linear congruences: i = 8 mod j; i = k; i = 0 mod 4
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov i, 0",
        "mov j, 0",
        "mov k, 0",
//        "mov.1 RND1, [0, 1]", // if one wants to maintain affine equalities on the variable then it needs to exist outside of the loop
        "loop:",
        "  add i, i, 4",
        "  add k, k, 4",
        "  brc RND1, else:",
        "  if:",
        "    add j, j, 4",
        "    br if_else_join:",
        "  else:",
        "    add j, j, 12",
        "  if_else_join:",
        "  brc RND2, loop:",
        "assert i *+= k",       // affine equality
//        "assert i ≡ 4",       // syntax not implemented yet! Thus tests are below.
//        "assert j ≡ 4",
//        "assert k ≡ 4",
        "exit:",
        "halt");
    AnalysisFactory analyzer = new AnalysisFactory();
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
    int lastInstructionAddress = analysis.getRReilCode().getAddressForLabel("exit").get().intValue();
    String[] variables = new String[]{"i", "j", "k"};
    for (String variable : variables) {
      assertResultOf(analysis).at(lastInstructionAddress).forVar(variable).withSize(32).is(congr(4, 0));
    }
  }

  private static Congruence congr (long scale, long offset) {
    return new Congruence(scale, offset);
  }

}
