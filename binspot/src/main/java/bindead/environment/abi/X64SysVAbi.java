package bindead.environment.abi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import javalx.data.products.P2;
import javalx.numeric.Range;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rvar;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.platform.Platform;

public class X64SysVAbi extends ABI {
  private static final String tempReg = "tmpReg_" + X64SysVAbi.class.getSimpleName();
  private static final String[] CLIB_PARAM_REGISTERS = {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
  public static final String CLIB_RETURN_REGISTERS_1 = "rax";
  public static final String CLIB_RETURN_REGISTERS_2 = "rdi";

  private static final String[] SYSCALL_INPUT_REGISTERS = {"rdi", "rsi", "rdx", "r10", "r8", "r9"};
  private static final String SYSCALL_RET_REGISTER = "rax";
  private static final String SYSCALL_NR_REGISTER = "rax";


  public X64SysVAbi (Platform platform) {
    super(platform);
  }

  public FunctionParameterAllocations getParameterAllocationsFor (RootDomain<?> state, EParamType retType,
      EParamType... paramTypes) {
    return getParameterAllocationsFor(state, new FunctionSignature(retType, paramTypes));
  }

  /**
   * Returns the parameter allocations following System V ABI amd64 "3.2.3 Parameter Passing"
   *
   * @see EParamType
   * @param state
   * @param signature
   * @return
   */
  public FunctionParameterAllocations getParameterAllocationsFor (RootDomain<?> state, FunctionSignature signature) {
    Platform platform = getPlatform();
    // Handle return value
    EParamType retType = signature.getReturnType();
    P2<Rvar, EParamType> retVar = null;
    switch (retType) {
    case INTEGER:
      retVar = P2.tuple2(platform.getRegisterAsVariable(CLIB_RETURN_REGISTERS_1), retType);
      break;

    case MEMORY:
      retVar = P2.tuple2(platform.getRegisterAsVariable(CLIB_RETURN_REGISTERS_2), retType);     // needs to be written to %rax in EPILOGUE!?
      break;

    default:
      throw new UnsupportedOperationException("Only parameter types INTEGER and MEMORY are allowed, yet!");
    }

    // Parameter
    int stackParamIndex = 0;
    int registerParamIndex = 0;
    List<P2<Rvar, EParamType>> params = new ArrayList<P2<Rvar, EParamType>>();
    for (EParamType param : signature.getParamTypes()) {
      switch (param) {
      case INTEGER:
        params.add(P2.tuple2(getFunctionParameterRegisterAllocation(registerParamIndex), param));
        registerParamIndex++;
        break;

      case MEMORY:
        params.add(P2.tuple2(getFunctionParameterStackAllocation(stackParamIndex, state), param));
        stackParamIndex++;
        break;

      default:
        throw new UnsupportedOperationException("Only parameter types INTEGER and MEMORY are allowed, yet!");
      }
    }

    return new FunctionParameterAllocations(retVar, params);
  }

  @Override public Rvar getReturnParameterAllocation () {
    return getPlatform().getRegisterAsVariable("rax");
  }

  private Rvar getFunctionParameterRegisterAllocation (int parameterNumber) {
    String parameterID = getFunctionParameterRegister(parameterNumber);
    return getPlatform().getRegisterAsVariable(parameterID);
  }

  private static String getFunctionParameterRegister (int parameterNumber) {
    if (parameterNumber > CLIB_PARAM_REGISTERS.length - 1)
      throw new IllegalArgumentException("Only the first 6 parameters are supported.");
    String parameterID = CLIB_PARAM_REGISTERS[parameterNumber];
    return parameterID;
  }

  @Override public Range getFunctionParameterValue (int parameterNumber, RootDomain<?> state) {
    return state.queryRange(getFunctionParameterRegisterAllocation(parameterNumber));
  }

  public Range getStackParameterValue (int parameterNumber, RootDomain<?> domainState) {
    String sp = getPlatform().getStackPointer();
    int size = getPlatform().defaultArchitectureSize();
    // SysV ABI amd64 says that the parameters are on the stack at ebp/esp + {16, 24, 32, 40 ...}
    // the initial offset 16 is because of the space reserved for the return address and frame pointer saving
    // as the frame pointer saving is done in the callee prolog and thus not done at function entry point the parameter
    // offset are really ebp/esp + {8, 16, 24, 32 ...}
    int parameterOffset = size / 8 * parameterNumber + 8;
    String[] insns = {
      add(tempReg, sp, parameterOffset),
      load(tempReg, tempReg)};
    RootDomain<?> state = (RootDomain<?>) domainState.eval(insns);
    Range value = getValueOf(tempReg, state);
    return value;
  }


  @Override public <D extends RootDomain<D>> D assignFunctionParameterToRegister (int parameterNumber, String reg,
      D domainState) {
    String parameterReg = getFunctionParameterRegister(parameterNumber);
    return domainState.eval(mov(reg, parameterReg));
  }

  @Override public <D extends RootDomain<D>> D setRegisterAsReturnValue (String reg, D domainState) {
    return domainState.eval(mov(CLIB_RETURN_REGISTERS_1, reg));
  }

  private Rvar getFunctionParameterStackAllocation (int parameterNumber, RootDomain<?> state) {
    Platform platform = getPlatform();
    Range stackOffset = getCurrentStackOffset(state);
    if (!stackOffset.isConstant())
      throw new UnsupportedOperationException("Non-constant stack offset.");
    // SysV ABI amd64 says that the parameters are on the stack at ebp/esp + {16, 24, 32, 40 ...}
    // the initial offset 16 is because of the space reserved for the return address and frame pointer saving
    // as the frame pointer saving is done in the callee prolog and thus not done at function entry point the parameter
    // offset are really ebp/esp + {8, 16, 24, 32 ...}
    int parameterOffset =
      (stackOffset.getConstantOrNull().intValue() + platform.defaultArchitectureSize() / 8 * parameterNumber + 8) * 8;
    return new Rvar(platform.defaultArchitectureSize(), parameterOffset, platform.getStackRegion());
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


  public static class FunctionSignature {
    private final EParamType returnType;
    private final List<EParamType> paramTypes;

    public FunctionSignature (EParamType retType, EParamType... paramTypes) {
      this.returnType = retType;
      this.paramTypes = Arrays.asList(paramTypes);
    }

    public List<EParamType> getParamTypes () {
      return paramTypes;
    }

    public EParamType getReturnType () {
      return returnType;
    }
  }

  public static class FunctionParameterAllocations {
    private final P2<Rvar, EParamType> retVar;
    private final List<P2<Rvar, EParamType>> params;

    public FunctionParameterAllocations (P2<Rvar, EParamType> retVar, List<P2<Rvar, EParamType>> params) {
      this.retVar = retVar;
      this.params = params;
    }

    public P2<Rvar, EParamType> getRetVar () {
      return retVar;
    }

    public List<P2<Rvar, EParamType>> getParams () {
      return params;
    }
  }


  /**
   * Different types of parameters defined in amd64 abi.
   * See ABI Sys V amd64, "3.2.3 Parameter Passing" for classification!
   */
  public enum EParamType {
    /**
     * Arguments of types (signed and unsigned) _Bool, char, short, int,
     * long, long long, and pointers are in the INTEGER class.
     */
    INTEGER,
    /**
     * Arguments of types float, double, _Decimal32, _Decimal64 and
     * __m64 are in class SSE
     */
    SSE,
    /**
     * Arguments of types __float128, _Decimal128 and __m128 are split
     * into two halves. The least significant ones belong to class SSE, the most
     * significant one to class SSEUP
     */
    SSEUP,
    /**
     * Arguments of type __m256 are split into four eightbyte chunks. The least
     * significant one belongs to class SSE and all the others to class SSEUP
     */
    X87,
    /**
     * The 64-bit mantissa of arguments of type long double belongs to class
     * X87, the 16-bit exponent plus 6 bytes of padding belongs to class X87UP
     */
    X87UP,
    /** @see abi */
    COMPLEX_X87,
    MEMORY;
  }
}
