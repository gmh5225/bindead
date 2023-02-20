package javalx.persistentcollections.tree;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import javalx.data.products.P2;
import javalx.numeric.FiniteRange;
import javalx.numeric.Interval;

import org.junit.Test;

/**
 *
 * @author mb0
 */
public class FiniteRangeTreeTest {
  private static final String testRoot = FiniteRangeTreeTest.class.getResource("/javalx/persistentcollections/tree")
      .getPath();


  @Test public void bindIntervals004 () {
    FiniteRangeTree<Integer> t = buildTree1();
    assertThat(t.size(), is(10));
  }

  @Test public void bindIntervals005 () {
    FiniteRangeTree<Integer> t = buildTree2();
    assertThat(t.size(), is(10));
  }

  @Test public void overlappingIntervals001 () {
    FiniteRangeTree<Integer> t = buildTree1();
    OverlappingRanges<Integer> overlapping = t.searchOverlaps(FiniteRange.of(2, 6));
    assertThat(containsInterval(overlapping, 5, 10), is(true));
    assertThat(containsInterval(overlapping, 4, 10), is(true));
    assertThat(containsInterval(overlapping, 3, 8), is(true));
    assertThat(containsInterval(overlapping, 1, 11), is(true));
    assertThat(containsInterval(overlapping, 2, 13), is(true));
    assertThat(containsInterval(overlapping, 6, 14), is(true));
    assertThat(overlapping.size(), is(6));
  }

  @Test public void overlappingIntervals002 () {
    FiniteRangeTree<Integer> t = buildTree2();
    OverlappingRanges<Integer> overlapping = t.searchOverlaps(FiniteRange.of(2, 6));
    assertThat(containsInterval(overlapping, 0, 3), is(true));
    assertThat(containsInterval(overlapping, 5, 8), is(true));
    assertThat(containsInterval(overlapping, 6, 10), is(true));
    assertThat(overlapping.size(), is(3));
  }


  @Test public void renderIntervalTree001 () {
    FiniteRangeTree<Integer> t = buildTree1();
    FiniteRangeTree.renderAsGml(t, testRoot + "/tree001.gml");
    assertThat(t.size(), is(10));
  }

  @Test public void renderIntervalTree002 () {
    FiniteRangeTree<Integer> t = buildTree2();
    FiniteRangeTree.renderAsGml(t, testRoot + "/tree002.gml");
    assertThat(t.size(), is(10));
  }

  private static boolean containsInterval (OverlappingRanges<Integer> overlapping, int low, int high) {
    final Interval i = Interval.of(low, high);
    for (P2<FiniteRange, ?> node : overlapping) {
      if (node._1().toInterval().isEqualTo(i))
        return true;
    }
    return false;
  }

  @SuppressWarnings("unused") private static boolean containsIntervalWithValue (List<P2<Interval, Integer>> intervals,
      int low, int high, int value) {
    final Interval i = Interval.of(low, high);
    for (P2<Interval, Integer> node : intervals) {
      if (node._1().equals(i) && value == node._2().intValue())
        return true;
    }
    return false;
  }

  private static FiniteRangeTree<Integer> buildTree1 () {
    FiniteRangeTree<Integer> t = FiniteRangeTree.empty();
    t = t.bind(FiniteRange.of(7, 11), 1);
    t = t.bind(FiniteRange.of(12, 13), 2);
    t = t.bind(FiniteRange.of(5, 10), 3);
    t = t.bind(FiniteRange.of(10, 14), 4);
    t = t.bind(FiniteRange.of(4, 10), 5);
    t = t.bind(FiniteRange.of(6, 14), 6);
    t = t.bind(FiniteRange.of(3, 8), 7);
    t = t.bind(FiniteRange.of(1, 11), 8);
    t = t.bind(FiniteRange.of(2, 13), 9);
    t = t.bind(FiniteRange.of(8, 9), 10);
    return t;
  }

  private static FiniteRangeTree<Integer> buildTree2 () {
    FiniteRangeTree<Integer> t = FiniteRangeTree.empty();
    t = t.bind(FiniteRange.of(0, 3), 1);
    t = t.bind(FiniteRange.of(5, 8), 2);
    t = t.bind(FiniteRange.of(6, 10), 3);
    t = t.bind(FiniteRange.of(8, 9), 4);
    t = t.bind(FiniteRange.of(15, 23), 5);
    t = t.bind(FiniteRange.of(16, 21), 6);
    t = t.bind(FiniteRange.of(25, 30), 7);
    t = t.bind(FiniteRange.of(17, 19), 8);
    t = t.bind(FiniteRange.of(19, 20), 9);
    t = t.bind(FiniteRange.of(26, 26), 10);
    return t;
  }
}
