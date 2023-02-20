package bindead.domains.widening.oldthresholds;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.fn.Fn2;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domains.widening.oldthresholds.Threshold.Origin;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The state for the thresholds widening domain. It contains thresholds that are marked with the locations they have
 * been used for narrowing.
 */
class ThresholdsWideningState extends FunctorState {
  // TODO: replace the map below with the new multimap
  private final AVLMap<Threshold, AVLSet<ProgramPoint>> appliedForNarrowingAt;
  private final AVLMap<Threshold.Origin, Threshold> origins;
  // TODO: add a reverse map from variables to thresholds denoting in which tests they occur
  public static final ThresholdsWideningState EMPTY = new ThresholdsWideningState();

  private ThresholdsWideningState () {
    appliedForNarrowingAt = AVLMap.<Threshold, AVLSet<ProgramPoint>>empty();
    origins = AVLMap.<Threshold.Origin, Threshold>empty();
  }

  private ThresholdsWideningState (MutableState state) {
    assert state.isConsistent();
    appliedForNarrowingAt = state.appliedForNarrowingAt;
    origins = state.origins;
  }

  MutableState mutableCopy () {
    return new MutableState(appliedForNarrowingAt, origins);
  }

  private static XMLBuilder pretty (Test test, XMLBuilder builder) {
    builder = builder.e("Test");
    builder = test.toXML(builder);
    builder = builder.up();
    return builder;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(ThresholdsWidening.NAME);
    for (final P2<Origin, Threshold> tuple : origins) {
      Origin origin = tuple._1();
      Threshold threshold = tuple._2();
      AVLSet<ProgramPoint> locations = appliedForNarrowingAt.get(threshold).get();
      builder = builder.e("Entry").a("type", "Thresholds")
          .e("Value")
          .e("Origin")
          .e("Location")
          .t(origin.getLocation().toString())
          .up(); // Location
      builder = pretty(origin.getTest(), builder);
      builder = builder.up(); // Origin
      builder = pretty(threshold.getTest(), builder);
      builder = builder.e("appliedAt");
      for (ProgramPoint point : locations) {
        builder = builder.e("point").t(point.toString()).up();
      }
      builder = builder.up()//appliedAt
          .up()//value
          .up(); //entry
    }
    return builder.up();
  }

  @Override public String toString () {
    return "#" + appliedForNarrowingAt.size() + " " + contentToString();
  }

