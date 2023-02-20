package javalx.numeric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;

import org.junit.Test;


/**
 * Direct tests for {@link IntervalSet}
 */
public class IntervalSetTest {

  @Test public void testNorm () {
    // [1,1], [-1,-1], [0,0] ==> [-1,1]
    final IntervalSet result1 = IntervalSet.valueOf("[1], [-1], [0]");
    compare(result1, arr("[-1,1]"));

    // [-4,-3], [-1,0], [1,1], [3,4], [-3,-2], [1,2] ==> [-4,4]
    final IntervalSet result2 = IntervalSet.valueOf("[-4,-3], [-1,0], [1,1], [3,4], [-3,-2], [1,2]");
    compare(result2, arr("[-4,4]"));

    // [0,1], [0,2], [0,3] ==> [0,3]
    final IntervalSet result3 = IntervalSet.valueOf("[0,1], [0,2], [0,3]");
    compare(result3, arr("[0, 3]"));
  }


  @Test public void testNormInfinity () {
    // [1,1], [-oo,-1], [-1,-1] ==> [-oo,-1], [1,1]
    final IntervalSet result1 = IntervalSet.valueOf("[1,1], [-oo,-1], [-1,-1]");
    compare(result1, arr("[-oo,-1]", "[1,1]"));

    // [1,1], [-oo,-1], [-1,-1], [0] ==> [-oo,1]
    final IntervalSet result2 = IntervalSet.valueOf("[1,1], [-oo,-1], [-1,-1], [0]");
    compare(result2, arr("[-oo,1]"));


    // [1,1], [-1,+oo], [-1,-1] ==> [-1,+oo]
    final IntervalSet result3 = IntervalSet.valueOf("[1,1], [-1,+oo], [-1,-1]");
    compare(result3, arr("[-1,+oo]"));

    // [1,1], [0,+oo], [-1,-1] ==> [-1,+oo]
    final IntervalSet result4 = IntervalSet.valueOf("[1,1], [0,+oo], [-1,-1]");
    compare(result4, arr("[-1,+oo]"));

    // [1,1], [0,+oo], [-oo,-1] ==> [-oo,+oo]
    final IntervalSet result5 = IntervalSet.valueOf("[1,1], [0,+oo], [-oo,-1]");
    compare(result5, arr(Interval.TOP));
  }


  @Test public void testJoin () {
    // Basically the same as norm, so no special cases for intervals needed here.
    // [-4,-3], [-1,-1], [1,2], [4,4] join [-2,0], [3,3] ==> [-4,4]
    final IntervalSet set11 = IntervalSet.valueOf("[-4, -3], [-1, -1], [1, 2], [4, 4]");
    final IntervalSet set12 = IntervalSet.valueOf("[-2, 0], [3, 3]");
    final IntervalSet result1 = set11.join(set12);
    compare(result1, arr("[-4, 4]"));

    final IntervalSet result12 = set12.join(set11);
    compare(result12, arr("[-4, 4]"));
  }


  @Test public void testJoinWithInfinity () {
    // Basically the same as norm, so no special cases for intervals needed here.
    // [-6] [-4,-3], [-1,-1], [1,2], [4,4] join [-oo,0] ==> [-oo,2], [4]
    final IntervalSet set11 = IntervalSet.valueOf("[-6], [-4, -3], [-1], [1, 2], [4, 4]");
    final IntervalSet set12 = IntervalSet.valueOf("[-oo, 0]");
    final IntervalSet result1 = set11.join(set12);
    compare(result1, arr("[-oo, 2]", "[4]"));
  }

