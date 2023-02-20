package bindead.domains;

import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;

/**
 * Collect regression bugs and have them as unit tests here until we know
 * what causes them. After fixing them move the unit tests to the package
 * for the domain or similar tests.
 *
 * @author Bogdan Mihaila
 */
public class BugsRegressions {
  @SuppressWarnings("unused")
  private final static AnalysisFactory analyzer = new AnalysisFactory();

  public void debug () {
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printMemVarOnly();
    DebugHelper.analysisKnobs.printSubsetOrEqual();
    DebugHelper.analysisKnobs.printWidening();
  }

}
