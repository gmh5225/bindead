package bindead.analyses.systems;

import static bindead.environment.abi.X64SysVAbi.EParamType.INTEGER;
import static bindead.environment.abi.X64SysVAbi.EParamType.MEMORY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Range;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Native;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RReilFactory;
import rreil.lang.util.RhsFactory;
import bindead.analyses.systems.natives.DefaultDefinitionProvider;
import bindead.analyses.systems.natives.DefinitionId;
import bindead.analyses.systems.natives.FileSystemLoader;
import bindead.analyses.systems.natives.FunctionDefinition;
import bindead.analyses.systems.natives.IDefinitionProvider;
import bindead.analyses.systems.natives.TemplateArguments;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.abi.ABI;
import bindead.environment.abi.X32SysVAbi;
import bindead.environment.abi.X64SysVAbi;
import bindead.environment.abi.X64SysVAbi.EParamType;
import bindead.environment.abi.X64SysVAbi.FunctionParameterAllocations;
import bindead.environment.abi.X64SysVAbi.FunctionSignature;
import binparse.Binary;
import binparse.Symbol;

public class LinuxX86Model extends GenericSystemModel {
  // Syscalls
  private static final Map<Integer, DefinitionId> SYSCALL_TABLE = new HashMap<Integer, DefinitionId>();
  static {
    SYSCALL_TABLE.put(13, DefinitionId.valueOf("sys_time"));
  }
  /** dec: -128 */
  private static final Byte _0x80 = new Integer(0x80).byteValue();


  private static final String[] CLIB_DEF_PATH = {"natives/linux/generic"};
  private static final String[] SYSCALL_PATH = {"natives/linux/syscalls"};

  private final Binary binary;

  // syscalls
  private final IDefinitionProvider syscallDefProvider;
  private final TemplateArguments syscallArgs;

  // clib
  private final IDefinitionProvider clibDefProvider;
  private static final String[] IN_PARAM_NAMES = {"param0", "param1", "param2", "param3", "param4", "param5", "param6"};
  // private static final String[] OUT_PARAM_NAMES = {"out0", "out1"};


  protected LinuxX86Model (ABI abi, Binary binary) {
    super(ESystemType.LINUX, abi);
    this.binary = binary;

    // Register syscalls handlers
    this.syscallArgs = new TemplateArguments(abi.getSyscallInputRegisterNames(), Arrays.asList(
        abi.getSyscallReturnRegisterName()), getPlatform().defaultArchitectureSize());
    this.syscallDefProvider = new DefaultDefinitionProvider(new FileSystemLoader(SYSCALL_PATH)).cached();

    registerNativeHandler("sysenter", new SysenterHandler());
    registerNativeHandler("syscall", new SyscallHandler());
    registerNativeHandler("int", new IntHandler());


    // Register clib handler
    this.clibDefProvider = new DefaultDefinitionProvider(new FileSystemLoader(CLIB_DEF_PATH)).cached();
    addClibHandler("time", INTEGER, INTEGER);
    addClibHandler("syscall", INTEGER, INTEGER);
  }

  /**
   * @param symbolName
   * @param retType Classification needed for x86-64 function calls
   * @param paramTypes Classification needed for x86-64 function calls
   */
  private void addClibHandler (String symbolName, EParamType retType, EParamType... paramTypes) {
    Option<Symbol> optSymbol = binary.getSymbol(symbolName);
    if (optSymbol.isNone()) {
      return;   // Symbol not in this binary, so no handler needed
    }
    Symbol symbol = optSymbol.get();
    RReilAddr callAddr = RReilAddr.valueOf(symbol.getAddress());

    // Now we need a destinction, as function parameters are handled totally different on amd64 and i386
    if (abi instanceof X64SysVAbi) {
      X64SysVAbi x64Abi = (X64SysVAbi) abi;
      FunctionSignature signature = new FunctionSignature(retType, paramTypes);
      registerFunctionHandler(callAddr, new X64ClibHandler(callAddr, symbol, clibDefProvider, x64Abi, signature));
    } else {
      X32SysVAbi x32Abi = (X32SysVAbi) abi;
      registerFunctionHandler(callAddr, new X32ClibHandler(callAddr, symbol, clibDefProvider, x32Abi, paramTypes.length));
    }
  }

