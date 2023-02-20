package javalx.persistentcollections;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Iterator;
import java.util.Random;

import javalx.data.products.P2;

import org.junit.Test;

@SuppressWarnings("deprecation") // REFACTOR all calls to assertEquals are deprecated
public class AVLMapTest {

  @Test
  public void testBind () {
    final AVLMap<Integer, Integer> tree0 = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 = tree0.bind(1, 1);
    final AVLMap<Integer, Integer> tree2 = tree1.bind(2, 2);
    final AVLMap<Integer, Integer> tree3 = tree2.bind(3, 3);
    final AVLMap<Integer, Integer> tree4 = tree3.bind(4, 4);
    final AVLMap<Integer, Integer> tree5 = tree4.bind(5, 5);
    final AVLMap<Integer, Integer> tree6 = tree5.bind(6, 6);

    assertEquals(Integer.valueOf(1), tree6.get(1).get());
    assertEquals(Integer.valueOf(2), tree6.get(2).get());
    assertEquals(Integer.valueOf(3), tree6.get(3).get());
    assertEquals(Integer.valueOf(4), tree6.get(4).get());
    assertEquals(Integer.valueOf(5), tree6.get(5).get());
    assertEquals(Integer.valueOf(6), tree6.get(6).get());
  }

  @Test
  public void testBind2 () {
    final AVLMap<Integer, Integer> tree0 = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 = tree0.bind(6, 6).bind(5, 5).bind(4, 4).bind(3, 3).bind(2, 2).bind(1, 1);

    assertEquals(Integer.valueOf(1), tree1.get(1).get());
    assertEquals(Integer.valueOf(2), tree1.get(2).get());
    assertEquals(Integer.valueOf(3), tree1.get(3).get());
    assertEquals(Integer.valueOf(4), tree1.get(4).get());
    assertEquals(Integer.valueOf(5), tree1.get(5).get());
    assertEquals(Integer.valueOf(6), tree1.get(6).get());
  }

  @Test
  public void testIterator () {
    final AVLMap<Integer, Integer> tree0 = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 = tree0.bind(6, 6).bind(5, 5).bind(4, 4).bind(3, 3).bind(2, 2).bind(1, 1);

    Iterator<P2<Integer, Integer>> it = tree1.iterator();
    for (int i = 1; i <= tree1.size(); i++) {
      final P2<Integer, Integer> keyAndValue = it.next();
      assertEquals(Integer.valueOf(i), keyAndValue._1());
    }

    assertTrue(!it.hasNext());
  }

  @Test
  public void testBindDifference () {
    final AVLMap<Integer, Integer> tree = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 =
      tree.bind(6, 6).bind(5, 5).bind(4, 4).bind(3, 3).bind(2, 2).bind(1, 1);
    final AVLMap<Integer, Integer> tree2 = tree.bind(5, 5).bind(3, 3);

    final AVLMap<Integer, Integer> difference = tree1.difference(tree2);

    assertEquals(Integer.valueOf(1), difference.get(1).get());
    assertEquals(Integer.valueOf(2), difference.get(2).get());
    assertEquals(Integer.valueOf(4), difference.get(4).get());
    assertEquals(Integer.valueOf(6), difference.get(6).get());
    assertTrue(!difference.contains(3));
    assertTrue(!difference.contains(5));
  }

  @Test
  public void testBindIntersectionLeftBiased () {
    final AVLMap<Integer, Integer> tree = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 =
      tree.bind(6, 6).bind(5, 5).bind(4, 4).bind(3, 3).bind(2, 2).bind(1, 1);
    final AVLMap<Integer, Integer> tree2 = tree.bind(5, 6).bind(3, 4);

    final AVLMap<Integer, Integer> intersection = tree1.intersection(tree2);

    assertEquals(Integer.valueOf(3), intersection.get(3).get());
    assertEquals(Integer.valueOf(5), intersection.get(5).get());
  }

  @Test
  public void testBind3 () {
    final AVLMap<Integer, Integer> tree = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 =
      tree.bind(-13, -13).bind(-3, -3).bind(58, 58).bind(-76, -76).bind(8, 8);

    assertEquals(Integer.valueOf(-13), tree1.get(-13).get());
    assertEquals(Integer.valueOf(-3), tree1.get(-3).get());
    assertEquals(Integer.valueOf(58), tree1.get(58).get());
    assertEquals(Integer.valueOf(-76), tree1.get(-76).get());
    assertEquals(Integer.valueOf(8), tree1.get(8).get());
  }

