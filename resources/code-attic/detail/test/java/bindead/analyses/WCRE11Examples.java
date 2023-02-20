package bindead.analyses;

import bindead.analyses.liveness.LivenessAnalysis;
import bindead.xml.HtmlDomainStateVisualizer.Table;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javalx.benchmark.Benchmark;
import javalx.data.BigInt;
import javalx.data.CollectionHelpers;
import javalx.data.products.P2;
import javalx.data.products.Tuple2;
import javalx.fn.Effect;
import javalx.fn.Fn;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa;
import rreil.cfa.util.CfaBuilder;
import rreil.cfa.util.CfaHelpers;
import rreil.cfa.CompositeCfa;

/**
 * Utility class to run and benchmark all the examples for the WCRE11 paper.
 */
public class WCRE11Examples {
  private final static String nameHeader = "Example Name";
  private final static String runtimeHeader = "Runtime (ms)";
  private final static String memoryHeader = "Memory (mb)";
  private final static String nativeInsnsHeader = "#Native Insns";
  private final static String LowLevelRReilsHeader = "#RREIL Insns";
  private final static String runtimeLivenessHeader = "Runtime (ms) (aL)";
  private final static String memoryLivenessHeader = "Memory (mb) (aL)";
  private final static String nativeInsnsLivenessHeader = "#Native Insns (aL)";
  private final static String LowLevelRReilsLivenessHeader = "#RREIL Insns (aL)";

  public static void main (String... args) throws IOException {
    StringBuilder output = new StringBuilder();
    benchmark32bitExamples(output);
    // benchmark64bitExamples(output);
    System.out.println(output.toString());
  }

  private static void printResultsTable (Table results, StringBuilder output) {
    output.append(results.getName() + ":");
    output.append("\n");
    results.asFormattedString(output);
  }

  private static void benchmark32bitExamples (StringBuilder output) throws IOException {
    Table results = new Table("Examples for x86-32");
    benchmarkExamples(getExamples("x86-32/"), results);
    printResultsTable(results, output);
  }

  @SuppressWarnings("unused")
  private static void benchmark64bitExamples (StringBuilder output) throws IOException {
    Table results = new Table("Examples for x86-64");
    benchmarkExamples(getExamples("x86-64/"), results);
    printResultsTable(results, output);
  }

  private static List<P2<String, String>> getExamples (String subDir) {
    String rootPath = WCRE11Examples.class.getResource("/binary/" + subDir).getPath();
    List<P2<String, String>> testExamples = new LinkedList<P2<String, String>>();
    testExamples.add(new Tuple2<String, String>("Return Obfuscation", rootPath + "return-obfuscation.o"));
    testExamples.add(new Tuple2<String, String>("McVeto Instruction Aliasing", rootPath + "mcveto-instraliasing-stack"));
    testExamples.add(new Tuple2<String, String>("Array Sum", rootPath + "array-sum-100"));
    testExamples.add(new Tuple2<String, String>("Matrix Sum", rootPath + "matrix"));
    testExamples.add(new Tuple2<String, String>("Square Root", rootPath + "isqrt"));
    testExamples.add(new Tuple2<String, String>("Matrix Sum (blowup)", rootPath + "matrix-blowup"));
    return testExamples;
  }

