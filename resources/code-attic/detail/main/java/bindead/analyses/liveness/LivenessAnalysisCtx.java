package bindead.analyses.liveness;

import bindead.analyses.Analysis;
import java.util.HashMap;
import java.util.Map;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa;

/**
 * Collects the results of the liveness analysis.
 */
public class LivenessAnalysisCtx {
  private final Map<RReilAddr, Cfa> cfas = new HashMap<RReilAddr, Cfa>();
  private final Map<RReilAddr, Analysis<?>> analyses = new HashMap<RReilAddr, Analysis<?>>();

  public Analysis<?> getAnalysis (RReilAddr address) {
    return analyses.get(address);
  }

  public Cfa getCfa (RReilAddr address) {
    return cfas.get(address);
  }

  public void put (Cfa cfa, Analysis<?> analysis) {
    RReilAddr address = cfa.getEntryAddress();
    cfas.put(address, cfa);
    analyses.put(address, analysis);
  }

}
