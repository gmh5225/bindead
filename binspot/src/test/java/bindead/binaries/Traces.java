package bindead.binaries;

import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import rreil.lang.RReilAddr;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.debug.DebugHelper;
import bindead.environment.platform.DynamicallyStartedPlatform;
import bindead.environment.platform.PlatformsRegistry;
import bindead.exceptions.DomainStateException.UnimplementedMethodException;
import binparse.Binary;
import binparse.elf.ElfBinary;
import binparse.trace.TraceBinary;
import binparse.trace.TraceDump;
import binparse.trace.Tracer;

@Ignore("Not functional anymore as the tracer itself needs to be fixed.")
public class Traces {
  public final static String tracerScript = System.getProperty("user.home") + "/devel/traces/bintrace/Tracers/trace.sh";
  private final static String binariesDirectoryPath = Traces.class.getResource("/bindead/binaries/x86_64/")
      .getPath();
  private final static String tracesDirectoryPath = Traces.class.getResource("/bindead/traces/x86_64/").getPath();
  @SuppressWarnings("unused")
  private final static String tracerLogfile = "-logfile " + System.getProperty("user.home") + "/pintool.log";

  /**
   * Ignore all the tests if the tracer is not present on this machine.
   */
  @BeforeClass public static void checkForPreconditions () {
    File tracer = new File(tracerScript);
    assumeTrue(tracer.exists());
  }

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  public void traceCatOfMemMap () throws IOException {
    String binaryFilePath = "/bin/cat";
    Binary elfBinary = new ElfBinary(binaryFilePath);
    String tracesPrefix = tracesDirectoryPath + "some.dump";
    Tracer.trace(Traces.tracerScript, binaryFilePath + " /proc/self/maps", tracesPrefix);
    TraceBinary tracesBinary = new TraceBinary(tracesPrefix, elfBinary);
    TraceDump trace = tracesBinary.getTraceDump();
    System.out.println(trace.getHeap());
    System.out.println(trace.getStack());
    System.out.println(tracesBinary.getSegment("vdso").get());
    System.out.println(tracesBinary.getSegment("vsyscall").get());
    // Analysis<?> analysis = runAnalysis(tracesBinary);
    // analysis.dumpWarnings(System.out);
  }

  /**
   * Run a normal call-string analysis on the binary-wrapper of the trace.
   */
  @Test public void testTracesBinaryWrapper () throws IOException {
    String fileName = "array-sum-int";
    TraceBinary tracesBinary = trace(fileName);
    // Tracer.dumpControlFlow(tracesBinary.getTraceDump(), true);
    Analysis<?> analysis = new AnalysisFactory().runAnalysis(tracesBinary);
  }


  /**
   * Run a trace analysis on a traced file.
   */
  // FIXME: move to new segmented root domain and make it work there as root is now broken
  @Test(expected=UnimplementedMethodException.class,timeout=10000)
  public void traceAbstractionArraySum () throws IOException {
    String fileName = "array-sum-int";
    TraceBinary tracesBinary = trace(fileName, "-start main");
//    Tracer.dumpControlFlow(tracesBinary.getTraceDump(), true);
    // AnalysisDebugHooks hook0 = DebugHelper.buildVariableWatcher(RReilAddr.valueOf(0x401196), null, "rsp", "rbp");
    // AnalysisDebugHooks hook1 = DebugHelper.buildVariableWatcher(RReilAddr.valueOf(0x401196),
    // RReilAddr.valueOf(0x4011a1),
    // "eax", "rax", "t0", "t1", "t2", "t3", "rsp");
    // AnalysisDebugHooks hook2 = DebugHelper.buildMemoryWatcher(RReilAddr.valueOf(0x401196),
    // RReilAddr.valueOf(0x4011a1),
    // Tuple3.tuple3("rbp", -8, 32));
    Analysis<?> analysis = runTraceAbstractionAnalysis(tracesBinary);
  }

  /**
   * Run a trace analysis on a traced file.
   */
  // FIXME: move to new segmented root domain and make it work there as root is now broken
  @Test(expected=UnimplementedMethodException.class,timeout=10000)
  public void traceAbstractionGlobalVars () throws IOException {
    String fileName = "global-vars-read-001";
    TraceBinary tracesBinary = trace(fileName, "-start main");
//    Tracer.dumpControlFlow(tracesBinary.getTraceDump(), true);
    // AnalysisDebugHooks hook = DebugHelper.buildVariableWatcher(RReilAddr.valueOf(0x401a10, 00), null, "rax", "r12");
    Analysis<?> analysis = runTraceAbstractionAnalysis(tracesBinary);
  }

  /**
   * Run a reachability analysis on a traced file started from main.
   */
  // FIXME: move to new segmented root domain and make it work there as root is now broken
  @Test(expected=UnimplementedMethodException.class,timeout=10000)
  public void analyzeDynamicallyStartedGlobalVars () throws IOException {
    String fileName = "global-vars-read-001";
    TraceBinary tracesBinary = trace(fileName, "-start ", "main", "-stop ", "main");
//    Tracer.dumpControlFlow(tracesBinary.getTraceDump(), true);
    Analysis<?> analysis = runDynamicallyStartedAnalysis(tracesBinary);
  }

  public static TraceBinary trace (String fileName, String... tracerOptions) throws IOException {
    String binaryFilePath = binariesDirectoryPath + fileName;
    String tracesPrefix = tracesDirectoryPath + fileName;
    Tracer.setExecutable(binaryFilePath);
    Tracer.trace(tracerScript, binaryFilePath, tracesPrefix, tracerOptions);
    Binary elfBinary = new ElfBinary(binaryFilePath);
    return new TraceBinary(tracesPrefix, elfBinary);
  }

  private static Analysis<?> runTraceAnalysis (TraceBinary binary, RReilAddr startAddress, AnalysisDebugHooks... hooks) {
    DynamicallyStartedPlatform platform = new DynamicallyStartedPlatform(binary, PlatformsRegistry.get(binary));
    Analysis<?> analysis = new AnalysisFactory().runAnalysis(binary, platform, startAddress, DebugHelper.combine(hooks), null);
    return analysis;
  }

  public static Analysis<?> runTraceAbstractionAnalysis (TraceBinary binary, AnalysisDebugHooks... hooks) {
    return runTraceAnalysis(binary, RReilAddr.valueOf(binary.getTraceDump().getTraceStartAddress()), hooks);
  }

  public static Analysis<?> runDynamicallyStartedAnalysis (TraceBinary binary, AnalysisDebugHooks... hooks) {
    return runTraceAnalysis(binary, RReilAddr.valueOf(binary.getTraceDump().getTraceEndAddress()), hooks);
  }

}
