package bindead.domains.intervals;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.num;
import static bindead.data.Linear.term;
import static bindead.debug.DebugHelper.log;
import static bindead.debug.DebugHelper.logln;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;



import org.junit.Test;

import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.analyses.DomainFactory;
import bindead.data.NumVar;
import bindead.domainnetwork.interfaces.FiniteDomain;

@SuppressWarnings({"rawtypes"})
public class IntervalSetsTest {
  private static final FiniteDomain top =
    DomainFactory.parseFiniteDomain("Wrapping Affine Congruences IntervalSets");

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
    logln("initialization:\n" + d);
    d.assertValueIs(x3, 3);
    d.assertValueIs(x5, Interval.top());
    d.assertValueIs(x9, 9);
    logln();
    logln("x5 = x3 + 2");
    logln("x6 = x6 + x2");
    d = d.assign(x5, linear(num(2), term(x3)));
    d = d.assign(x6, x6, x2);
    logln();
    logln(d);
    d.assertValueIs(x5, 5);
    d.assertValueIs(x6, Interval.top());
  }

  @Test public void testJoin () {
    RichFiniteDomain d1 = genInitialSystem1(top);
    RichFiniteDomain d2 = genInitialSystem2(top);
    RichFiniteDomain d12 = d1.join(d2);
    logln("d1.join(d2):");
    logln("d1:\n" + d1);
    logln("d2:\n" + d2);
    logln("d12:\n" + d12);
    // d12 = d12.substitute(x11, x3); // works, commented because of eval followed by subsetOrEqual
    // logln("\nsubstitute(x11,x3):\nd12:\n" + d12);
    assertThat(d1.subsetOrEqual(d2), is(false));
    d12.assertValueIs(x6, Interval.TOP);
    RichFiniteDomain fst = d1.equalTo(x1, 1);
    fst = fst.substitute(x5, x11);
    fst = fst.substitute(x11, x5);
    logln("\nsetting x1 to 1 in d1:\n" + fst);
    fst.assertValueIs(x6, 6);
    assertThat(fst.subsetOrEqual(d1), is(true));
    assertThat(d1.subsetOrEqual(fst), is(true));
    RichFiniteDomain snd = d2.equalTo(x1, 2);
    snd = snd.substitute(x5, x11);
    snd = snd.substitute(x11, x5);
    logln("\nsetting x1 to 2 in d2:\n" + snd);
    snd.assertValueIs(x5, 5);
    assertThat(snd.subsetOrEqual(d2), is(true));
    assertThat(d2.subsetOrEqual(snd), is(true));
    RichFiniteDomain fstsnd = fst.join(snd);
    assertThat(fstsnd.subsetOrEqual(d12), is(true));
    assertThat(d12.subsetOrEqual(fstsnd), is(true));
    logln("\njoining both:\n" + fstsnd);
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
    logln("\nsetting x1 to 1 and then setting x6 to top in d1:\n" + fst);
    RichFiniteDomain snd = d2.equalTo(x1, 2);
    snd.assertValueIs(x5, 5);
    // create an additional non-top variable in the state where x6 was undefined
    snd = snd.assign(x6, linear(Bound.ONE, term(x5)));
    logln("\nsetting x1 to 2 and then set x6:=x5+1 in d2:\n" + snd);

    // create an additional non-top variable in the state where x4 was undefined
    snd = snd.assign(x4, linear(BigInt.of(3), term(x5), term(BigInt.of(2), x6)));
    logln("\nsetting x1 to 2 and then set x4:=x5 + 2*x6 + 3 in d2:\n" + snd);

    RichFiniteDomain fstsnd = fst.join(snd);
    logln("\njoining both:\n" + fstsnd);
    fstsnd.assertValueIs(x6, Interval.TOP);
    fstsnd = fstsnd.equalTo(x1, 2);
    logln("\nsecond state of join (x1=2):\n" + fstsnd);
    snd.assertValueIs(x5, 5);
  }

  @Test public void testWhackierJoin () {
    RichFiniteDomain d3 = genInitialSystem3(top);
    RichFiniteDomain d4 = genInitialSystem4(top);
    RichFiniteDomain d34 = d3.join(d4);
    logln("d3:\n" + d3);
    logln("d4:\n" + d4);
    logln("d34:\n" + d34);

    RichFiniteDomain fst = d34.assign(x4, x1, x3);
    logln("\nsetting x4:=x1+x3 in d34:\n" + fst);

    RichFiniteDomain snd = d34.assign(x6, x7);
    logln("\nsetting x6:=x7 in d34:\n" + snd);

    RichFiniteDomain thrd = d34.lessOrEqualTo(x5, x6);
    logln("\nsetting x5<=x6 in d34:\n" + thrd);

    RichFiniteDomain fourth = d34.lessOrEqualTo(x8, x6);
    logln("\nsetting x8<=x6 in d34:\n" + fourth);

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
}
