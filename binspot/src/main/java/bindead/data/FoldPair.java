package bindead.data;

/**
 * A pair of variables used during folding and expanding of abstract states.
 *
 * @author Axel Simon
 */
class FoldPair<V> {
  private final V permanent;
  private final V ephemeral;

  /**
   * Create a new tuple of variables that for folding and expanding.
   *
   * @param permanent the variable that stays in the support set
   * @param ephemeral the variable that is added to or removed from the support set
   */
  FoldPair (V permanent, V ephemeral) {
    assert permanent != null;
    assert ephemeral != null;
    this.permanent = permanent;
    this.ephemeral = ephemeral;
  }

  /**
   * @return the variable that stays in the domain during fold and that is not new after expand
   */
  public V getPermanent () {
    return permanent;
  }

  /**
   * @return the variable that is removed after fold and that is new after expand
   */
  public V getEphemeral () {
    return ephemeral;
  }

  @Override public String toString () {
    return "<" + permanent + ", " + ephemeral + ">";
  }
}
