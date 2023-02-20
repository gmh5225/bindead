package bindead.analyses.algorithms;

import java.util.Iterator;

import javalx.data.products.P3;
import rreil.lang.RReilAddr;
import binparse.trace.TraceDump;

public class TraceIterator {
  private final Iterator<P3<Long, Long, Boolean>> branches;
  private RReilAddr nextBranchLocation;
  private RReilAddr nextBranchTarget;
  private boolean nextBranchConditionEvaluation;

  public TraceIterator (TraceDump trace) {
    branches = trace.getControlFlow().iterator();
    advance(); // initialize
    advance(); // the first jump to the entry is ignored
  }

  public boolean hasNext () {
    return branches.hasNext();
  }

  public boolean nextBranchIsAt (RReilAddr address) {
    return nextBranchLocation.base() == address.base();
  }

  public boolean getBranchConditionEvaluation () {
    return nextBranchConditionEvaluation;
  }

  public RReilAddr getBranchTarget () {
    return nextBranchTarget;
  }

  public void advance () {
    if (hasNext())
      loadNextBranch();
  }

  private void loadNextBranch () {
    P3<Long, Long, Boolean> nextBranch = branches.next();
    nextBranchLocation = RReilAddr.valueOf(nextBranch._1());
    nextBranchTarget = RReilAddr.valueOf(nextBranch._2());
    nextBranchConditionEvaluation = nextBranch._3();
  }
}