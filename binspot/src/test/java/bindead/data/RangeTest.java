package bindead.data;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.numeric.Range;

import org.junit.Test;

public class RangeTest {
  @Test public void wrapUnsigned001 () {
    Range r = Range.from(Interval.of(254, 258));
    r = r.wrapUnsigned(8);
    assertThat(r.convexHull(), is(Interval.of(0, 255)));
  }

  @Test public void wrapUnsigned002 () {
    Range r = Range.from(Interval.of(256, 258));
    r = r.wrapUnsigned(8);
    assertThat(r.convexHull(), is(Interval.of(0, 2)));
  }

  @Test public void wrapUnsigned003 () {
    Range r = Range.from(Interval.of(512, 513));
    r = r.wrapUnsigned(8);
    assertThat(r.convexHull(), is(Interval.of(0, 1)));
  }

  @Test public void wrapUnsigned004 () {
    Range r = Range.from(Interval.of(13, 42));
    r = r.wrapUnsigned(8);
    assertThat(r.convexHull(), is(Interval.of(13, 42)));
  }

  @Test public void wrapUnsigned005 () {
    Range r = Range.from(Interval.of(-1, 1));
    r = r.wrapUnsigned(8);
    assertThat(r.convexHull(), is(Interval.of(0, 255)));
  }

  @Test public void wrapUnsigned006 () {
    Range r = Range.from(Interval.of(-3, 1));
    r = r.wrapUnsigned(8);
    assertThat(r.convexHull(), is(Interval.of(0, 255)));
  }

  @Test public void wrapUnsigned007 () {
    Range r = Range.from(Interval.of(-3, -1));
    r = r.wrapUnsigned(8);
    assertThat(r.convexHull(), is(Interval.of(253, 255)));
  }

  @Test public void wrapUnsigned008 () {
    Range r = Range.from(Interval.of(-3, 128));
    r = r.wrapUnsigned(8);
    assertThat(r.convexHull(), is(Interval.of(0, 255)));
  }

  @Test public void wrapSigned001 () {
    Range r = Range.from(Interval.of(-3, 128));
    r = r.wrapSigned(8);
    assertThat(r.convexHull(), is(Interval.of(-128, 127)));
  }

  @Test public void wrapSigned002 () {
    Range r = Range.from(Interval.of(-129, -129));
    r = r.wrapSigned(8);
    assertThat(r.convexHull(), is(Interval.of(127)));
  }

  @Test public void wrapSigned003 () {
    Range r = Range.from(Interval.of(128, 128));
    r = r.wrapSigned(8);
    assertThat(r.convexHull(), is(Interval.of(-128)));
  }

  @Test public void wrapSigned004 () {
    Range r = Range.from(Interval.of(129, 129));
    r = r.wrapSigned(8);
    assertThat(r.convexHull(), is(Interval.of(-127)));
  }

  @Test public void wrapSigned005 () {
    Range r = Range.from(Interval.of(-128, -128));
    r = r.wrapSigned(8);
    assertThat(r.convexHull(), is(Interval.of(-128)));
  }

  @Test public void wrapSigned006 () {
    Range r = Range.from(Interval.of(-256, -256));
    r = r.wrapSigned(8);
    assertThat(r.convexHull(), is(Interval.of(0)));
  }

  @Test public void interval000 () {
    Interval i1 = Interval.of(0, 4);
    Interval i2 = Interval.of(0, 8);
    Interval w = i1.widen(i2);
    assertThat(w, is(Interval.upFrom(0)));
  }


  @Test public void wrapSigned007 () {
    Range r = Range.from(Interval.of(-257, -257));
    r = r.wrapSigned(8);
    assertThat(r.convexHull(), is(Interval.of(-1)));
  }

  @Test public void bigint001 () {
    BigInt a = Bound.ZERO;
    BigInt b = BigInt.powerOfTwo(8);
    BigInt r = a.not().and(b);
    assertThat(r, is(BigInt.powerOfTwo(8)));
  }

  @Test public void bigint002 () {
    BigInt a = BigInt.of(0x02);
    BigInt b = BigInt.of(0x04);
    BigInt c = BigInt.of(0x09);
    BigInt d = BigInt.of(0x14);
    BigInt min = BigInt.minOr(a, b, c, d, 8);
    assertThat(min, is(BigInt.of(0xA)));
  }

  @Test public void bigint003 () {
    BigInt a = BigInt.of(0x02);
    BigInt b = BigInt.of(0x04);
    BigInt c = BigInt.of(0x09);
    BigInt d = BigInt.of(0x14);
    BigInt min = BigInt.minOr(a, b, c, d);
    assertThat(min, is(BigInt.of(0xA)));
  }

  @Test public void bigint004 () {
    BigInt a = BigInt.of(0x02);
    BigInt b = BigInt.of(0x04);
    BigInt c = BigInt.of(0x09);
    BigInt d = BigInt.of(0x14);
    BigInt max = BigInt.maxOr(a, b, c, d, 8);
    assertThat(max, is(BigInt.of(0x17)));
  }

  @Test public void bigint005 () {
    BigInt a = BigInt.of(0x02);
    BigInt b = BigInt.of(0x04);
    BigInt c = BigInt.of(0x09);
    BigInt d = BigInt.of(0x14);
    BigInt max = BigInt.maxOr(a, b, c, d);
    assertThat(max, is(BigInt.of(0x17)));
  }

  @Test public void leadingZeroTest001 () {
    Range r = Range.from(Interval.of(54, 63));
    int zeros = r.upperZeroBits(8);
    assertThat(zeros, is(2));
  }

  @Test public void leadingZeroTest002 () {
    Range r = Range.from(Interval.of(54, 256));
    int zeros = r.upperZeroBits(8);
    assertThat(zeros, is(0));
  }

  @Test public void leadingZeroTest003 () {
    Range r = Range.from(Interval.of(0, 0));
    int zeros = r.upperZeroBits(8);
    assertThat(zeros, is(8));
  }

  @Test public void leadingOneTest001 () {
    Range r = Range.from(Interval.of(56, 63));
    int zeros = r.upperOneBits(6);
    assertThat(zeros, is(3));
  }

}
