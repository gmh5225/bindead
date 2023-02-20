package bindead.domains.undef;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.num;
import static bindead.data.Linear.term;
import static bindead.debug.DebugHelper.logln;
import static bindead.debug.StringHelpers.AnalysisSymbols.join;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.numeric.Interval;

import org.junit.Test;

import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.analyses.DomainFactory;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.interfaces.FiniteDomain;

@SuppressWarnings("rawtypes")
public class TransferFunctions {
  private static final FiniteDomain top = DomainFactory.parseFiniteDomain(
      "Undef SupportSet Predicates(F) Wrapping Affine Congruences Intervals");

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

  private static final NumVar y1 = NumVar.fresh("y1");
  private static final NumVar y2 = NumVar.fresh("y2");
  private static final NumVar y3 = NumVar.fresh("y3");
  private static final NumVar y4 = NumVar.fresh("y4");

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
   * Assign defined and undefined variables to other variables.
   */
  @Test public void testUndefSet () {
    RichFiniteDomain d = genInitialSystem1(top);
    logln(d);
    d.assertValueIs(x5, Interval.TOP);
    d.assertValueIs(x3, 3);
    logln("x5 = x3 + 2");
    d = d.assign(x5, linear(num(2), term(x3)));
    logln(d);
    d.assertValueIs(x5, 5);
    d.assertValueIs(x2, Interval.TOP);
    d.assertValueIs(x6, 6);
    logln("x6 = x2 + x6");
    d = d.assign(x6, linear(num(0), term(x2), term(x6)));
    logln(d);
    d.assertValueIs(x6, Interval.TOP);
  }

  /**
   * Tests the copy and paste of variables from one domain to another with the different
   * configurations that can exist for variables in the undef domain.
   */
  @Test public void copyAndPaste () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    logln("d1:");
    logln(d1);
    RichFiniteDomain d2 = d1;
    d2 = d2.introduce(y1);
    d2 = d2.introduce(y2, 2);
    d2 = d2.introduce(y3);
    RichFiniteDomain d3 = d1;
    d3 = d3.introduce(y1);
    d3 = d3.introduce(y2, 2);
    d3 = d3.introduce(y3, 3);
    d2 = d2.join(d3);
    logln("d2:");
    logln(d2);
    d2.assertValueIs(y1, Interval.TOP);
    d2.assertValueIs(y2, 2);
    d2.assertValueIs(y3, Interval.TOP);
    VarSet copyVars = VarSet.of(y1, y2, y3);
    logln("copying " + copyVars + " from d2 into d1:");
    RichFiniteDomain result = d1.copyAndPaste(copyVars, d2);
    logln(result);
    // TODO: add some variable equalities and see if they are preserved during c&p
    result.assertValueIs(y1, Interval.TOP);
    result.assertValueIs(y2, 2);
    result.assertValueIs(y3, Interval.TOP);
  }

  /**
   * Tests the fold of variables with the different
   * configurations that can exist for variables in the undef domain.
   */
  @Test public void fold () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    RichFiniteDomain d2 = genInitialSystem2(top);
    d1 = d2.join(d1);
    d1 = d1.assign(x10, 10); // a variable with a defined value
    d1 = d1.introduce(y1);
    d1 = d1.introduce(y2, 2);
    d1 = d1.introduce(y3, 3);
    d1 = d1.introduce(y4, 4);
    logln("d1:");
    logln(d1);
    d1.assertValueIs(x1, Interval.of(1, 2));
    d1.assertValueIs(x2, Interval.top());
    d1.assertValueIs(x6, Interval.top());
    d1.assertValueIs(y1, Interval.top());
    d1.assertValueIs(y2, 2);
    d1.assertValueIs(y3, 3);
    ListVarPair pairs = new ListVarPair();
    pairs.add(x1, y1);
    pairs.add(x2, y2);
    pairs.add(x6, y3);
    pairs.add(x10, y4);
    logln();
    logln("Folding variables: " + pairs);
    logln();
    d1 = d1.fold(pairs);
    logln(d1);
    d1.assertValueIs(x1, Interval.top()); // maybe value of [1, 2]
    d1.assertValueIs(x2, Interval.top()); // maybe value of 2
    d1.assertValueIs(x6, Interval.top()); // maybe value of [3, 6]
    d1.assertValueIs(x10, Interval.of(4, 10));
    // TODO: see how to get an affine equality for the x6 flag