  @Test
  public void testRemove () {
    final AVLMap<Integer, Integer> tree0 = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 =
      tree0.bind(-13, -13).bind(-3, -3).bind(58, 58).bind(-76, -76).bind(8, 8);
    final AVLMap<Integer, Integer> tree2 = tree1.remove(-3);

    assertEquals(Integer.valueOf(-13), tree1.get(-13).get());
    assertEquals(Integer.valueOf(-3), tree1.get(-3).get());
    assertEquals(Integer.valueOf(58), tree1.get(58).get());
    assertEquals(Integer.valueOf(-76), tree1.get(-76).get());
    assertEquals(Integer.valueOf(8), tree1.get(8).get());

    assertEquals(Integer.valueOf(-13), tree2.get(-13).get());
    assertEquals(Integer.valueOf(58), tree2.get(58).get());
    assertEquals(Integer.valueOf(-76), tree2.get(-76).get());
    assertEquals(Integer.valueOf(8), tree2.get(8).get());
    assertTrue(!tree2.contains(-3));
  }

  @Test
  public void testBind4 () {
    final AVLMap<Integer, Integer> tree = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 =
      tree.bind(-68, -68).bind(65, 65).bind(19, 19).bind(-32, -32).bind(76, 76);

    assertEquals(Integer.valueOf(-68), tree1.get(-68).get());
    assertEquals(Integer.valueOf(65), tree1.get(65).get());
    assertEquals(Integer.valueOf(19), tree1.get(19).get());
    assertEquals(Integer.valueOf(-32), tree1.get(-32).get());
    assertEquals(Integer.valueOf(76), tree1.get(76).get());
  }

  @Test
  public void testUnion () {
    final AVLMap<Integer, Integer> tree0 = AVLMap.empty();
    final AVLMap<Integer, Integer> tree1 = tree0.bind(-13, -13).bind(-3, -3).bind(58, 58).bind(-76, -76).bind(8, 8);
    final AVLMap<Integer, Integer> tree2 = tree0.bind(-68, -68).bind(65, 65).bind(19, 19).bind(-32, -32).bind(76, 76);

    final AVLMap<Integer, Integer> union = tree1.union(tree2);

    assertEquals(Integer.valueOf(-13), union.get(-13).get());
    assertEquals(Integer.valueOf(-3), union.get(-3).get());
    assertEquals(Integer.valueOf(58), union.get(58).get());
    assertEquals(Integer.valueOf(-76), union.get(-76).get());
    assertEquals(Integer.valueOf(8), union.get(8).get());

    assertEquals(Integer.valueOf(-68), union.get(-68).get());
    assertEquals(Integer.valueOf(65), union.get(65).get());
    assertEquals(Integer.valueOf(19), union.get(19).get());
    assertEquals(Integer.valueOf(-32), union.get(-32).get());
    assertEquals(Integer.valueOf(76), union.get(76).get());
  }

  @Test
  public void testIntBindLinear () {
    AVLMap<Integer, Integer> t = AVLMap.empty();

    int n = 100;
    int[] ints = new int[n];

    for (int i = 0; i < n; i++) {
      ints[i] = i;
    }

    for (int i : ints) {
      t = t.bind(i, i);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(t.size()));

    for (int i : ints) {
      assertEquals(Integer.valueOf(i), t.get(i).get());
    }
  }

  @Test
  public void testUnionIsLeftBiased () {
    AVLMap<Integer, Integer> lefts = AVLMap.empty();
    AVLMap<Integer, Integer> rights = AVLMap.empty();

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

    AVLMap<Integer, Integer> union = lefts.union(rights);

    assertEquals(Integer.valueOf(n), Integer.valueOf(union.size()));

    for (int i = 0; i < n; i++) {
      assertEquals(Integer.valueOf(ints[i][0]), union.get(i).get());
    }
  }

  @Test
  public void testIntBindRandom () {
    AVLMap<Integer, Integer> t = AVLMap.empty();

    int n = 100;
    int[] ints = new int[n];

    Random r = new Random();
    for (int i = 0; i < n; i++) {
      ints[i] = r.nextInt();
    }

    for (int i = 0; i < n; i++) {
      t = t.bind(i, ints[i]);
    }

    for (int i = 0; i < n; i++) {
      assertEquals(Integer.valueOf(ints[i]), t.get(i).get());
    }
  }
}