  @Test public void testWidenGrowOutside () {
    // [0] [2,4] [6] widen [-1,0] [2,4] [6] => [-oo,0] [2,4] [6]
    final IntervalSet set11 = IntervalSet.valueOf("[0], [2, 4], [6, 6]");
    final IntervalSet set12 = IntervalSet.valueOf("[-1, 0], [2, 4], [6, 6]");
    final IntervalSet result1 = set11.widen(set12);
    compare(result1, arr("[-oo,0]", "[2, 4]", "[6, 6]"));

    // [0] [2,3] [5] widen [0] [2,3] [5,6] => [0] [2,3] [5,+oo]
    final IntervalSet set21 = IntervalSet.valueOf("[0], [2, 3], [5]");
    final IntervalSet set22 = IntervalSet.valueOf("[0], [2, 3], [5, 6]");
    final IntervalSet result2 = set21.widen(set22);
    compare(result2, arr("[0]", "[2, 3]", "[5, +oo]"));

    // [0] [4] widen [0] [4] [8] => [0] [4, +oo]
    final IntervalSet set31 = IntervalSet.valueOf("[0], [4]");
    final IntervalSet set32 = IntervalSet.valueOf("[0], [4], [8]");
    final IntervalSet result3 = set31.widen(set32);
    compare(result3, arr("[0]", "[4, +oo]"));

    // [4] [8] widen [0] [4] [8] => [-oo, 4] [8]
    final IntervalSet set41 = IntervalSet.valueOf("[4], [8]");
    final IntervalSet set42 = IntervalSet.valueOf("[0], [4], [8]");
    final IntervalSet result4 = set41.widen(set42);
    compare(result4, arr("[-oo, 4]", "[8]"));
  }

  @Test public void testWidenGrowInsidePos () {
    // [0] [2,3] [6] widen [0] [2,4] [6] => [0] [2,6]
    final IntervalSet set31 = IntervalSet.valueOf("[0], [2, 3], [6]");
    final IntervalSet set32 = IntervalSet.valueOf("[0], [2, 4], [6]");
    final IntervalSet result3 = set31.widen(set32);
    compare(result3, arr("[0]", "[2, 6]"));

    // [0] [2,4] [6] widen [0] [2,6] => [0] [2,6]
    final IntervalSet set41 = IntervalSet.valueOf("[0], [2, 4], [6]");
    final IntervalSet set42 = IntervalSet.valueOf("[0], [2, 6]");
    final IntervalSet result4 = set41.widen(set42);
    compare(result4, arr("[0]", "[2, 6]"));

    // [0] [6] widen [0] [2] [6] => [0] [2] [6]
    final IntervalSet set51 = IntervalSet.valueOf("[0], [6]");
    final IntervalSet set52 = IntervalSet.valueOf("[0], [2], [6]");
    final IntervalSet result5 = set51.widen(set52);
    compare(result5, arr("[0]", "[2]", "[6]"));
  }

  @Test public void testWidenGrowInsideEat () {
    // [0] [2,3] [5,6] [8] widen [0,6] [8] => [0,6] [8]
    final IntervalSet set11 = IntervalSet.valueOf("[0], [2, 3], [5, 6], [8]");
    final IntervalSet set12 = IntervalSet.valueOf("[0, 6], [8]");
    final IntervalSet result1 = set11.widen(set12);
    compare(result1, arr("[0, 6]", "[8]"));
  }

  @Test public void testWidenGrowInsideNeg () {
    // [0] [3,4] [6] widen [0] [2,4] [6] => [0,4] [6]
    final IntervalSet set51 = IntervalSet.valueOf("[0], [3, 4], [6]");
    final IntervalSet set52 = IntervalSet.valueOf("[0], [2, 4], [6]");
    final IntervalSet result5 = set51.widen(set52);
    compare(result5, arr("[0, 4]", "[6]"));

    // [0] [2,4] [6] widen [0,4] [6] => [0,4] [6]
    final IntervalSet set61 = IntervalSet.valueOf("[0], [2, 4], [6]");
    final IntervalSet set62 = IntervalSet.valueOf("[0, 4], [6]");
    final IntervalSet result6 = set61.widen(set62);
    compare(result6, arr("[0, 4]", "[6]"));
  }

  @Test public void testWidenGrowAndAppearOutside () {
    // [-2,-1] [1,3] widen [-2,-1] [1,4] [6] => [-2,-1] [1,+oo]
    final IntervalSet set11 = IntervalSet.valueOf("[-2, -1], [1, 3]");
    final IntervalSet set12 = IntervalSet.valueOf("[-2, -1], [1, 4], [6]");
    final IntervalSet result1 = set11.widen(set12);
    compare(result1, arr("[-2, -1]", "[1, +oo]"));

    // [-2,-1] [1,3] widen [-6] [-3,-1] [1,3] => [-oo,-1] [1,3]
    final IntervalSet set21 = IntervalSet.valueOf("[-2, -1], [1, 3]");
    final IntervalSet set22 = IntervalSet.valueOf("[-6], [-3, -1], [1, 3]");
    final IntervalSet result2 = set21.widen(set22);
    compare(result2, arr("[-oo, -1]", "[1, 3]"));
  }

