package bindead.analyses.systems;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javalx.data.Option;
import rreil.lang.RReil;
import rreil.lang.RReil.Native;
import rreil.lang.RReilAddr;
import bindead.analyses.systems.natives.DefinitionId;
import bindead.analyses.systems.natives.FunctionDefinition;
import bindead.analyses.systems.natives.IDefinitionProvider;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.abi.ABI;
import binparse.Symbol;

public abstract class GenericSystemModel extends SystemModel {
  private final Map<String, INativeHandler> nativesMap = new HashMap<String, INativeHandler>();
  private final Map<RReilAddr, INativeFunctionHandler> functionMap = new HashMap<RReilAddr, INativeFunctionHandler>();

  protected GenericSystemModel (ESystemType type, ABI abi) {
    super(type, abi);
  }


  @Override public <D extends RootDomain<D>> List<Option<FunctionDefinition>> handleNative (Native stmt, D state,
      ProgramPoint ctx) {
    String name = stmt.getName();

    final INativeHandler handler = nativesMap.get(name);
    if (handler != null) {
      try {
        return handler.handleNative(stmt, state, ctx);
      } catch (Exception err) {
        System.err.println("Error while fetching native function:");
        err.printStackTrace();
      }
    }
    // If no handler is registered, or anything else went wrong: continue
    return defaultReaction(stmt, state, ctx);
  }

  protected void registerNativeHandler (String key, INativeHandler handler) {
    nativesMap.put(key, handler);
  }

  /**
   * Meant to be overriden by implementations
   */
  protected <D extends RootDomain<D>> List<Option<FunctionDefinition>> defaultReaction (
      Native stmt, D state,
      ProgramPoint programPoint) {
    return Arrays.asList(Option.<FunctionDefinition>none());
  }


  @Override public <D extends RootDomain<D>> Option<FunctionDefinition> handleNativeFunction (RReilAddr addr,
      RReil stmt,
      D state) {
    INativeFunctionHandler functionHandler = functionMap.get(addr);
    if (functionHandler == null) {
      return defaultReaction(addr, stmt, state);
    }
    return functionHandler.handleNativeFunction(addr, stmt, state);
  }

  protected void registerFunctionHandler (RReilAddr callAddr, INativeFunctionHandler handler) {
    functionMap.put(callAddr, handler);
  }

  /**
   * Meant to be overriden by implementations
   */
  <D extends RootDomain<D>> Option<FunctionDefinition> defaultReaction (RReilAddr addr, RReil stmt, D state) {
    return Option.<FunctionDefinition>none();
  }


  abstract static class ANativeFunctionHandler implements INativeFunctionHandler {
    private final RReilAddr addr;
    private final Symbol symbol;
    protected final DefinitionId defId;

    protected final IDefinitionProvider defProvider;

    ANativeFunctionHandler (RReilAddr addr, Symbol symbol, IDefinitionProvider defProvider) {
      this.addr = addr;
      this.symbol = symbol;
      this.defId = DefinitionId.valueOf(symbol.getName().get());
      this.defProvider = defProvider;
    }

    public RReilAddr getAddr () {
      return addr;
    }

    public Symbol getSymbol () {
      return symbol;
    }

    public DefinitionId getDefinitionId () {
      return defId;
    }
  }
}
