package bindead.environment.abi;

import java.util.List;

import javalx.data.products.P2;
import javalx.numeric.FiniteRange;
import javalx.numeric.Range;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Branch;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rvar;
import bindead.analyses.ProgramAddress;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.platform.Platform;

/**
 * Utility objects to manipulate the domain state in a platform and ABI agnostic way.
 *
 * @author Bogdan Mihaila
 */
public abstract class ABI {
  private static final String tempReg = "tmpReg_" + ABI.class.getSimpleName();
  private final Platform platform;
  private final int archSize;

  public ABI (Platform platform) {
    this.platform = platform;
    archSize = platform.defaultArchitectureSize();
  }

  // REFACTOR: move all this methods and the stack inc/dec, canary to the platform class
  protected String sub (String targetReg, String sourceReg, int value) {
    return String.format("sub.%d %s, %s, %d", archSize, targetReg, sourceReg, value);
  }

  protected String add (String targetReg, String sourceReg, int value) {
    return String.format("add.%d %s, %s, %d", archSize, targetReg, sourceReg, value);
  }

  protected String load (String targetReg, String sourceReg) {
    return String.format("load.%d.%d %s, %s", archSize, archSize, targetReg, sourceReg);
  }

  protected String store (String addressReg, long value) {
    return String.format("store.%d.%d %s, %s", archSize, archSize, addressReg, value);
  }

  protected String mov (String targetReg, String sourceReg) {
    return String.format("mov.%d %s, %s", archSize, targetReg, sourceReg);
  }

  protected String call (long address) {
    return String.format("call.%d %s", archSize, address);
  }

  protected String ret (String targetRegister) {
    return String.format("return.%d %s", archSize, targetRegister);
  }

  protected Range getValueOf (String register, RootDomain<?> domainState) {
    return domainState.queryRange(MemVar.getVarOrFresh(register), FiniteRange.of(0, archSize - 1));
  }

  /**
   * @return The analysis platform that this ABI is operating on.
   */
  public Platform getPlatform () {
    return platform;
  }

  /**
   * Add 1 to the stack pointer, where "1" means one time the platform's addressable unit size.
   */
  public <D extends RootDomain<D>> D incStackPointer (D state) {
    int stackCellSizeInBytes = archSize / 8;
    String sp = getPlatform().getStackPointer();
    return state.eval(add(sp, sp, stackCellSizeInBytes));
  }

  /**
   * Subtract 1 from the stack pointer, where "1" means one time the platform's addressable unit size.
   */
  public <D extends RootDomain<D>> D decStackPointer (D state) {
    int stackCellSizeInBytes = archSize / 8;
    String sp = getPlatform().getStackPointer();
    return state.eval(sub(sp, sp, stackCellSizeInBytes));
  }

  /**
   * Set the value of the instruction pointer/program counter.
   */
  public <D extends RootDomain<D>> D setInstructionPointer (D state, long pcValue) {
    state = state.eval(String.format("mov.%d %s, %d", archSize, platform.getInstructionPointer(), pcValue));
    return state;
  }

  /**
   * Retrieves the return address of the current function. Depending on the platform it can be found on the stack or
   * in a special register. In both cases this method should be called on the function entry as the return address
   * could be modified or spilled during the function's execution or even impossible to find if the stack level changed.
   *
   * @param domainState The domain state to query.
   * @return The address that this function should return to.
   */
  public abstract RReilAddr getReturnAddress (RootDomain<?> domainState);

  /**
   * Retrieves the value of the n-th function parameter as specified by {@code parameterNumber}.
   *
   * @param parameterNumber The number of the parameter as seen from left-to-right.
   * @param domainState The domain state to query.
   * @return The value of the n-th function parameter
   */
  public abstract Range getFunctionParameterValue (int parameterNumber, RootDomain<?> domainState);


  /**
   * Assigns the value of the n-th function parameter to a register.
   *
   * @param parameterNumber The number of the parameter as seen from left-to-right.
   * @param reg The name of a register
   * @param domainState The domain state to apply the operation on.
   * @return The resulting domain state where the given register contains the value of the n-th parameter
   */
  public abstract <D extends RootDomain<D>> D assignFunctionParameterToRegister (int parameterNumber, String reg,
      D domainState);

  /**
   * Assigns the value of the given register to the return value location of the function.
   *
   * @param reg The name of a register
   * @param domainState The domain state to apply the operation on.
   * @return The resulting domain state where the return value of the current function is the value of the given register
   */
  public abstract <D extends RootDomain<D>> D setRegisterAsReturnValue (String reg, D domainState);

  /**
   * @return the variable that will be used as the return location of the return value of the function.
   */
  public abstract Rvar getReturnParameterAllocation ();

