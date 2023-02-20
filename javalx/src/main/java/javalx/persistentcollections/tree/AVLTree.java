package javalx.persistentcollections.tree;

import static javalx.data.Option.none;
import static javalx.data.Option.some;

import java.util.Iterator;
import java.util.Stack;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn;
import javalx.fn.Fn2;
import javalx.persistentcollections.ThreeWaySplit;

/**
 * Balanced Binary Search Trees.
 *
 * See Stephen Adams, "Efficient sets: a balancing act", Journal of Functional Programming 3(4):553-562, October 1993,
 * <a href="http://www.swiss.ai.mit.edu/~adams/BB/">homepage</a>.
 *
 * @param <K> The type of the keys
 * @param <V> The type of the values
 */
public final class AVLTree<K, V> implements ExtendedTree<K, V, AVLTree<K, V>> {
  private static final int WEIGHT = 3;
  private final AVLTree<K, V> left;
  private final AVLTree<K, V> right;
  private final Entry<K, V> entry;
  private final int size;

  /**
   * Construct an empty tree. The keys in the tree will be ordered by using the "natural" order of the keys. Natural oder
   * means that the keys must implement the {@link java.lang.Comparable} interface.
   *
   * @return An empty tree.
   */
  public static <K, V> AVLTree<K, V> empty () {
    return new AVLTree<K, V>(null, 0, null, null);
  }

  private static <K, V> AVLTree<K, V> node (Entry<K, V> entry, AVLTree<K, V> left, AVLTree<K, V> right) {
    return new AVLTree<K, V>(entry, left.size + right.size + 1, left, right);
  }

  private AVLTree (Entry<K, V> entry, int size, AVLTree<K, V> left, AVLTree<K, V> right) {
    this.left = left;
    this.right = right;
    this.entry = entry;
    this.size = size;
  }

  @Override public final Option<V> get (K key) {
    if (isEmpty())
      return none();
    @SuppressWarnings("unchecked")
    int compareTo = ((Comparable<K>) key).compareTo(entry.getKey());
    if (compareTo < 0)
      return left.get(key);
    else if (compareTo > 0)
      return right.get(key);
    else
      return some(entry.getValue());
  }

  @Override public final V getOrNull (K key) {
    if (isEmpty())
      return null;
    @SuppressWarnings("unchecked")
    int compareTo = ((Comparable<K>) key).compareTo(entry.getKey());
    if (compareTo < 0)
      return left.getOrNull(key);
    else if (compareTo > 0)
      return right.getOrNull(key);
    else
      return entry.getValue();
  }

  @Override public final AVLTree<K, V> bind (K key, V value) {
    assert value != null;
    return bind(new Entry<K, V>(key, value));
  }

  private final AVLTree<K, V> bind (Entry<K, V> e) {
    @SuppressWarnings("unchecked")
    Comparable<K> eComparable = (Comparable<K>) e.getKey();
    if (isEmpty()) {
      return node(e, this, this);
    }
    int compareTo = eComparable.compareTo(entry.getKey());
    if (compareTo < 0)
      return balance(this.entry, left.bind(e), right);
    else if (compareTo > 0)
      return balance(this.entry, left, right.bind(e));
    else
      return node(e, left, right);
  }

  @Override public final AVLTree<K, V> remove (K key) {
    if (isEmpty())
      return this;
    @SuppressWarnings("unchecked")
    int compareTo = ((Comparable<K>) key).compareTo(entry.getKey());
    if (compareTo < 0)
      return balance(entry, left.remove(key), right);
    else if (compareTo > 0)
      return balance(entry, left, right.remove(key));
    else
      return remove(left, right);
  }

  private static <K, V> AVLTree<K, V> remove (AVLTree<K, V> left, AVLTree<K, V> right) {
    if (left.isEmpty())
      return right;
    else if (right.isEmpty())
      return left;
    else {
      final AVLTree<K, V> min = right.min();
      return balance(min.entry, left, right.removeMin());
    }
  }

  @Override public AVLTree<K, V> removeMin () {
    if (left.isEmpty())
      return right;
    return balance(entry, left.removeMin(), right);
  }

  public AVLTree<K, V> removeMinUnbalanced () {
    if (left.isEmpty())
      return right;
    return node(entry, left.removeMin(), right);
  }

  @Override public Option<P2<K, V>> getMin () {
    if (isEmpty())
      return none();
    else {
      final AVLTree<K, V> min = min();
      return Option.<P2<K, V>>some(min.entry);
    }
  }

