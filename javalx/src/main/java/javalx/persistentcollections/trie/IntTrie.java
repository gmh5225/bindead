package javalx.persistentcollections.trie;

import static javalx.data.Option.none;
import static javalx.data.Option.some;
import static javalx.data.Unit.unit;
import javalx.data.Option;
import javalx.data.Unit;
import javalx.data.products.P2;
import javalx.fn.Effect;
import javalx.fn.Fn2;

/**
 * Big Endian Patricia Tree.<br>
 * Specialized persistent ordered map structure for integer keys.
 * Based on Okasaki & Gill, "Fast Mergeable Integer Maps".
 *
 * @param <A> The type of the values.
 *
 * TODO: Implement path/key compression
 */
public abstract class IntTrie<A> {
  private final Fn2<P2<Integer, A>, P2<Integer, A>, A> rightBiasedSelector =
    new Fn2<P2<Integer, A>, P2<Integer, A>, A>() {
      @Override public A apply (P2<Integer, A> a, P2<Integer, A> b) {
        return b._2();
      }
    };

  public static <A> IntTrie<A> empty () {
    return new Empty<A>();
  }

  public final IntTrie<A> bind (int key, A value) {
    return bind(rightBiasedSelector, key, value);
  }

  public final IntTrie<A> bind (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, int key, A value) {
    return insert(f, key, value);
  }

  public final boolean contains (int key) {
    return lookup(key).isSome();
  }

  public final Option<A> get (int key) {
    return lookup(key);
  }

  public final IntTrie<A> union (IntTrie<A> other) {
    return merge(rightBiasedSelector, other);
  }

  public final IntTrie<A> remove (int key) {
    return remove(Effect.<P2<Integer, A>>ignoring(), Effect.<Unit>ignoring(), key);
  }

  public final IntTrie<A> remove (Effect<P2<Integer, A>> runIfFound, Effect<Unit> runIfNotFound, int key) {
    return removeTree(runIfFound, runIfNotFound, key);
  }

  public abstract int size ();

  public abstract int height ();

  protected abstract Option<A> lookup (int key);

  protected abstract IntTrie<A> removeTree (
    Effect<P2<Integer, A>> runIfFound, Effect<Unit> runIfNotFound, int key);

  protected abstract IntTrie<A> insert (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, int key, A value);

