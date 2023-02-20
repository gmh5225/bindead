package javalx.persistentcollections;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn;
import javalx.persistentcollections.tree.AVLTree;

/**
 * An immutable ordered set implementation based on AVL trees.
 *
 * @see AVLTree
 * @param <K> The type of the set elements.
 */
public class AVLSet<K> implements Iterable<K> {
  private final AVLTree<K, K> tree;

  private final Fn<P2<K, K>, K> getSecond = new Fn<P2<K, K>, K>() {
    @Override public K apply (final P2<K, K> p2) {
      return p2._2();
    }
  };

  private final Fn<AVLTree<K, K>, AVLSet<K>> buildSetFromTree =
      new Fn<AVLTree<K, K>, AVLSet<K>>() {
        @Override public AVLSet<K> apply (final AVLTree<K, K> tree) {
          return build(tree);
        }
      };

  private AVLSet (final AVLTree<K, K> tree) {
    this.tree = tree;
  }

  /**
   * Construct an empty set. The elements in the set will be ordered by using the "natural" order of the elements.
   * Natural order means that the elements must implement the {@link java.lang.Comparable} interface.
   *
   * @return An empty set.
   */
  public static <K> AVLSet<K> empty () {
    return new AVLSet<K>(AVLTree.<K, K>empty());
  }

  public static <K> AVLSet<K> singleton (K element) {
    return AVLSet.<K>empty().add(element);
  }

  public static <K> AVLSet<K> fromIterable (Iterable<K> values) {
    AVLSet<K> s = empty();
    for (K v : values)
      s = s.add(v);
    return s;
  }

  private AVLSet<K> build (AVLTree<K, K> tree) {
    return new AVLSet<K>(tree);
  }

  public int size () {
    return tree.size();
  }

  public boolean isEmpty () {
    return tree.isEmpty();
  }

  public boolean contains (K key) {
    return tree.get(key).isSome();
  }

  /**
   * When the order on key induces a partitioning that is not an injective function,
   * it may be interesting to know which representative is stored in the set for a given key.
   */
  public K get (K key) {
    return tree.getOrNull(key);
  }

  public AVLSet<K> add (K key) {
    return build(tree.bind(key, key));
  }

  public AVLSet<K> remove (K key) {
    return build(tree.remove(key));
  }

  public Option<K> getMin () {
    return tree.getMin().fmap(getSecond);
  }

  public Option<K> getMax () {
    return tree.getMax().fmap(getSecond);
  }

  public AVLSet<K> removeMin () {
    return build(tree.removeMin());
  }

  public AVLSet<K> removeMax () {
    return build(tree.removeMax());
  }

  public AVLSet<K> union (AVLSet<K> other) {
    return build(tree.union(other.tree));
  }

  public AVLSet<K> difference (AVLSet<K> right) {
    return build(tree.difference(right.tree));
  }

  public AVLSet<K> intersection (AVLSet<K> right) {
    return build(tree.intersection(right.tree));
  }

  public ThreeWaySplit<AVLSet<K>> split (AVLSet<K> other) {
    return ThreeWaySplit.map(tree.split(other.tree), buildSetFromTree);
  }

  @Override public Iterator<K> iterator () {
    return new Iterator<K>() {
      private final Iterator<P2<K, K>> iterator = tree.iterator();

      @Override public boolean hasNext () {
        return iterator.hasNext();
      }

      @Override public K next () {
        return iterator.next()._2();
      }

      @Override public void remove () {
        iterator().remove();
      }
    };
  }

  @Override public String toString () {
    Iterator<K> iterator = iterator();
    if (!iterator.hasNext())
      return "{}";

    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      K element = iterator.next();
      builder.append(element == this ? "(this Set)" : element);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  /**
   * Needed to be able to compare sets for equality when used as values in collections.<br>
   * NOTE that {@link #hashCode()} is not implemented
   * thus the contract between hashCode and equals is not upheld. The reason is that
   * trees that have the same elements can have a different structure and thus equals is not easy
   * to implement to comply with hashCode. Hence do not use this in collections where {@link #equals(Object)}
   * and {@link #hashCode()} have to be implemented right.
   */
  @SuppressWarnings("unchecked")
  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof AVLSet))
      return false;
    AVLSet<K> other = (AVLSet<K>) obj;
    return difference(other).isEmpty() && other.difference(this).isEmpty();
  }

  /**
   * Applies the given function on all elements of this set.
   */
  public <R> AVLSet<R> map (Fn<K, R> fn) {
    AVLSet<R> result = empty();
    for (K element : this) {
      result = result.add(fn.apply(element));
    }
    return result;
  }

  /**
   * Return a mutable ordered set containing all the elements in this set.
   * This method instantiates a new set each time, so modifying one does not affect
   * any of the others.
   *
   * @return a new mutable set containing all the elements in this set.
   */
  public SortedSet<K> toMutable () {
    SortedSet<K> set = new TreeSet<K>();
    for (K element : this) {
      set.add(element);
    }
    return set;
  }

}
