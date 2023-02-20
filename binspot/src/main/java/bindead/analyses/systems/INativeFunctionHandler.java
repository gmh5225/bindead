package bindead.analyses.systems;

import javalx.data.Option;
import rreil.lang.RReil;
import rreil.lang.RReil.Branch;
import rreil.lang.RReilAddr;
import bindead.analyses.systems.natives.FunctionDefinition;
import bindead.domainnetwork.interfaces.RootDomain;

public interface INativeFunctionHandler {
  /**
   * Tries to handle the given {@link Branch}. If no {@link FunctionDefinition} for the target can be found ,
   * {@link Option#none()} is returned!
   *
   * @return The resulting flows.
   */
  public abstract <D extends RootDomain<D>> Option<FunctionDefinition> handleNativeFunction (RReilAddr addr, RReil stmt, D state);
}