  private <D extends RootDomain<D>> List<Option<FunctionDefinition>> handleSyscall (Native stmt, D state,
      ProgramPoint programPoint) {
    Range sysnrRange = state.queryRange(getPlatform().getRegisterAsVariable(abi.getSyscallNrRegisterName()));
    List<Option<FunctionDefinition>> possibleSyscalls = new LinkedList<Option<FunctionDefinition>>();

    if (!sysnrRange.isFinite()) {
      // Infinite syscall nr!
      return defaultReaction(stmt, state, programPoint);
    }

    Iterator<BigInt> sysnrRangeIt = sysnrRange.iterator();
    while (sysnrRangeIt.hasNext()) {
      BigInt sysnr = sysnrRangeIt.next();
      DefinitionId id = SYSCALL_TABLE.get(sysnr.intValue());
      if (id != null) {
        possibleSyscalls.add(syscallDefProvider.getNativeFunction(id, syscallArgs));
      } else {
        // Unhandled syscall or Invalid syscall nr
        possibleSyscalls.add(Option.<FunctionDefinition>none());
      }
    }

    return possibleSyscalls;
  }


  private class IntHandler implements INativeHandler {
    @Override public <D extends RootDomain<D>> List<Option<FunctionDefinition>> handleNative (Native stmt, D state,
        ProgramPoint programPoint) {
      Rlit op0Val = stmt.getOpnd();
      if (op0Val.getValue().getValue().byteValue() == _0x80) {
        return handleSyscall(stmt, state, programPoint);
      } else {
        return defaultReaction(stmt, state, programPoint);
      }
    }
  }

  private class SysenterHandler implements INativeHandler {
    @Override public <D extends RootDomain<D>> List<Option<FunctionDefinition>> handleNative (Native stmt, D state,
        ProgramPoint programPoint) {
      return handleSyscall(stmt, state, programPoint);
    }
  }

  private class SyscallHandler extends SysenterHandler {
    @Override public <D extends RootDomain<D>> List<Option<FunctionDefinition>> handleNative (Native stmt, D state,
        ProgramPoint programPoint) {
      return handleSyscall(stmt, state, programPoint);
    }
  }


  private static class X32ClibHandler extends ANativeFunctionHandler {
    private static final RReilFactory rreilFactory = new RReilFactory();
    private static final int REGISTER_SIZE = 32;
    private final X32SysVAbi x32Abi;
    private final int paramsSize;

    public X32ClibHandler (RReilAddr addr, Symbol symbol, IDefinitionProvider defProvider, X32SysVAbi x32Abi,
        int paramsSize) {
      super(addr, symbol, defProvider);
      this.x32Abi = x32Abi;
      this.paramsSize = paramsSize;
    }

    @Override public <D extends RootDomain<D>> Option<FunctionDefinition> handleNativeFunction (RReilAddr addr,
        RReil stmt,
        D state) {
      // Create template parameters
      List<String> inputIds = new ArrayList<String>();
      {
        for (int i = 0; i < paramsSize; i++) {
          inputIds.add(IN_PARAM_NAMES[i]);
        }
      }
      String outputId = X32SysVAbi.CLIB_RETURN_REGISTER;
      TemplateArguments args = new TemplateArguments(inputIds, Arrays.asList(outputId), REGISTER_SIZE);

      Option<FunctionDefinition> optFunction = defProvider.getNativeFunction(defId, args);
      if (optFunction.isNone()) {
        return optFunction;
      }

      // Now the tricky part: PROLOG and EPILOG!
      FunctionDefinition function = optFunction.get();

      // Intro: Assign all parameters to local variables
      List<Lhs> localVariables = new ArrayList<Lhs>();
      List<RReil> prolog = new ArrayList<RReil>();
      for (int i = 0; i < paramsSize; i++) {
        String name = IN_PARAM_NAMES[i];
        Lhs newVar = new Lhs(REGISTER_SIZE, 0, MemVar.fresh(name));
        Rhs.Rvar rhs = x32Abi.getFunctionParameterAllocation(i, state);
        prolog.add(rreilFactory.assign(null, newVar, rhs));
        localVariables.add(newVar);
      }
      function = function.prepend(prolog);

      // Outro: nix

      return Option.some(function);
    }
  }


