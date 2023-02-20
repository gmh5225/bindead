package bindead.exceptions;

import bindead.xml.StringHelpers;

import bindead.analyses.ReconstructionCallString;
import bindead.analysis.util.CallString;
import javalx.digraph.Digraph.Vertex;
import javalx.exceptions.UncheckedExceptionWrapper;

/**
 * Thrown to indicate an exception that occurred during the CFA reconstruction.
 */
public class ReconstructionException extends UncheckedExceptionWrapper {
  private static final long serialVersionUID = 1L;
  private final ReconstructionCallString<?> reconstruction;
  private final CallString callPath;
  private final FixpointException fixpointException;

  public ReconstructionException (Exception cause, ReconstructionCallString<?> analysis, CallString callPath) {
    super(cause);
    this.reconstruction = analysis;
    this.callPath = callPath;
    this.fixpointException = null;
  }

  public ReconstructionException (FixpointException cause, ReconstructionCallString<?> analysis, CallString callPath) {
    super(cause.getWrappedException());
    this.reconstruction = analysis;
    this.callPath = callPath;
    this.fixpointException = cause;
  }

  /**
   * @return The reconstruction analysis that was performed while the exception occurred.
   */
  public ReconstructionCallString<?> getPartialReconstruction () {
    return reconstruction;
  }

  /**
   * @return The vertex in the CFA that was processed while the exception occurred.
   */
  public Vertex getVertex () {
    return fixpointException.getVertex();
  }

  @Override public String getMessage () {
    StringBuilder builder = new StringBuilder();
    builder.append("\n");
    builder.append(StringHelpers.repeatString("*", 100)).append("\n");
    if (fixpointException != null)
      builder.append(fixpointException.getMessage());
    else
      builder.append(super.getMessage()).append("\n");
    builder.append("Location was reached by the path of calls: " + callPath.pretty(30)).append("\n");
    builder.append(StringHelpers.repeatString("*", 100));
    return builder.toString();
  }
}