//    logln();
//    logln("Setting flag for x6 to true.");
//    logln();
//    RichFiniteDomain dx6 = d1.equalTo(x3, 0); // set flag for x6 partition to true
//    logln(dx6);
//    dx6.assertValueIs(x6, Interval.of(3, 6));
//    dx6.assertValueIs(x10, Interval.of(4, 10));
    logln();
    logln("Setting flag for x2 to true.");
    logln();
    RichFiniteDomain dx2 = d1.equalTo(x10, 4); // set flag for x2 partition to true
    logln(dx2);
    dx2.assertValueIs(x2, 2);
    dx2.assertValueIs(x10, 4);
    logln();
    logln("Setting flag for x1 to true.");
    logln();
    RichFiniteDomain dx1 = d1.equalTo(x10, 10); // set flag for x1 partition to true
    logln(dx1);
    dx1.assertValueIs(x1, Interval.of(1, 2));
    dx1.assertValueIs(x10, 10);
  }

  /**
   * Tests the expand of variables with the different
   * configurations that can exist for variables in the undef domain.
   */
  @Test public void expand () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    RichFiniteDomain d2 = genInitialSystem2(top);
    d1 = d2.join(d1);
    logln("d1:");
    logln(d1);
    d1.assertValueIs(x1, Interval.of(1, 2));
    d1.assertValueIs(x2, Interval.top());
    d1.assertValueIs(x6, Interval.top());
    ListVarPair pairs = new ListVarPair();
    pairs.add(x1, y1);
    pairs.add(x2, y2);
    pairs.add(x6, y3);
    logln("Expanding variables: " + pairs);
    d1 = d1.expandNG(pairs);
    logln(d1);
    d1.assertValueIs(y1, Interval.of(1, 2));
    d1.assertValueIs(y2, Interval.top());
    d1.assertValueIs(y3, Interval.top());
  }

  /**
   * Tests the expand of variables with the different equality configurations.
   */
  @Test public void expandWithEqualities () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    RichFiniteDomain d2 = genInitialSystem2(top);
    d1 = d2.join(d1);
    logln("d1:");
    logln(d1);
    logln("x5 = [0, 5]");
    logln("x10 = [0, 10]");
    logln("x7 = x10");
    logln("x6 = x4 + x5");
    d1 = d1.assign(x5, Interval.of(0, 5));
    d1 = d1.assign(x10, Interval.of(0, 10));
    d1 = d1.assign(x7, x10);
    d1 = d1.assign(x6, x4, x5);
    logln("d1:");
    logln(d1);
    d1.assertValueIs(x5, Interval.of(0, 5));
    d1.assertValueIs(x2, Interval.top());
    d1.assertValueIs(x6, Interval.top());
    d1.assertValueIs(x10, Interval.of(0, 10));
    d1.assertValueIs(x7, Interval.of(0, 10));
    ListVarPair pairs = new ListVarPair();
    pairs.add(x4, y1);
    pairs.add(x5, y2);
    pairs.add(x6, y3);
    pairs.add(x7, y4);
    logln();
    logln("Expanding variables: " + pairs);
    logln();
    d1 = d1.expandNG(pairs);
    logln(d1);
    d1.assertValueIs(y1, Interval.top());
    d1.assertValueIs(y2, Interval.of(0, 5));
    d1.assertValueIs(y3, Interval.top());
    d1.assertValueIs(y4, Interval.of(0, 10));
  }

  @Test public void assign () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    RichFiniteDomain d2 = genInitialSystem2(top);
    d1 = d2.join(d1);
    logln("d1:");
    logln(d1);
    logln("x5 = [0, 5]");
    logln("x10 = [0, 10]");
    logln("x7 = x10");
    logln("x6 = x4 + x5");
    d1 = d1.assign(x5, Interval.of(0, 5));
    d1 = d1.assign(x10, Interval.of(0, 10));
    d1 = d1.assign(x7, x10);
    d1 = d1.assign(x6, x4, x5);
    logln("d1:");
    logln(d1);
    d1.assertValueIs(x5, Interval.of(0, 5));
    d1.assertValueIs(x2, Interval.top());
    d1.assertValueIs(x6, Interval.top());
    d1.assertValueIs(x10, Interval.of(0, 10));
    d1.assertValueIs(x7, Interval.of(0, 10));
  }

  @Test public void tests () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    RichFiniteDomain d2 = genInitialSystem2(top);
    d1 = d2.join(d1);
    logln(d1);
    logln("x5 = [0, 5]");
    logln("x10 = [0, 10]");
    logln("x7 = x10");
    logln("x6 = x4 + x5");
    d1 = d1.assign(x5, Interval.of(0, 5));
    d1 = d1.assign(x10, Interval.of(0, 10));
    d1 = d1.assign(x7, x10);
    d1 = d1.assign(x6, x4, x5);
    logln(d1);
    logln("x4 == x5");
    logln("x7 == x10");
    logln("x8 == 8");
    logln("x1 == 2"); // set x8 as defined
    logln("x7 == x9");
    logln("x7 != x3");
    d1 = d1.equalTo(x4, x5);
    d1 = d1.equalTo(x7, x10);
    d1 = d1.equalTo(x8, 8);
    d1 = d1.equalTo(x1, 2);
    d1 = d1.equalTo(x7, x9);
    d1 = d1.notEqualTo(x7, x3);
    logln(d1);
    d1.assertValueIs(x5, Interval.of(0, 5));
    d1.assertValueIs(x2, Interval.top());
    d1.assertValueIs(x6, Interval.top());
    d1.assertValueIs(x8, 8);
    d1.assertValueIs(x9, Interval.top());
    d1.assertValueIs(x7, Interval.of(0, 10));
    d1.assertValueIs(x10, Interval.of(0, 10));
  }

  @Test public void join1 () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    logln("d1:\n" + d1);
    logln();
    RichFiniteDomain d2 = genInitialSystem2(top);
    logln("d2:\n" + d2);
    logln();
    RichFiniteDomain d1vd2 = d1.join(d2);
    logln("d1 " + join + " d2:\n" + d1vd2);
    logln();
    d1vd2.assertValueIs(x6, Interval.TOP);
    RichFiniteDomain fst = d1vd2.equalTo(x1, 1);
    logln("setting x1 to 1:\n" + fst);
    logln();
    fst = fst.substitute(x5, x11);
    fst = fst.substitute(x11, x5);
    fst.assertValueIs(x6, 6);
    assertThat(fst.subsetOrEqual(d1), is(true));
    assertThat(d1.subsetOrEqual(fst), is(true));
    RichFiniteDomain snd = d1vd2.equalTo(x1, 2);
    logln("setting x1 to 2:\n" + snd);
    logln();
    snd = snd.substitute(x5, x11);
    snd = snd.substitute(x11, x5);
    snd.assertValueIs(x5, 5);
    assertThat(snd.subsetOrEqual(d2), is(true));
    assertThat(d2.subsetOrEqual(snd), is(true));
    RichFiniteDomain fstsnd = fst.join(snd);
    assertThat(fstsnd.subsetOrEqual(d1vd2), is(true));
    assertThat(d1vd2.subsetOrEqual(fstsnd), is(true));
    logln("joining both:\n" + fstsnd);
  }

  /**
   * Join of two domains where<br>
   * <pre>
   * d1:= {f1->{x1}, f2->{x2}, f3->{x3}}
   * d2:= {f4->{x1, x2, x3}}
   * </pre>
   */
  @Test public void join2 () {
    // y1 is used as a flag manipulator for x1
    RichFiniteDomain d1 = wrap(top);
    d1 = d1.introduce(x1);
    d1 = d1.introduce(x2);
    d1 = d1.introduce(x3);
    d1 = d1.introduce(y1, 0);
    // put the vars each in its own partition
    d1 = d1.join(d1.assign(x1, 1).assign(y1, 1));
    d1 = d1.join(d1.assign(x2, 2));
    d1 = d1.join(d1.assign(x3, 3));
    logln("d1:\n" + d1);
    logln();

    RichFiniteDomain d2 = wrap(top);
    d2 = d2.introduce(x1);
    d2 = d2.introduce(x2);
    d2 = d2.introduce(x3);
    d2 = d2.introduce(y1, 0);
    // put all the vars in the same partition
    RichFiniteDomain tmp2 = wrap(top);
    tmp2 = tmp2.introduce(x1, 11);
    tmp2 = tmp2.introduce(x2, 22);
    tmp2 = tmp2.introduce(x3, 33);
    tmp2 = tmp2.introduce(y1, 1);
    d2 = d2.join(tmp2);
    logln("d2:\n" + d2);
    logln();
    RichFiniteDomain d2vd1 = d2.join(d1);
    RichFiniteDomain d1vd2 = d1.join(d2);
    logln("d1 " + join + " d2:\n" + d1vd2);
    logln();
    // both must be the same
    assertThat(d1vd2.subsetOrEqual(d2vd1), is(true));
    assertThat(d2vd1.subsetOrEqual(d1vd2), is(true));
    d1vd2.assertValueIs(x1, Interval.TOP);
    logln("setting y1 to 1 and thus making x1 to be defined:");
    RichFiniteDomain fst = d1vd2.equalTo(y1, 1);
    logln(fst);
    fst.assertValueIs(x1, Interval.of(1, 11));
  }

  @Test public void whackyJoin () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    logln("d1:\n" + d1);
    RichFiniteDomain d2 = genInitialSystem2(top);
    logln("d2:\n" + d2);
    RichFiniteDomain d1vd2 = d1.join(d2);
    logln("d1 " + join + " d2:\n" + d1vd2);
    d1vd2.assertValueIs(x6, Interval.top());
    RichFiniteDomain fst = d1vd2.equalTo(x1, 1);
    fst.assertValueIs(x6, 6);
    // create an additional top variable in the state where x6 was set
    fst = fst.project(x6);
    fst = fst.introduce(x6);
    logln("setting x1 to 1 and then setting x6 to top:\n" + fst);
    RichFiniteDomain snd = d1vd2.equalTo(x1, 2);
    snd.assertValueIs(x5, 5);
    // create an additional non-top variable in the state where x6 was undefined
    snd = snd.assign(x6, x5);
    logln("setting x1 to 2 and then set x6:=x5+1:\n" + snd);
    RichFiniteDomain fstsnd = fst.join(snd);
    logln("joining both:\n" + fstsnd);
    fstsnd.assertValueIs(x6, Interval.top());
    fstsnd = fstsnd.equalTo(x1, 2);
    logln("second state of join (x1=2):\n" + fstsnd);
    fstsnd.assertValueIs(x5, 5);
  }

}
