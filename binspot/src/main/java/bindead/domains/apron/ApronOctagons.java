package bindead.domains.apron;

import javalx.numeric.Range;
import javalx.persistentcollections.BiMap;
import apron.Abstract1;
import apron.ApronException;
import apron.Manager;
import apron.Octagon;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;

/**
 * Abstract Numeric Domain that uses the Apron Library with the Octagons domain.
 *
 * @author Bogdan Mihaila
 */
public class ApronOctagons extends Apron {
  public static final String NAME = "APRON(Octagons)";

  public ApronOctagons () {
    super();
  }

  private ApronOctagons (Abstract1 state, BiMap<NumVar, String> variablesMapping, SynthChannel synth, AnalysisCtx ctx) {
    super(state, variablesMapping, synth, ctx);
  }

  @Override protected Manager getDomainManager () {
    return new Octagon();
  }

  @Override protected Apron build (Abstract1 state, BiMap<NumVar, String> variablesMapping, SynthChannel synth,
      AnalysisCtx ctx) {
    return new ApronOctagons(state, variablesMapping, synth, ctx);
  }

  @Override protected String printDomainValues () {
//    return state.toString();
    return PrettyPrinters.compactOctagons(state, variablesMapping);
  }

  @Override VarSet getVarsWithTransitiveRelations (VarSet vars) throws ApronException {
    return VarSet.from(variablesMapping.keys(), variablesMapping.size());
  }

  @Override public void toCompactString (StringBuilder builder) {
    String domainValues = PrettyPrinters.asIntervals(state, variablesMapping, false) + "\n" + printDomainValues();
    String string = StringHelpers.indentMultiline(NAME + ": #" + variablesMapping.size() + " ", domainValues);
    builder.append(string);
  }


  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    Range range = queryRange(Linear.linear(var));
    if (range.isConstant())
      builder.append(range.getMin());
    else
      builder.append(var);
  }
}
