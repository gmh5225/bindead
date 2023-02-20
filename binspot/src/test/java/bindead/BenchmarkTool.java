package bindead;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.fn.Effect;

/**
 * Helper class to run benchmarks and measure the meanRuntime and memory usage.
 */
public class BenchmarkTool {
  private static final int DEFAULTWARMUPTIMES = 5;
  private static final int DEFAULTRUNTIMES = 10;
  private final Runnable action;
  private final int times;
  private final int timesWarmup;

  private BenchmarkTool (Runnable action, int times, int timesWarmup) {
    this.action = action;
    this.times = times;
    this.timesWarmup = timesWarmup;
  }

  public static BenchmarkResult run (int times, Runnable action) {
    return run("benchmark", times, action);
  }

  public static BenchmarkResult run (Runnable action) {
    return run("benchmark", action);
  }

  public static BenchmarkResult run (String name, Runnable action) {
    return run(name, DEFAULTRUNTIMES, action);
  }

  public static BenchmarkResult run (String name, int times, Runnable action) {
    return run(name, times, DEFAULTWARMUPTIMES, action);
  }

  public static BenchmarkResult run (String name, int times, int timesWarmup, Runnable action) {
    BenchmarkTool benchmark = new BenchmarkTool(action, times, timesWarmup);
    long memoryUsage = benchmark.getMemory(); // measure memory before the long benchmark runs
    P2<Long, Long> runtime = benchmark.getTime();
    return new BenchmarkResult(name, times, timesWarmup, runtime._1(), runtime._2(), memoryUsage);
  }

