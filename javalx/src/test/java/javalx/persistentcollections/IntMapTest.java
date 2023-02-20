package javalx.persistentcollections;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.data.Option;
import javalx.fn.Fn2;
import javalx.numeric.Interval;


import org.junit.Test;

@SuppressWarnings("deprecation") // REFACTOR assertEquals is deprecated
public class IntMapTest {
  @Test
  public void difference000 () {
    final IntMap<Integer> tree = IntMap.empty();
    final IntMap<Integer> tree1 = tree.bind(6, 6).bind(5, 5).bind(4, 4).bind(3, 3).bind(2, 2).bind(1, 1);
    final IntMap<Integer> tree2 = tree.bind(5, 5).bind(3, 3);

    final IntMap<Integer> difference = tree1.difference(tree2);

    assertEquals(Integer.valueOf(1), difference.get(1).get());
    assertEquals(Integer.valueOf(2), difference.get(2).get());
    assertEquals(Integer.valueOf(4), difference.get(4).get());
    assertEquals(Integer.valueOf(6), difference.get(6).get());
    assertTrue(!difference.contains(3));
    assertTrue(!difference.contains(5));
  }

  @Test
  public void leftbiased000 () {
    final IntMap<Integer> tree = IntMap.empty();
    final IntMap<Integer> tree1 = tree.bind(6, 6).bind(5, 5).bind(4, 4).bind(3, 3).bind(2, 2).bind(1, 1);
    final IntMap<Integer> tree2 = tree.bind(5, 6).bind(3, 4);

    final IntMap<Integer> intersection = tree1.intersection(tree2);

    assertEquals(Integer.valueOf(3), intersection.get(3).get());
    assertEquals(Integer.valueOf(5), intersection.get(5).get());
  }

  @Test
  public void leftbiased001 () {
    IntMap<Integer> lefts = IntMap.empty();
    IntMap<Integer> rights = IntMap.empty();

    int n = 100;
    int[][] ints = new int[n][2];

    for (int i = 0; i < n; i++) {
      ints[i][0] = i;
      ints[i][1] = i + 1;
    }

    for (int i = 0; i < n; i++) {
      lefts = lefts.bind(i, ints[i][0]);
      rights = rights.bind(i, ints[i][1]);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(lefts.size()));
    assertEquals(Integer.valueOf(n), Integer.valueOf(rights.size()));

    IntMap<Integer> union = lefts.union(rights);

    assertEquals(Integer.valueOf(n), Integer.valueOf(union.size()));

    for (int i = 0; i < n; i++) {
      assertEquals(Integer.valueOf(ints[i][0]), union.get(i).get());
    }
  }

  @Test
  public void leftbiased002 () {
    IntMap<Integer> lefts = IntMap.empty();
    IntMap<Integer> rights = IntMap.empty();

    int n = 100;
    int[][] ints = new int[n][2];

    for (int i = 0; i < n; i++) {
      ints[i][0] = i;
      ints[i][1] = i + 1;
    }

    for (int i = 0; i < n; i++) {
      lefts = lefts.bind(i, ints[i][0]);
      rights = rights.bind(i, ints[i][1]);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(lefts.size()));
    assertEquals(Integer.valueOf(n), Integer.valueOf(rights.size()));

    IntMap<Integer> union = lefts.intersection(rights);

    assertEquals(Integer.valueOf(n), Integer.valueOf(union.size()));

    for (int i = 0; i < n; i++) {
      assertEquals(Integer.valueOf(ints[i][0]), union.get(i).get());
    }
  }

  @Test
  public void leftbiased003 () {
    final IntMap<Integer> tree = IntMap.empty();
    final IntMap<Integer> tree1 = tree.bind(6, 6).bind(5, 5).bind(4, 4).bind(3, 3).bind(2, 2).bind(1, 1);
    final IntMap<Integer> tree2 = tree.bind(5, 6).bind(3, 4);
    final Fn2<Integer, Integer, Integer> selector = Fn2.__1();
    final IntMap<Integer> intersection = tree1.intersection(selector, tree2);

    assertEquals(Integer.valueOf(3), intersection.get(3).get());
    assertEquals(Integer.valueOf(5), intersection.get(5).get());
  }

  @Test
  public void leftbiased005 () {
    final Fn2<Integer, Integer, Integer> selector = Fn2.__1();
    IntMap<Integer> lefts = IntMap.empty();
    IntMap<Integer> rights = IntMap.empty();

    int n = 100;
    int[][] ints = new int[n][2];

    for (int i = 0; i < n; i++) {
      ints[i][0] = i;
      ints[i][1] = i + 1;
    }

    for (int i = 0; i < n; i++) {
      lefts = lefts.bind(i, ints[i][0]);
      rights = rights.bind(i, ints[i][1]);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(lefts.size()));
    assertEquals(Integer.valueOf(n), Integer.valueOf(rights.size()));

    IntMap<Integer> union = lefts.union(selector, rights);

    assertEquals(Integer.valueOf(n), Integer.valueOf(union.size()));

    for (int i = 0; i < n; i++) {
      assertEquals(Integer.valueOf(ints[i][0]), union.get(i).get());
    }
  }

