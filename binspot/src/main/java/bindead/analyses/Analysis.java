package bindead.analyses;

import javalx.data.Option;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import rreil.lang.Field;
import rreil.lang.MemVar;
import rreil.lang.RReilAddr;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.analyses.algorithms.data.CallString;
import bindead.analyses.algorithms.data.TransitionSystem;
import bindead.analyses.warnings.WarningsMap;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import bindead.environment.platform.Platform;

public abstract class Analysis<D extends RootDomain<D>> {
  protected final AnalysisEnvironment environment;
  protected AnalysisDebugHooks debugHooks;

  public Analysis (AnalysisEnvironment environment) {
    this.environment = environment;
  }

  public abstract void runFrom (RReilAddr address);

  public void setDebugHooks (AnalysisDebugHooks hooks) {
    this.debugHooks = hooks;
  }

  public Platform getPlatform () {
    return environment.getPlatform();
  }

  /**
   * Return an object to monitor and query the status of an ongoing analysis.
   */
  public ProgressReporter getProgressMonitoring () {
    return null;
  }

  public Option<Interval> query (RReilAddr address, String name, int size, int offset) {
    Option<Range> range = queryRange(address, name, size, offset);
    if (range.isNone())
      return Option.none();
    return Option.some(range.get().convexHull());
  }

  public Option<Range> queryRange (RReilAddr address, String name, int size, int offset) {
    Option<D> state = getState(CallString.root(), address);
    if (state.isNone())
      return Option.none();
    return Option.some(state.get().queryRange(MemVar.getVarOrFresh(name), Field.finiteRangeKey(offset, size)));
  }

  public abstract Option<D> getState (CallString callString, RReilAddr address);

  public abstract BinaryCodeCache getBinaryCode ();

  public abstract RReilCodeCache getRReilCode ();

  public abstract TransitionSystem getTransitionSystem ();

  public abstract WarningsMap getWarnings ();
}