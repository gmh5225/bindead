package javalx.numeric;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javalx.data.Option;

import org.junit.Test;

public class IntervalTest {
  @Test public void parseFromString () {
    assertThat(Interval.of("[-1, 14]"), is(Interval.of(-1, 14)));
    assertThat(Interval.of("[5]"), is(Interval.of(5)));
    assertThat(Interval.of("8"), is(Interval.of(8)));
    assertThat(Interval.of("[-oo, 10]"), is(Interval.downFrom(10)));
    assertThat(Interval.of("[0, +oo]"), is(Interval.upFrom(0)));
    assertThat(Interval.of("[-3, 0x12]"), is(Interval.of(-3, 0x12)));
  }

  @Test public void subtraction () {
    Interval a = Interval.of("[-oo, 0]");
    Interval b = Interval.of("[-oo, +oo]");
    Interval c = a.sub(b);
    assertThat(c, is(Interval.of("[-oo, +oo]")));
  }

  @Test public void multiplication1 () {
    Interval a = Interval.of(BigInt.of(-2), BigInt.of(3));
    Interval b = Interval.of(BigInt.of(-3), BigInt.of(-2));
    Interval m = a.mul(b);
    assertThat(BigInt.of(-9), is(m.low().asInteger()));
    assertThat(BigInt.of(6), is(m.high().asInteger()));
  }

  @Test public void multiplication2 () {
    Interval a = Interval.downFrom(BigInt.of(9));
    Interval b = Interval.MINUSONE;
    Interval m = a.mul(b);
    assertThat(BigInt.of(-9), is(m.low().asInteger()));
  }

  @Test public void division () {
    Interval a = Interval.upFrom(10); // [10, +oo]
    Interval result1 = a.divRoundInvards(BigInt.of(11));
    assertThat(result1.low().asInteger(), is(BigInt.of(1)));
    Interval result2 = a.divRoundZero(Interval.of(11));
    assertThat(result2.low().asInteger(), is(BigInt.of(0)));
  }

  @Test public void unsignedOr001 () {
    Interval x = Interval.of(0x02, 0x04);
    Interval y = Interval.of(0x09, 0x14);
    assertThat(x.unsignedOr(y), is(Interval.of(0x0A, 0x17)));
  }

  @Test public void unsignedOr002 () {
    Interval x = Interval.of(0x02, 0x04);
    Interval y = Interval.of(0x09, 0x14);
    assertThat(x.unsignedOr(y), is(x.slowUnsignedOr(y)));
  }

  @Test public void isAdjacent() {
     Interval a = Interval.of(BigInt.of(-2), BigInt.of(0));
     Interval b = Interval.of(BigInt.of(1), BigInt.of(2));
     assertTrue(a.isAdjacent(b));
     assertTrue(b.isAdjacent(a));

     // Overlaps and...?
     Interval c = Interval.of(BigInt.of(0), BigInt.of(2));
     assertFalse(a.isAdjacent(c));
     assertFalse(b.isAdjacent(c));

     // Contains and...?
     Interval d = Interval.of(BigInt.of(-3), BigInt.of(0));
     assertFalse(a.isAdjacent(d));
     assertFalse(d.isAdjacent(a));
     Interval e = Interval.of(BigInt.of(-3), BigInt.of(1));
     assertFalse(a.isAdjacent(e));
     assertFalse(e.isAdjacent(a));

     // Unbounded?
     Interval f = Interval.downFrom(BigInt.of(0));
     assertFalse(a.isAdjacent(f));
     assertFalse(f.isAdjacent(a));
  }

  @Test public void joinOfDisjointIntervals () {
    Interval a = Interval.of(BigInt.of(-2), BigInt.of(-1));
    Interval b = Interval.of(BigInt.of(1), BigInt.of(2));
    Interval joined = a.join(b);
    assertThat(BigInt.of(-2), is(joined.low().asInteger()));
    assertThat(BigInt.of(2), is(joined.high().asInteger()));
  }

  @Test public void meetOfDisjointIntervalsShouldBeNone () {
    Interval a = Interval.of(BigInt.of(-2), BigInt.of(-1));
    Interval b = Interval.of(BigInt.of(1), BigInt.of(2));
    Option<Interval> meet = a.meet(b);
    assertThat(meet.isNone(), is(true));
  }

  @Test public void meetOfOverlappingButNotContainedIntervals () {
    Interval a = Interval.of(BigInt.of(0), BigInt.of(2));
    Interval b = Interval.of(BigInt.of(1), BigInt.of(3));
    Interval meet = a.meet(b).get();
    assertThat(BigInt.of(1), is(meet.low().asInteger()));
    assertThat(BigInt.of(2), is(meet.high().asInteger()));
  }

  @Test public void joinOfOverlappingButNotContainedIntervals () {
    Interval a = Interval.of(BigInt.of(0), BigInt.of(2));
    Interval b = Interval.of(BigInt.of(1), BigInt.of(3));
    Interval join = a.join(b);
    assertThat(BigInt.of(0), is(join.low().asInteger()));
    assertThat(BigInt.of(3), is(join.high().asInteger()));
  }

