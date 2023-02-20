package bindead.analyses.callback;

import rreil.lang.RReil;
import bindead.analyses.algorithms.data.Flows;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;

/**
 * Interface to implement to inject callbacks to your own code during an analysis.
 *
 * @author Bogdan Mihaila
 */
public interface CallbackHandler {

  /**
   * Method executed when this callback is triggered.
   *
   * @param insn The instruction that was executed before and caused the control flow to reach the address.
   * @param domainState The incoming domain state for this point.
   * @param point The current location of the analysis.
   * @param env The environment configuration of the analysis. It provides tools for state manipulation.
   * @param flow A collection of control flow changes that should be updated by the callback.
   */
  // REFACTOR bm: find out what is necessary to be passed in and out
  public <D extends RootDomain<D>> void run (
      RReil insn, D domainState, ProgramPoint point, AnalysisEnvironment env, Flows<D> flow);
}