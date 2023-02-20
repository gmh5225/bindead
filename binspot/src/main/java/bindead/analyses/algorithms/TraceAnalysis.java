package bindead.analyses.algorithms;

import java.util.List;

import bindead.analyses.algorithms.data.Flows.Successor;
import bindead.analyses.algorithms.data.ProgramCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import binparse.trace.TraceBinary;

/**
 * Executes the transfer functions of the abstract domains along a recorded trace or does a normal fixpoint analysis if
 * the trace has ended.
 */
public class TraceAnalysis<D extends RootDomain<D>> extends FixpointAnalysis<D> {
  // FIXME: made public for a certain testcase, remove when done
  public final TraceIterator traceIterator;
  private TraceEvaluator<D> evaluator;

  public TraceAnalysis (AnalysisEnvironment environment, TraceBinary binary, D initialState) {
    super(environment, binary, initialState);
    this.traceIterator = new TraceIterator(binary.getTraceDump());
  }

  @Override protected FixpointAnalysisEvaluator<D> getEvaluator () {
    if (isFollowingTrace())
      return getTraceEvaluator();
    else
      return super.getEvaluator();
  }

  private FixpointAnalysisEvaluator<D> getTraceEvaluator () {
    if (evaluator == null)
      evaluator = new TraceEvaluator<D>(traceIterator);
    return evaluator;
  }

  @Override protected boolean updateWorklist (ProgramCtx from, Successor<D> successor, List<ProgramCtx> queue) {
    ProgramCtx to = new ProgramCtx(from.getCallString(), successor.getAddress());
    // do not use widening if we follow the trace
    boolean updated = states.update(from, successor.getType(), to, successor.getState(), !isFollowingTrace());
    if (updated || isFollowingTrace()) {
      queue.add(to);
      return true;
    }
    return false;
  }

  private boolean isFollowingTrace () {
    return traceIterator.hasNext();
  }

}
