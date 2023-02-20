package bindead.environment.platform;

import rreil.lang.MemVar;
import bindead.analyses.Bootstrap;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindis.x86.x32.X32Disassembler;

/**
 * Platform for Intel x86-32bit processors using the old disassembler implemented in Java.
 */
public class LegacyX86_32Platform extends Platform {
  private static Platform INSTANCE;
  private static final String stackPointerName = "esp";
  private static final String instructionPointerName = "eip";

  private LegacyX86_32Platform () {
    super(X32Disassembler.INSTANCE);
  }

  public static Platform instance () {
    if (INSTANCE == null)
      INSTANCE = new LegacyX86_32Platform();
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
        state = initializeFlags(state);
        state = initializeInstructionPointer(state, initialPC);
        return state;
      }
    };
  }

  /**
   * Set initial values for some flags (bits in the flag register).
   */
  static <D extends RootDomain<D>> D initializeFlags (D state) {
    state = state.eval(String.format("mov.%d %s, 0", 1, "DF"));
    return state;
  }

  @Override public String getStackPointer () {
    return stackPointerName;
  }

  @Override public String getInstructionPointer () {
    return instructionPointerName;
  }

  @Deprecated @Override public MemVar getStackRegion () {
    return MemVar.getVarOrFresh("stack.x86_32");
  }

}
