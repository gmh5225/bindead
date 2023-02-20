package javalx.mutablecollections;

import static javalx.data.Option.none;
import static javalx.data.Option.some;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn;
import javalx.fn.Predicate;
import javalx.persistentcollections.AVLSet;

/**
 * A batch of utility methods to deal with collections.
 */
public final class CollectionHelpers {
  private CollectionHelpers () {
  }

  public static <T> Iterable<T> toIterable (final Iterator<T> iterator) {
    return new Iterable<T>() {
      @Override public Iterator<T> iterator () {
        return iterator;
      }
    };
  }

  /**
   * Return an iterable for the list that traverses the list in the reverse order.
   */
  public static <T> Iterable<T> reversedIterable (List<T> list) {
    if (list.size() == 1)
      return list;
    final ListIterator<T> iterator = list.listIterator(list.size());
    return new Iterable<T>() {
      @Override public Iterator<T> iterator () {
        return new Iterator<T>() {
          @Override public boolean hasNext () {
            return iterator.hasPrevious();
          }

          @Override public T next () {
            return iterator.previous();
          }

          @Override public void remove () {
            iterator.remove();
          }
        };
      }
    };
  }

  /**
   * Reverses the mappings in a map from keys -> values to values -> keys.<br>
   * Note that if there is a value more than once in the map then it depends on the
   * iteration order which value will be used a the only key. Thus use this only
   * if you know that the values are unique.
   *
   * @param map A map for which the mapping direction should be reversed
   * @return The reversed mappings
   */
  public static <K, V> Map<V, K> reverseMapping (Map<K, V> map) {
    Map<V, K> newMap = new HashMap<V, K>();
    for (Entry<K, V> entry : map.entrySet()) {
      newMap.put(entry.getValue(), entry.getKey());
    }
    return newMap;
  }

  /**
   * Build a list from an array of elements.
   */
  public static <A> List<? extends A> list (A... elements) {
    return Arrays.asList(elements);
  }

  /**
   * Build a set from an array of elements.
   */
  public static <A> Set<? extends A> set (A... elements) {
    return new HashSet<A>(list(elements));
  }

  /**
   * Filter a set by keeping only the elements that satisfy a given predicate.
   *
   * @param set The set to filter.
   * @param predicate A predicate to test an element with.
   * @return The filtered set.
   */
  public static <A> AVLSet<A> filter (AVLSet<A> set, Predicate<A> predicate) {
    AVLSet<A> output = set;
    for (A element : set) {
      if (!predicate.apply(element))
        output = output.remove(element);
    }
    return output;
  }

  /**
   * Filter a set by keeping only the elements that satisfy a given predicate.
   *
   * @param set The set to filter.
   * @param predicate A predicate to test an element with.
   * @return The filtered set.
   */
  public static <A> Set<A> filter (Set<A> set, Predicate<A> predicate) {
    return filter(set, new HashSet<A>(), predicate);
  }

  /**
   * Filter a collection by keeping only the elements that satisfy a given predicate.
   *
   * @param collection The collection to filter.
   * @param predicate A predicate to test an element with.
   * @return The filtered collection.
   */
  public static <A> Collection<A> filter (Collection<A> collection, Predicate<A> predicate) {
    return filter(collection, new ArrayList<A>(), predicate);
  }

  /**
   * Filter a list by keeping only the elements that satisfy a given predicate.
   *
   * @param list The list to filter.
   * @param predicate A predicate to test an element with.
   * @return The filtered list.
   */
  public static <A> List<A> filter (List<A> list, Predicate<A> predicate) {
    return filter(list, new ArrayList<A>(), predicate);
  }

  /**
   * Filter an array by keeping only the elements that satisfy a given predicate. For convenience returns a list
   * instead of an array.
   *
   * @param list The array to filter.
   * @param predicate A predicate to test an element with.
   * @return The filtered array as list.
   */
  public static <A> List<A> filter (A[] list, Predicate<A> predicate) {
    return filter(Arrays.asList(list), new ArrayList<A>(), predicate);
  }

  private static <A, C extends Collection<? super A>> C filter (Collection<A> input, C output, Predicate<A> predicate) {
    for (A element : input) {
      if (predicate.apply(element))
        output.add(element);
    }
    return output;
  }

