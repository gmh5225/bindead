package bindead.binaries;

import java.io.IOException;

import javalx.data.Option;
import javalx.numeric.Range;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import bindead.TestsHelper;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.algorithms.data.Flows;
import bindead.analyses.callback.CallbackHandler;
import bindead.analyses.callback.Callbacks;
import bindead.debug.DebugHelper;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import bindead.environment.abi.ABI;
import bindead.exceptions.DomainStateException.UnimplementedMethodException;
import binparse.Binary;
import binparse.Symbol;

// XXX: Note that most of the tests do not work as long as tailcalls handling is not implemented in StackSeg
public class AVLTree {
  private final static String mallocName = "malloc@plt";
  private final static AnalysisFactory analyzer = new AnalysisFactory();

  private static void addMallocCallback (Binary binary, Callbacks callbacks) {
    CallbackHandler mallocPrimitive = new CallbackHandler() {
      @Override public <D extends RootDomain<D>> void run (
          RReil insn, D domainState, ProgramPoint point, AnalysisEnvironment env, Flows<D> flow) {
        D state = domainState;
        ABI abi = env.getABI();
        RReilAddr returnAddress = abi.getReturnAddress(state);
        Range allocationSize = abi.getFunctionParameterValue(0, state);
        System.out.println();
        System.out.println("malloc call at:  " + insn.getRReilAddress());
        System.out.println(" with requested region size of: " + allocationSize);
        System.out.println("call returns to: " + returnAddress);
        System.out.println();
        String parameterRegister = "mallocSize";
        String returnValueRegister = "mallocPointer";
        int archSize = abi.getPlatform().defaultArchitectureSize();
        state = abi.assignFunctionParameterToRegister(0, parameterRegister, state);
        state = state.eval(
            String.format("prim (%s.%d) = malloc(%s.%d)", returnValueRegister, archSize, parameterRegister, archSize));
        state = abi.setRegisterAsReturnValue(returnValueRegister, state);
        // execute return on domain and then let the fixpoint continue at the return point
        state = abi.evalReturn(state);
        flow.addReturn(returnAddress, state);
      }
    };
    RReilAddr mallocAddress = RReilAddr.valueOf(binary.getSymbol(mallocName).get().getAddress());
    callbacks.addHandler(mallocAddress, mallocPrimitive);
  }

  private static void addExitCallback (final String functionName, Binary binary, Callbacks callbacks) {
    CallbackHandler exitPrimitive = new CallbackHandler() {
      @Override public <D extends RootDomain<D>> void run (
          RReil insn, D domainState, ProgramPoint point, AnalysisEnvironment env, Flows<D> flow) {
        ABI abi = env.getABI();
        RReilAddr returnAddress = abi.getReturnAddress(domainState);
        System.out.println();
        System.out.println(functionName + " call at:  " + insn.getRReilAddress());
        System.out.println("call _would_ return to: " + returnAddress);
        System.out.println();
        flow.addHalt(domainState);
      }
    };
    Option<Symbol> symbol = binary.getSymbol(functionName);
    if (symbol.isNone())
      return;
    RReilAddr functionAddress = RReilAddr.valueOf(symbol.get().getAddress());
    callbacks.addHandler(functionAddress, exitPrimitive);
  }

  private static void addNopCallback (final String functionName, Binary binary, Callbacks callbacks) {
    CallbackHandler nopPrimitive = new CallbackHandler() {
      @Override public <D extends RootDomain<D>> void run (
          RReil insn, D domainState, ProgramPoint point, AnalysisEnvironment env, Flows<D> flow) {
        ABI abi = env.getABI();
        RReilAddr returnAddress = abi.getReturnAddress(domainState);
        System.out.println();
        System.out.println(functionName + " call at:  " + insn.getRReilAddress());
        System.out.println("call returns to: " + returnAddress);
        System.out.println();
        D state = domainState;
        state = abi.evalReturn(state);
        flow.addReturn(returnAddress, state);
      }
    };
    Option<Symbol> symbol = binary.getSymbol(functionName);
    if (symbol.isNone())
      return;
    RReilAddr functionAddress = RReilAddr.valueOf(symbol.get().getAddress());
    callbacks.addHandler(functionAddress, nopPrimitive);
  }

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  @Test(expected = UnimplementedMethodException.class)
  public void avltreeWeiss32_O0 () throws IOException {
    run32("avltreeWeiss-O0");
  }

  @Ignore // program contains cmovnl (conditional move non-zero or less) which is unsupported
  @Test public void avltreeWeiss32_O2 () throws IOException {
    run32("avltreeWeiss-O2");
  }

  @Test(expected = UnimplementedMethodException.class)
  public void avltreeWeiss64_O0 () throws IOException {
    run64("avltreeWeiss-O0");
  }

  @Test(expected = UnimplementedMethodException.class)
  public void avltreeWeiss64_O2 () throws IOException {
    run64("avltreeWeiss-O2");
  }

  @Test(expected = UnimplementedMethodException.class)
  public void avltreePineWiki32_O0 () throws IOException {
    run32("avlTreePineWiki-O0");
  }

  @Test(expected = UnimplementedMethodException.class)
  public void avltreePineWiki32_O2 () throws IOException {
    run32("avlTreePineWiki-O2");
  }

  @Test(expected = UnimplementedMethodException.class)
  public void avltreePineWiki64_O0 () throws IOException {
    run64("avlTreePineWiki-O0");
  }

  @Test(expected = UnimplementedMethodException.class)
  public void avltreePineWiki64_O2 () throws IOException {
    run64("avlTreePineWiki-O2");
  }

  private static void run32 (String resourcePath) throws IOException {
    run(TestsHelper.get32bitExamplesBinary(resourcePath));
  }

  private static void run64 (String resourcePath) throws IOException {
    run(TestsHelper.get64bitExamplesBinary(resourcePath));
  }

  private static void run (Binary binary) {
    Callbacks callbacks = new Callbacks();
    addMallocCallback(binary, callbacks);
    addExitCallback("exit", binary, callbacks);
    addExitCallback("__assert_fail", binary, callbacks);
    addNopCallback("__printf_chk", binary, callbacks);
    addNopCallback("__fprintf_chk", binary, callbacks);
    analyzer.runAnalysis(binary, null, callbacks);
  }

}
