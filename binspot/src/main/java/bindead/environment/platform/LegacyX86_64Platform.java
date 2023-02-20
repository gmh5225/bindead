package bindead.environment.platform;

import rreil.lang.MemVar;
import bindead.analyses.Bootstrap;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindis.x86.x64.X64Disassembler;

/**
 * Platform for Intel x86-64bit processors using the old disassembler implemented in Java.
 */
public class LegacyX86_64Platform extends Platform {
  private static Platform INSTANCE;
  private static final String stackPointerName = "rsp";
  private static final String instructionPointerName = "rip";

  public LegacyX86_64Platform () {
    super(X64Disassembler.INSTANCE);
  }

  public static Platform instance () {
    if (INSTANCE == null)
      INSTANCE = new LegacyX86_64Platform();
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Bootstrap forwardAnalysisBootstrap () {
    return new Bootstrap() {
      @Override public <D extends RootDomain<D>> D bootstrap (D state, long initialPC) {
        // TODO: only here for the old ROOT domain. remove when root is deleted
        state = state.introduceRegion(MemVar.getVarOrFresh("stack"), RegionCtx.EMPTYSTICKY);
        state = initializeStackPointer(state);
        state = LegacyX86_32Platform.initializeFlags(state);
        state = initializeInstructionPointer(state, initialPC);
        return state;
      }
    };
  }

  @Override public String getStackPointer () {
    return stackPointerName;
  }

  @Override public String getInstructionPointer () {
    return instructionPointerName;
  }

  @Override public MemVar getStackRegion () {
    return MemVar.getVarOrFresh("stack.x86_64");
  }

}