  private String contentToString () {
    Iterator<P2<Threshold, AVLSet<ProgramPoint>>> iterator = appliedForNarrowingAt.iterator();
    if (!iterator.hasNext())
      return "{}";
    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<Threshold, AVLSet<ProgramPoint>> element = iterator.next();
      Threshold key = element._1();
      AVLSet<ProgramPoint> value = element._2();
      builder.append(key);
      builder.append(" : ");
      builder.append(value);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  /**
   * Same state data as the parent class but exposes an API to modify the state.
   */
  static class MutableState implements Iterable<Threshold> {
    private AVLMap<Threshold, AVLSet<ProgramPoint>> appliedForNarrowingAt;
    private AVLMap<Threshold.Origin, Threshold> origins;

    private MutableState (AVLMap<Threshold, AVLSet<ProgramPoint>> appliedForNarrowingAt,
        AVLMap<Threshold.Origin, Threshold> origins) {
      this.appliedForNarrowingAt = appliedForNarrowingAt;
      this.origins = origins;
    }

    ThresholdsWideningState immutableCopy () {
      return new ThresholdsWideningState(this);
    }

    /**
     * Checks if the mappings in the internal state are consistent. Use for assertions and debugging only.
     */
    public boolean isConsistent () {
      for (Threshold threshold : this) {
        Origin original = threshold.getOrigin();
        if (!origins.contains(original) || origins.get(original).get().compareTo(threshold) != 0)
          return false;
      }
      return true;
    }

    private MutableState rebuildReverseMapping () {
      origins = AVLMap.<Threshold.Origin, Threshold>empty();
      for (Threshold threshold : this) {
        origins = origins.bind(threshold.getOrigin(), threshold);
      }
      return this;
    }

    private static abstract class Merger<T> extends Fn2<T, T, T> {
      // only used as type alias to save the typing of the generic parameter 3 times
    }

    /**
     * Returns a triple {@code a, b, c} where {@code a} is the set of thresholds only in this and {@code c} is the set of
     * thresholds only in other. {@code b} is the set of thresholds that are in both and their set of narrowing points is
     * the union of both sides.
     */
    public P3<MutableState, MutableState, MutableState> split (MutableState other) {
      P3<AVLMap<Threshold, AVLSet<ProgramPoint>>, AVLMap<Threshold, AVLSet<ProgramPoint>>, AVLMap<Threshold, AVLSet<ProgramPoint>>> narrowingSplit =
        splitWithMergerForDiffering(this.appliedForNarrowingAt, other.appliedForNarrowingAt,
            new Merger<AVLSet<ProgramPoint>>() {
              @Override public AVLSet<ProgramPoint> apply (AVLSet<ProgramPoint> a, AVLSet<ProgramPoint> b) {
                return a.union(b);
              }
            });
      // TODO: see if it is possible to maintain the origins during the split in an easier/more efficient way
      AVLMap<Threshold.Origin, Threshold> empty = AVLMap.<Threshold.Origin, Threshold>empty();
      return P3.tuple3(new MutableState(narrowingSplit._1(), empty).rebuildReverseMapping(),
          new MutableState(narrowingSplit._2(), empty).rebuildReverseMapping(),
          new MutableState(narrowingSplit._3(), empty).rebuildReverseMapping());
    }

    /**
     * Three-way split of two AVL maps that map to a set. The special feature is that it uses a provided function to
     * handle the elements that are in both maps but differ.
     */
    private static <A, B> P3<AVLMap<A, B>, AVLMap<A, B>, AVLMap<A, B>> splitWithMergerForDiffering (
        AVLMap<A, B> first, AVLMap<A, B> second, Fn2<B, B, B> differingValuesMerger) {
      ThreeWaySplit<AVLMap<A, B>> triple = first.split(second);
      AVLMap<A, B> inBoth = first.difference(triple.onlyInFirst()); // values equal in both
      for (P2<A, B> differingPair : triple.inBothButDiffering()) {
        A threshold = differingPair._1();
        B thisValue = differingPair._2();
        B thatValue = second.get(threshold).get();
        inBoth = inBoth.bind(threshold, differingValuesMerger.apply(thisValue, thatValue));
      }
      return P3.tuple3(triple.onlyInFirst(), inBoth, triple.onlyInSecond());
    }

    /**
     * Sees if in {@code this} we have some thresholds that are marked as applied at more program points
     * than they are marked in {@code other}.
     */
    public boolean hasAppliedThresholdsNotIn (MutableState other) {
      AVLMap<Threshold, AVLSet<ProgramPoint>> first = this.appliedForNarrowingAt;
      AVLMap<Threshold, AVLSet<ProgramPoint>> second = other.appliedForNarrowingAt;
      ThreeWaySplit<AVLMap<Threshold, AVLSet<ProgramPoint>>> triple = first.split(second);
      for (P2<Threshold, AVLSet<ProgramPoint>> differingPair : triple.inBothButDiffering()) {
        Threshold threshold = differingPair._1();
        AVLSet<ProgramPoint> thisValue = differingPair._2();
        AVLSet<ProgramPoint> thatValue = second.get(threshold).get();
        AVLSet<ProgramPoint> difference = thisValue.difference(thatValue);
        if (!difference.isEmpty())
          return false;
      }
      for (P2<Threshold, AVLSet<ProgramPoint>> differingThreshold : triple.onlyInFirst()) {
        Threshold threshold = differingThreshold._1();
        AVLSet<ProgramPoint> thisValue = differingThreshold._2();
        // we need to see if other has maybe the same threshold but transformed by going the detour over origins
        Option<Threshold> origin = other.origins.get(threshold.getOrigin());
        if (origin.isNone() && !thisValue.isEmpty())
          return false;
        if (origin.isSome()) {
          AVLSet<ProgramPoint> thatValue = second.get(origin.get()).get();
          AVLSet<ProgramPoint> difference = thisValue.difference(thatValue);
          if (!difference.isEmpty())
            return false;
        }
      }
      return true;
    }

    public void add (Threshold threshold) {
      add(threshold, AVLSet.<ProgramPoint>empty());
    }

    public void add (Threshold threshold, AVLSet<ProgramPoint> appliedAt) {
      Option<Threshold> contained = origins.get(threshold.getOrigin());
      // FIXME: this is buggy. We should not always overwrite thresholds blindly.
      // The bug appears when we transform a threshold in one branch and not in the other.
      // Our mapping currently will store only one version of the threshold, thus it is a
      // matter of luck (branch evaluation order) which threshold will overwrite the other one
      // at the join of the branches. If we lose the transformed one and keep only the non-transformed one
      // we will not be stable after the widening but have consumed the threshold thus lose it for the next
      // widening round. See the CollectedExamples.ifElseJoinWiden() tests for examples. To make them fail
      // one must iterate over the if and else branches before the join. This can be enabled in the Worklist class.
      if (contained.isSome()) // need to replace an old one with the new one
        appliedForNarrowingAt = appliedForNarrowingAt.remove(contained.get());
      appliedForNarrowingAt = appliedForNarrowingAt.bind(threshold, appliedAt);
      origins = origins.bind(threshold.getOrigin(), threshold);
    }

    public void remove (Threshold threshold) {
      assert appliedForNarrowingAt.contains(threshold);
      appliedForNarrowingAt = appliedForNarrowingAt.remove(threshold);
      origins = origins.remove(threshold.getOrigin());
    }

    /**
     * Substitute {@code threshold} with {@code newThreshold}.
     *
     * @param threshold the threshold to replace
     * @param newThreshold the threshold to add
     */
    public void substitute (Threshold threshold, Threshold newThreshold) {
      assert appliedForNarrowingAt.contains(threshold);
      AVLSet<ProgramPoint> appliedSet = getAppliedPoints(threshold);
      remove(threshold);
      add(newThreshold, appliedSet);
    }

    /**
     * Return {@code true} if the state contains exactly the same threshold.
     */
    public boolean contains (Threshold threshold) {
      return appliedForNarrowingAt.contains(threshold);
    }

    /**
     * Return {@code true} if the state contains the threshold. The difference to {@link #contains(Threshold)} is that
     * it matches thresholds even if they have been transformed.
     */
    public boolean contains (Threshold.Origin originalThreshold) {
      return origins.contains(originalThreshold);
    }

    public boolean containsAnyAppliedFrom (ProgramPoint point) {
      for (P2<Origin, Threshold> pair : origins) {
        Origin original = pair._1();
        if (original.getLocation().equals(point) && isApplied(original))
          return true;
      }
      return false;
    }

    public boolean isApplied (Threshold.Origin originalThreshold) {
      assert origins.contains(originalThreshold);
      Threshold threshold = origins.get(originalThreshold).get();
      assert appliedForNarrowingAt.contains(threshold);
      return !appliedForNarrowingAt.get(threshold).get().isEmpty();
    }

    public boolean isAppliedAt (Threshold threshold, ProgramPoint point) {
      assert appliedForNarrowingAt.contains(threshold);
      return appliedForNarrowingAt.get(threshold).get().contains(point);
    }

    public void markAppliedAt (Threshold threshold, ProgramPoint point) {
      assert appliedForNarrowingAt.contains(threshold);
      AVLSet<ProgramPoint> newAppliedSet = appliedForNarrowingAt.get(threshold).get().add(point);
      appliedForNarrowingAt = appliedForNarrowingAt.bind(threshold, newAppliedSet);
    }

    public AVLSet<ProgramPoint> getAppliedPoints (Threshold threshold) {
      assert appliedForNarrowingAt.contains(threshold);
      return appliedForNarrowingAt.get(threshold).get();
    }

    @Override public Iterator<Threshold> iterator () {
      return appliedForNarrowingAt.keys().iterator();
    }

    @Override public String toString () {
      return new ThresholdsWideningState(this).toString();
    }

  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    // nop
  }
}
