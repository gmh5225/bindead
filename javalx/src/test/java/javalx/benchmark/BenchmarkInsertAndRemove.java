package javalx.benchmark;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javalx.data.products.P2;
import javalx.fn.Effect;
import javalx.persistentcollections.FiniteMap;

public class BenchmarkInsertAndRemove {
  public static class QuickAndDirtyBenchmark extends Benchmark<P2<List<Integer>, List<Integer>>> {
    public QuickAndDirtyBenchmark (final String name, final FiniteMap<Integer, Integer, ?> empty) {
      super(
          name + ": MapInsertAndRemove",
          new Effect<P2<List<Integer>, List<Integer>>>() {
            @Override
            public void observe (P2<List<Integer>, List<Integer>> lists) {
              final List<Integer> lefts = lists._1();
              final List<Integer> rights = lists._2();

              FiniteMap<Integer, Integer, ?> map = empty;
              for (int i : lefts) {
                map = map.bind(i, i);
              }

              for (int i : rights) {
                map = map.bind(i, i);
              }

              FiniteMap<Integer, Integer, ?> leftsOnly = map;
              FiniteMap<Integer, Integer, ?> rightsOnly = map;

              //System.out.println("MEMORY: " + MemoryUtil.deepMemoryUsageOf(map));

              for (int i : lefts) {
                rightsOnly = rightsOnly.remove(i);
              }

              for (int i : rights) {
                leftsOnly = leftsOnly.remove(i);
              }

              for (int i : lefts) {
                assertTrue(leftsOnly.contains(i));
              }

              for (int i : rights) {
                assertTrue(rightsOnly.contains(i));
              }
            }
          });
    }
  }

  public static void run (final String name, final FiniteMap<Integer, Integer, ?> empty) {
    QuickAndDirtyBenchmark bench = new QuickAndDirtyBenchmark(name, empty);

    int n = 100000;
    List<Integer> lefts = new ArrayList<Integer>(n);
    List<Integer> rights = new ArrayList<Integer>(n);

    Random r = new Random();

    for (int i = 0; i < n; i++) {
      lefts.add(r.nextInt(n));
    }

    for (int i = 0; i < n; i++) {
      rights.add((r.nextInt(n) + n + 1));
    }

    //System.out.println("MEMORY_LEFTS: " + MemoryUtil.deepMemoryUsageOf(lefts));

    bench.run(5).andThen(bench.showResultsTo(System.out)).apply(P2.tuple2(lefts, rights));
  }
}
