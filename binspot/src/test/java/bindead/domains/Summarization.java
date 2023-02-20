package bindead.domains;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.numeric.Interval;

import org.junit.Test;

import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.analyses.DomainFactory;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.domainnetwork.interfaces.FiniteDomain;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Summarization {
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

  private static final FiniteDomain topInterval =
      DomainFactory.parseFiniteDomain("Predicates(F) PointsTo Wrapping Intervals");
  private static final FiniteDomain topAffineInterval =
      DomainFactory.parseFiniteDomain("Predicates(F) PointsTo Wrapping RedundantAffine Intervals");
  private static final FiniteDomain topCongruencesInterval =
      DomainFactory.parseFiniteDomain("Predicates(F) PointsTo Wrapping Congruences Intervals");


  private static FiniteDomain genInitialSystem1 (FiniteDomain domain) {
    RichFiniteDomain d = FiniteDomainHelper.for32bitVars(domain);
    d = d.introduce(x1);
    d = d.introduce(x4);
    d = d.introduce(x6);
    d = d.introduce(x7);
    d = d.introduce(x10);
    d = d.assign(x4, Interval.of(0, 4));
    d = d.assign(x7, Interval.of(0, 7));
    d = d.assign(x10, Interval.of(0, 10));
    d = d.assign(x1, x4, x10);
    d = d.assign(x3, x7);
    d = d.assign(x6, x7, x10);
    return d.getWrappedDomain();
  }

  private static FiniteDomain genInitialSystem3 (FiniteDomain domain) {
    RichFiniteDomain d = FiniteDomainHelper.for32bitVars(domain);
    d = d.introduce(x1, 2);
    d = d.introduce(x2, 1);
    d = d.introduce(x3, 5);
    d = d.introduce(x4);
    d = d.introduce(x5, 5);
    d = d.introduce(x6, 6);
    return d.getWrappedDomain();
  }

  private static FiniteDomain genInitialSystem4 (FiniteDomain domain) {
    RichFiniteDomain d = FiniteDomainHelper.for32bitVars(domain);
    d = d.introduce(x1, 4);
    d = d.introduce(x2, 4);
    d = d.introduce(x3, 5);
    d = d.introduce(x4, 1);
    d = d.introduce(x5, 10);
    d = d.introduce(x6, 9);
    return d.getWrappedDomain();
  }

  private static ListVarPair genVarPair1 () {
    ListVarPair vp = new ListVarPair();
    vp.add(x7, x2);
    vp.add(x1, x9);
    vp.add(x6, x8);
    vp.add(x4, x5);
    return vp;
  }

  private static ListVarPair genVarPair2 () {
    ListVarPair vp = new ListVarPair();
    vp.add(x2, x7);
    vp.add(x1, x9);
    vp.add(x6, x8);
    vp.add(x4, x10);
    return vp;
  }

  @Test public void testInterval () {
    FiniteDomain d = genInitialSystem1(topInterval);
    FiniteDomain dRef = d;
    System.out.println("Domain: " + d);
    ListVarPair pairs = genVarPair1();
    d = d.expandNG(pairs);
    System.out.println("Expanding with mapping: " + pairs + "\nResult: " + d);
    d = d.foldNG(pairs);
    System.out.println("Folding with mapping: " + pairs + "\nResult: " + d);
    assertThat(dRef.subsetOrEqual(d), is(true));
    assertThat(d.subsetOrEqual(dRef), is(true));
  }

  @Test public void testCongruence () {
    FiniteDomain d1 = genInitialSystem3(topCongruencesInterval);
    FiniteDomain d2 = genInitialSystem4(topCongruencesInterval);
    FiniteDomain d = (FiniteDomain) d1.join(d2);
    FiniteDomain dRef = d;
    System.out.println("Domain: " + d);
    ListVarPair pairs = genVarPair2();
    d = d.expandNG(pairs);
    System.out.println("Expanding with mapping: " + pairs + "\nResult: " + d);
    assert d!=null;
    d = d.foldNG(pairs);
    System.out.println("Folding with mapping: " + pairs + "\nResult: " + d);
    assertThat(dRef.subsetOrEqual(d), is(true));
    assertThat(d.subsetOrEqual(dRef), is(true));
  }

  @Test public void testAffine () {
    FiniteDomain d = genInitialSystem1(topAffineInterval);
    System.out.println("Domain: " + d);
    FiniteDomain dRef = d;
    ListVarPair pairs = genVarPair1();
    d = d.expandNG(pairs);
    System.out.println("Expanding with mapping: " + pairs + "\nResult: " + d);
    d = d.foldNG(pairs);
    System.out.println("Folding with mapping: " + pairs + "\nResult: " + d);
    assertThat(dRef.subsetOrEqual(d), is(true));
    assertThat(d.subsetOrEqual(dRef), is(true));
  }

}
