package javalx.persistentcollections.tree;

import static javalx.data.Option.none;
import static javalx.data.Option.some;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn2;
import javalx.numeric.Bound;
import javalx.numeric.FiniteRange;
import javalx.numeric.Interval;
import javalx.persistentcollections.ThreeWaySplit;
import javalx.xml.XmlPrintable;

import com.jamesmurty.utils.XMLBuilder;

/**
 * FiniteRange-tree using an augmented balanced binary search tree.
 *
 * @param <V>
 */
public abstract class FiniteRangeTree<V>
    implements ExtendedTree<FiniteRange, V, FiniteRangeTree<V>>, XmlPrintable {

  private static final int WEIGHT = 3;
  FiniteRange fst;
  V snd;
  private final int size;
  private final Bound max;
  final FiniteRangeTree<V> left;
  final FiniteRangeTree<V> right;

  private FiniteRangeTree (
      FiniteRange key, V value, int size, Bound maxBoundInSubtree, FiniteRangeTree<V> left, FiniteRangeTree<V> right) {
    this.fst = key;
    this.snd = value;
    this.size = size;
    this.max = maxBoundInSubtree;
    this.left = left;
    this.right = right;
  }

  public OverlappingRanges<V> searchOverlaps (FiniteRange key) {
    return searchOverlaps(key.toInterval());
  }

  /* == Special Interval-tree operations == */
  /**
   * For all nodes i in the Interval tree, return i where <i>not<i> ((key.high &lt; i.low) or (key.low &gt; i.high)).
   * That is, all nodes that partially or totally overlap the Interval [key.low, key.high].
   *
   * @param key The FiniteRange used as query-key.
   * @return All overlapping FiniteRanges.
   */
  public OverlappingRanges<V> searchOverlaps (Interval key) {
    OverlappingRanges<V> overlapping = new OverlappingRanges<V>();
    searchOverlaps(key, overlapping);
    return overlapping;
  }

  private void searchOverlaps (Interval key, OverlappingRanges<V> overlapping) {
    if (!isEmpty()) {
      if (this.fst.overlaps(key))
        overlapping.push(fst, snd);
      if (!left.isEmpty() && key.low().isLessThanOrEqualTo(left.max))
        left.searchOverlaps(key, overlapping);
      if (!right.isEmpty() && key.low().isLessThanOrEqualTo(right.max))
        right.searchOverlaps(key, overlapping);
    }
  }

  /* == Default avl-tree operations == */
  private static boolean lessThan (FiniteRange leftKey, FiniteRange rightKey) {
    return leftKey.compareTo(rightKey) < 0;
  }

  public static <V> FiniteRangeTree<V> empty () {
    return new Node<V>();
  }

  @Override public final Option<V> get (FiniteRange key) {
    if (isEmpty())
      return none();

    if (lessThan(key, this.fst))
      return left.get(key);
    else if (lessThan(this.fst, key))
      return right.get(key);
    else
      return some(snd);
  }

  @Override public final V getOrNull (FiniteRange key) {
    if (isEmpty())
      return null;

    if (lessThan(key, this.fst))
      return left.getOrNull(key);
    else if (lessThan(this.fst, key))
      return right.getOrNull(key);
    else
      return snd;
  }


  @Override public final FiniteRangeTree<V> bind (FiniteRange key, V value) {
    if (isEmpty())
      return node(key, value, 1, this, this);

    if (lessThan(key, this.fst))
      return balance(this.fst, this.snd, left.bind(key, value), right);
    else if (lessThan(this.fst, key))
      return balance(this.fst, this.snd, left, right.bind(key, value));
    else
      return node(key, value, size, left, right);
  }

  @Override public final FiniteRangeTree<V> remove (FiniteRange key) {
    if (isEmpty())
      return this;

    if (lessThan(key, this.fst))
      return balance(this.fst, snd, left.remove(key), right);
    else if (lessThan(this.fst, key))
      return balance(this.fst, snd, left, right.remove(key));
    else
      return remove(left, right);
  }

  private static <V> FiniteRangeTree<V> remove (FiniteRangeTree<V> left, FiniteRangeTree<V> right) {
    if (left.isEmpty())
      return right;
    else if (right.isEmpty())
      return left;
    else {
      final FiniteRangeTree<V> min = right.min();
      return balance(min.fst, min.snd, left, right.removeMin());
    }
  }

  @Override public FiniteRangeTree<V> removeMin () {
    if (left.isEmpty())
      return right;
    return balance(fst, snd, left.removeMin(), right);
  }

  @Override public Option<P2<FiniteRange, V>> getMin () {
    if (isEmpty())
      return none();
    else {
      final FiniteRangeTree<V> min = min();
      return Option.<P2<FiniteRange, V>>some(P2.<FiniteRange, V>tuple2(min.fst, min.snd));
    }
  }

  private FiniteRangeTree<V> min () {
    if (left.isEmpty())
      return this;
    return left.min();
  }

  @Override public FiniteRangeTree<V> removeMax () {
    if (right.isEmpty())
      return left;
    return balance(fst, snd, left, right.removeMax());
  }

  @Override public Option<P2<FiniteRange, V>> getMax () {
    if (isEmpty())
      return none();
    else {
      final FiniteRangeTree<V> maxNode = max();
      return Option.<P2<FiniteRange, V>>some(P2.<FiniteRange, V>tuple2(maxNode.fst, maxNode.snd));
    }
  }

  private FiniteRangeTree<V> max () {
    if (right.isEmpty())
      return this;
    return right.max();
  }

  private final FiniteRangeTree<V> node (FiniteRange key, V value, int size, FiniteRangeTree<V> left,
      FiniteRangeTree<V> right) {
    return new Node<V>(key, value, size, left, right);
  }

  @Override public final int size () {
    return size;
  }

  private static <V> FiniteRangeTree<V> join (FiniteRange key, V value, FiniteRangeTree<V> left, FiniteRangeTree<V> right) {
    return new Node<V>(key, value, left.size + right.size + 1, left, right);
  }

  private static <V> FiniteRangeTree<V> balance (FiniteRange key, V value, final FiniteRangeTree<V> left,
      final FiniteRangeTree<V> right) {
    if (Math.abs(left.size - right.size) > 1)
      if (right.size >= WEIGHT * left.size)
        if (right.left.size < right.right.size)
          return rotateSingleLeft(key, value, left, right);
        else
          return rotateDoubleLeft(key, value, left, right);
      else if (left.size >= WEIGHT * right.size)
        if (left.right.size < left.left.size)
          return rotateSingleRight(key, value, left, right);
        else
          return rotateDoubleRight(key, value, left, right);
      else
        return new Node<V>(key, value, left.size + right.size + 1, left, right);
    else
      return new Node<V>(key, value, left.size + right.size + 1, left, right);
  }

  private static <V> FiniteRangeTree<V> rotateDoubleLeft (
      final FiniteRange key, final V value, final FiniteRangeTree<V> left, final FiniteRangeTree<V> right) {
    return join(
        right.left.fst,
        right.left.snd,
        join(key, value, left, right.left.left),
        join(right.fst, right.snd, right.left.right, right.right));
  }

  private static <V> FiniteRangeTree<V> rotateDoubleRight (
      final FiniteRange key, final V value, final FiniteRangeTree<V> left, final FiniteRangeTree<V> right) {
    return join(
        left.right.fst,
        left.right.snd,
        join(left.fst, left.snd, left.left, left.right.left),
        join(key, value, left.right.right, right));
  }

  private static <V> FiniteRangeTree<V> rotateSingleLeft (
      final FiniteRange key, final V value, final FiniteRangeTree<V> left, final FiniteRangeTree<V> right) {
    return join(right.fst, right.snd, join(key, value, left, right.left), right.right);
  }

  private static <V> FiniteRangeTree<V> rotateSingleRight (
      final FiniteRange key, final V value, final FiniteRangeTree<V> left, final FiniteRangeTree<V> right) {
    return join(left.fst, left.snd, left.left, join(key, value, left.right, right));
  }

  // TODO: switch to hedgeUnion
  @Override public final FiniteRangeTree<V> union (FiniteRangeTree<V> other) {
    if (isEmpty())
      return other;
    else if (other.isEmpty())
      return this;
    else {
      final FiniteRangeTree<V> lowerSubTree = other.splitTail(fst);
      final FiniteRangeTree<V> upperSubTree = other.splitHead(fst);

      return concat3(fst, snd, left.union(lowerSubTree), right.union(upperSubTree));
    }
  }

  // TODO: switch to hedgeUnion
  @Override public final FiniteRangeTree<V> union (Fn2<V, V, V> selector, FiniteRangeTree<V> other) {
    if (isEmpty())
      return other;
    else if (other.isEmpty())
      return this;
    else {
      final FiniteRangeTree<V> lowerSubTree = other.splitTail(fst);
      final FiniteRangeTree<V> upperSubTree = other.splitHead(fst);

      final Option<V> valueInOther = other.get(fst);
      if (valueInOther.isSome()) {
        final V v = selector.apply(snd, valueInOther.get());
        return concat3(fst, v, left.union(selector, lowerSubTree), right.union(selector, upperSubTree));
      } else
        return concat3(fst, snd, left.union(selector, lowerSubTree), right.union(selector, upperSubTree));
    }
  }

  @Override public final FiniteRangeTree<V> intersection (FiniteRangeTree<V> other) {
    if (isEmpty())
      return this;
    else if (other.isEmpty())
      return other;
    else {
      final FiniteRangeTree<V> lowerSubTree = this.splitTail(other.fst);
      final FiniteRangeTree<V> upperSubTree = this.splitHead(other.fst);

      final Option<V> valueInThis = this.get(other.fst);
      if (valueInThis.isSome())
        return concat3(
            // Be left biased.
            other.fst, valueInThis.get(), lowerSubTree.intersection(other.left), upperSubTree.intersection(other.right));
      else
        return concat(lowerSubTree.intersection(other.left), upperSubTree.intersection(other.right));
    }
  }

  @Override public final FiniteRangeTree<V> intersection (Fn2<V, V, V> selector, FiniteRangeTree<V> other) {
    if (isEmpty())
      return this;
    else if (other.isEmpty())
      return other;
    else {
      final FiniteRangeTree<V> lowerSubTree = this.splitTail(other.fst);
      final FiniteRangeTree<V> upperSubTree = this.splitHead(other.fst);

      final Option<V> valueInThis = this.get(other.fst);
      if (valueInThis.isSome()) {
        final V v = selector.apply(valueInThis.get(), other.snd);
        return concat3(
            other.fst,
            v,
            lowerSubTree.intersection(selector, other.left),
            upperSubTree.intersection(selector, other.right));
      } else
        return concat(lowerSubTree.intersection(selector, other.left), upperSubTree.intersection(selector, other.right));
    }
  }

  @Override public final FiniteRangeTree<V> difference (FiniteRangeTree<V> other) {
    if (isEmpty())
      return this;
    else if (other.isEmpty())
      return this;
    else {
      final FiniteRangeTree<V> lowerSubTree = this.splitTail(other.fst);
      final FiniteRangeTree<V> upperSubTree = this.splitHead(other.fst);

      return concat(lowerSubTree.difference(other.left), upperSubTree.difference(other.right));
    }
  }

  private static <V> FiniteRangeTree<V> concat3 (FiniteRange key, V value, FiniteRangeTree<V> left,
      FiniteRangeTree<V> right) {
    if (left.isEmpty())
      return right.bind(key, value);
    else if (right.isEmpty())
      return left.bind(key, value);
    else if (WEIGHT * left.size < right.size)
      return balance(right.fst, right.snd, concat3(key, value, left, right.left), right.right);
    else if (WEIGHT * right.size < left.size)
      return balance(left.fst, left.snd, left.left, concat3(key, value, left.right, right));
    else
      return join(key, value, left, right);
  }

  private static <V> FiniteRangeTree<V> concat (FiniteRangeTree<V> left, FiniteRangeTree<V> right) {
    if (left.isEmpty())
      return right;
    else if (right.isEmpty())
      return left;
    else if (WEIGHT * left.size < right.size)
      return balance(right.fst, right.snd, concat(left, right.left), right.right);
    else if (WEIGHT * right.size < left.size)
      return balance(left.fst, left.snd, left.left, concat(left.right, right));
    else {
      FiniteRangeTree<V> min = right.min();
      return balance(min.fst, min.snd, left, right.removeMin());
    }
  }

  @Override public final FiniteRangeTree<V> splitHead (FiniteRange key) {
    if (isEmpty())
      return this;

    if (lessThan(this.fst, key))
      return right.splitHead(key);
    else if (lessThan(key, this.fst))
      return concat3(this.fst, this.snd, left.splitHead(key), right);
    else
      return right;
  }

  @Override public final FiniteRangeTree<V> splitTail (FiniteRange key) {
    if (isEmpty())
      return this;

    if (lessThan(key, this.fst))
      return left.splitTail(key);
    else if (lessThan(this.fst, key))
      return concat3(this.fst, this.snd, left, right.splitTail(key));
    else
      return left;
  }

  private final FiniteRangeTree<V> greedyIntersection (FiniteRangeTree<V> other) {
    if (isEmpty())
      return this;
    else if (other.isEmpty())
      return other;
    else {
      final FiniteRangeTree<V> lowerSubTree = this.splitTail(other.fst);
      final FiniteRangeTree<V> upperSubTree = this.splitHead(other.fst);
      final Option<V> valueInThis = this.get(other.fst);
      if (valueInThis.isSome()) {
        final V actualValueInThis = valueInThis.get();
        final V actualValueInOther = other.snd;
        // Filter values; Only non-equal values are kept.
        if (actualValueInThis == actualValueInOther || actualValueInThis.equals(actualValueInOther))
          return concat(lowerSubTree.greedyIntersection(other.left), upperSubTree.greedyIntersection(other.right));
        else
          return concat3(
              // Be left biased.
              other.fst, valueInThis.get(), lowerSubTree.greedyIntersection(other.left),
              upperSubTree.greedyIntersection(other.right));
      } else
        return concat(lowerSubTree.greedyIntersection(other.left), upperSubTree.greedyIntersection(other.right));
    }
  }

  @Override public final ThreeWaySplit<FiniteRangeTree<V>> split (FiniteRangeTree<V> other) {
    FiniteRangeTree<V> inBoth = this.intersection(other);
    FiniteRangeTree<V> inBothWithNonEqualKeys = this.greedyIntersection(other);
    FiniteRangeTree<V> onlyInLeft = this.difference(inBoth);
    FiniteRangeTree<V> onlyInRight = other.difference(inBoth);
    return ThreeWaySplit.<FiniteRangeTree<V>>make(onlyInLeft, inBothWithNonEqualKeys, onlyInRight);
  }

  @Override public String toString () {
    Iterator<P2<FiniteRange, V>> iterator = iterator();
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

  @Override public final Iterator<P2<FiniteRange, V>> iterator () {
    return new FiniteRangeTreeIterator<V>(this);
  }

  private static final class FiniteRangeTreeIterator<V> implements Iterator<P2<FiniteRange, V>> {
    FiniteRangeTree<V> tree;

    private FiniteRangeTreeIterator (FiniteRangeTree<V> tree) {
      this.tree = tree;
    }

    @Override public boolean hasNext () {
      return !tree.isEmpty();
    }

    @Override public P2<FiniteRange, V> next () {
      final FiniteRangeTree<V> min = tree.min();
      tree = tree.removeMin();
      return P2.<FiniteRange, V>tuple2(min.fst, min.snd);
    }

    @Override public void remove () {
      throw new UnsupportedOperationException();
    }
  }

  private static final class Node<V> extends FiniteRangeTree<V> {
    Node (
        FiniteRange key,
        V value,
        int size,
        FiniteRangeTree<V> left,
        FiniteRangeTree<V> right) {
      super(key, value, size, FiniteRangeTree.max(key, left, right), left, right);
    }

    private Node () {
      super(null, null, 0, null, null, null);
    }

  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    XMLBuilder xml = builder.e("FiniteRangeTree");
    for (P2<FiniteRange, V> binding : this) {
      xml = xml.e("Binding").e("Key").t(binding._1().toString()).up();
      xml = xml.e("Value").t(binding._2().toString()).up().up();
    }
    return xml.up();
  }

  @Override public boolean isEmpty () {
    return left == null && right == null;
  }

  public boolean hasOverlaps (FiniteRange rangeInBytes) {
    OverlappingRanges<?> overlappingSegments = searchOverlaps(rangeInBytes.toInterval());
    return !overlappingSegments.isEmpty();
  }

  static Bound max (FiniteRange key, FiniteRangeTree<?> left, FiniteRangeTree<?> right) {
    Bound maxInSubtree = key.high();
    if (!left.isEmpty())
      maxInSubtree = left.max.max(maxInSubtree);
    if (!right.isEmpty())
      maxInSubtree = right.max.max(maxInSubtree);
    return maxInSubtree;
  }

  static void renderAsGml (FiniteRangeTree<?> tree, String filePath) {
    new TreeWriter().renderAsGml(tree, filePath);
  }

  private static final class TreeWriter {
    private void renderAsGml (FiniteRangeTree<?> tree, String filePath) {
      try {
        PrintWriter out = new PrintWriter(new File(filePath));
        renderAsGml(tree, out);
        out.close();
      } catch (FileNotFoundException ex) {
        Logger.getLogger(FiniteRangeTree.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    private void renderAsGml (FiniteRangeTree<?> tree, PrintWriter out) {
      out.println("graph [");
      out.println(" directed 1");
      renderNodes(tree, out);
      out.println("]");
    }

    private void renderNodes (FiniteRangeTree<?> n, PrintWriter out) {
      if (!n.isEmpty()) {
        renderNode(n, out);
        renderNodes(n.left, out);
        renderNodes(n.right, out);
        renderEdge(n, n.left, out);
        renderEdge(n, n.right, out);
      }
    }

    private static void renderNode (FiniteRangeTree<?> n, PrintWriter out) {
      out.println(" node [");
      out.println("  id " + n.hashCode());
      out.println("  label " + "\"" + n.toString() + "\"");
      out.println("  graphics [");
      out.println("   type \"roundrectangle\"");
      out.println("   fill \"#C0C0C0\"");
      out.println("   outline \"#000000\"");
      out.println("  ]");
      out.println(" ]");
    }

    private static void renderEdge (FiniteRangeTree<?> u, FiniteRangeTree<?> v, PrintWriter out) {
      if (v.isEmpty())
        return;
      out.println(" edge [");
      out.println("  source " + u.hashCode());
      out.println("  target " + v.hashCode());
      out.println("  graphics [");
      out.println("   fill \"#000000\"");
      out.println("   targetArrow \"standard\"");
      out.println("  ]");
      out.println(" ]");
    }
  }
}
