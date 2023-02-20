package javalx.persistentcollections;

import java.util.Iterator;

import javalx.data.products.P2;
import javalx.fn.Fn;

/**
 * A wrapper for an iterator that maps a function on each element.
 */
final class IterableWrapper<A, B> implements Iterable<B> {
  private final Iterable<A> iterable;
  private final Fn<A, B> mapper;

  private IterableWrapper (Iterable<A> iterable, Fn<A, B> mapper) {
    this.iterable = iterable;
    this.mapper = mapper;
  }

  @Override public Iterator<B> iterator () {
    final Iterator<A> it = iterable.iterator();
    return new Iterator<B>() {
      @Override public boolean hasNext () {
        return it.hasNext();
      }

      @Override public B next () {
        return mapper.apply(it.next());
      }

      @Override public void remove () {
        it.remove();
      }
    };
  }

  /**
   * Return an iterator over the keys of a map.
   */
  static <K, V> Iterator<K> getKeysIterator (FiniteMap<K, V, ?> map) {
    return new IterableWrapper<P2<K, V>, K>(map, new Fn<P2<K, V>, K>() {
      @Override public K apply (P2<K, V> a) {
        return a._1();
      }
    }).iterator();
  }

  /**
   * Return an iterator over the values of a map.
   */
  static <K, V> Iterator<V> getValuesIterator (FiniteMap<K, V, ?> map) {
    return new IterableWrapper<P2<K, V>, V>(map, new Fn<P2<K, V>, V>() {
      @Override public V apply (P2<K, V> a) {
        return a._2();
      }
    }).iterator();
  }

}
