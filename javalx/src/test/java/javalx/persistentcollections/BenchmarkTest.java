package javalx.persistentcollections;

import static javalx.data.Unit.unit;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javalx.benchmark.Benchmark;
import javalx.data.Unit;
import javalx.data.products.P2;
import javalx.fn.Effect;
import javalx.persistentcollections.trie.IntTrie;

import org.junit.Test;

public class BenchmarkTest {
  @Test
  public void testBenchmark () {
    Effect<Unit> effect = new Effect<Unit>() {
      @Override
      public void observe (Unit unit) {
      }
    };

    Benchmark<Unit> bench = new Benchmark<Unit>("Foo", effect);

    bench.run(1).andThen(bench.showResultsTo(System.out)).apply(unit());
  }

  @Test
  public void testBenchmarkWithIntTrees () {
    Effect<P2<List<Integer>, List<Integer>>> effect = new Effect<P2<List<Integer>, List<Integer>>>() {
      @Override
      public void observe (P2<List<Integer>, List<Integer>> lists) {
        final List<Integer> lefts = lists._1();
        final List<Integer> rights = lists._2();

        IntTrie<Integer> left = IntTrie.empty();
        for (int i : lefts) {
          left = left.bind(i, i);
        }

        IntTrie<Integer> right = IntTrie.empty();
        for (int i : rights) {
          right = right.bind(i, i);
        }

        IntTrie<Integer> union = left.union(right);

        for (int i : lefts) {
          assertTrue(union.contains(i));
        }

        for (int i : rights) {
          assertTrue(union.contains(i));
        }
      }
    };

    Benchmark<P2<List<Integer>, List<Integer>>> bench =
      new Benchmark<P2<List<Integer>, List<Integer>>>("Merge", effect);

    int n = 10000;
    List<Integer> lefts = new ArrayList<Integer>(n);
    List<Integer> rights = new ArrayList<Integer>(n);

    Random r = new Random();

    for (int i = 0; i < n; i++) {
      lefts.add(r.nextInt());
    }

    for (int i = 0; i < n; i++) {
      rights.add(r.nextInt());
    }

    bench.run(5).andThen(bench.showResultsTo(System.out)).apply(P2.tuple2(lefts, rights));
  }
}
