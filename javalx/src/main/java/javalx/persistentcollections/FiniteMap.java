package javalx.persistentcollections;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn;
import javalx.fn.Fn2;

/**
 * An immutable map.
 *
 * @param <K> The type of the keys
 * @param <V> The type of the values
 * @param <M> The self type of the map
 */
public interface FiniteMap<K, V, M extends FiniteMap<K, V, M>> extends Iterable<P2<K, V>> {

  public int size ();

  public boolean isEmpty ();

  public Option<V> get (K key);

  public boolean contains (K key);

  public M bind (K key, V value);

  public M remove (K key);

  /**
   * Combine this map with another. If both contain the same keys then the values from {@code this} map are taken
   * (left biased operation).
   */
  public M union (M other);

  /**
   * Intersect this map with another and return the common elements. The values for the elements are taken from
   * {@code this} map (left biased operation).
   */
  public M intersection (M right);

  /**
   * Combine this map with another. If both contain the same keys then the given selector function is applied
   * to produce the combined value for the common key.
   */
  public M union (Fn2<V, V, V> selector, M other);

  /**
   * Intersect this map with another and return the common elements. The values for the elements are taken by applying
   * the given selector.
   */
  public M intersection (Fn2<V, V, V> selector, M other);

  /**
   * Remove all the elements from this map that occur in other.
   */
  public M difference (M right);

  /**
   * Perform a three way split of this map with other. Returns a 3-tuple {@code <a, b, c>} where {@code a} are the
   * bindings only in this, {@code c} the bindings only in other, and {@code b} all
   * bindings of this map whose keys are in this and the other map and where the values
   * of the key-value bindings are not equal. The split is left-biased because the values of the common keys {@code b}
   * are taken from the this map.
   *
   * @param other The other map
   * @return 3-way split of this and other.
   */
  public ThreeWaySplit<M> split (M other);

  public String toString (Fn<K, String> keyRenderer);

  public Iterable<K> keys ();

  public Iterable<V> values ();

  @Override public Iterator<P2<K, V>> iterator ();

}
