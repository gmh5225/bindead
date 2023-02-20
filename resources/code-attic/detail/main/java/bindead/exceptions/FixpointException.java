package bindead.exceptions;

import bindead.exceptions.EvaluationException;
import javalx.data.Option;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.exceptions.UncheckedExceptionWrapper;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa;

/**
 * Thrown to indicate an exception that occurred during the fixpoint computation.
 */
public class FixpointException extends UncheckedExceptionWrapper {
  private final Cfa cfa;
  private final Vertex vertex;
  private final EvaluationException evaluationException;

  public FixpointException (Throwable cause, Cfa cfa, Vertex vertex) {
    super(cause);
    this.cfa = cfa;
    this.vertex = vertex;
    this.evaluationException = null;
  }

  public FixpointException (EvaluationException cause, Cfa cfa, Vertex vertex) {
    super(cause.getWrappedException());
    this.cfa = cfa;
    this.vertex = vertex;
    this.evaluationException = cause;
  }

  /**
   * @return The CFA that was processed while the exception occurred.
   */
  public Cfa getCfa () {
    return cfa;
  }

  /**
   * @return The address of the vertex in the CFA that was processed while the exception occurred.
   */
  public Option<RReilAddr> getVertexAddress () {
    return getCfa().getAddress(getVertex());
  }

  /**
   * @return The vertex in the CFA that was processed while the exception occurred.
   */
  public Vertex getVertex () {
    return vertex;
  }

  @Override public String getMessage () {
    StringBuilder builder = new StringBuilder();
    if (evaluationException != null)
      builder.append(prettyPrintEvaluationExceptionLocation());
    else
      builder.append(prettyPrintExceptionLocation());
    return builder.toString();
  }

  private StringBuilder prettyPrintEvaluationExceptionLocation () {
    StringBuilder builder = new StringBuilder();
    builder.append("Cause: " + getWrappedException() + "\n");
    builder.append("Location: ");
    builder.append("CFA(" + cfa.getEntryAddress() + ")");
    builder.append(" while evaluating the transfer function for ");
    builder.append('"');
    builder.append(evaluationException.getAddress() + ": ");
    builder.append(evaluationException.getInstruction());
    builder.append('"');
    builder.append("\n");
    return builder;
  }

  private StringBuilder prettyPrintExceptionLocation () {
    StringBuilder builder = new StringBuilder();
    builder.append("Cause: " + getWrappedException() + "\n");
    builder.append("Location: ");
    builder.append("CFA(" + cfa.getEntryAddress() + ")");
    builder.append(" while processing the ");
    Option<RReilAddr> vertexAddress = cfa.getAddress(vertex);
    if (vertexAddress.isSome())
      builder.append("Vertex(" + vertexAddress.get() + ")\n");
    else if (cfa.getExit().equals(vertex))
      builder.append("CFA exit vertex\n");
    else
      builder.append("vertex with the incoming edges: ").append(printIncomingEdges(cfa, vertex));
    return builder;
  }

  private static StringBuilder printIncomingEdges (Cfa cfa, Vertex vertex) {
    StringBuilder builder = new StringBuilder();
    builder.append("\n");
    builder.append("--------------------");
    builder.append("\n");
    for (Edge edge : vertex.incoming()) {
      builder.append(cfa.labelOfEdge(edge));
      builder.append("\n");
      builder.append("--------------------");
      builder.append("\n");
    }
    return builder;
  }

}
