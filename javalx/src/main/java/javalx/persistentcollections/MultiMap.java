package javalx.persistentcollections;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn2;

/**
 * A map based on AVLMaps and AVLSets. The difference to normal maps is that
 * each key can be mapped to a set of values. It can also be viewed as a set
 * of mappings/tuples.
 *
 * @see AVLMap
 * @see AVLSet
 * @param <K> The type of the keys
 * @param <V> The type of the values
 *
 * @author Bogdan Mihaila
 */
public class MultiMap<K, V> implements Iterable<P2<K, V>> {
  private final AVLMap<K, AVLSet<V>> map;
  private final Fn2<AVLSet<V>, AVLSet<V>, AVLSet<V>> setUnion = new Fn2<AVLSet<V>, AVLSet<V>, AVLSet<V>>() {
    @Override public AVLSet<V> apply (AVLSet<V> a, AVLSet<V> b) {
      return a.union(b);
    }
  };
  private final Fn2<AVLSet<V>, AVLSet<V>, AVLSet<V>> setIntersection = new Fn2<AVLSet<V>, AVLSet<V>, AVLSet<V>>() {
    @Override public AVLSet<V> apply (AVLSet<V> a, AVLSet<V> b) {
      return a.intersection(b);
    }
  };

  private MultiMap (AVLMap<K, AVLSet<V>> map) {
    this.map = map;
  }

  private MultiMap<K, V> build (AVLMap<K, AVLSet<V>> map) {
    return new MultiMap<K, V>(map);
  }

  public static <K, V> MultiMap<K, V> empty () {
    return new MultiMap<K, V>(AVLMap.<K, AVLSet<V>>empty());
  }

  public int size () {
    return map.size();
  }

  public boolean isEmpty () {
    return map.isEmpty();
  }

  /**
   * Returns the set of values that are mapped to the key.
   * Note that it returns and empty set if the key does not exist in this map.
   * Note also that modifying the returned set does not modify this map. Use
   * the proper functions to add or remove mappings.
   *
   * @param key A key for which to retrieve the mapped values
   * @return A set of values that are mapped to the key
   */
  public AVLSet<V> get (K key) {
    Option<AVLSet<V>> values = map.get(key);
    if (values.isSome())
      return values.get();
    else
      return AVLSet.<V>empty();
  }

  public boolean contains (K key) {
    return map.contains(key) && !map.get(key).get().isEmpty();
  }

  /**
   * Add a mapping from key to value.
   * Note that all the other mappings for the key will not be removed.
   *
   * @return The updated map.
   */
  public MultiMap<K, V> add (K key, V value) {
    AVLSet<V> values = get(key);
    values = values.add(value);
    if (values.isEmpty())
      return this;
    return build(map.bind(key, values));
  }

  /**
   * Add a set of new values for the given key.
   * Note that all the previous mappings for the key will not be removed.
   *
   * @return The updated map.
   */
  public MultiMap<K, V> add (K key, AVLSet<V> values) {
    AVLSet<V> oldValues = get(key);
    AVLSet<V> newValues = oldValues.union(values);
    if (newValues.isEmpty())
      return this;
    return build(map.bind(key, newValues));
  }

  /**
   * Remove a key and thus all the associated values from this map.
   *
   * @param key The key to remove.
   * @return The updated map.
   */
  public MultiMap<K, V> remove (K key) {
    return build(map.remove(key));
  }

  /**
   * Remove a mapping from key to value from this map.
   * Note that all the other mappings for the key will not be removed.
   *
   * @return The updated map.
   */
  public MultiMap<K, V> remove (K key, V value) {
    AVLSet<V> values = get(key);
    if (!values.contains(value))
      return this;
    values = values.remove(value);
    if (values.isEmpty())
      return remove(key);
    else
      return build(map.bind(key, values));
  }

  /**
   * Replace a key with a new key. Note that if the new key already exists
   * in the map and is thus mapped to some values, the mappings of the old
   * key and the new key will be unioned.
   *
   * @return The updated map.
   */
  public MultiMap<K, V> replaceKey (K oldKey, K newKey) {
    AVLSet<V> values = get(oldKey);
    if (values.isEmpty())
      return this;
    AVLMap<K, AVLSet<V>> newMap = map.remove(oldKey);
    values = values.union(get(newKey));
    newMap = newMap.bind(newKey, values);
    return build(newMap);
  }

  public MultiMap<K, V> replaceValue (K key, V oldValue, V newValue) {
    if (!contains(key))
      return this;
    MultiMap<K, V> newMap = remove(key, oldValue);
    return newMap.add(key, newValue);
  }

  /**
   * Remove all the values associated with the given key and
   * add a new set of new values for the key.
   *
   * @return The updated map.
   */
  public MultiMap<K, V> replaceValues (K key, AVLSet<V> values) {
    if (values.isEmpty())
      return this.remove(key);
    return build(map.bind(key, values));
  }

  /**
   * Union this map with another. The values for common keys are themselves joined together.
   *
   * @return The updated map.
   */
  public MultiMap<K, V> union (MultiMap<K, V> other) {
    return build(map.union(setUnion, other.map));
  }