  private static class X64ClibHandler extends ANativeFunctionHandler {
    private static final RReilFactory rreilFactory = new RReilFactory();
    private static final int REGISTER_SIZE = 64;
    private final X64SysVAbi x64Abi;
    private final FunctionSignature funcSignature;

    public X64ClibHandler (RReilAddr addr, Symbol symbol, IDefinitionProvider defProvider, X64SysVAbi amd64Abi,
        FunctionSignature funcSignature) {
      super(addr, symbol, defProvider);
      this.x64Abi = amd64Abi;
      this.funcSignature = funcSignature;
    }

    @Override
    public <D extends RootDomain<D>> Option<FunctionDefinition> handleNativeFunction (RReilAddr addr, RReil stmt,
        D state) {
      FunctionParameterAllocations funcAllocations = x64Abi.getParameterAllocationsFor(state, funcSignature);

      // Create template parameters
      List<String> inputIds = new ArrayList<String>();
      {
        int paramIndex = 0;
        for (P2<Rvar, EParamType> param : funcAllocations.getParams()) {
          String paramName;
          if (param._2() == EParamType.MEMORY) {
            paramName = IN_PARAM_NAMES[paramIndex];
            paramIndex++;
          } else {
            paramName = param._1().getRegionId().getName();
          }
          inputIds.add(paramName);
        }
      }
      String outputId = funcAllocations.getRetVar()._1().getRegionId().getName();
      TemplateArguments args = new TemplateArguments(inputIds, Arrays.asList(outputId), REGISTER_SIZE);

      // Get function
      Option<FunctionDefinition> optFunction = defProvider.getNativeFunction(defId, args);
      if (optFunction.isNone()) {
        return optFunction;
      }


      // Now the tricky part: PROLOG and EPILOG!
      FunctionDefinition function = optFunction.get();

      // Intro: For each parameter that is passed via stack we have to create a function-local variable (and remove it
      // afterwards)
      List<Lhs> localVariables = new ArrayList<Lhs>();
      List<RReil> prolog = new ArrayList<RReil>();
      {
        int paramIndex = 0;
        for (P2<Rvar, EParamType> param : funcAllocations.getParams()) {
          if (param._2() == MEMORY) {
            String name = inputIds.get(paramIndex);
            Lhs newVar = new Lhs(param._1().getSize(), 0, MemVar.fresh(name));
            prolog.add(rreilFactory.assign(null, newVar, param._1()));
            localVariables.add(newVar);
            paramIndex++;
          }
        }
      }
      function = function.prepend(prolog);

      // Outro
      // TODO: Remove local variables!
      // IFF return type is classified as MEMORY: value of %rdi must be copied to %rax
      List<RReil> epilog = new ArrayList<RReil>();
      {
        Rhs rhs =
          RhsFactory.getInstance().variable(REGISTER_SIZE, 0, MemVar.getVarOrFresh(X64SysVAbi.CLIB_RETURN_REGISTERS_2));
        Lhs lhs = new Lhs(REGISTER_SIZE, 0, MemVar.getVarOrFresh(X64SysVAbi.CLIB_RETURN_REGISTERS_1));
        epilog.add(rreilFactory.assign(null, lhs, rhs));
      }
      function = function.append(epilog);

      return Option.some(function);
    }
  }
}
