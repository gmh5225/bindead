package bindead.binaries;

import java.io.IOException;

import javalx.numeric.Range;

import org.junit.Ignore;
import org.junit.Test;

import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.algorithms.data.Flows;
import bindead.analyses.callback.CallbackHandler;
import bindead.analyses.callback.Callbacks;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import bindead.environment.abi.ABI;
import bindead.environment.abi.X64SysVAbi;
import binparse.Binary;

@Ignore // see if it should be fixed or thrown away
public class Primitives {
  private final static String mallocName = "__libc_malloc";

  /**
   * Test the execution of a primitive for the malloc function.
   */
  @Test public void malloc () throws IOException {
    Binary binary = TestsHelper.get64bitExamplesBinary("malloc-simple");
    RReilAddr mallocAddress = getMallocAddress(binary);
    Analysis<?> analysis = new AnalysisFactory().runAnalysis(binary, null, buildMallocPrimitive(mallocAddress));
  }

  private static Callbacks buildMallocPrimitive (RReilAddr mallocAddress) {
    Callbacks registry = new Callbacks();
    CallbackHandler mallocPrimitive = new CallbackHandler() {
      @Override public <D extends RootDomain<D>> void run (
          RReil insn, D domainState, ProgramPoint point, AnalysisEnvironment env, Flows<D> flow) {
        ABI abi = new X64SysVAbi(env.getPlatform());
        RReilAddr returnAddress = abi.getReturnAddress(domainState);
        Range allocationSize = abi.getFunctionParameterValue(0, domainState);
        System.out.println("malloc call at:  " + insn.getRReilAddress());
        System.out.println(" with requested region size of: " + allocationSize);
        System.out.println("call returns to: " + returnAddress);
        MemVar regionID = MemVar.fresh();
        domainState = domainState.introduceRegion(regionID, RegionCtx.EMPTYSTICKY);
        // FIXME: does not exist anymore
        // domainState = domainState.assignSymbolicAddressOf(abi.getReturnParameterAllocation().asLhs(), regionID);
        flow.addJump(returnAddress, domainState);
      }
    };
    registry.addHandler(mallocAddress, mallocPrimitive);
    return registry;
  }

  private static RReilAddr getMallocAddress (Binary binary) {
    return RReilAddr.valueOf(binary.getSymbol(mallocName).get().getAddress());
  }
}