  @Test
  public void leftbiased006 () {
    IntMap<Integer> map = IntMap.<Integer>empty().bind(5, 5).bind(5, 6);
    assertEquals(map.get(5), Option.some(6));
  }

  @Test
  public void rightbiased000 () {
    final IntMap<Integer> tree = IntMap.empty();
    final IntMap<Integer> tree1 = tree.bind(6, 6).bind(5, 5).bind(4, 4).bind(3, 3).bind(2, 2).bind(1, 1);
    final IntMap<Integer> tree2 = tree.bind(5, 6).bind(3, 4);
    final Fn2<Integer, Integer, Integer> selector = Fn2.__2();
    final IntMap<Integer> intersection = tree1.intersection(selector, tree2);

    assertEquals(Integer.valueOf(4), intersection.get(3).get());
    assertEquals(Integer.valueOf(6), intersection.get(5).get());
  }

  @Test
  public void rightbiased001 () {
    final Fn2<Integer, Integer, Integer> selector = Fn2.__2();
    IntMap<Integer> lefts = IntMap.empty();
    IntMap<Integer> rights = IntMap.empty();

    int n = 100;
    int[][] ints = new int[n][2];

    for (int i = 0; i < n; i++) {
      ints[i][0] = i;
      ints[i][1] = i + 1;
    }

    for (int i = 0; i < n; i++) {
      lefts = lefts.bind(i, ints[i][0]);
      rights = rights.bind(i, ints[i][1]);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(lefts.size()));
    assertEquals(Integer.valueOf(n), Integer.valueOf(rights.size()));

    IntMap<Integer> union = lefts.union(selector, rights);

    assertEquals(Integer.valueOf(n), Integer.valueOf(union.size()));

    for (int i = 0; i < n; i++) {
      assertEquals(Integer.valueOf(ints[i][1]), union.get(i).get());
    }
  }

  @Test
  public void rightbiased002 () {
    final Fn2<Integer, Integer, Integer> selector = Fn2.__2();
    IntMap<Integer> lefts = IntMap.empty();
    IntMap<Integer> rights = IntMap.empty();

    int n = 100;
    int[][] ints = new int[n][2];

    for (int i = 0; i < n; i++) {
      ints[i][0] = i;
      ints[i][1] = i + 1;
    }

    for (int i = 0; i < n; i++) {
      lefts = lefts.bind(i, ints[i][0]);
      rights = rights.bind(i, ints[i][1]);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(lefts.size()));
    assertEquals(Integer.valueOf(n), Integer.valueOf(rights.size()));

    IntMap<Integer> union = lefts.intersection(selector, rights);

    assertEquals(Integer.valueOf(n), Integer.valueOf(union.size()));

    for (int i = 0; i < n; i++) {
      assertEquals(Integer.valueOf(ints[i][1]), union.get(i).get());
    }
  }

  @Test
  public void split001 () {
    IntMap<Integer> lefts = IntMap.<Integer>empty().bind(1, 1);
    IntMap<Integer> rights = IntMap.<Integer>empty().bind(1, 2);
    ThreeWaySplit<IntMap<Integer>> split = lefts.split(rights);
    assertThat(split.onlyInFirst().isEmpty(), is(true));
    assertThat(split.inBothButDiffering().isEmpty(), is(false));
    assertThat(split.onlyInSecond().isEmpty(), is(true));
  }

  @Test
  public void split002 () {
    IntMap<Integer> lefts = IntMap.<Integer>empty().bind(1, 0).bind(2, 100001);
    IntMap<Integer> rights = IntMap.<Integer>empty().bind(1, 1).bind(2, 100001);
    ThreeWaySplit<IntMap<Integer>> split = lefts.split(rights);
    assertThat(split.onlyInFirst().isEmpty(), is(true));
    assertThat(split.inBothButDiffering().size(), is(1));
    assertThat(split.onlyInSecond().isEmpty(), is(true));
  }

  @Test public void split003 () {
    IntMap<Interval> l = IntMap.<Interval>empty().bind(1, Interval.of(0, 1)).bind(2, Interval.of(-1));
    IntMap<Interval> r = IntMap.<Interval>empty().bind(1, Interval.of(0, 2)).bind(2, Interval.of(-1));
    ThreeWaySplit<IntMap<Interval>> split = l.split(r);
    assertThat(split.onlyInFirst().isEmpty(), is(true));
    assertThat(split.inBothButDiffering().size(), is(1));
    assertThat(split.onlyInSecond().isEmpty(), is(true));
  }
}
