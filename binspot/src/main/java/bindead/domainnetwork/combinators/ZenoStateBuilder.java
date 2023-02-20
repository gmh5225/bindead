package bindead.domainnetwork.combinators;

import bindead.domainnetwork.interfaces.ZenoDomain;

/**
 * Abstract base class for builders in the zeno domain hierarchy.
 */
public abstract class ZenoStateBuilder {
  private final ZenoChildOp.Sequence childOps = new ZenoChildOp.Sequence();

  /**
   * Apply all outstanding child operations to the given state.
   *
   * @param state The child domain.
   */
  public <D extends ZenoDomain<D>> D applyChildOps (D state) {
    return childOps.apply(state);
  }

  /**
   * Retrieve the sequence of operations on the child domain. This function can be called either to add more
   * operations on the child domain or to actually execute the queued operations on the child domain.
   *
   * @return the current sequence of child operations
   */
  public ZenoChildOp.Sequence getChildOps () {
    return childOps;
  }

  @Override public String toString () {
    return childOps.toString();
  }

}
