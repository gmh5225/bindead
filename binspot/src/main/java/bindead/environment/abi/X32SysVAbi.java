package bindead.environment.abi;

import java.util.Arrays;
import java.util.List;

import javalx.numeric.Range;



import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rvar;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.platform.Platform;

public class X32SysVAbi extends ABI {
  public static final String CLIB_RETURN_REGISTER = "eax";
  private static final String[] SYSCALL_INPUT_REGISTERS = {"ebx", "ecx", "edx", "esi", "edi", "ebp"};
  private static final String SYSCALL_RET_REGISTER = "eax";
  private static final String SYSCALL_NR_REGISTER = "eax";
  private static final String tempReg = "tmpReg_" + X32SysVAbi.class.getSimpleName();

  public X32SysVAbi (Platform platform) {
    super(platform);
  }

  @Override public Rvar getReturnParameterAllocation () {
    return getPlatform().getRegisterAsVariable(CLIB_RETURN_REGISTER);
  }

  @Deprecated public Rvar getFunctionParameterAllocation (int parameterNumber, RootDomain<?> state) {
    Platform platform = getPlatform();
    Range stackOffset = getCurrentStackOffset(state);
    if (!stackOffset.isConstant())
      throw new UnsupportedOperationException("Non-constant stack offset.");
    // SysV ABI says that the parameters are on the stack at ebp/esp + {8, 12, 16, 20 ...}
    // the initial offset 8 is because of the space reserved for the return address and frame pointer saving
    // as the frame pointer saving is done in the callee prolog and thus not done at function entry point the parameter
    // offset are really ebp/esp + {4, 8, 12, 16 ...}
    int parameterOffset =
      (stackOffset.getConstantOrNull().intValue() + platform.defaultArchitectureSize() / 8 * parameterNumber + 4) * 8;
    return new Rvar(platform.defaultArchitectureSize(), parameterOffset, platform.getStackRegion());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override public Range getFunctionParameterValue (int parameterNumber, RootDomain<?> domainState) {
    // need the cast to raw type without generic param to be able to pass it to the method call below
    RootDomain state = domainState;
    state = assignFunctionParameterToRegister(parameterNumber, tempReg, state);
    Range value = getValueOf(tempReg, state);
    return value;
  }

  @Override
  public <D extends RootDomain<D>> D assignFunctionParameterToRegister (int parameterNumber, String reg, D domainState) {
    String sp = getPlatform().getStackPointer();
    int size = getPlatform().defaultArchitectureSize();
    // SysV ABI says that the parameters are on the stack at ebp/esp + {8, 12, 16, 20 ...}
    // the initial offset 8 is because of the space reserved for the return address and frame pointer saving
    // as the frame pointer saving is done in the callee prolog and thus not done at function entry point the parameter
    // offset are really ebp/esp + {4, 8, 12, 16 ...}
    int parameterOffset = size / 8 * parameterNumber + 4;
    String[] insns = {
      add(tempReg, sp, parameterOffset),
      load(reg, tempReg)};
    return domainState.eval(insns);
  }

  @Override public <D extends RootDomain<D>> D setRegisterAsReturnValue (String reg, D domainState) {
    return domainState.eval(mov(CLIB_RETURN_REGISTER, reg));
  }

  /**
   * Retrieves the return address from the top of the stack. Must thus be called at function entry to infer the right
   * value.
   */
  @Override public RReilAddr getReturnAddress (RootDomain<?> domainState) {
    String sp = getPlatform().getStackPointer();
    RootDomain<?> state = domainState;
    state = (RootDomain<?>) state.eval(load(tempReg, sp));
    Range returnAddress = getValueOf(tempReg, state);
    if (!returnAddress.isConstant())
      throw new UnsupportedOperationException("Non-constant return address.");
    return RReilAddr.valueOf(returnAddress.getConstantOrNull());
  }

  @Override public List<String> getSyscallInputRegisterNames () {
    return Arrays.asList(SYSCALL_INPUT_REGISTERS);
  }

  @Override public String getSyscallNrRegisterName () {
    return SYSCALL_NR_REGISTER;
  }

  @Override public String getSyscallReturnRegisterName () {
    return SYSCALL_RET_REGISTER;
  }

}