  private static void benchmarkExamples (List<P2<String, String>> testExamples, Table results) throws IOException {
    results.addColumn(nameHeader);
    results.addColumn(runtimeHeader);
    results.addColumn(memoryHeader);
    results.addColumn(nativeInsnsHeader);
    results.addColumn(LowLevelRReilsHeader);
    results.addColumn(runtimeLivenessHeader);
    results.addColumn(memoryLivenessHeader);
    results.addColumn(nativeInsnsLivenessHeader);
    results.addColumn(LowLevelRReilsLivenessHeader);

    for (final P2<String, String> example : testExamples) {
      Runnable normalReconstruction = new Runnable() {
        @Override public void run () {
          try {
            ReconstructionCallString.runElf(example._2());
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      };
      ReconstructionBenchmark normalBenchmark = new ReconstructionBenchmark(normalReconstruction);
      long runtime = normalBenchmark.getTime();
      long memory = normalBenchmark.getMemory();

      final ReconstructionCallString<?> reconst = ReconstructionCallString.runElf(example._2());
      int nativeInstructions = numberOfNativeInstructions(reconst.getCompositeCfa());
      int rreilInstructions = numberOfRReilInstructions(reconst.getCompositeCfa());

      final CompositeCfa cleanedForest = performLiveness(reconst);

      Cfa entryCfa = reconst.getCompositeCfa().lookupCfa("main").get();
      final RReilAddr entryAddress = entryCfa.getAddress(entryCfa.getEntry()).get();
      Runnable reconstructionAfterLiveness = new Runnable() {
        @Override public void run () {
          ReconstructionCallString.rerun(reconst.getBinary(), BigInt.valueOf(entryAddress.base()), reconst.getCompositeCfa());
        }
      };
      ReconstructionBenchmark benchmarkAfterLiveness = new ReconstructionBenchmark(reconstructionAfterLiveness);
      long runtimeAfterLiveness = benchmarkAfterLiveness.getTime();
      long memoryAfterLiveness = benchmarkAfterLiveness.getMemory();

      CompositeCfa choppedForest = chopDeadWoodOff(cleanedForest);
      int nativeInstructionsAfterLiveness = numberOfNativeInstructions(choppedForest);
      int rreilInstructionsAfterLiveness = numberOfRReilInstructions(choppedForest);
      String exampleName = example._1();
      results.bind(exampleName, nameHeader, exampleName);
      results.bind(exampleName, runtimeHeader, runtime);
      results.bind(exampleName, memoryHeader, memory);
      results.bind(exampleName, nativeInsnsHeader, nativeInstructions);
      results.bind(exampleName, LowLevelRReilsHeader, rreilInstructions);
      results.bind(exampleName, runtimeLivenessHeader, runtimeAfterLiveness);
      results.bind(exampleName, memoryLivenessHeader, memoryAfterLiveness);
      results.bind(exampleName, nativeInsnsLivenessHeader, nativeInstructionsAfterLiveness);
      results.bind(exampleName, LowLevelRReilsLivenessHeader, rreilInstructionsAfterLiveness);
    }
  }

  private static CompositeCfa performLiveness (ReconstructionCallString<?> reconst) {
    CompositeCfa forest = reconst.getCompositeCfa();
    LivenessAnalysis liveness = new LivenessAnalysis(forest, reconst.getPlatform());
    CompositeCfa choppedForest = liveness.runOnAll();
    return choppedForest;
  }

  private static CompositeCfa chopDeadWoodOff (CompositeCfa forest) {
    CompositeCfa choppedForest = new CompositeCfa(forest.getDisassemblyProvider());
    for (Cfa cfa : forest) {
      cfa = CfaBuilder.cleanup(cfa); // also get rid of any nops
      choppedForest.put(cfa);
    }
    return choppedForest;
  }

  private static int numberOfRReilInstructions (CompositeCfa forest) {
    int instructions = 0;
    for (Cfa cfa : forest) {
      cfa = CfaBuilder.expandBasicBlocks(cfa);
      instructions = instructions + CfaHelpers.getAddressableVertices(cfa).size();
    }
    return instructions;
  }

  private static int numberOfNativeInstructions (CompositeCfa forest) {
    int instructions = 0;
    for (Cfa cfa : forest) {
      cfa = CfaBuilder.expandBasicBlocks(cfa);
      Set<RReilAddr> rreilAddresses = CfaHelpers.getAddressableVertices(cfa).keySet();
      int nativeInstructions = CollectionHelpers.map(rreilAddresses, new Fn<RReilAddr, Long>() {
        @Override public Long apply (RReilAddr address) {
          return address.base();
        }
      }).size();
      instructions = instructions + nativeInstructions;
    }
    return instructions;
  }

  private static class ReconstructionBenchmark extends Benchmark<Void> {
    private final Runnable action;

    public ReconstructionBenchmark (final Runnable action) {
      super(new Effect<Void>() {
        @Override public void observe (Void _) {
          action.run();
        }
      });
      this.action = action;
    }

    public long getTime () {
      List<Long> runtimes = run(10).apply(null)._2();
      long median = 0;
      for (Long time : runtimes) {
        median = median + time;
      }
      median = median / runtimes.size();
      return median;
    }

    public long getMemory () {
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
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }

    private static void resetMemUsage () {
      for (MemoryPoolMXBean mem : ManagementFactory.getMemoryPoolMXBeans()) {
        mem.resetPeakUsage();
      }
    }
  }
}