  private AVLTree<K, V> min () {
    if (left.isEmpty())
      return this;
    return left.min();
  }

  @Override public AVLTree<K, V> removeMax () {
    if (right.isEmpty())
      return left;
    return balance(entry, left, right.removeMax());
  }

  @Override public Option<P2<K, V>> getMax () {
    if (isEmpty())
      return none();
    else {
      final AVLTree<K, V> max = max();
      return Option.<P2<K, V>>some(max.entry);
    }
  }

  private AVLTree<K, V> max () {
    if (right.isEmpty())
      return this;
    return right.max();
  }

  @Override public final int size () {
    return size;
  }

  private static <K, V> AVLTree<K, V> join (Entry<K, V> e, AVLTree<K, V> left, AVLTree<K, V> right) {
    return node(e, left, right);
  }

  private static <K, V> AVLTree<K, V> balance (Entry<K, V> e, final AVLTree<K, V> left, final AVLTree<K, V> right) {
    if (Math.abs(left.size - right.size) > 1)
      if (right.size >= WEIGHT * left.size)
        if (right.left.size < right.right.size)
          return rotateSingleLeft(e, left, right);
        else
          return rotateDoubleLeft(e, left, right);
      else if (left.size >= WEIGHT * right.size)
        if (left.right.size < left.left.size)
          return rotateSingleRight(e, left, right);
        else
          return rotateDoubleRight(e, left, right);
      else
        return node(e, left, right);
    else
      return node(e, left, right);
  }

  private static <K, V> AVLTree<K, V> rotateDoubleLeft (
      Entry<K, V> e, final AVLTree<K, V> left, final AVLTree<K, V> right) {
    return join(
        right.left.entry,
        join(e, left, right.left.left),
        join(right.entry, right.left.right, right.right));
  }

  private static <K, V> AVLTree<K, V> rotateDoubleRight (Entry<K, V> e,
      final AVLTree<K, V> left, final AVLTree<K, V> right) {
    return join(
        left.right.entry,
        join(left.entry, left.left, left.right.left),
        join(e, left.right.right, right));
  }

  private static <K, V> AVLTree<K, V> rotateSingleLeft (Entry<K, V> e, final AVLTree<K, V> left,
      final AVLTree<K, V> right) {
    return join(right.entry, join(e, left, right.left), right.right);
  }

  private static <K, V> AVLTree<K, V> rotateSingleRight (Entry<K, V> e,
      final AVLTree<K, V> left, final AVLTree<K, V> right) {
    return join(left.entry, left.left, join(e, left.right, right));
  }

  // TODO: switch to hedgeUnion
  @Override public final AVLTree<K, V> union (AVLTree<K, V> other) {
    if (isEmpty())
      return other;
    else if (other.isEmpty())
      return this;
    else if (this == other)
      return this;
    else {
      final AVLTree<K, V> lowerSubTree = other.splitTail(entry.getKey());
      final AVLTree<K, V> upperSubTree = other.splitHead(entry.getKey());

      return concat3(entry, left.union(lowerSubTree), right.union(upperSubTree));
    }
  }

  // TODO: switch to hedgeUnion
  @Override public final AVLTree<K, V> union (Fn2<V, V, V> selector, AVLTree<K, V> other) {
    if (isEmpty())
      return other;
    else if (other.isEmpty())
      return this;
    else if (this == other)
      return this;
    else {
      final AVLTree<K, V> lowerSubTree = other.splitTail(entry.getKey());
      final AVLTree<K, V> upperSubTree = other.splitHead(entry.getKey());

      final Option<V> valueInOther = other.get(entry.getKey());
      if (valueInOther.isSome()) {
        final V v = selector.apply(entry.getValue(), valueInOther.get());
        Entry<K, V> e = new Entry<K, V>(entry.getKey(), v);
        return concat3(e, left.union(selector, lowerSubTree), right.union(selector, upperSubTree));
      } else
        return concat3(entry, left.union(selector, lowerSubTree), right.union(selector, upperSubTree));
    }
  }

