package bindead.domains.fields;

import javalx.numeric.FiniteRange;
import javalx.persistentcollections.tree.FiniteRangeTree;

/**
 * A map that stores ranges of region offsets that are marked as
 * clobbered, that is potentially overwritten.
 */
class ClobberedMap {
  private final FiniteRangeTree<Void> tree;

  private ClobberedMap (FiniteRangeTree<Void> tree) {
    this.tree = tree;
  }

  static ClobberedMap empty () {
    return new ClobberedMap(FiniteRangeTree.<Void>empty());
  }

  ClobberedMap union (ClobberedMap other) {
    return new ClobberedMap(tree.union(other.tree));
  }

  /**
   * Marks an offset range clobbered.
   *
   * @param range The range to mark as invalidated
   * @return The updated map
   */
  ClobberedMap markClobbered (FiniteRange range) {
    return new ClobberedMap(tree.bind(range, null));
  }

  /**
   * Returns true if the given offset is not clobbered.
   *
   * @param range A range to test for validity
   * @return
   */
  boolean isClobbered (FiniteRange range) {
    return tree.hasOverlaps(range);
  }

  @Override public String toString () {
    return "ClobberedMap{" + tree + "}";
  }
}