  @Test public void evalEqualToZero000 () {
    Interval i = Interval.ZERO;
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.ONE));
  }

  @Test public void evalEqualToZero001 () {
    Interval i = Interval.BOOLEANTOP;
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalEqualToZero002 () {
    Interval i = Interval.ONE;
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalEqualToZero003 () {
    Interval i = Interval.MINUSONE.join(Interval.ZERO);
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalEqualToZero004 () {
    Interval i = Interval.MINUSONE;
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalEqualToZero005 () {
    Interval i = Interval.MINUSONE.join(Interval.ONE);
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalEqualToZero006 () {
    Interval i = Interval.TOP;
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalEqualToZero007 () {
    Interval i = Interval.downFrom(BigInt.of(0));
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalEqualToZero008 () {
    Interval i = Interval.downFrom(BigInt.of(-1));
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalEqualToZero009 () {
    Interval i = Interval.upFrom(BigInt.of(0));
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalEqualToZero010 () {
    Interval i = Interval.upFrom(BigInt.of(1));
    Interval bool = i.evalEqualToZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalNotEqualToZero000 () {
    Interval i = Interval.ZERO;
    Interval bool = i.evalNotEqualToZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalNotEqualToZero001 () {
    Interval i = Interval.BOOLEANTOP;
    Interval bool = i.evalNotEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalNotEqualToZero002 () {
    Interval i = Interval.ONE;
    Interval bool = i.evalNotEqualToZero();
    assertThat(bool, is(Interval.ONE));
  }

  @Test public void evalNotEqualToZero003 () {
    Interval i = Interval.MINUSONE.join(Interval.ZERO);
    Interval bool = i.evalNotEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalNotEqualToZero004 () {
    Interval i = Interval.MINUSONE;
    Interval bool = i.evalNotEqualToZero();
    assertThat(bool, is(Interval.ONE));
  }

  @Test public void evalNotEqualToZero005 () {
    Interval i = Interval.MINUSONE.join(Interval.ONE);
    Interval bool = i.evalNotEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalNotEqualToZero006 () {
    Interval i = Interval.TOP;
    Interval bool = i.evalNotEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalLessThanOrEquaToZero000 () {
    Interval i = Interval.ZERO;
    Interval bool = i.evalLessThanOrEqualToZero();
    assertThat(bool, is(Interval.ONE));
  }

  @Test public void evalLessThanOrEquaToZero001 () {
    Interval i = Interval.BOOLEANTOP;
    Interval bool = i.evalLessThanOrEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalLessThanOrEquaToZero002 () {
    Interval i = Interval.ONE;
    Interval bool = i.evalLessThanOrEqualToZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalLessThanOrEquaToZero003 () {
    Interval i = Interval.MINUSONE.join(Interval.ZERO);
    Interval bool = i.evalLessThanOrEqualToZero();
    assertThat(bool, is(Interval.ONE));
  }

  @Test public void evalLessThanOrEquaToZero004 () {
    Interval i = Interval.MINUSONE;
    Interval bool = i.evalLessThanOrEqualToZero();
    assertThat(bool, is(Interval.ONE));
  }

  @Test public void evalLessThanOrEquaToZero005 () {
    Interval i = Interval.MINUSONE.join(Interval.ONE);
    Interval bool = i.evalLessThanOrEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalLessThanOrEquaToZero006 () {
    Interval i = Interval.TOP;
    Interval bool = i.evalLessThanOrEqualToZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalLessThanZero000 () {
    Interval i = Interval.ZERO;
    Interval bool = i.evalLessThanZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalLessThanZero001 () {
    Interval i = Interval.BOOLEANTOP;
    Interval bool = i.evalLessThanZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalLessThanZero002 () {
    Interval i = Interval.ONE;
    Interval bool = i.evalLessThanZero();
    assertThat(bool, is(Interval.ZERO));
  }

  @Test public void evalLessThanZero003 () {
    Interval i = Interval.MINUSONE.join(Interval.ZERO);
    Interval bool = i.evalLessThanZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalLessThanZero004 () {
    Interval i = Interval.MINUSONE;
    Interval bool = i.evalLessThanZero();
    assertThat(bool, is(Interval.ONE));
  }

  @Test public void evalLessThanZero005 () {
    Interval i = Interval.MINUSONE.join(Interval.ONE);
    Interval bool = i.evalLessThanZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void evalLessThanZero006 () {
    Interval i = Interval.TOP;
    Interval bool = i.evalLessThanZero();
    assertThat(bool, is(Interval.BOOLEANTOP));
  }

  @Test public void intervalIterator001 () {
    Interval i = Interval.of(0, 0);
    List<BigInt> values = new ArrayList<BigInt>();
    for (BigInt x : i) {
      values.add(x);
    }
    assertThat(values.size(), is(1));
    assertThat(values, hasItem(BigInt.ZERO));
  }

  @Test public void intervalIterator002 () {
    Interval i = Interval.of(0, 2);
    List<BigInt> values = new ArrayList<BigInt>();
    for (BigInt x : i) {
      values.add(x);
    }
    assertThat(values.size(), is(3));
    assertThat(values, hasItems(BigInt.ZERO, BigInt.ONE, BigInt.of(2)));
  }

  @Test(expected = UnsupportedOperationException.class) public void intervalIterator003 () {
    Interval i = Interval.upFrom(BigInt.ZERO);
    List<BigInt> values = new ArrayList<BigInt>();
    for (BigInt x : i) { // infinite intervals cannot be iterated over
      values.add(x);
    }
  }
}
