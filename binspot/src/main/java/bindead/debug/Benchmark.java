package bindead.debug;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

public abstract class Benchmark {
  public static boolean measureTimings = true;
  public static boolean quiet = false;
  private static String bname;
  private static final HashMap<String, Integer> counters = new HashMap<String, Integer>();
  private static boolean logMessages = false;
  private static final int RUNS = 5;
  private static final int WARMUP_SECS = 2;

  public Benchmark () {
    bname = getClass().getName();
  }

  public static void count (String what, int amount) {
    if (logMessages) {
      final Integer old = counters.get(what);
      counters.put(what, old == null ? amount : old + amount);
    }
  }

  public static void log (String s) {
    if (logMessages && !quiet)
      printb(s);
  }

  public static void warn (String s) {
    if (logMessages)
      printb("*** WARNING ***\n" + s);
  }

  private static long average (long[] samples) {
    long total = 0;
    for (final long s : samples)
      total += s;
    return total / samples.length;
  }

  private static void displayCounters () {
    for (final Entry<String, Integer> e : counters.entrySet())
      System.out
          .println(String.format("%5d %s", e.getValue(), e.getKey()));
  }

  private static long getMemUsage () {
    long memUsage = 0;
    for (final MemoryPoolMXBean mem : ManagementFactory
        .getMemoryPoolMXBeans())
      memUsage += mem.getPeakUsage().getUsed();
    return memUsage;
  }

  private static void printb (String s) {
    System.out.println("[" + bname + "] " + s);
  }

  private static long quantile (int quantile, int divisor, long[] sortedValues) {
    assert 0 <= quantile && quantile < divisor;
    assert divisor > 0;
    assert sortedValues.length > 0;
    long q = 0;
    for (int j = 0; j < divisor; j++)
      q += sortedValues[(quantile * (sortedValues.length - 1) + j)
        / divisor];
    return (q + divisor / 2) / divisor;
  }

  private static void resetMemUsage () {
    for (final MemoryPoolMXBean mem : ManagementFactory
        .getMemoryPoolMXBeans())
      mem.resetPeakUsage();
  }

  private static void waitForGC () {
    System.gc();
    try {
      Thread.sleep(1000);
    } catch (final InterruptedException e1) {
      printb("benchmark interrupted.");
    }
  }

  public abstract void benchmark ();

  public final void run () {
    counters.clear();
    waitForGC();
    resetMemUsage();
    logMessages = false;
    printb("Initializing.");
    setup();
    logMessages = true;
    printb("Running.");
    benchmark();
    final long memUsage = getMemUsage() / 1000;
    printb(String.format("Ok (memory usage: %6d kB).", memUsage));
    logMessages = false;
    if (measureTimings) {
      printb("warming up for " + WARMUP_SECS + "s.");
      final long end = System.currentTimeMillis() + WARMUP_SECS * 1000;
      System.out.print('|');
      int j = 1, times = 0;
      do {
        setup();
        benchmark();
        if (++times == j) {
          System.out.print('-');
          j *= 2;
        }
      } while (System.currentTimeMillis() < end);
      System.out.println("|");
      times = Math.max(times, RUNS);
      printb("taking " + times + " samples");
      final long[] runtimes1 = new long[times];
      System.out.print('|');
      int j1 = 1;
      for (int i = 0; i < times; i++) {
        setup();
        final long start = System.currentTimeMillis();
        benchmark();
        runtimes1[i] = System.currentTimeMillis() - start;
        if (i == j1) {
          System.out.print('-');
          j1 *= 2;
        }
      }
      System.out.println("|");
      Arrays.sort(runtimes1);
      final long[] runtimes = runtimes1;
      System.out.println(String.format("Average time: %6d ms",
          average(runtimes)));
      System.out.println(String.format("  Minimum:    %6d ms",
          runtimes[0]));
      for (int q = 1; q < 4; q++)
        System.out.println(String.format("  Quartile %1d: %6d ms", q,
            quantile(q, 4, runtimes)));
      System.out.println(String.format("  Maximum:    %6d ms",
          runtimes[runtimes.length - 1]));
    }
    displayCounters();
  }

  public void setup () {
    // overwrite this to setup your benchmark
  }
}