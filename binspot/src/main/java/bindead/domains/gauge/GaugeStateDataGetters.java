package bindead.domains.gauge;

import javalx.data.Option;
import javalx.persistentcollections.AVLMap;
import bindead.data.NumVar;
import bindead.data.VarSet;

public interface GaugeStateDataGetters {

  public VarSet getCandidates ();

  public boolean isCandidate (NumVar x);

  public VarSet getCounters ();

  public boolean isCounter (NumVar var);

  public AVLMap<NumVar, Wedge> getWedges ();

  public Option<Wedge> getWedgeOption (NumVar var);

  public AVLMap<NumVar, VarSet> getReverse ();

  public VarSet getOccurences (NumVar var);

  public Wedge getWedge (NumVar var);

  public VarSet getWedgesDomain ();

  public Option<ImmutableGaugeStateData> getWideningReference ();

}