  /**
   * Run a method in the given class instance as a benchmark. Useful to run certain unit tests as benchmarks.
   * Note that it runs the method {@link #DEFAULTRUNTIMES} times to benchmark it.
   */
  public static BenchmarkResult benchmarkMethod (String methodName, Object clazz) {
    Method method = null;
    try {
      method = clazz.getClass().getMethod(methodName);
    } catch (SecurityException e) {
    } catch (NoSuchMethodException e) {
    }
    final Object methodEnclosingObject = clazz;
    final Method testMethod = method;
    if (testMethod == null)
      throw new IllegalStateException("Could not find the method: " + methodName + " in the class: " + clazz);
    BenchmarkResult result = BenchmarkTool.run(methodName, new Runnable() {
      @Override public void run () {
        try {
          testMethod.invoke(methodEnclosingObject);
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    return result;
  }

  /**
   * Run all the methods annotated with {@link Benchmark} in the given
   * class instance as a benchmark. Useful to run certain unit tests as benchmarks.
   */
  public static Iterable<BenchmarkResult> benchmarkMethods (final Object clazz) {
    final List<P2<Method, Benchmark>> benchmarkMethods = new LinkedList<P2<Method, Benchmark>>();
    try {
      Method[] allMethods = clazz.getClass().getDeclaredMethods();
      for (Method method : allMethods) {
        Benchmark annotation = method.getAnnotation(Benchmark.class);
        if (annotation == null)
          continue;
        benchmarkMethods.add(P2.tuple2(method, annotation));
      }
    } catch (SecurityException e) {
    }
    final Iterator<BenchmarkResult> resultsIterator = new Iterator<BenchmarkResult>() {
      Iterator<P2<Method, Benchmark>> methodsIterator = benchmarkMethods.iterator();

      @Override public boolean hasNext () {
        return methodsIterator.hasNext();
      }

      @Override public BenchmarkResult next () {
        P2<Method, Benchmark> tuple = methodsIterator.next();
        final Object methodEnclosingObject = clazz;
        final Method testMethod = tuple._1();
        Benchmark annotation = tuple._2();
        String methodName = annotation.name();
        if (methodName.isEmpty())
          methodName = testMethod.getName();
        int timesToWarmup = annotation.timesToWarmup();
        int timesToRun = annotation.timesToRun();
        BenchmarkResult result = BenchmarkTool.run(methodName, timesToRun, timesToWarmup, new Runnable() {
          @Override public void run () {
            try {
              testMethod.invoke(methodEnclosingObject);
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
        return result;
      }

      @Override public void remove () {
        throw new UnimplementedException();
      }

    };
    Iterable<BenchmarkResult> stream = new Iterable<BenchmarkResult>() {
      @Override public Iterator<BenchmarkResult> iterator () {
        return resultsIterator;
      }
    };
    return stream;
  }

  // uses older functional benchmark code.
  private P2<Long, Long> getTimeOld () {
    javalx.benchmark.Benchmark<Void> benchmark = new javalx.benchmark.Benchmark<Void>(new Effect<Void>() {
      @Override public void observe (Void _) {
        action.run();
      }
    });
    final long start = System.nanoTime();
    List<Long> runtimes = benchmark.run(times, timesWarmup).apply(null)._2();
    final long finished = System.nanoTime();
    long totalRuntime = (finished - start) / (1000 * 1000);
    long average = 0;
    for (Long time : runtimes) {
      average = average + time;
    }
    average = average / runtimes.size();
    return P2.tuple2(average, totalRuntime);
  }

  private P2<Long, Long> getTime () {
    for (int i = 1; i <= timesWarmup; i++) {
      action.run();
    }
    final long start = System.nanoTime();
    for (int i = 1; i <= times; i++) {
      action.run();
    }
    final long finished = System.nanoTime();
    long totalRuntime = (finished - start) / (1000 * 1000);
    long average = totalRuntime / times;
    return P2.tuple2(average, totalRuntime);
  }

  private long getMemory () {
    waitForGC();
    waitForGC();
    resetMemUsage();
    action.run();
    long peakMem = getMemUsage() / 1000000;
    return peakMem;
  }


  private static long getMemUsage () {
    long memUsage = 0;
    for (MemoryPoolMXBean mem : ManagementFactory.getMemoryPoolMXBeans()) {
      memUsage += mem.getPeakUsage().getUsed();
    }
    return memUsage;
  }

  private static void waitForGC () {
    System.gc();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void resetMemUsage () {
    for (MemoryPoolMXBean mem : ManagementFactory.getMemoryPoolMXBeans()) {
      mem.resetPeakUsage();
    }
  }

  public static class BenchmarkResult {
    private final int repetitions;
    private final int warmupRepetitions;
    private final long averageRuntime;
    private final long totalRuntime;
    private final long memoryUsage;
    private final String name;

    private BenchmarkResult (String name, int repetitions, int warmupRepetitions, long averageRuntime, long totalRuntime, long memoryUsage) {
      this.name = name;
      this.warmupRepetitions = warmupRepetitions;
      this.repetitions = repetitions;
      this.averageRuntime = averageRuntime;
      this.totalRuntime = totalRuntime;
      this.memoryUsage = memoryUsage;
    }

    /**
     * @return The name of the benchmark.
     */
    public String getName () {
      return name;
    }

    /**
     * @return How often the program was executed repeatedly.
     */
    public int getRepetitions () {
      return repetitions;
    }

    /**
     * @return How often the program was executed repeatedly in the warmup phase.
     */
    public int getWarmupRepetitions () {
      return warmupRepetitions;
    }

    /**
     * @return The average execution time of the program in milliseconds.
     */
    public long getAverageRuntime () {
      return averageRuntime;
    }

    /**
     * @return The accumulated total runtime of the benchmark.
     */
    public long getTotalRuntime () {
      return totalRuntime;
    }

    /**
     * @return The peak memory usage of the program.
     */
    public long getMemoryUsage () {
      return memoryUsage;
    }

    @Override public String toString () {
      return String.format("%s: %,d ms, %,d mb; took %,d ms to benchmark doing %,d warmups and %,d repetitions.",
          getName(), getAverageRuntime(), getMemoryUsage(), getTotalRuntime(), getWarmupRepetitions(), getRepetitions());
    }
  }

  /**
   * Add this annotation to a method that should be benchmarked
   * using {@link BenchmarkTool#benchmarkMethods(Object)} on the object containing the method.
   * It is thus easily possible to choose which methods of an object should be benchmarked.
   *
   * @author Bogdan Mihaila
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public static @interface Benchmark {

    String name() default "";

    int timesToWarmup() default 5;

    int timesToRun() default 10;
  }
}
