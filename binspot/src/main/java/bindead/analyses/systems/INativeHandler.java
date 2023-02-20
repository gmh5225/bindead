package bindead.analyses.systems;

import java.util.List;

import javalx.data.Option;
import rreil.lang.RReil.Native;
import bindead.analyses.systems.natives.FunctionDefinition;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;

public interface INativeHandler {
  /**
   * Actually handles the given {@link Native}
   *
   * @return The resulting flows.
   */
  public abstract <D extends RootDomain<D>> List<Option<FunctionDefinition>> handleNative (Native stmt, D state,
      ProgramPoint programPoint);
}
