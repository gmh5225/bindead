package bindead.binaries;

import java.io.IOException;

import javalx.data.Option;

import org.junit.Ignore;
import org.junit.Test;

import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RhsFactory;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.analyses.algorithms.TraceAnalysis;
import bindead.analyses.algorithms.data.Flows;
import bindead.analyses.callback.CallbackHandler;
import bindead.analyses.callback.Callbacks;
import bindead.debug.DebugHelper;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import bindead.environment.abi.ABI;
import bindead.environment.abi.X64SysVAbi;
import bindead.environment.platform.DynamicallyStartedPlatform;
import bindead.environment.platform.Platform;
import bindead.environment.platform.PlatformsRegistry;
import binparse.Binary;
import binparse.Symbol;
import binparse.elf.ElfBinary;
import binparse.trace.TraceBinary;
import binparse.trace.Tracer;

@Ignore("Not functional anymore at the moment.")
public class DynamicallyStarted {
  public final static String tracerScript = System.getProperty("user.home") + "/devel/traces/bintrace/Tracers/trace.sh";
  private final static String binariesDirectoryPath = System.getProperty("user.home") +
    "/devel/work/paper/experiments/Static+Debug/";
  private final static String tracesDirectoryPath = Traces.class.getResource("/bindead/traces/x86_64/").getPath();

  @Test public void analyzeBufferOverflow () throws IOException {
    // String fileName = "intoverflow";
    // String fileName = "charcast";
    String fileName = "Experiments";
    TraceBinary tracesBinary = trace(fileName, "-start main");
    Tracer.dumpControlFlow(tracesBinary.getTraceDump(), true);
    Analysis<?> analysis = getAnalysis(tracesBinary);
    RReilAddr startAddress = RReilAddr.valueOf(tracesBinary.getTraceDump().getTraceStartAddress());
//    analysis.setPrimitivesHandler(buildTopPrimitiveFor("read_number_of_elements", tracesBinary, analysis));
    RReilAddr enableAddress = RReilAddr.valueOf(0x40133e);
    AnalysisDebugHooks hook1 =
      DebugHelper.printers.variablesWatcher(enableAddress, null, "rsp", "rax", "t0", "t1");
//    AnalysisDebugHooks hook2 = DebugHelper.buildMemoryWatcher(analysis, enableAddress, null, Tuple3.tuple3("rbp", -0x28, 64),
//        Tuple3.tuple3("rbp", -0x18, 64));
    AnalysisDebugHooks hook3 = buildDebugHook();
    analysis.setDebugHooks(DebugHelper.combine(hook1, hook3));
    analysis.runFrom(startAddress);
  }

  private static AnalysisDebugHooks buildDebugHook () {
    AnalysisDebugHooks debugger = new AnalysisDebugHooks() {
      @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint ctx, D domainState,
          Analysis<D> analysis) {
//         if (insn.getRReilAddress().base() == 0x40134a)
        if (insn.getRReilAddress().equals(RReilAddr.valueOf("0x4013a7")))
          System.out.println("bla");
      }

      @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target,
          D domainState, Analysis<D> analysis) {
        // empty
      }
    };
    return debugger;
  }

  public static Callbacks buildTopPrimitiveFor (String functionName, Binary binary, final Analysis<?> analysis) {
    Callbacks registry = new Callbacks();
    Option<Symbol> functionOption = binary.getSymbol(functionName);
    if (functionOption.isNone())
      return registry;
    Symbol function = functionOption.get();
    RReilAddr functionAddress = RReilAddr.valueOf(function.getAddress());
    CallbackHandler functionPrimitive = new CallbackHandler() {
      @Override public <D extends RootDomain<D>> void run (
          RReil insn, D domainState, ProgramPoint point, AnalysisEnvironment env, Flows<D> flow) {
        ABI abi = new X64SysVAbi(env.getPlatform());
        RReilAddr returnAddress = abi.getReturnAddress(domainState);
        RhsFactory rhsFactory = RhsFactory.getInstance();
        RangeRhs top = rhsFactory.arbitrary(64);
        Rvar returnRegister = abi.getReturnParameterAllocation();
        RReil.Assign setTarget = new RReil.Assign(RReilAddr.ZERO, returnRegister, top);
        domainState = domainState.eval(setTarget);
        flow.addJump(returnAddress, domainState);
        if (analysis instanceof TraceAnalysis) {
          ((TraceAnalysis<?>) analysis).traceIterator.advance(); // hack to remove any tracing in the primitive function
        }
      }
    };
    registry.addHandler(functionAddress, functionPrimitive);
    return registry;
  }

  private static TraceBinary trace (String fileName, String... tracerOptions) throws IOException {
    String binaryFilePath = binariesDirectoryPath + fileName;
    String tracesPrefix = tracesDirectoryPath + fileName;
    Tracer.setExecutable(binaryFilePath);
    Tracer.trace(tracerScript, binaryFilePath, tracesPrefix, tracerOptions);
    Binary elfBinary = new ElfBinary(binaryFilePath);
    return new TraceBinary(tracesPrefix, elfBinary);
  }

  @SuppressWarnings({"unchecked", "rawtypes"}) public static Analysis<?> getAnalysis (TraceBinary binary) {
    Platform platform = new DynamicallyStartedPlatform(binary, PlatformsRegistry.get(binary));
    // AnalysisPlatform platform = PlatformsRegistry.get(binary);
    AnalysisEnvironment environment = new AnalysisEnvironment(platform);
    Analysis<?> analysis = new TraceAnalysis(environment, binary, new AnalysisFactory().initialDomain());
    return analysis;
  }

}
