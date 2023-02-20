package javalx.persistentcollections.tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import javalx.data.products.P2;
import javalx.numeric.FiniteRange;

public class OverlappingRanges<V> implements Iterable<P2<FiniteRange, V>> {
  private final static Comparator<P2<FiniteRange, ?>> comparator = new Comparator<P2<FiniteRange, ?>>() {
    @Override public int compare (P2<FiniteRange, ?> a, P2<FiniteRange, ?> b) {
      return a._1().compareTo(b._1());
    }
  };

  final private LinkedList<P2<FiniteRange, V>> overlaps;

  public OverlappingRanges () {
    overlaps = new LinkedList<P2<FiniteRange, V>>();
  }

  public void sortByFiniteRangeKey () {
    Collections.sort(overlaps, comparator);
  }

  void push (FiniteRange k, V v) {
    overlaps.push(new P2<FiniteRange, V>(k, v));
  }

  @Override public String toString () {
    Iterator<P2<FiniteRange, V>> iterator = overlaps.iterator();
    if (!iterator.hasNext())
      return "{}";
    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<FiniteRange, V> element = iterator.next();
      FiniteRange key = element._1();
      V value = element._2();
      builder.append(key.toString());
      builder.append('=');
      builder.append(value == this ? "(this Map)" : value);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  public int size () {
    return overlaps.size();
  }

  public boolean isEmpty () {
    return overlaps.isEmpty();
  }

  public P2<FiniteRange, V> getFirst () {
    return overlaps.getFirst();
  }

  @Override public Iterator<P2<FiniteRange, V>> iterator () {
    return overlaps.iterator();
  }

  public void add (FiniteRange of, V v) {
    overlaps.add(new P2<FiniteRange, V>(of, v));
  }

}