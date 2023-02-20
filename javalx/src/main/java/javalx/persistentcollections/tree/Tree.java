package javalx.persistentcollections.tree;

import javalx.data.Option;
import javalx.data.products.P2;

/**
 * An immutable tree.
 *
 * @param <K> The type of the keys
 * @param <V> The type of the values
 * @param <M> The self type of the tree
 */
interface Tree<K, V, T extends Tree<K, V, T>> extends Iterable<P2<K, V>> {
  int size ();

  boolean isEmpty ();

  Option<V> get (K key);

  V getOrNull (K key);

  T bind (K key, V value);

  T remove (K key);

  T splitHead (K key);

  T splitTail (K key);

  T removeMin ();

  T removeMax ();

  Option<P2<K, V>> getMin ();

  Option<P2<K, V>> getMax ();
}