  @Test public void testWidenAppearInside () {
    // [0] [6] widen [0] [3,4] [6] => [0] [3,4] [6]
    final IntervalSet set11 = IntervalSet.valueOf("[0], [6, 6]");
    final IntervalSet set12 = IntervalSet.valueOf("[0], [3, 4], [6, 6]");
    final IntervalSet result1 = set11.widen(set12);
    compare(result1, arr("[0]", "[3, 4]", "[6]"));
  }

  @Test public void testWidenAppearOutside () {
    // [2,3] [5,6] widen [0] [2,3] [5,6] => [-oo,3] [5,6]
    final IntervalSet set11 = IntervalSet.valueOf("[2, 3], [5, 6]");
    final IntervalSet set12 = IntervalSet.valueOf("[0], [2, 3], [5, 6]");
    final IntervalSet result1 = set11.widen(set12);
    compare(result1, arr("[-oo, 3]", "[5, 6]"));

    // [2,3] [7] widen [0] [2,5] [7] => [-oo,7]
    final IntervalSet set21 = IntervalSet.valueOf("[2, 3], [7]");
    final IntervalSet set22 = IntervalSet.valueOf("[0], [2, 5], [7]");
    final IntervalSet result2 = set21.widen(set22);
    compare(result2, arr("[-oo, 7]"));

    // [-5,+oo] widen [-9], [-6,+oo] => [-oo, +oo]]
    final IntervalSet set31 = IntervalSet.valueOf("[-5,+oo]");
    final IntervalSet set32 = IntervalSet.valueOf("[-9], [-6,+oo]");
    final IntervalSet result3 = set31.widen(set32);
    compare(result3, arr("[-oo, +oo]"));
  }

  @Test public void testWidenDisappearInside () {
    try {
      // [0] [2,3] [5,6] widen [0] [5,6] => [0] [5,6]
      final IntervalSet set11 = IntervalSet.valueOf("[0], [2, 3], [5, 6]");
      final IntervalSet set12 = IntervalSet.valueOf("[0], [5, 6]");
      final IntervalSet result1 = set11.widen(set12);
      fail("Expected IllegalArgumentException!");
      compare(result1, arr("[0]", "[5, 6]"));
    } catch (IllegalArgumentException err) {
      // Success
    }

    try {
      // [-1] [1] [7] widen [-2,-1] [7] => [-oo,-1] [7]
      final IntervalSet set21 = IntervalSet.valueOf("[-1], [1], [7]");
      final IntervalSet set22 = IntervalSet.valueOf("[-2, -1], [7]");
      final IntervalSet result2 = set21.widen(set22);
      fail("Expected IllegalArgumentException!");
      compare(result2, arr("[-oo, -1]", "[7]"));
    } catch (IllegalArgumentException err) {
      // Success
    }
  }

  @Test public void testWidenShrinkAndGrow () {
    try {
      // [0,1] [4,5] [7,8] widen [1,2] [4,5] [7,8] => [1,5] [7,8]
      final IntervalSet set11 = IntervalSet.valueOf("[0, 1], [4, 5], [7, 8]");
      final IntervalSet set12 = IntervalSet.valueOf("[1, 2], [4, 5], [7, 8]");
      final IntervalSet result1 = set11.widen(set12);
      fail("Expected IllegalArgumentException!");
      compare(result1, arr("[1, 5]", "[7, 8]"));
    } catch (IllegalArgumentException err) {
      // Success
    }

    try {
      // [-1] [1,2] [7] widen [-1] [2,5] [7] => [-1] [2,7]
      final IntervalSet set21 = IntervalSet.valueOf("[-1], [1, 2], [7]");
      final IntervalSet set22 = IntervalSet.valueOf("[-1], [2, 5], [7]");
      final IntervalSet result2 = set21.widen(set22);
      fail("Expected IllegalArgumentException!");
      compare(result2, arr("[-1]", "[2, 7]"));
    } catch (IllegalArgumentException err) {
      // Success
    }
  }

