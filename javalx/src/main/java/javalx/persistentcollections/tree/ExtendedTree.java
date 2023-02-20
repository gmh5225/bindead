package javalx.persistentcollections.tree;

import javalx.fn.Fn2;
import javalx.persistentcollections.ThreeWaySplit;

/**
 * An immutable tree with set functionality.
 *
 * @param <K> The type of the keys
 * @param <V> The type of the values
 * @param <M> The self type of the tree
 */
interface ExtendedTree<K, V, T extends ExtendedTree<K, V, T>> extends Tree<K, V, T> {
  /**
   * Create a tree containing a mapping if either this or the other domain
   * mapped the key. If both trees mapped the key, the value of the first tree
   * is preserved (left-biased).
   *
   * @param other the other domain
   * @return the new domain mapping keys of the union of both trees
   */
  public T union (T other);

  /**
   * Like #union(T other) but allows the use of a selector function that is
   * applied to those values whose keys occur in both trees. The selector
   * function is not called for values that are represented by the same pointer.
   *
   * @param selector the selector that is applied if values occur in both trees
   * @param other the other tree
   * @return the union of both trees
   */
  public T union (Fn2<V, V, V> selector, T other);

  /**
   * Create a tree containing mappings for keys that occur in both trees. If
   * both trees mapped the key, the value of the first tree is preserved.
   *
   * @param other the other domain
   * @return the intersection of both trees
   */
  public T intersection (T other);

  /**
   * Like #intersection(T other) but allows the use of a selector function that
   * is applied to those values whose keys occur in both trees. The selector
   * function is not called for values that are represented by the same pointer.
   *
   * @param selector the selector that is applied if values occur in both trees
   * @param other the other tree
   * @return the intersection of both trees
   */
  public T intersection (Fn2<V, V, V> selector, T other);

  /**
   * Return a tree in which those keys are removed that occur in other.
   *
   * @param other the other tree
   * @return a tree containing keys to be removed from this tree
   */
  public T difference (T other);

  /**
   * Perform a three way split of this tree with other. Returns a 3-tuple {@code <a, b, c>} where {@code a} are the
   * bindings only in this, {@code c} the bindings only in other, and {@code b} all
   * bindings of this tree whose keys are in this and the other map and where the values
   * of the key-value bindings are not equal. The split is left-biased because the values of the common keys {@code b}
   * are taken from the this map.
   *
   * @param other The other map
   * @return 3-way split of this and other.
   */
  public ThreeWaySplit<T> split (T other);
}
