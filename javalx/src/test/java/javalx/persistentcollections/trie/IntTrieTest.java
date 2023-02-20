package javalx.persistentcollections.trie;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Random;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn2;

import org.junit.Test;

@SuppressWarnings("deprecation") // REFACTOR assertEquals is deprecated
public class IntTrieTest {
  private static final Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer> RAISING = IntTrieTest
      .raising(new IllegalArgumentException("Duplicate entry"));

  @Test public void testIntTreeOps1 () {
    final IntTrie<Integer> t0 = IntTrie.empty();
    final IntTrie<Integer> t1 = t0.bind(1, 2).bind(2, 3).bind(3, 4);

    Option<Integer> i1 = t0.get(2);
    Option<Integer> i2 = t1.get(2);

    assertTrue(i1.isNone());
    assertEquals(Integer.valueOf(3), i2.get());
  }

  @Test(expected = IllegalArgumentException.class) public void testIntTreeDuplicateEntry () {
    final Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer> duplicateEntryThrower = RAISING;
    final IntTrie<Integer> t0 = IntTrie.empty();
    @SuppressWarnings("unused")
    final IntTrie<Integer> t1 = t0.bind(1, 2).bind(duplicateEntryThrower,
        1, 3);
    assertTrue(false);
  }

  @Test public void testIntTreeNoDuplicateEntry () {
    final Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer> duplicateEntryThrower = IntTrieTest
        .raising(new IllegalArgumentException("Duplicate entry"));
    final IntTrie<Integer> t0 = IntTrie.empty();
    final IntTrie<Integer> t1 = t0.bind(duplicateEntryThrower, 1, 3);

    assertEquals(Integer.valueOf(3), Integer.valueOf(t1.get(1).get()));
  }

  @Test public void testRebind () {
    final IntTrie<Integer> t0 = IntTrie.empty();
    final IntTrie<Integer> t1 = t0.bind(1, 2).bind(1, 3);

    Option<Integer> i1 = t1.get(1);

    assertEquals(Integer.valueOf(3), i1.get());
  }

  @Test public void testDrop () {
    final IntTrie<Integer> t0 = IntTrie.empty();
    final IntTrie<Integer> t1 = t0.bind(1, 1).bind(3, 3).remove(1);

    assertEquals(Integer.valueOf(3), t1.get(3).get());
    assertTrue(!t1.contains(1));
  }

  @Test public void testNoRebindWithLeftSelector () {
    final Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer> leftBiasedSelector =
      new Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer>() {
        @Override public Integer apply (P2<Integer, Integer> a, P2<Integer, Integer> b) {
          return a._2();
        }
      };
    final IntTrie<Integer> t0 = IntTrie.empty();
    final IntTrie<Integer> t1 = t0.bind(1, 2).bind(leftBiasedSelector, 1, 3);
    Option<Integer> i1 = t1.get(1);
    assertEquals(Integer.valueOf(2), i1.get());
  }

  @Test public void testRebindWithRightSelector () {
    final Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer> rightBiasedSelector =
      new Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer>() {
        @Override public Integer apply (P2<Integer, Integer> a, P2<Integer, Integer> b) {
          return b._2();
        }
      };
    final IntTrie<Integer> t0 = IntTrie.empty();
    final IntTrie<Integer> t1 = t0.bind(1, 2).bind(rightBiasedSelector, 1, 3);
    Option<Integer> i1 = t1.get(1);
    assertEquals(Integer.valueOf(3), i1.get());
  }

  @Test public void testIntBindLinear () {
    IntTrie<Integer> t = IntTrie.empty();

    int n = 100;
    int[] ints = new int[n];

    // Random r = new Random();
    for (int i = 0; i < n; i++) {
      ints[i] = i;// r.nextInt();
    }

    for (int i : ints) {
      t = t.bind(i, i);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(t.size()));

    for (int i : ints) {
      // System.out.println("XXx:" + i);
      assertEquals(Integer.valueOf(i), t.get(i).get());
    }

  }