  /**
   * Intersect this map with another. The values for common keys are themselves intersected.
   *
   * @return The updated map.
   */
  public MultiMap<K, V> intersection (MultiMap<K, V> other) {
    // Note that the set intersection selector can introduce empty sets for a key,
    // thus the map is not canonical and can contain keys that are mapped to empty sets.
    // We need to clean this up here in a post-pass as otherwise the iterator over the map is broken.
    AVLMap<K, AVLSet<V>> intersection = map.intersection(setIntersection, other.map);
    for (P2<K, AVLSet<V>> entry : intersection) {
      if (entry._2().isEmpty())
        intersection = intersection.remove(entry._1());
    }
    return build(intersection);
  }

  /**
   * Remove all the mappings from this map that occur in other.
   * This means that a key is only removed from this if it is mapped to
   * the same values in the other map, e.g. if the mappings k->v1, k->v2 occurs in
   * this and in other only the mapping k->v1, then the result will keep the mapping k->v2.
   *
   * @return The updated map.
   */
  public MultiMap<K, V> difference (MultiMap<K, V> other) {
    ThreeWaySplit<MultiMap<K, V>> split = this.splitWithDifference(other);
    return split.onlyInFirst().union(split.inBothButDiffering());
  }

  /**
   * Perform a three way split of this map with other. Returns a 3-tuple {@code <a, b, c>} where {@code a} are the
   * mappings only in this, {@code c} the mappings only in other, and {@code b} all mappings with differing values but
   * same keys. The split is left-biased because the values of the common keys {@code b} are taken from the this
   * map minus the ones from the other map. That means only values in this map that are not in the other map are in
   * the {@code b} mappings.
   *
   * @param other The other map
   * @return 3-way split of this and other.
   */
  public ThreeWaySplit<MultiMap<K, V>> splitWithDifference (MultiMap<K, V> other) {
    ThreeWaySplit<AVLMap<K, AVLSet<V>>> split = map.split(other.map);
    MultiMap<K, V> onlyInFirst = build(split.onlyInFirst());
    MultiMap<K, V> onlyInSecond = build(split.onlyInSecond());
    // the map split will use the values from the first for the inBothButDiffering mappings
    // as we use sets we want to return the intersection thus only the differing values from the first map
    MultiMap<K, V> inBothButDiffering = empty();
    for (P2<K, AVLSet<V>> tuple : split.inBothButDiffering()) {
      K key = tuple._1();
      AVLSet<V> values = tuple._2().difference(other.map.getOrNull(key));
      inBothButDiffering = inBothButDiffering.add(key, values);
    }
    return ThreeWaySplit.<MultiMap<K, V>>make(onlyInFirst, inBothButDiffering, onlyInSecond);
  }

  /**
   * Perform a three way split of this map with other. Returns a 3-tuple {@code <a, b, c>} where {@code a} are the
   * mappings only in this, {@code c} the mappings only in other, and {@code b} all mappings with differing values but
   * same keys. The values for the common keys is the union of the values in this and other.
   *
   * @param other The other map
   * @return 3-way split of this and other.
   */
  public ThreeWaySplit<MultiMap<K, V>> splitWithUnion (MultiMap<K, V> other) {
    ThreeWaySplit<AVLMap<K, AVLSet<V>>> split = map.split(other.map);
    MultiMap<K, V> onlyInFirst = build(split.onlyInFirst());
    MultiMap<K, V> onlyInSecond = build(split.onlyInSecond());
    // the map split will use the values from the first for the inBothButDiffering mappings
    // as we use sets we want to return the intersection thus only the differing values from the first map
    MultiMap<K, V> inBothButDiffering = empty();
    for (P2<K, AVLSet<V>> tuple : split.inBothButDiffering()) {
      K key = tuple._1();
      AVLSet<V> values = tuple._2().union(other.map.getOrNull(key));
      inBothButDiffering = inBothButDiffering.add(key, values);
    }
    return ThreeWaySplit.<MultiMap<K, V>>make(onlyInFirst, inBothButDiffering, onlyInSecond);
  }

  public Iterable<K> keys () {
    return map.keys();
  }

  /**
   * Iterate the map as tuples of (key -> {value})
   */
  public Iterator<P2<K, AVLSet<V>>> iteratorKeyValueSet () {
    return map.iterator();
  }

  /**
   * Iterate the map as tuples of (key -> value)
   */
  @Override public Iterator<P2<K, V>> iterator () {
    return new Iterator<P2<K, V>>() {
      private final Iterator<P2<K, AVLSet<V>>> mapIterator = map.iterator();
      private Iterator<V> valuesIterator;
      private K currentKey;

      @Override public boolean hasNext () {
        if (mapIterator.hasNext())
          return true;
        if (valuesIterator != null && valuesIterator.hasNext())
          return true;
        return false;
      }

      @Override public P2<K, V> next () {
        if (!hasNext())
          throw new java.util.NoSuchElementException();
        if (valuesIterator == null || !valuesIterator.hasNext()) {
          P2<K, AVLSet<V>> nextElement = mapIterator.next();
          currentKey = nextElement._1();
          AVLSet<V> values = nextElement._2();
          assert !values.isEmpty();
          valuesIterator = values.iterator();
        }
        return P2.tuple2(currentKey, valuesIterator.next());
      }

      @Override public void remove () {
        throw new UnsupportedOperationException("Remove operation not supported with immutable maps.");
      }
    };
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    Iterator<P2<K, AVLSet<V>>> iterator = map.iterator();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<K, AVLSet<V>> element = iterator.next();
      K key = element._1();
      AVLSet<V> values = element._2();
      builder.append(key == this ? "(this Map)" : key);
      builder.append('=');
      builder.append(values);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

}