  @Test public void testWidenMoveOutside () {
    try {
      // [0] [2,3] [8] widen [0] [2,4] [6] => [0] [2,6]
      final IntervalSet set11 = IntervalSet.valueOf("[0], [2, 3], [8]");
      final IntervalSet set12 = IntervalSet.valueOf("[0], [2, 4], [6]");
      final IntervalSet result1 = set11.widen(set12);
      fail("Expected IllegalArgumentException!");
      compare(result1, arr("[0]", "[2, 6]"));
    } catch (IllegalArgumentException err) {
      // Success
    }

    try {
      // [0] [2,3] [6] widen [0] [2,4] [7] => [0] [2,7]
      final IntervalSet set21 = IntervalSet.valueOf("[0], [2, 3], [6]");
      final IntervalSet set22 = IntervalSet.valueOf("[0], [2, 4], [7]");
      final IntervalSet result2 = set21.widen(set22);
      fail("Expected IllegalArgumentException!");
      compare(result2, arr("[0]", "[2, 7]"));
    } catch (IllegalArgumentException err) {
      // Success
    }
  }

  @Test public void testSubsetOrEqual () {
    // [0] [2,3] [6,8] subsetOrEqual [0] [2,4] [6,8] => True
    final IntervalSet set11 = IntervalSet.valueOf("[0], [2, 3], [6, 8]");
    final IntervalSet set12 = IntervalSet.valueOf("[0], [2, 4], [6, 8]");
    assertTrue(set11.subsetOrEqual(set12));

    // [0] [2,3] [5,6] subsetOrEqual [0] [2,3] [5,6] [8] => True
    final IntervalSet set21 = IntervalSet.valueOf("[0], [2, 3], [5, 6]");
    final IntervalSet set22 = IntervalSet.valueOf("[0], [2, 3], [5, 6], [8]");
    assertTrue(set21.subsetOrEqual(set22));
  }

  @Test public void testMeet () {
    // [0] [2,3] [5,8] meet [-3,-2] [2,4] [6,8] => [2,3] [6,8]
    final IntervalSet set11 = IntervalSet.valueOf("[0], [2, 3], [5, 8]");
    final IntervalSet set12 = IntervalSet.valueOf("[-3, -2], [2, 4], [6, 8]");
    final Option<IntervalSet> result11 = set11.meet(set12);
    final Option<IntervalSet> result12 = set12.meet(set11);
    compare(result11.get(), arr("[2, 3]", "[6, 8]"));
    compare(result11.get(), result12.get());

    // [-2,0] [2,3] [5,6] meet [0,2] [5,7] => [0] [2] [5,6]
    final IntervalSet set21 = IntervalSet.valueOf("[-2, 0], [2, 3], [5, 6]");
    final IntervalSet set22 = IntervalSet.valueOf("[0, 2], [5, 7]");
    final Option<IntervalSet> result21 = set21.meet(set22);
    final Option<IntervalSet> result22 = set22.meet(set21);
    compare(result21.get(), arr("[0]", "[2]", "[5, 6]"));
    compare(result21.get(), result22.get());
  }

  @Test public void testCompareSameLength () {
    // [0] [2] [4] [6] compareTo [0] [2] [4] [8] => < 0
    final IntervalSet set11 = IntervalSet.valueOf("[0], [2], [4], [6]");
    final IntervalSet set12 = IntervalSet.valueOf("[0], [2], [4], [8]");
    final int result1 = set11.compareTo(set12);
    assertTrue(result1 < 0);

    // [0] [2] [4] [8] compareTo [0] [2] [4] [6] => > 0
    final IntervalSet set21 = IntervalSet.valueOf("[0], [2], [4], [8]");
    final IntervalSet set22 = IntervalSet.valueOf("[0], [2], [4], [6]");
    final int result2 = set21.compareTo(set22);
    assertTrue(result2 > 0);
  }