  @Test public void testIntUnionRandom () {
    IntTrie<Integer> left = IntTrie.empty();
    IntTrie<Integer> right = IntTrie.empty();

    int n = 100;
    int[][] ints = new int[n][2];

    Random r = new Random();
    for (int i = 0; i < n; i++) {
      ints[i][0] = r.nextInt();
      ints[i][1] = r.nextInt();
    }

    for (int i[] : ints) {
      left = left.bind(i[0], i[0]);
      right = right.bind(i[1], i[1]);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(left.size()));
    assertEquals(Integer.valueOf(n), Integer.valueOf(right.size()));

    final IntTrie<Integer> union = left.union(right);

    for (int i[] : ints) {
      assertTrue(union.contains(i[0]));
      assertTrue(union.contains(i[1]));
      // Right biased union
      assertEquals(Integer.valueOf(i[1]), union.get(i[1]).get());
    }

  }

  @Test public void testDropMany () {
    IntTrie<Integer> left = IntTrie.empty();
    IntTrie<Integer> right = IntTrie.empty();

    int n = 100;
    int[][] ints = new int[n][2];

    for (int i = 0; i < n; i++) {
      ints[i][0] = i;
      ints[i][1] = i + n + 1;
    }

    for (int i[] : ints) {
      left = left.bind(i[0], i[0]);
      right = right.bind(i[1], i[1]);
    }

    assertEquals(Integer.valueOf(n), Integer.valueOf(left.size()));
    assertEquals(Integer.valueOf(n), Integer.valueOf(right.size()));

    IntTrie<Integer> union = left.union(right);

    for (int i[] : ints) {
      union = union.remove(i[1]);
    }

    for (int i[] : ints) {
      assertTrue(union.contains(i[0]));
      assertTrue(!union.contains(i[1]));
    }
  }

  @Test public void testMergeDisjointKeysets () {
    final IntTrie<Integer> left = IntTrie.<Integer>empty().bind(3, 3)
        .bind(5, 5);
    final IntTrie<Integer> right = IntTrie.<Integer>empty().bind(1, 1)
        .bind(4, 4);

    final IntTrie<Integer> union = left.union(right);

    assertEquals(Integer.valueOf(3), union.get(3).get());
    assertEquals(Integer.valueOf(1), union.get(1).get());
    assertEquals(Integer.valueOf(5), union.get(5).get());
    assertEquals(Integer.valueOf(4), union.get(4).get());
  }

  @Test public void testMergeRightBiased () {
    final IntTrie<Integer> left = IntTrie.<Integer>empty().bind(1, 3)
        .bind(5, 5);
    final IntTrie<Integer> right = IntTrie.<Integer>empty().bind(1, 1)
        .bind(4, 4);

    final IntTrie<Integer> union = left.union(right);

    assertEquals(Integer.valueOf(1), union.get(1).get());
    assertEquals(Integer.valueOf(5), union.get(5).get());
    assertEquals(Integer.valueOf(4), union.get(4).get());
  }

  @Test public void testBindRightBiased () {
    final IntTrie<Integer> trie = IntTrie.<Integer>empty().bind(1, 3)
        .bind(1, 5);

    assertEquals(Integer.valueOf(5), trie.get(1).get());
  }

  @Test public void testBindLeftBiased () {
    final Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer> leftBiasedSelector =
      new Fn2<P2<Integer, Integer>, P2<Integer, Integer>, Integer>() {
        @Override public Integer apply (P2<Integer, Integer> a, P2<Integer, Integer> b) {
          return a._2();
        }
      };

    final IntTrie<Integer> trie = IntTrie
        .<Integer>empty()
        .bind(1, 3)
        .bind(leftBiasedSelector, 1, 5);

    assertEquals(Integer.valueOf(3), trie.get(1).get());
  }

  @Test public void testMergeDisjointKeysetsWithNegativeInts () {
    final IntTrie<Integer> left = IntTrie.<Integer>empty().bind(3, 3)
        .bind(5, 5);
    final IntTrie<Integer> right = IntTrie.<Integer>empty().bind(1, 1)
        .bind(-1, -1);

    final IntTrie<Integer> union = left.union(right);

    assertEquals(Integer.valueOf(3), union.get(3).get());
    assertEquals(Integer.valueOf(1), union.get(1).get());
    assertEquals(Integer.valueOf(5), union.get(5).get());
    assertEquals(Integer.valueOf(-1), union.get(-1).get());
  }

  /**
   * Creates a function that when applied any value will throw the given exception.
   *
   * @return function raising an error when applied an argument.
   */
  public static <A, B, C, E extends RuntimeException> Fn2<A, B, C> raising (final E e) {
    return new Fn2<A, B, C>() {
      @Override public C apply (A a, B b) {
        throw e;
      }
    };
  }
}
