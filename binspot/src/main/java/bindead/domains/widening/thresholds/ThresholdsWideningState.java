package bindead.domains.widening.thresholds;

import java.util.Iterator;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.fn.Fn2;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.MultiMap;
import javalx.persistentcollections.ThreeWaySplit;
import rreil.lang.RReilAddr;
import bindead.analyses.ProgramAddress;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.ProgramPoint;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The state for the thresholds widening domain. It contains thresholds that are marked with the locations they have
 * been used for narrowing.
 */
class ThresholdsWideningState extends FunctorState {
  // NOTE: this is hacky to use 0 but we need to use some program point
  protected static final ProgramPoint zeroPoint = new ProgramAddress(RReilAddr.ZERO);
  protected static final AVLSet<ProgramPoint> emptyTransformations = AVLSet.<ProgramPoint>empty().add(zeroPoint);
  public static final ThresholdsWideningState EMPTY = new ThresholdsWideningState();
  // a map to know where a threshold has been transformed.
  // We do not want to transform it more than one at a program point to ensure termination for such cases:
  // while (true) { if (x < 10) x++; }
  // here the threshold would be transformed forever and be bigger than the widened state and restrict it each time again.
  private final MultiMap<Threshold, ProgramPoint> thresholds;
  private final MultiMap<NumVar, Threshold> reverse;
  private final AVLMap<ProgramPoint, Integer> wideningCounter;

  private ThresholdsWideningState () {
    thresholds = MultiMap.empty();
    reverse = MultiMap.empty();
    wideningCounter = AVLMap.empty();
  }

  private ThresholdsWideningState (MutableState state) {
    assert state.isConsistent();
    thresholds = state.thresholds;
    reverse = state.reverse;
    wideningCounter = state.wideningCounter;
  }

