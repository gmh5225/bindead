package bindead.domains.gauge;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.interfaces.FunctorState;

import com.jamesmurty.utils.XMLBuilder;

/**
 * @author sgreben
 */
class GaugeState extends FunctorState {
  private final ImmutableGaugeStateData data;
  public static final GaugeState EMPTY = new GaugeState(GaugeStateData.EMPTY);

  public GaugeState (ImmutableGaugeStateData data) {
    this.data = data;
  }

  public GaugeStateBuilder getBuilder () {
    return new GaugeStateBuilder(data.getMutableCopy());
  }

  public boolean isCounter (NumVar var) {
    return data.getCounters().contains(var);
  }

  public VarSet getCounters () {
    return data.getCounters();
  }

  public Option<Wedge> getWedgeOption (NumVar var) {
    return data.getWedges().get(var);
  }

  public boolean subsetOrEqualTo (GaugeState other) {
//		// Checks: other counters \ these counters = Ø
//		if (!(other.counters.difference(this.counters).isEmpty()))
//			return false;
    ThreeWaySplit<AVLMap<NumVar, Wedge>> D = data.getWedges().split(other.data.getWedges());
    // Checks: No bindings in this, but not in other
    if (!D.onlyInSecond().isEmpty())
      return false;
    // Checks: All common bindings are such that "this wedge for var <= other wedge for var"
    for (P2<NumVar, Wedge> entry : D.inBothButDiffering()) {
      NumVar var = entry._1();
      Wedge thisWedge = entry._2();
      Wedge otherWedge = other.getWedgeOption(var).get();
      if (!thisWedge.subsetOrEqualTo(otherWedge))
        return false;
    }
    return true;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override public String toString () {
    return data.toString();
  }

}