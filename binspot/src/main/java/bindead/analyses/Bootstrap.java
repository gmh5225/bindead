package bindead.analyses;

import bindead.domainnetwork.interfaces.RootDomain;

/**
 * Implement this interface to bootstrap an analysis by manipulating an initial state.
 * This should be used to set initial values (e.g. for some registers or memory areas)
 * that depend on the context (platform) the analysis is applied in.
 */
public interface Bootstrap {
  public <D extends RootDomain<D>> D bootstrap (D state, long initialPC);
}
