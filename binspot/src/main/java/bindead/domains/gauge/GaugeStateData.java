package bindead.domains.gauge;

import static bindead.data.Linear.linear;
import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.Bound;
import javalx.persistentcollections.AVLMap;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domains.affine.Substitution;

/**
 * The data comprising a Gauge domain state and methods to manipulate it.
 *
 * @author sergey
 */
public class GaugeStateData implements ImmutableGaugeStateData, MutableGaugeStateData {
  private VarSet candidates;
  private VarSet counters;
  private AVLMap<NumVar, Wedge> wedges;
  private AVLMap<NumVar, VarSet> reverse;
  private final Option<ImmutableGaugeStateData> wideningReference = Option.<ImmutableGaugeStateData>none();

  @Override public Option<ImmutableGaugeStateData> getWideningReference () {
    return wideningReference;
  }

  @Override public String toString () {
    return "candidates=" + candidates + "\n\tcounters="
      + counters + "\n\twedges=" + wedges + "]";
  }

  public static final GaugeStateData EMPTY =
    new GaugeStateData(VarSet.empty(),
      VarSet.empty(),
      AVLMap.<NumVar, Wedge>empty(),
      AVLMap.<NumVar, VarSet>empty());

  @Override public ImmutableGaugeStateData getImmutableCopy () {
    return new GaugeStateData(candidates, counters, wedges, reverse);
  }

  @Override public MutableGaugeStateData getMutableCopy () {
    return new GaugeStateData(candidates, counters, wedges, reverse);
  }


  private GaugeStateData (VarSet candidates, VarSet counters,
      AVLMap<NumVar, Wedge> wedges, AVLMap<NumVar, VarSet> reverse) {
    super();
    this.candidates = candidates;
    this.counters = counters;
    this.wedges = wedges;
    this.reverse = reverse;
  }

  @Override public VarSet getCandidates () {
    return candidates;
  }

  @Override public void setCandidates (VarSet candidates) {
    this.candidates = candidates;
  }

  @Override public VarSet getCounters () {
    return counters;
  }

  @Override public void setCounters (VarSet counters) {
    this.counters = counters;
  }

  @Override public AVLMap<NumVar, Wedge> getWedges () {
    return wedges;
  }

  @Override public void setWedges (AVLMap<NumVar, Wedge> wedges) {
    this.wedges = wedges;
  }

  @Override public AVLMap<NumVar, VarSet> getReverse () {
    return reverse;
  }

  @Override public void setReverse (AVLMap<NumVar, VarSet> reverse) {
    this.reverse = reverse;
  }

  public void addReverse (NumVar lambda, NumVar x) {
    if (reverse.contains(lambda))
      reverse = reverse.bind(lambda, reverse.getOrNull(lambda).add(x));
    else
      reverse = reverse.bind(lambda, VarSet.of(x));
  }

  public void removeReverse (NumVar lambda, NumVar x) {
    if (reverse.contains(lambda))
      reverse = reverse.bind(lambda, reverse.get(lambda).get().remove(x));
  }

  @Override public Option<Wedge> getWedgeOption (NumVar var) {
    return getWedges().get(var);
  }

  /**
   * Return a variable [var]'s wedge if [var] is constrained in this state
   * or the full (-oo,+oo) wedge if [var] is not constrained.
   *
   * @param var
   * @return
   */
  @Override public Wedge getWedge (NumVar var) {
    Option<Wedge> optionWedge = getWedgeOption(var);
    return optionWedge.getOrElse(Wedge.FULL);
  }

  @Override public VarSet getWedgesDomain () {
    VarSet domain = VarSet.empty();
    for (P2<NumVar, Wedge> entry : getWedges())
      domain = domain.add(entry._1());
    return domain;
  }


  @Override public VarSet getOccurences (NumVar counter) {
    Option<VarSet> occurences = getReverse().get(counter);
    return occurences.getOrElse(VarSet.empty());
  }

  @Override public void setWedge (NumVar var, Wedge wedge) {
    VarSet newWedgeCounters = wedge.getVars();
    VarSet oldWedgeCounters = wedges.contains(var) ? wedges.get(var).get().getVars()
        : VarSet.empty();
    wedges = wedges.bind(var, wedge);
    for (NumVar removedCounter : oldWedgeCounters.difference(newWedgeCounters))
      removeReverse(removedCounter, var);
    for (NumVar addedCounter : newWedgeCounters.difference(oldWedgeCounters))
      addReverse(addedCounter, var);
  }

  @Override public void removeWedge (NumVar var) {
    if (wedges.contains(var)) {
      for (NumVar counter : getWedge(var).getVars())
        removeReverse(counter, var);
      wedges = wedges.remove(var);
    }
  }

  @Override public boolean isCandidate (NumVar x) {
    return getCandidates().contains(x);
  }

  @Override public boolean isCounter (NumVar var) {
    return getCounters().contains(var);
  }

  @Override public void substituteCounter (NumVar x, NumVar y) {
    Substitution sigma = new Substitution(x, linear(y), Bound.ONE);
    for (NumVar v : getOccurences(x)) {
      setWedge(v, getWedge(v).applySubstitution(sigma));
    }

  }
}