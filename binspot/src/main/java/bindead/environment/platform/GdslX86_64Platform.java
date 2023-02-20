package bindead.environment.platform;

import java.util.HashMap;
import java.util.Map;

import rreil.gdsl.builder.IdBuilder;
import rreil.lang.MemVar;
import bindead.analyses.Bootstrap;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindis.Disassembler;
import bindis.gdsl.GdslX86_64Disassembler;

/**
 * Platform for Intel x86-32bit processors using the old disassembler implemented in Java.
 */
public class GdslX86_64Platform extends Platform {
  private static Platform INSTANCE;
  private static final String stackPointerName = "rsp";
  private static final String instructionPointerName = "rip";
  private static Map<String, String> renamings;
  static {
    renamings = new HashMap<String, String>() {
      {
        put("virt_leu", "BE");
        put("virt_lts", "LT");
        put("virt_les", "LE");
        put("a", "rax");
        put("b", "rbx");
        put("c", "rcx");
        put("d", "rdx");
        put("si", "rsi");
        put("di", "rdi");
        put("bp", "rbp");
        put("sp", "rsp");
        put("ip", "rip");
      }
    };
  }

  private GdslX86_64Platform () {
    super(GdslX86_64Disassembler.instance);
  }

  public static Platform instance () {
    if (INSTANCE == null)
      INSTANCE = new GdslX86_64Platform();
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
        state = GdslX86_32Platform.initializeSegmentRegisters(state);
        state = GdslX86_32Platform.initializeFlags(state);
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

  @Deprecated @Override public MemVar getStackRegion () {
    return MemVar.getVarOrFresh("stack.x86_64");
  }

}