  /**
   * Fetches the current stack offset from domain state {@code domainState}.
   *
   * @param domainState The domain state to query.
   * @return The current stack offset.
   */
  // REFACTOR: remove or implement it similar to how the offset is retrieved in StackSegment
  public Range getCurrentStackOffset (RootDomain<?> domainState) {
    return domainState.queryRange(getPlatform().getRegisterAsVariable(getPlatform().getStackPointer()));
  }

  /**
   * Fetches the value at a certain offset from the stack using the stack pointer. The offset is to be seen as a multiple
   * of the platform's addressable unit size (on most platforms that is a byte).
   *
   * @param offset The offset on the stack at which to get the value from.
   * @param domainState The domain state to query.
   * @return The value at the specified stack offset.
   */
  public Range getValueFromStack (int offset, RootDomain<?> domainState) {
    int stackCellSizeInBytes = archSize / 8;
    String sp = getPlatform().getStackPointer();
    RootDomain<?> state = domainState;
    // XXX bm: the subtraction should actually be an addition on some platforms where the stack grows upwards
    String[] insns = {
      sub(sp, sp, stackCellSizeInBytes * offset),
      load(tempReg, sp)};
    state = (RootDomain<?>) state.eval(insns);
    return getValueOf(tempReg, state);
  }

  /**
   * Write a canary value to the stack and do a call to the start address. By doing this we use a new stack frame for
   * the analysis start and an artificial stack frame where only the canary is saved. When the analysis performs a
   * return at the end of the analyzed function it will jump to the canary value as that one is saved on the stack.
   *
   * TODO: for platforms using a link register for returns this has to be done differently and the code moved into
   * the corresponding platform
   */
  public <D extends RootDomain<D>> D writeAnalysisStartStackCanary (RReilAddr startAddress, long stackCanary,
      D domainState) {
    D state = domainState;
    String sp = getPlatform().getStackPointer();
    // save the canary value on the stack to mark the beginning of the stack
    state = decStackPointer(state);
    state = state.eval(store(sp, stackCanary));
    // make a new stack frame for the analysis to start at
    ProgramAddress dummyPoint = new ProgramAddress(RReilAddr.ZERO);
    ProgramAddress canaryPoint = new ProgramAddress(RReilAddr.valueOf(stackCanary));
    // the address given as next instruction address must be the canary point such that the domains recognizes the call
    List<P2<RReilAddr, D>> result =
      ABI.evalBranch(state, call(startAddress.base()), dummyPoint, canaryPoint);
    assert result.size() == 1;
    state = result.get(0)._2();
    return state;
  }

  /**
   * Execute the effects of a "return" instruction on the domain state.
   */
  public <D extends RootDomain<D>> D evalReturn (D domainState) {
    D state = domainState;
    String sp = getPlatform().getStackPointer();
    // save return address in tempReg
    state = state.eval(load(tempReg, sp));
    // adjust stack pointer to point before the return address, i.e. into the previous stack frame
    state = incStackPointer(state);
    // evaluate the return on the domain
    ProgramAddress dummyPoint = new ProgramAddress(RReilAddr.ZERO);
    List<P2<RReilAddr, D>> result = ABI.evalBranch(state, ret(tempReg), dummyPoint, dummyPoint);
    assert result.size() == 1;
    state = result.get(0)._2();
    return state;
  }

  /**
   * @return A {@link List} of register names which are used to pass arguments to syscall functions, from 1. to last.
   */
  public abstract List<String> getSyscallInputRegisterNames ();

  /**
   * @return The name of the register which is used to pass the syscall identifier
   */
  public abstract String getSyscallNrRegisterName ();

  /**
   * @return The name of the register which is used for the return value of a syscall
   */
  public abstract String getSyscallReturnRegisterName ();

  /**
   * Convenience method to manipulate the current state by using a RREIL instruction given in RREIL assembler syntax.
   * Note that only branch instructions will be executed.
   *
   * @param instructions An RREIL assembler branch instruction to be executed on this state
   * @param current The current program point of the instruction
   * @param next The next program point after this instruction
   * @return The resulting state for each branch target after executing the instruction
   * @see #eval(String...)
   */
  final private static <D extends RootDomain<D>> List<P2<RReilAddr, D>> evalBranch (D state,
      String instruction, ProgramPoint current, ProgramPoint next) {
    RReil rreilInstruction = RReil.from(instruction);
    if (!(rreilInstruction instanceof Branch))
      throw new UnsupportedOperationException("Instruction not applicable to this eval on the state " + rreilInstruction);
    Branch branch = (Branch) rreilInstruction;
    return state.eval(branch, current, next);
  }
}