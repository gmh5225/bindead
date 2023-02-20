package javalx.persistentcollections;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn;
import javalx.fn.Fn2;
import javalx.persistentcollections.tree.AVLTree;

/**
 * An immutable ordered map implementation based on AVL trees.
 *
 * @see AVLTree
 * @param <K> The type of the keys
 * @param <V> The type of the values
 */
public class AVLMap<K, V> implements OrderedMap<K, V, AVLMap<K, V>> {
  private final AVLTree<K, V> tree;
  private final Fn<AVLTree<K, V>, AVLMap<K, V>> buildMapFromTree =
    new Fn<AVLTree<K, V>, AVLMap<K, V>>() {
      @Override public AVLMap<K, V> apply (final AVLTree<K, V> tree) {
        return build(tree);
      }
    };

  protected AVLMap (AVLTree<K, V> tree) {
    this.tree = tree;
  }

  private AVLMap<K, V> build (AVLTree<K, V> tree) {
    return new AVLMap<K, V>(tree);
  }

  /**
   * Construct an empty map. The keys in the map will be ordered by using the "natural" order of the keys. Natural oder
   * means that the keys must implement the {@link java.lang.Comparable} interface.
   *
   * @return An empty map.
   */
  public static <K, V> AVLMap<K, V> empty () {
    return new AVLMap<K, V>(AVLTree.<K, V>empty());
  }

  @Override public int size () {
    return tree.size();
  }

  @Override public boolean isEmpty () {
    return tree.isEmpty();
  }

  @Override public boolean contains (K key) {
    return tree.get(key).isSome();
  }

  @Override public Option<V> get (K key) {
    return tree.get(key);
  }

  public V getOrNull (K key) {
    return tree.getOrNull(key);
  }

  @Override public AVLMap<K, V> bind (K key, V value) {
    return build(tree.bind(key, value));
  }

  @Override public AVLMap<K, V> remove (K key) {
    return build(tree.remove(key));
  }

  @Override public AVLMap<K, V> removeMin () {
    return build(tree.removeMin());
  }

  @Override public AVLMap<K, V> removeMax () {
    return build(tree.removeMax());
  }

  @Override public Option<P2<K, V>> getMin () {
    return tree.getMin();
  }

  @Override public Option<P2<K, V>> getMax () {
    return tree.getMax();
  }

  @Override public AVLMap<K, V> union (AVLMap<K, V> other) {
    return build(tree.union(other.tree));
  }

  @Override public AVLMap<K, V> union (Fn2<V, V, V> selector, AVLMap<K, V> other) {
    return build(tree.union(selector, other.tree));
  }

  @Override public AVLMap<K, V> difference (AVLMap<K, V> right) {
    return build(tree.difference(right.tree));
  }

  @Override public AVLMap<K, V> intersection (AVLMap<K, V> right) {
    return build(tree.intersection(right.tree));
  }

  @Override public AVLMap<K, V> intersection (Fn2<V, V, V> selector, AVLMap<K, V> other) {
    return build(tree.intersection(selector, other.tree));
  }

  @Override public Iterator<P2<K, V>> iterator () {
    return tree.iterator();
  }

  @Override public Iterable<K> keys () {
    return new Iterable<K>() {
      @Override public Iterator<K> iterator () {
        return IterableWrapper.getKeysIterator(AVLMap.this);
      }
    };
  }

  @Override public Iterable<V> values () {
    return new Iterable<V>() {
      @Override public Iterator<V> iterator () {
        return IterableWrapper.getValuesIterator(AVLMap.this);
      }
    };
  }

  public AVLSet<V> valueSet () {
    return AVLSet.<V>fromIterable(values());
  }

  @Override public ThreeWaySplit<AVLMap<K, V>> split (AVLMap<K, V> other) {
    return ThreeWaySplit.map(tree.split(other.tree), buildMapFromTree);
  }

  /**
   * Applies the given function on the keys of this map.
   */
  public <R> AVLMap<R, V> mapOnKeys (Fn<K, R> fn) {
    return new AVLMap<R, V>(tree.mapOnKeys(fn));
  }

  /**
   * Applies the given function on the values of this map.
   */
  public <R> AVLMap<K, R> mapOnValues (Fn<V, R> fn) {
    return new AVLMap<K, R>(tree.mapOnValues(fn));
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    Iterator<P2<K, V>> iterator = iterator();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<K, V> element = iterator.next();
      K key = element._1();
      V value = element._2();
      builder.append(key == this ? "(this Map)" : key);
      builder.append('=');
      builder.append(value == this ? "(this Map)" : value);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  @Override public String toString (Fn<K, String> keyRenderer) {
    Iterator<P2<K, V>> iterator = iterator();
    if (!iterator.hasNext())
      return "{}";

    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<K, V> element = iterator.next();
      K key = element._1();
      V value = element._2();
      builder.append(key == this ? "(this Map)" : keyRenderer.apply(key));
      builder.append('=');
      builder.append(value == this ? "(this Map)" : value);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  /**
   * Needed to be able to compare maps for equality when used as values in collections.<br>
   * NOTE that {@link #hashCode()} is not implemented
   * thus the contact between hashCode and equals is not upheld. The reason is that
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
    if (!(obj instanceof AVLTree))
      return false;
    AVLMap<K, V> other = (AVLMap<K, V>) obj; // unchecked generics cast, will only be caught in compareTo at runtime
    ThreeWaySplit<AVLMap<K, V>> split = split(other);
    return split.inBothButDiffering().isEmpty() && split.onlyInFirst().isEmpty() && split.onlyInSecond().isEmpty();
  }

}