  @Override public final AVLTree<K, V> intersection (AVLTree<K, V> other) {
    if (isEmpty())
      return this;
    else if (other.isEmpty())
      return other;
    else if (this == other)
      return this;
    else {
      K otherKey = other.entry.getKey();
      final AVLTree<K, V> lowerSubTree = this.splitTail(otherKey);
      final AVLTree<K, V> upperSubTree = this.splitHead(otherKey);
      final Option<V> valueInThis = this.get(otherKey);
      if (valueInThis.isSome())
        return concat3( // Be left biased.
            new Entry<K, V>(otherKey, valueInThis.get()),
            lowerSubTree.intersection(other.left),
            upperSubTree.intersection(other.right));
      else
        return concat(lowerSubTree.intersection(other.left), upperSubTree.intersection(other.right));
    }
  }

  @Override public final AVLTree<K, V> intersection (Fn2<V, V, V> selector, AVLTree<K, V> other) {
    if (isEmpty())
      return this;
    else if (other.isEmpty())
      return other;
    else if (this == other)
      return this;
    else {
      final AVLTree<K, V> lowerSubTree = this.splitTail(other.entry.getKey());
      final AVLTree<K, V> upperSubTree = this.splitHead(other.entry.getKey());
      final Option<V> valueInThis = this.get(other.entry.getKey());
      if (valueInThis.isSome()) {
        final V v = selector.apply(valueInThis.get(), other.entry.getValue());
        return concat3(new Entry<K, V>(other.entry.getKey(), v),
            lowerSubTree.intersection(selector, other.left),
            upperSubTree.intersection(selector, other.right));
      } else
        return concat(lowerSubTree.intersection(selector, other.left), upperSubTree.intersection(selector, other.right));
    }
  }

  @Override public final AVLTree<K, V> difference (AVLTree<K, V> other) {
    if (isEmpty())
      return this;
    else if (other.isEmpty())
      return this;
    else if (this == other)
      return AVLTree.<K, V>empty();
    else {
      K otherKey = other.entry.getKey();
      final AVLTree<K, V> lowerSubTree = this.splitTail(otherKey);
      final AVLTree<K, V> upperSubTree = this.splitHead(otherKey);
      return concat(lowerSubTree.difference(other.left), upperSubTree.difference(other.right));
    }
  }

  private final AVLTree<K, V> intersectionWithNonEqualValues (AVLTree<K, V> other) {
    if (isEmpty()) {
      return this;
    } else if (other.isEmpty()) {
      return other;
    } else if (this == other) {
      return AVLTree.<K, V>empty();
    } else {
      K otherKey = other.entry.getKey();
      final AVLTree<K, V> lowerSubTree = this.splitTail(otherKey);
      final AVLTree<K, V> upperSubTree = this.splitHead(otherKey);
      final Option<V> valueInThis = this.get(otherKey);
      if (valueInThis.isSome()) {
        final V actualValueInThis = valueInThis.get();
        final V actualValueInOther = other.entry.getValue();
        // Filter values; Only non-equal values are kept.
        if (actualValueInThis == actualValueInOther || actualValueInThis.equals(actualValueInOther))
          return concat(lowerSubTree.intersectionWithNonEqualValues(other.left),
              upperSubTree.intersectionWithNonEqualValues(other.right));
        else
          return concat3(// Be left biased.
              new Entry<K, V>(otherKey, valueInThis.get()), lowerSubTree.intersectionWithNonEqualValues(other.left),
              upperSubTree.intersectionWithNonEqualValues(other.right));
      } else {
        return concat(lowerSubTree.intersectionWithNonEqualValues(other.left),
            upperSubTree.intersectionWithNonEqualValues(other.right));
      }
    }
  }

  @Override public final ThreeWaySplit<AVLTree<K, V>> split (AVLTree<K, V> other) {
    AVLTree<K, V> inBoth = this.intersection(other);
    AVLTree<K, V> inBothWithNonEqualValues = this.intersectionWithNonEqualValues(other);
    AVLTree<K, V> onlyInLeft = this.difference(inBoth);
    AVLTree<K, V> onlyInRight = other.difference(inBoth);
    return ThreeWaySplit.<AVLTree<K, V>>make(onlyInLeft, inBothWithNonEqualValues, onlyInRight);
  }

  private static <K, V> AVLTree<K, V> concat3 (Entry<K, V> e, AVLTree<K, V> left, AVLTree<K, V> right) {
    if (left.isEmpty())
      return right.bind(e);
    else if (right.isEmpty())
      return left.bind(e);
    else if (WEIGHT * left.size < right.size)
      return balance(right.entry, concat3(e, left, right.left), right.right);
    else if (WEIGHT * right.size < left.size)
      return balance(left.entry, left.left, concat3(e, left.right, right));
    else
      return join(e, left, right);
  }

