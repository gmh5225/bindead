package javalx.benchmark;

import java.io.PrintStream;
import java.util.List;

import javalx.data.products.P2;
import javalx.fn.Effect;
import javalx.fn.Fn;
import javalx.fn.Fn2;

public class Benchmark<A> {
  private int DEFAULT_WARMUP_RUNS = 10;
  private final String name;
  private final Fn<A, Long> runtimeObserver;
  private final Fn2<Effect<A>, A, Long> observer =
    new Fn2<Effect<A>, A, Long>() {
      @Override
      public Long apply (final Effect<A> effect, final A parameter) {
        final long start = System.nanoTime();
        effect.observe(parameter);
        final long finished = System.nanoTime();
        return (finished - start) / (1000 * 1000);
      }
    };

  public Benchmark (Effect<A> effect) {
    this("Unnamed Benchmark", effect);
  }

  public Benchmark (final String name, final Effect<A> effect) {
    this.name = name;
    runtimeObserver = curriedApply(effect);
  }

  private Fn<A, Long> curriedApply (final Effect<A> effect) {
    return observer.curry().apply(effect);
  }


  private Fn<List<Long>, P2<List<Long>, List<Long>>> curriedApply2 (A parameter,
      Fn2<A, List<Long>, P2<List<Long>, List<Long>>> benchmark) {
    return benchmark.curry().apply(parameter);
  }


  private Fn<A, List<Long>> warmUp () {
    return runtimeObserver.times(DEFAULT_WARMUP_RUNS).andThen(gc());
  }

  private static Fn<List<Long>, List<Long>> gc () {
    return new Fn<List<Long>, List<Long>>() {
      @Override
      public List<Long> apply (final List<Long> warmUpTimes) {
        System.gc();
        return warmUpTimes;
      }
    };
  }

  private Fn2<A, List<Long>, P2<List<Long>, List<Long>>> benchmark (final int times) {
    return new Fn2<A, List<Long>, P2<List<Long>, List<Long>>>() {
      @Override
      public P2<List<Long>, List<Long>> apply (A parameter, List<Long> warmUpTimes) {
        List<Long> runTimes = runtimeObserver.times(times).apply(parameter);
        return P2.tuple2(warmUpTimes, runTimes);
      }
    };
  }

  private P2<List<Long>, List<Long>> benchmark (int times, A parameter) {
    Fn2<A, List<Long>, P2<List<Long>, List<Long>>> benchmark = benchmark(times);
    Fn<List<Long>, P2<List<Long>, List<Long>>> applied = curriedApply2(parameter, benchmark);
    return warmUp().andThen(applied).apply(parameter);
  }

  public Fn<A, P2<List<Long>, List<Long>>> run (final int times, int timesWarmup) {
    DEFAULT_WARMUP_RUNS = timesWarmup;
    return new Fn<A, P2<List<Long>, List<Long>>>() {
      @Override
      public P2<List<Long>, List<Long>> apply (A parameter) {
        return benchmark(times, parameter);
      }
    };
  }

  public Fn<A, P2<List<Long>, List<Long>>> run (final int times) {
    return new Fn<A, P2<List<Long>, List<Long>>>() {
      @Override
      public P2<List<Long>, List<Long>> apply (A parameter) {
        return benchmark(times, parameter);
      }
    };
  }

  public Fn<P2<List<Long>, List<Long>>, P2<List<Long>, List<Long>>> showResultsTo (final PrintStream out) {
    return new Fn<P2<List<Long>, List<Long>>, P2<List<Long>, List<Long>>>() {
      @Override
      public P2<List<Long>, List<Long>> apply (final P2<List<Long>, List<Long>> warmUpAndRuntimes) {
        final String msgTemplate = "[%s] [%8d] %s: %5d ms";

        final List<Long> warmUpTimes = warmUpAndRuntimes._1();
        long total = 0;
        for (int i = 0; i < warmUpTimes.size(); i++) {
          final String msg = String.format(msgTemplate, "WW", i + 1, name, warmUpTimes.get(i));
          out.println(msg);
          total += warmUpTimes.get(i);
        }
        out.println(String.format("[%s] Total: %6d Mean: %5d", "WW", total, total / warmUpTimes.size()));

        total = 0;
        final List<Long> runTimes = warmUpAndRuntimes._2();
        for (int i = 0; i < runTimes.size(); i++) {
          final String msg = String.format(msgTemplate, "BB", i + 1, name, runTimes.get(i));
          out.println(msg);
          total += runTimes.get(i);
        }
        out.println(String.format("[%s] Total: %6d Mean: %5d", "BB", total, total / runTimes.size()));
        return warmUpAndRuntimes;
      }
    };
  }
}
