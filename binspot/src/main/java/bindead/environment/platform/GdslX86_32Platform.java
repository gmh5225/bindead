package bindead.environment.platform;

import java.util.HashMap;
import java.util.Map;

import rreil.gdsl.builder.IdBuilder;
import rreil.lang.MemVar;
import bindead.analyses.Bootstrap;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindis.Disassembler;
import bindis.gdsl.GdslX86_32Disassembler;

/**
 * Platform for Intel x86-32bit processors using the old disassembler implemented in Java.
 */
public class GdslX86_32Platform extends Platform {
  private static Platform INSTANCE;
  private static final String stackPointerName = "esp";
  private static final String instructionPointerName = "eip";
  private static Map<String, String> renamings;
  static {
    renamings = new HashMap<String, String>() {
      {
        put("virt_leu", "BE");
        put("virt_lts", "LT");
        put("virt_les", "LE");
        put("a", "eax");
        put("b", "ebx");
        put("c", "ecx");
        put("d", "edx");
        put("si", "esi");
        put("di", "edi");
        put("bp", "ebp");
        put("sp", "esp");
        put("ip", "eip");
      }
    };
  }

  private GdslX86_32Platform () {
    super(GdslX86_32Disassembler.instance);
  }

  public static Platform instance () {
    if (INSTANCE == null)
      INSTANCE = new GdslX86_32Platform();
    return INSTANCE;
  }

  @Override public Disassembler getDisassembler () {
    IdBuilder.setRenamings(renamings);
    return super.getDisassembler();
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
        // set initial value of GDSL stack-segment base register
        state = initializeSegmentRegisters(state);
        state = initializeFlags(state);
        state = initializeInstructionPointer(state, initialPC);
        return state;
      }
    };
  }

  /**
   * Initialize all the segment registers with 0.
   */
  static <D extends RootDomain<D>> D initializeSegmentRegisters (D state) {
    String[] segPrefixes = {"e", "d", "s", "f", "g", "c"};
    for (String segPrefix : segPrefixes) {
      // FIXME: the segments registers are currently 64 bits on all x86 platforms.
      // Retrieve it from the platform config.
      state = state.eval(String.format("mov.%d %s, 0", 64, segPrefix + "s_base"));
    }
    return state;
  }

  /**
   * Set initial values for some flags (bits in the flag register).
   */
  static <D extends RootDomain<D>> D initializeFlags (D state) {
    state = state.eval(String.format("mov.%d %s, 0", 1, "flags/10"));
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
