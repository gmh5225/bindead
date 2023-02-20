package javalx.persistentcollections;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn;
import javalx.fn.Fn2;
import javalx.persistentcollections.tree.AVLTree;

/**
 * An immutable map from integers to values.
 *
 * @param <V> The type of the values.
 */
public final class IntMap<V> implements OrderedMap<Integer, V, IntMap<V>> {
  private final AVLTree<Integer, V> tree;
  private final Fn<AVLTree<Integer, V>, IntMap<V>> buildFn = new Fn<AVLTree<Integer, V>, IntMap<V>>() {
    @Override public IntMap<V> apply (AVLTree<Integer, V> tree) {
      return build(tree);
    }
  };

  private IntMap (final AVLTree<Integer, V> tree) {
    this.tree = tree;
  }

  private static <V> IntMap<V> build (AVLTree<Integer, V> tree) {
    return new IntMap<V>(tree);
  }

  @Override public int size () {
    return tree.size();
  }

  @Override public boolean isEmpty () {
    return tree.isEmpty();
  }

  @Override public boolean contains (Integer key) {
    return tree.get(key).isSome();
  }

  public static <V> IntMap<V> empty () {
    return build(AVLTree.<Integer, V>empty());
  }

  @Override public IntMap<V> union (IntMap<V> other) {
    return build(tree.union(other.tree));
  }

  @Override public IntMap<V> union (Fn2<V, V, V> selector, IntMap<V> other) {
    return build(tree.union(selector, other.tree));
  }

  @Override public IntMap<V> difference (IntMap<V> other) {
    return build(tree.difference(other.tree));
  }

  @Override public IntMap<V> intersection (IntMap<V> other) {
    return build(tree.intersection(other.tree));
  }

  @Override public IntMap<V> intersection (Fn2<V, V, V> selector, IntMap<V> other) {
    return build(tree.intersection(selector, other.tree));
  }

  @Override public Option<V> get (Integer key) {
    return tree.get(key);
  }

  @Override public IntMap<V> bind (Integer key, V value) {
    return build(tree.bind(key, value));
  }

  @Override public IntMap<V> remove (Integer key) {
    return build(tree.remove(key));
  }

  @Override public Option<P2<Integer, V>> getMax () {
    return tree.getMax();
  }

  @Override public Option<P2<Integer, V>> getMin () {
    return tree.getMin();
  }

  @Override public IntMap<V> removeMax () {
    return build(tree.removeMax());
  }

  @Override public IntMap<V> removeMin () {
    return build(tree.removeMin());
  }

  @Override public ThreeWaySplit<IntMap<V>> split (IntMap<V> other) {
    return ThreeWaySplit.map(tree.split(other.tree), buildFn);
  }

  @Override public Iterable<Integer> keys () {
    return new Iterable<Integer>() {
      @Override public Iterator<Integer> iterator () {
        return IterableWrapper.getKeysIterator(IntMap.this);
      }
    };
  }

  @Override public Iterable<V> values () {
    return new Iterable<V>() {
      @Override public Iterator<V> iterator () {
        return IterableWrapper.getValuesIterator(IntMap.this);
      }
    };
  }

  @Override public Iterator<P2<Integer, V>> iterator () {
    return tree.iterator();
  }

  @Override public String toString () {
    Iterator<P2<Integer, V>> iterator = iterator();
    if (!iterator.hasNext())
      return "{}";

    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<Integer, V> element = iterator.next();
      Integer key = element._1();
      V value = element._2();
      builder.append(key);
      builder.append('=');
      builder.append(value == this ? "(this Map)" : value);
      if (iterator.hasNext())
        builder.append(", ");
    }

    return builder.append('}').toString();
  }

  @Override public String toString (Fn<Integer, String> keyRenderer) {
    Iterator<P2<Integer, V>> iterator = iterator();
    if (!iterator.hasNext())
      return "{}";

    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<Integer, V> element = iterator.next();
      Integer key = element._1();
      V value = element._2();
      builder.append(keyRenderer.apply(key));
      builder.append('=');
      builder.append(value == this ? "(this Map)" : value);
      if (iterator.hasNext())
        builder.append(", ");
    }

    return builder.append('}').toString();
  }
}
