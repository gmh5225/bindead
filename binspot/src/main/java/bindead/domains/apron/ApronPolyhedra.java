package bindead.domains.apron;

import javalx.persistentcollections.BiMap;
import apron.Abstract1;
import apron.Manager;
import apron.Polka;
import bindead.data.NumVar;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;

/**
 * Abstract Numeric Domain that uses the Apron Library with the Polyhedra domain.
 *
 * @author Bogdan Mihaila
 */
public class ApronPolyhedra extends Apron {
  public static final String NAME = "APRON(Polyhedra)";

  public ApronPolyhedra () {
    super();
  }

  private ApronPolyhedra (Abstract1 state, BiMap<NumVar, String> variablesMapping, SynthChannel synth, AnalysisCtx ctx) {
    super(state, variablesMapping, synth, ctx);
  }

  @Override protected Manager getDomainManager () {
 // "false" means no strictness, i.e. < also allowed not only <= but as it is slower we don't want it
//  return new PplPoly(false);
//  return new PplGrid();
//  return new PolkaGrid(false);
    return new Polka(false);
  }

  @Override protected Apron build (Abstract1 state, BiMap<NumVar, String> variablesMapping, SynthChannel synth,
      AnalysisCtx ctx) {
    return new ApronPolyhedra(state, variablesMapping, synth, ctx);
  }

}