  private static <K, V> AVLTree<K, V> concat (AVLTree<K, V> left, AVLTree<K, V> right) {
    if (left.isEmpty())
      return right;
    else if (right.isEmpty())
      return left;
    else if (WEIGHT * left.size < right.size)
      return balance(right.entry, concat(left, right.left), right.right);
    else if (WEIGHT * right.size < left.size)
      return balance(left.entry, left.left, concat(left.right, right));
    else {
      AVLTree<K, V> min = right.min();
      return balance(min.entry, left, right.removeMin());
    }
  }

  @Override public final AVLTree<K, V> splitHead (K key) {
    if (isEmpty())
      return this;
    @SuppressWarnings("unchecked")
    int compareTo = ((Comparable<K>) entry.getKey()).compareTo(key);
    if (compareTo < 0)
      return right.splitHead(key);
    else if (compareTo > 0)
      return concat3(entry, left.splitHead(key), right);
    else
      return right;
  }

  @Override public final AVLTree<K, V> splitTail (K key) {
    if (isEmpty())
      return this;
    @SuppressWarnings("unchecked")
    int compareTo = ((Comparable<K>) key).compareTo(entry.getKey());
    if (compareTo < 0)
      return left.splitTail(key);
    else if (compareTo > 0)
      return concat3(entry, left, right.splitTail(key));
    else
      return left;
  }

  @Override public boolean isEmpty () {
    return size == 0;
  }

  /**
   * Applies the given function on the keys of this tree.
   */
  public <R> AVLTree<R, V> mapOnKeys (Fn<K, R> fn) {
    AVLTree<R, V> result = empty();
    for (P2<K, V> pair : this) {
      result = result.bind(fn.apply(pair._1()), pair._2());
    }
    return result;
  }

  /**
   * Applies the given function on the values of this tree.
   */
  public <R> AVLTree<K, R> mapOnValues (Fn<V, R> fn) {
    AVLTree<K, R> result = empty();
    for (P2<K, V> pair : this) {
      result = result.bind(pair._1(), fn.apply(pair._2()));
    }
    return result;
  }

  @Override public final Iterator<P2<K, V>> iterator () {
    return new AVLTreeIterator<K, V>(this);
  }

  /**
   * Needed to be able to compare maps for equality when used as values in collections.<br>
   * NOTE that {@link #hashCode()} is not implemented
   * thus the contact between hashCode and equals is not upheld. The reason is that
   * trees that have the same elements can have a different structure and thus equals is not easy
   * to implement to comply with hashCode. Hence do not use this in collections where {@link #equals(Object)} and
   * {@link #hashCode()} have to be implemented right.
   */
  @SuppressWarnings("unchecked") @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof AVLTree))
      return false;
    AVLTree<K, V> other = (AVLTree<K, V>) obj; // unchecked generics cast, will only be caught in compareTo at runtime
    ThreeWaySplit<AVLTree<K, V>> split = split(other);
    return split.inBothButDiffering().isEmpty() && split.onlyInFirst().isEmpty() && split.onlyInSecond().isEmpty();
  }


  @Override public String toString () {
    if (size == 0)
      return "Empty{}";
    else
      return "Node{key=" + entry.getKey() + '}';
  }

  private static final class AVLTreeIterator<K, V> implements Iterator<P2<K, V>> {
    Stack<AVLTree<K, V>> stack;

    private AVLTreeIterator (AVLTree<K, V> tree) {
      stack = new Stack<AVLTree<K, V>>();
      push(tree);
    }

    private void push (AVLTree<K, V> tree) {
      while (tree.size > 0) {
        stack.push(tree);
        tree = tree.left;
      }
    }

    @Override public boolean hasNext () {
      return !stack.isEmpty();
    }

    @Override public P2<K, V> next () {
      try {
        AVLTree<K, V> curr = stack.pop();
        push(curr.right);
        return curr.entry;
      } catch (java.util.EmptyStackException e) {
        throw new java.util.NoSuchElementException();
      }
    }

    @Override public void remove () {
      throw new UnsupportedOperationException("Remove not supported using AVLTree iterators");
    }
  }

  public static class Entry<K, V> extends P2<K, V> implements java.util.Map.Entry<K, V> {
    public Entry (K a, V b) {
      super(a, b);
    }

    @Override public K getKey () {
      return _1();
    }

    @Override public V getValue () {
      return _2();
    }

    @Override public V setValue (V value) {
      throw new UnsupportedOperationException();
    }

  }
}
