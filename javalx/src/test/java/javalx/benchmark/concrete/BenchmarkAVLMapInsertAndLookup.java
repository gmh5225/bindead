package javalx.benchmark.concrete;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javalx.benchmark.Benchmark;
import javalx.data.products.P2;
import javalx.fn.Effect;
import javalx.persistentcollections.AVLMap;

public class BenchmarkAVLMapInsertAndLookup {

  public static class QuickAndDirtyBenchmark extends Benchmark<P2<List<Integer>, List<Integer>>> {
    public QuickAndDirtyBenchmark () {
      super("AVLMapInsertAndLookup", effect);
    }
    private static final Effect<P2<List<Integer>, List<Integer>>> effect =
      new Effect<P2<List<Integer>, List<Integer>>>() {
        @Override
        public void observe (P2<List<Integer>, List<Integer>> lists) {
          final List<Integer> lefts = lists._1();
          final List<Integer> rights = lists._2();

          AVLMap<Integer, Integer> map = AVLMap.empty();
          for (int i : lefts) {
            map = map.bind(i, i);
          }

          for (int i : rights) {
            map = map.bind(i, i);
          }

          //System.out.println("MEMORY: " + MemoryUtil.deepMemoryUsageOf(map));

          for (int i : lefts) {
            assertTrue(map.contains(i));
          }

          for (int i : rights) {
            assertTrue(map.contains(i));
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

    //System.out.println("MEMORY_LEFTS: " + MemoryUtil.deepMemoryUsageOf(lefts));

    bench.run(5).andThen(bench.showResultsTo(System.out)).apply(P2.tuple2(lefts, rights));
  }
}
