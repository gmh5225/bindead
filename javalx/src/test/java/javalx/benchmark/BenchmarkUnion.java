package javalx.benchmark;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javalx.data.products.P2;
import javalx.fn.Effect;
import javalx.persistentcollections.FiniteMap;

public class BenchmarkUnion {
  public static class QuickAndDirtyBenchmark<T extends FiniteMap<Integer, Integer, T>>
    extends Benchmark<P2<List<Integer>, List<Integer>>> {
    public QuickAndDirtyBenchmark (final String name, final T empty) {
      super(
        name + ": MapUnion",
        new Effect<P2<List<Integer>, List<Integer>>>() {
          @Override
          public void observe (P2<List<Integer>, List<Integer>> lists) {
            final List<Integer> lefts = lists._1();
            final List<Integer> rights = lists._2();

            T leftsOnly = empty;
            T rightsOnly = empty;
            for (int i : lefts) {
              leftsOnly = leftsOnly.bind(i, i);
            }

            for (int i : rights) {
              rightsOnly = rightsOnly.bind(i, i);
            }

            T union = leftsOnly.union(rightsOnly);

            //System.out.println("MEMORY: " + MemoryUtil.deepMemoryUsageOf(leftsMap));

            for (int i : lefts) {
              assertTrue(union.contains(i));
            }

            for (int i : rights) {
              assertTrue(union.contains(i));
            }
          }
        });
    }
  }

  public static <T extends FiniteMap<Integer, Integer, T>> void run (final String name, final T empty) {
    QuickAndDirtyBenchmark<T> bench = new QuickAndDirtyBenchmark<T>(name, empty);

    int n = 100000;
    List<Integer> lefts = new ArrayList<Integer>(n);
    List<Integer> rights = new ArrayList<Integer>(n);

    Random r = new Random();

    for (int i = 0; i < n; i++) {
      lefts.add(r.nextInt(n));
    }

    for (int i = 0; i < n; i++) {
      rights.add(r.nextInt(n));
    }

    //System.out.println("MEMORY_LEFTS: " + MemoryUtil.deepMemoryUsageOf(lefts));

    bench.run(5).andThen(bench.showResultsTo(System.out)).apply(P2.tuple2(lefts, rights));
  }
}