  /**
   * Apply a function for all elements of a collection.
   *
   * @param collection A collection of elements.
   * @param f A function.
   * @return A collection of the results of the function applied to each element.
   */
  public static <A, B> Collection<B> map (Collection<A> collection, Fn<A, B> f) {
    return map(collection, new ArrayList<B>(collection.size()), f);
  }

  /**
   * Apply a function for all elements of a list.
   *
   * @param list A list of elements.
   * @param f A function.
   * @return A list of the results of the function applied to each element.
   */
  public static <A, B> List<B> map (List<A> list, Fn<A, B> f) {
    return map(list, new ArrayList<B>(list.size()), f);
  }

  /**
   * Apply a function for all elements of a set.
   *
   * @param set A set of elements.
   * @param f A function.
   * @return A set of the results of the function applied to each element.
   */
  public static <A, B> Set<B> map (Set<A> set, Fn<A, B> f) {
    return map(set, new HashSet<B>(set.size()), f);
  }

  public static <A, B> Iterator<B> map (final Iterator<A> iterator, final Fn<A, B> fun) {
    return new Iterator<B>() {
  
      @Override public boolean hasNext () {
        return iterator.hasNext();
      }
  
      @Override public B next () {
        return fun.apply(iterator.next());
      }
  
      @Override public void remove () {
        iterator.remove();
      }
    };
  }

  private static <A, B, C extends Collection<? super B>> C map (Collection<A> input, C output, Fn<A, B> f) {
    for (A element : input) {
      output.add(f.apply(element));
    }
    return output;
  }

  /**
   * Split a list of tuples into two lists.
   *
   * @param list A list of tuples
   * @return A tuple of two lists. The first list contains all the first elements in the tuples and the second all the
   *            second elements in the tuples.
   */
  public static <A, B> P2<List<A>, List<B>> split (List<P2<A, B>> list) {
    List<A> listA = new ArrayList<A>();
    List<B> listB = new ArrayList<B>();
    for (P2<A, B> tuple : list) {
      listA.add(tuple._1());
      listB.add(tuple._2());
    }
    return new P2<List<A>, List<B>>(listA, listB);
  }

  /**
   * Zip two lists into a list of tuples. The method assumes that the lists are of equal length.
   *
   * @param listA The first list
   * @param listB The second list
   * @return A list of tuples where the first tuple elements are from {@code listA} and the second from {@code listB}
   */
  public static <A, B> List<P2<A, B>> zip (List<A> listA, List<B> listB) {
    if (listA.size() != listB.size())
      throw new IllegalArgumentException("The lists are not of equal size.");
    ArrayList<P2<A, B>> list = new ArrayList<P2<A, B>>();
    Iterator<A> iterA = listA.iterator();
    Iterator<B> iterB = listB.iterator();
    while (iterA.hasNext()) {
      P2<A, B> tuple = new P2<A, B>(iterA.next(), iterB.next());
      list.add(tuple);
    }
    return list;
  }

  /**
   * Cast the elements of a list to a different type and return the resulting list.
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> cast (List<?> list) {
    return (List<T>) list;
  }

  /**
   * Test if all elements in a collection satisfy a given predicate.
   *
   * @param collection A collection to test.
   * @param predicate A predicate to test an element with.
   * @return {@code true} if all the elements in the collection satisfy the predicate or {@code false} if not.
   */
  public static <A> boolean all (Collection<A> collection, Predicate<A> predicate) {
    for (A element : collection) {
      if (!predicate.apply(element))
        return false;
    }

    return true;
  }

  /**
   * Find and return the first element in the collection that satisfies the given predicate. The "first element" means
   * that the collection is iterated in its natural order.
   *
   * @param collection A collection to search.
   * @param predicate A predicate to test an element with.
   * @return The first element satisfying the predicate or none if there does not exist one.
   */
  public static <A> Option<A> find (Collection<A> collection, Predicate<A> predicate) {
    for (A elem : collection) {
      if (predicate.apply(elem))
        return some(elem);
    }
    return none();
  }

  /**
   * Test if there exists an element in the given collection that satisfies the predicate.
   *
   * @param collection A collection to search.
   * @param predicate A predicate to test an element with.
   * @return {@code true} if there exists an element in the collection that satisfies the predicate or {@code false} if
   *         there does not exist one.
   */
  public static <A> boolean exists (Collection<A> collection, Predicate<A> predicate) {
    return find(collection, predicate).isSome();
  }
}
