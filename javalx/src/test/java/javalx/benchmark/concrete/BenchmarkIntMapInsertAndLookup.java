package javalx.benchmark.concrete;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javalx.benchmark.Benchmark;
import javalx.data.products.P2;
import javalx.fn.Effect;
import javalx.persistentcollections.IntMap;

public class BenchmarkIntMapInsertAndLookup {
  public static class QuickAndDirtyBenchmark extends Benchmark<P2<List<Integer>, List<Integer>>> {
    public QuickAndDirtyBenchmark () {
      super("IntMapInsertAndLookup", effect);
    }
    private static final Effect<P2<List<Integer>, List<Integer>>> effect =
      new Effect<P2<List<Integer>, List<Integer>>>() {
        @Override
        public void observe (P2<List<Integer>, List<Integer>> lists) {
          final List<Integer> lefts = lists._1();
          final List<Integer> rights = lists._2();

          IntMap<Integer> tree = IntMap.empty();
          for (Integer i : lefts) {
            tree = tree.bind(i, i);
          }

          for (Integer i : rights) {
            tree = tree.bind(i, i);
          }

          //System.out.println("MEMORY: " + MemoryUtil.deepMemoryUsageOf(tree));

          for (Integer i : lefts) {
            assertTrue(tree.contains(i));
          }

          for (Integer i : rights) {
            assertTrue(tree.contains(i));
          }
        }
      };
  }

  public static void main (String[] args) {
    QuickAndDirtyBenchmark bench = new QuickAndDirtyBenchmark();

    int n = 100000;
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
