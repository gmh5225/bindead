package bindead.analyses;

import bindead.exceptions.FixpointException;
import bindead.exceptions.EvaluationException;
import java.util.ArrayDeque;
import java.util.Deque;
import javalx.digraph.Digraph.Vertex;

public final class WorklistSolver {
  private final Analysis<?> analysis;
  private final Deque<Vertex> worklist = new ArrayDeque<Vertex>();

  public WorklistSolver (Analysis<?> analysis) {
    this.analysis = analysis;
  }

  public void solve () {
    Vertex initial = analysis.getStartVertex();
    addToWorklist(analysis.influences(initial));
    while (!worklist.isEmpty()) {
      Vertex v = worklist.pop();
      try {
        Iterable<Vertex> influenced = analysis.solve(v);
        addToWorklist(influenced);
        if (Thread.interrupted())
          throw new InterruptedException("Fixpoint computation was stopped.");
      } catch (EvaluationException cause) {
        throw new FixpointException(cause, analysis.getCfa(), v);
      } catch (FixpointException cause) {
        // catch and re-throw to avoid the accumulation of FixpointExceptions by putting one into another below
        throw cause;
      } catch (Throwable cause) {
        // by using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
        // might also be useful to have a partial analysis result even on unexpected errors. Also we need to catch
        // assertion errors.
        throw new FixpointException(cause, analysis.getCfa(), v);
      }
    }
  }

  private void addToWorklist (Iterable<Vertex> influences) {
    for (Vertex v : influences) {
      if (!worklist.contains(v))
        worklist.push(v);
    }
  }
}