  MutableState mutableCopy () {
    return new MutableState(thresholds, reverse, wideningCounter);
  }

//  private static XMLBuilder pretty (Test test, XMLBuilder builder) {
//    builder = builder.e("Test");
//    builder = test.toXML(builder);
//    builder = builder.up();
//    return builder;
//  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(ThresholdsWidening.NAME);
    // TODO: implement similar to below
//    for (final P2<Origin, Threshold> tuple : origins) {
//      Origin origin = tuple._1();
//      Threshold threshold = tuple._2();
//      AVLSet<ProgramPoint> locations = appliedForNarrowingAt.get(threshold).get();
//      builder = builder.e("Entry").a("type", "Thresholds")
//          .e("Value")
//          .e("Origin")
//          .e("Location")
//          .t(origin.getLocation().toString())
//          .up(); // Location
//      builder = pretty(origin.getTest(), builder);
//      builder = builder.up(); // Origin
//      builder = pretty(threshold.getTest(), builder);
//      builder = builder.e("appliedAt");
//      for (ProgramPoint point : locations) {
//        builder = builder.e("point").t(point.toString()).up();
//      }
//      builder = builder.up()//appliedAt
//          .up()//value
//          .up(); //entry
//    }
    return builder.up();
  }

  @Override public String toString () {
    return "#" + thresholds.size() + " " + contentToString();
  }

  private String contentToString () {
    Iterator<P2<Threshold, AVLSet<ProgramPoint>>> iterator = thresholds.iteratorKeyValueSet();
    if (!iterator.hasNext())
      return "{}";
    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<Threshold, AVLSet<ProgramPoint>> element = iterator.next();
      Threshold key = element._1();
      // remove the artificial transformation program point
      AVLSet<ProgramPoint> value = element._2().remove(zeroPoint);
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
    MultiMap<Threshold, ProgramPoint> thresholds = MultiMap.empty();
    private MultiMap<NumVar, Threshold> reverse = MultiMap.empty();
    private AVLMap<ProgramPoint, Integer> wideningCounter = AVLMap.empty();
    private final Fn2<Integer, Integer, Integer> maxSelector = new Fn2<Integer, Integer, Integer>() {

      @Override public Integer apply (Integer a, Integer b) {
        return Math.max(a, b);
      }
    };

    private MutableState (MultiMap<Threshold, ProgramPoint> thresholds, MultiMap<NumVar, Threshold> reverse,
        AVLMap<ProgramPoint, Integer> wideningCounter) {
      this.thresholds = thresholds;
      this.reverse = reverse;
      this.wideningCounter = wideningCounter;
    }

    ThresholdsWideningState immutableCopy () {
      return new ThresholdsWideningState(this);
    }

    /**
     * Checks if the mappings in the internal state are consistent. Use for assertions and debugging only.
     */
    boolean isConsistent () {
      for (Threshold threshold : this) {
        for (NumVar variable : threshold.getTest().getVars()) {
          if (!reverse.get(variable).contains(threshold))
            return false;
        }
      }
      for (P2<NumVar, Threshold> mapping : reverse) {
        if (!contains(mapping._2()))
          return false;
      }
      return true;
    }

    public int getWideningNumber (ProgramPoint location) {
      return wideningCounter.get(location).getOrElse(0);
    }

    public void appliedNarrowingAt (ProgramPoint location) {
      Integer count = wideningCounter.get(location).getOrElse(0);
      wideningCounter = wideningCounter.bind(location, count + 1);
    }

    public int thresholdsSize () {
      return thresholds.size();
    }

    public int wideningPointsSize () {
      return wideningCounter.size();
    }

    /**
     * Returns a triple {@code a, b, c} where {@code a} is the set of thresholds only in this and {@code c} is the set of
     * thresholds only in other. {@code b} is the set of thresholds that are in both and their set of narrowing points is
     * the union of both sides.
     */
    public P3<MutableState, MutableState, MutableState> split (MutableState other) {
      ThreeWaySplit<MultiMap<Threshold, ProgramPoint>> narrowingSplit = thresholds.splitWithUnion(other.thresholds);
      MultiMap<Threshold, ProgramPoint> inBoth = thresholds.intersection(other.thresholds);
      inBoth = inBoth.union(narrowingSplit.inBothButDiffering());
      AVLMap<ProgramPoint, Integer> newWideningCounter = wideningCounter.union(maxSelector, other.wideningCounter);
      MultiMap<NumVar, Threshold> emptyReverse = MultiMap.empty();
      return P3.tuple3(new MutableState(narrowingSplit.onlyInFirst(), emptyReverse, newWideningCounter).rebuildReverseMapping(),
          new MutableState(inBoth, emptyReverse, newWideningCounter).rebuildReverseMapping(),
          new MutableState(narrowingSplit.onlyInSecond(), emptyReverse, newWideningCounter).rebuildReverseMapping());
    }

    public MutableState union (MutableState other) {
      MultiMap<Threshold, ProgramPoint> newThresholds = this.thresholds.union(other.thresholds);
      MultiMap<NumVar, Threshold> newReverse = this.reverse.union(other.reverse);
      AVLMap<ProgramPoint, Integer> newWideningCounter = wideningCounter.union(maxSelector, other.wideningCounter);
      return new MutableState(newThresholds, newReverse, newWideningCounter);
    }

    public void add (Threshold threshold) {
      add(threshold, emptyTransformations);
    }

    public void add (Threshold threshold, AVLSet<ProgramPoint> transformedAt) {
      thresholds = thresholds.add(threshold, transformedAt);
      addToReverse(threshold);
    }

    public void remove (Threshold threshold) {
      assert contains(threshold);
      thresholds = thresholds.remove(threshold);
      removeFromReverse(threshold);
    }

    /**
     * Substitute {@code threshold} with {@code newThreshold}.
     *
     * @param threshold the threshold to replace
     * @param newThreshold the threshold to add
     */
    public void substitute (Threshold threshold, Threshold newThreshold) {
      assert thresholds.contains(threshold);
      if (threshold.equals(newThreshold))
        return;
      thresholds = thresholds.replaceKey(threshold, newThreshold);
      removeFromReverse(threshold);
      addToReverse(newThreshold);
    }

    public AVLSet<Threshold> getTresholdsContaining (NumVar variable) {
      return reverse.get(variable);
    }

    public AVLSet<Threshold> getTresholdsContainingAnyOf (VarSet variables) {
      AVLSet<Threshold> result = AVLSet.empty();
      for (NumVar variable : variables) {
        result = result.union(reverse.get(variable));
      }
      return result;
    }

    public AVLSet<Threshold> getNotTransformedHere (ProgramPoint location) {
      AVLSet<Threshold> result = AVLSet.empty();
      for (Threshold threshold : this) {
        if (!getTransformations(threshold).contains(location))
          result = result.add(threshold);
      }
      return result;
    }

    private MutableState rebuildReverseMapping () {
      reverse = MultiMap.empty();
      for (Threshold threshold : this) {
        for (NumVar variable : threshold.getTest().getVars()) {
          reverse = reverse.add(variable, threshold);
        }
      }
      return this;
    }

    private void addToReverse (Threshold threshold) {
      for (NumVar variable : threshold.getTest().getVars()) {
        reverse = reverse.add(variable, threshold);
      }
    }

    private void removeFromReverse (Threshold threshold) {
      for (NumVar variable : threshold.getTest().getVars()) {
        reverse = reverse.remove(variable, threshold);
      }
    }

    public void markTransformedAt (Threshold threshold, ProgramPoint location) {
      thresholds = thresholds.add(threshold, location);
    }

    public AVLSet<ProgramPoint> getTransformations (Threshold threshold) {
      return thresholds.get(threshold);
    }

    /**
     * Return {@code true} if the state contains exactly the same threshold.
     */
    public boolean contains (Threshold threshold) {
      return thresholds.contains(threshold);
    }

    public Iterable<Threshold> asSet () {
      return thresholds.keys();
    }

    @Override public Iterator<Threshold> iterator () {
      return thresholds.keys().iterator();
    }

    @Override public String toString () {
      return new ThresholdsWideningState(this).toString();
    }

  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    // nope
  }
}
