package javalx.persistentcollections;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javalx.data.Option;
import javalx.data.products.P2;

/**
 * A bidirectional immutable map based on AVLMaps. The map ensures that the mapping between a key and a value
 * and vice versa is unique.
 *
 * @see AVLMap
 * @param <K> The type of the keys
 * @param <V> The type of the values
 *
 * TODO bm: makes this inherit from the FiniteMap or OrderedMap interface. Add the missing methods implementation.
 *
 * @author Bogdan Mihaila
 */
public class BiMap<K, V> implements Iterable<P2<K, V>> {
  private final AVLMap<K, V> forward;
  private final AVLMap<V, K> backward;

  private BiMap (AVLMap<K, V> forward, AVLMap<V, K> backward) {
    this.forward = forward;
    this.backward = backward;
  }

  private BiMap<K, V> build (AVLMap<K, V> newForward, AVLMap<V, K> newBackward) {
    return new BiMap<K, V>(newForward, newBackward);
  }

  public static <K, V> BiMap<K, V> empty () {
    return new BiMap<K, V>(AVLMap.<K, V>empty(), AVLMap.<V, K>empty());
  }

  /**
   * Return a bidirectional map from a normal map.<br>
   * Be careful that if there exists a value more than once in the map then it depends on the
   * iteration order which value will be used a the only key. Thus use this only
   * if you know that the values are unique.
   * @param map A map to be converted to a bidirectional map
   * @return A bidirectional map from the given mappings
   */
  public static <K, V> BiMap<K, V> from (Map<K, V> map) {
    BiMap<K, V> newMap = empty();
    for (Entry<K, V> entry : map.entrySet()) {
      newMap = newMap.bind(entry.getKey(), entry.getValue());
    }
    return newMap;
  }

  /**
   * @return A bidirectional map of the reverse mappings values -> keys.
   */
  public BiMap<V, K> inverted () {
    return new BiMap<V, K>(backward, forward);
  }

  public BiMap<K, V> remove (K key) {
    if (!forward.contains(key))
      return this;
    V value = forward.get(key).get();
    assert backward.contains(value);
    return remove(key, value);
  }

  public BiMap<K, V> removeValue (V value) {
    if (!backward.contains(value))
      return this;
    K key = backward.get(value).get();
    assert forward.contains(key);
    return remove(key, value);
  }

  private BiMap<K, V> remove (K key, V value) {
    AVLMap<K, V> newForward = forward.remove(key);
    AVLMap<V, K> newBackward = backward.remove(value);
    return build(newForward, newBackward);
  }

  /**
   * Add a new mapping {@code key <-> value} to this map. If this map already contains the key or the value this
   * operation will remove the previous mappings for them.
   * @return The new updated map.
   */
  public BiMap<K, V> bind (K key, V value) {
    BiMap<K, V> newMap = this;
    newMap = newMap.remove(key);
    newMap = newMap.removeValue(value);
    AVLMap<K, V> newForward = newMap.forward.bind(key, value);
    AVLMap<V, K> newBackward = newMap.backward.bind(value, key);
    return build(newForward, newBackward);
  }

  public Option<V> get (K key) {
    return forward.get(key);
  }

  public Option<K> getKey (V value) {
    return backward.get(value);
  }

  public boolean contains (K key) {
    return forward.contains(key);
  }

  public boolean containsValue (V value) {
    return backward.contains(value);
  }

  public Iterable<K> keys () {
    return forward.keys();
  }

  public Iterable<V> values () {
    return backward.keys();
  }

  public int size () {
    return forward.size();
  }

  public boolean isEmpty () {
    return forward.isEmpty();
  }

  @Override public Iterator<P2<K, V>> iterator () {
    return forward.iterator();
  }

  @Override public String toString () {
    return forward.toString();
  }
}