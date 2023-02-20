package bindead.environment.platform;

import rreil.lang.MemVar;
import rreil.lang.Rhs.Rvar;
import bindead.analyses.Bootstrap;
import bindead.domainnetwork.interfaces.RootDomain;
import bindis.Disassembler;

/**
 * Objects providing platform/processor specific data.
 */
public abstract class Platform {
  private final Disassembler disassembler;
  private final String platformName;

  public Platform (Disassembler disassembler) {
    this.disassembler = disassembler;
    this.platformName = disassembler.getArchitectureName();
  }

  /**
   * @return {@code this} platform's name.
   */
  public String getName () {
    return platformName;
  }

  /**
   * Returns domain bootstrap code for generic forward analyses. This should set up at least a stack-region and a
   * valid stack-pointer pointing to the stack-region.
   *
   * @return Generic domain bootstrap code for forward analyses.
   */
  public abstract Bootstrap forwardAnalysisBootstrap ();

  /**
   * Returns the name for the canonical stack pointer register on the platform.
   */
  public abstract String getStackPointer ();

  /**
   * Returns the name for the canonical instruction pointer register on the platform.
   */
  public abstract String getInstructionPointer ();


  /**
   * TODO: remove as it was only a hack
   */
  @Deprecated public abstract MemVar getStackRegion ();

  /**
   * The default register size in bits of the underlying processor architecture.
   *
   * @return The default architecture size in bits.
   */
  public int defaultArchitectureSize () {
    return disassembler.defaultArchitectureSize();
  }

  /**
   * @return The disassembler of this platform.
   */
  public Disassembler getDisassembler () {
    return disassembler;
  }

  /**
   * Translate a register name to its corresponding "right-hand-side" variable, that is a unique identifier with
   * size and offset annotation.
   *
   * @param name
   * @return The unique {@code Rvar} identifier corresponding to {@code name}.
   */
  public Rvar getRegisterAsVariable (String name) {
    return (Rvar) disassembler.translateIdentifier(name).toRReil();
  }

  /**
   * To be called by subclasses during the bootstrapping.
   */
  protected <D extends RootDomain<D>> D initializeStackPointer (D state) {
    state = state.eval(String.format("prim addRegisters (%s.%d)", getStackPointer(), defaultArchitectureSize()));
    state = state.eval(String.format("prim (%s.%d) = currentStackFrameAddress ()", getStackPointer(),
        defaultArchitectureSize()));
    return state;
  }

  /**
   * To be called by subclasses during the bootstrapping.
   */
  protected <D extends RootDomain<D>> D initializeInstructionPointer (D state, long initialPC) {
    state = state.eval(String.format("prim addRegisters (%s.%d)", getInstructionPointer(), defaultArchitectureSize()));
    state = state.eval(String.format("mov.%d %s, %d", defaultArchitectureSize(), getInstructionPointer(), initialPC));
    return state;
  }

}