  protected abstract IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, Branch<A> right);

  protected abstract IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, Leaf<A> right);

  protected final IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, IntTrie<A> right) {
    if (right instanceof Branch)
      return merge(f, (Branch<A>) right);
    else if (right instanceof Leaf)
      return merge(f, (Leaf<A>) right);

    return this;
  }

  public static <A> IntTrie<A> singleton (final int key, final A value) {
    return new Leaf<A>(key, value);
  }

  public abstract boolean isEmpty ();

  /**
   *  Combine two trees with prefixes p1 and p2, where p1 and p2 are known to disagree.
   */
  protected final IntTrie<A> join (int p1, IntTrie<A> t1, int p2, IntTrie<A> t2) {
    final int m = branchMask(p1, p2);
    final int p = mask(p1, m);

    if (zero(p1, m))
      return new Branch<A>(p, m, t1, t2);
    else
      return new Branch<A>(p, m, t2, t1);
  }

  protected IntTrie<A> branch (int prefix, int mask, IntTrie<A> left, IntTrie<A> right) {
    if (right.isEmpty())
      return left;
    else if (left.isEmpty())
      return right;
    else
      return new Branch<A>(prefix, mask, left, right);
  }

  private static final class Empty<A> extends IntTrie<A> {
    @Override
    protected Option<A> lookup (int key) {
      return none();
    }

    @Override
    protected IntTrie<A> insert (Fn2<P2<Integer, A>, P2<Integer, A>, A> _, int key, A value) {
      return new Leaf<A>(key, value);
    }

    @Override
    public int size () {
      return 0;
    }

    @Override
    public int height () {
      return 0;
    }

    @Override
    protected IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, Branch<A> right) {
      return right;
    }

    @Override
    protected IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, Leaf<A> right) {
      return right;
    }

    @Override
    protected IntTrie<A> removeTree (Effect<P2<Integer, A>> runIfFound, Effect<Unit> runIfNotFound, int key) {
      runIfNotFound.observe(unit());
      return this;
    }

    @Override
    public boolean isEmpty () {
      return true;
    }
  }

  private static final class Leaf<A> extends IntTrie<A> {
    final A value;
    final int key;

    public Leaf (int key, A value) {
      this.key = key;
      this.value = value;
    }

    @Override
    protected Option<A> lookup (int key) {
      if (this.key == key)
        return some(value);
      else
        return none();
    }

    @Override
    protected IntTrie<A> removeTree (Effect<P2<Integer, A>> runIfFound, Effect<Unit> runIfNotFound, int key) {
      if (this.key == key) {
        runIfFound.observe(P2.tuple2(this.key, value));
        return empty();
      }
      else {
        runIfNotFound.observe(unit());
        return this;
      }
    }

    @Override
    protected IntTrie<A> insert (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, int key, A value) {
      if (this.key == key)
        return new Leaf<A>(key, f.apply(P2.tuple2(this.key, this.value), P2.tuple2(key, value)));
      else
        return join(key, new Leaf<A>(key, value), this.key, this);
    }

    @Override
    public int size () {
      return 1;
    }

    @Override
    public int height () {
      return 1;
    }

    @Override
    protected IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, Branch<A> right) {
      return right.insert(f.flip(), key, value);
    }

    @Override
    protected IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, Leaf<A> right) {
      return right.insert(f.flip(), key, value);
    }

    @Override
    public boolean isEmpty () {
      return false;
    }
  }

  private static final class Branch<A> extends IntTrie<A> {
    final int largestCommonPrefix;
    final int branchingBit;
    final IntTrie<A> left;
    final IntTrie<A> right;

    public Branch (int largestCommonPrefix, int branchingBit, IntTrie<A> left, IntTrie<A> right) {
      this.largestCommonPrefix = largestCommonPrefix;
      this.branchingBit = branchingBit;
      this.left = left;
      this.right = right;
    }

    @Override
    protected Option<A> lookup (int key) {
      if (zero(key, branchingBit))
        return left.lookup(key);
      else
        return right.lookup(key);
    }

    @Override
    protected IntTrie<A> removeTree (Effect<P2<Integer, A>> runIfFound, Effect<Unit> runIfNotFound, int key) {
      if (match(key, largestCommonPrefix, branchingBit))
        if (zero(key, branchingBit))
          return branch(largestCommonPrefix, branchingBit, left.remove(runIfFound, runIfNotFound, key), right);
        else
          return branch(largestCommonPrefix, branchingBit, left, right.remove(runIfFound, runIfNotFound, key));

      return this;
    }

    @Override
    protected IntTrie<A> insert (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, int key, A value) {
      if (match(key, largestCommonPrefix, branchingBit))
        if (zero(key, branchingBit))
          return new Branch<A>(largestCommonPrefix, branchingBit, left.insert(f, key, value), right);
        else
          return new Branch<A>(largestCommonPrefix, branchingBit, left, right.insert(f, key, value));
      else
        return join(key, new Leaf<A>(key, value), largestCommonPrefix, this);
    }

    @Override
    public int size () {
      return left.size() + right.size();
    }

    @Override
    public int height () {
      return 1 + Math.max(left.height(), right.height());
    }

    @Override
    protected IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, Branch<A> other) {
      if (unsignedLessThan(other.branchingBit, branchingBit))
        if (match(other.largestCommonPrefix, largestCommonPrefix, branchingBit))
          if (zero(other.largestCommonPrefix, branchingBit))
            return new Branch<A>(largestCommonPrefix, branchingBit, left.merge(f, other), right);
          else
            return new Branch<A>(largestCommonPrefix, branchingBit, left, right.merge(f, other));
        else
          return join(largestCommonPrefix, this, other.largestCommonPrefix, other);
      else if (unsignedLessThan(branchingBit, other.branchingBit))
        if (match(largestCommonPrefix, other.largestCommonPrefix, other.branchingBit))
          if (zero(largestCommonPrefix, other.branchingBit))
            return new Branch<A>(other.largestCommonPrefix, other.branchingBit, merge(f, other.left), other.right);
          else
            return new Branch<A>(other.largestCommonPrefix, other.branchingBit, other.left, merge(f, other.right));
        else
          return join(largestCommonPrefix, this, other.largestCommonPrefix, other);
      else if (largestCommonPrefix == other.largestCommonPrefix)
        return new Branch<A>(
          largestCommonPrefix, branchingBit, left.merge(f, other.left), right.merge(f, other.right));
      else
        return join(largestCommonPrefix, this, other.largestCommonPrefix, other);
    }

    @Override
    protected IntTrie<A> merge (Fn2<P2<Integer, A>, P2<Integer, A>, A> f, Leaf<A> right) {
      // Flip arguments of the provided function, i.o. to keep the "conflict" selector order.
      return insert(f, right.key, right.value);
    }

    @Override
    public boolean isEmpty () {
      return false;
    }
  }

  /* == Bit-twiddling for big endian trees == */
  private static int mask (int k, int m) {
    return k & (~(m - 1) ^ m);
  }

  private static boolean match (int k, int p, int m) {
    return mask(k, m) == p;
  }

  private static boolean zero (int k, int m) {
    return (k & m) == 0;
  }

  private static int highestOneBit (int x) {
    int i = x;
    i |= i >> 1;
    i |= i >> 2;
    i |= i >> 4;
    i |= i >> 8;
    i |= i >> 16;
    // for 64bit: i |= (i >> 32);
    return i - (i >>> 1);
  }

  private static int branchMask (int p1, int p2) {
    return highestOneBit(p1 ^ p2);
  }

  private static boolean unsignedLessThan (int i, int j) {
    return i < j ^ i < 0 ^ j < 0;
  }
}
