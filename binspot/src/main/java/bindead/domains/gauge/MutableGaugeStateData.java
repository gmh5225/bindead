package bindead.domains.gauge;

import javalx.persistentcollections.AVLMap;
import bindead.data.NumVar;
import bindead.data.VarSet;

public interface MutableGaugeStateData extends GaugeStateDataGetters {
  public ImmutableGaugeStateData getImmutableCopy ();

  public void setCandidates (VarSet candidates);

  public void setCounters (VarSet counters);

  public void setWedges (AVLMap<NumVar, Wedge> wedges);

  public void setWedge (NumVar var, Wedge wedge);

  public void removeWedge (NumVar var);

  public void setReverse (AVLMap<NumVar, VarSet> reverse);

  public void substituteCounter (NumVar x, NumVar y);

}