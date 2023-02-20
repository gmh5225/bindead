package bindead.domainnetwork.combinators;

import bindead.domainnetwork.interfaces.FiniteDomain;

/**
 * Abstract base class for builders whose child state is in the finite domain hierarchy.
 */
public abstract class FiniteStateBuilder {
  private final FiniteSequence childOps = new FiniteSequence();

  /**
   * Apply all outstanding child operations to the given state.
   *
   * @param state The child domain.
   */

  public final <D extends FiniteDomain<D>> D applyChildOps (D state) {
    return childOps.apply(state);
  }

  /**
   * Retrieve the sequence of operations on the child domain. This function can be called either to add more
   * operations on the child domain or to actually execute the queued operations on the child domain.
   *
   * @return the current sequence of child operations
   */
  public FiniteSequence getChildOps () {
    return childOps;
  }

  @Override public String toString () {
    return childOps.toString();
  }
}
