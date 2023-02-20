package bindead.domains.apron;

import javalx.persistentcollections.BiMap;
import apron.Abstract1;
import apron.Box;
import apron.Manager;
import bindead.data.NumVar;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;

/**
 * Abstract Numeric Domain that uses the Apron Library with the Intervals domain.
 *
 * @author Bogdan Mihaila
 */
public class ApronIntervals extends Apron {
  public static final String NAME = "APRON(Intervals)";

  public ApronIntervals () {
    super();
  }

  private ApronIntervals (Abstract1 state, BiMap<NumVar, String> variablesMapping, SynthChannel synth, AnalysisCtx ctx) {
    super(state, variablesMapping, synth, ctx);
  }

  @Override protected Manager getDomainManager () {
    return new Box();
  }

  @Override protected Apron build (Abstract1 state, BiMap<NumVar, String> variablesMapping, SynthChannel synth,
      AnalysisCtx ctx) {
    return new ApronIntervals(state, variablesMapping, synth, ctx);
  }

  @Override protected String printDomainValues () {
    return PrettyPrinters.asIntervals(state, variablesMapping, true);
  }

  @Override public String toString () {
    String domainValues = printDomainValues();
    return StringHelpers.indentMultiline(NAME + ": #" + variablesMapping.size() + " ", domainValues);
  }

}