  @Test public void testCompareDifferentLength () {
    // [0] [2] [4] compareTo [0] [2] [4] [8] => < 0
    final IntervalSet set11 = IntervalSet.valueOf("[0], [2], [4]");
    final IntervalSet set12 = IntervalSet.valueOf("[0], [2], [4], [8]");
    final int result1 = set11.compareTo(set12);
    assertTrue(result1 < 0);

    // [0] [2] [4] [8] compareTo [0] [2] [4] => > 0
    final IntervalSet set21 = IntervalSet.valueOf("[0], [2], [4], [8]");
    final IntervalSet set22 = IntervalSet.valueOf("[0], [2], [4]");
    final int result2 = set21.compareTo(set22);
    assertTrue(result2 > 0);
  }

  @Test public void iteratorIntWithStride () {
    // [[0,3] [5,8] [10,15]].iterate(stride = 3) => "0, 3, 6, 12, 15"
    final IntervalSet set1 = IntervalSet.valueOf("[0, 3], [5, 8], [10, 15]");
    final Iterator<BigInt> it1 = set1.iteratorIntWithStride(BigInt.of(3));
    final List<BigInt> result1 = new LinkedList<BigInt>();
    while (it1.hasNext()) {
      result1.add(it1.next());
    }
    compare(result1, list(0, 3, 6, 12, 15));


    // [[-1,3] [5,7] [10,15]].iterate(stride = 3) => "-1, 2, 5, 11, 14"
    final IntervalSet set2 = IntervalSet.valueOf("[-1, 3], [5, 7], [10, 15]");
    final Iterator<BigInt> it2 = set2.iteratorIntWithStride(BigInt.of(3));
    final List<BigInt> result2 = new LinkedList<BigInt>();
    while (it2.hasNext()) {
      result2.add(it2.next());
    }
    compare(result2, list(-1, 2, 5, 11, 14));
  }

  @Test public void testValueOfString() throws NumberFormatException {
    IntervalSet result1 = IntervalSet.valueOf("{1, 3, 5, [7,8]}");
    IntervalSet expected1 = IntervalSet.valueOf("[1], [3], [5], [7,8]");
    compare(result1, expected1);

    IntervalSet result2 = IntervalSet.valueOf("{1, 3, [5,7], 9}");
    IntervalSet expected2 = IntervalSet.valueOf("[1], [3], [5,7], [9]");
    compare(result2, expected2);

    IntervalSet result3 = IntervalSet.valueOf("{1, 3, [5,7], [9,+oo]}");
    IntervalSet expected3 = IntervalSet.valueOf("[1], [3], [5,7], [9,+oo]");
    compare(result3, expected3);

    IntervalSet result4 = IntervalSet.valueOf("{[-oo,1], 3, [5,7], 9}");
    IntervalSet expected4 = IntervalSet.valueOf("[-oo,1], [3], [5,7], [9]");
    compare(result4, expected4);

    IntervalSet result5 = IntervalSet.valueOf("{1, 3, [-oo,+oo], 9}");
    IntervalSet expected5 = IntervalSet.valueOf("[-oo,+oo]");
    compare(result5, expected5);
  }

  private static void compare (IntervalSet actual, IntervalSet expected) {
    compare(actual, expected.getIntervals());
  }

  private static void compare (IntervalSet actual, Interval[] expected) {
    compare(actual, Arrays.asList(expected));
  }

  private static void compare (IntervalSet actual, List<Interval> expected) {
    compare(actual.getIntervals(), expected);
  }

  private static <T> void compare (List<T> actual, List<T> expected) {
    final Iterator<T> it = actual.iterator();
    for (T expectedInterval : expected) {
      final T actualInterval = it.next();
      assertEquals(expectedInterval, actualInterval);
    }
    assertEquals(expected.size(), actual.size());
  }

  private static Interval[] arr (String... strs) {
    LinkedList<Interval> intervals = new LinkedList<Interval>();
    for (String str : strs) {
      intervals.add(Interval.of(str));
    }
    return intervals.toArray(new Interval[intervals.size()]);
  }

  private static <T> T[] arr (T... ts) {
    return ts;
  }

  private static List<BigInt> list (long... longs) {
    final List<BigInt> result = new ArrayList<BigInt>(longs.length);
    for (Long l : longs)
      result.add(BigInt.of(l));
    return result;
  }
}
