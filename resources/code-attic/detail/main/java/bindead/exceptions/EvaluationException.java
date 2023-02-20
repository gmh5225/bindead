package bindead.exceptions;

import javalx.exceptions.UncheckedExceptionWrapper;
import rreil.abstractsyntax.RReilAddr;

/**
 * Thrown to indicate an exception that occurred during the evaluation of the transfer function for an edge.
 */
public class EvaluationException extends UncheckedExceptionWrapper {
  private final RReilAddr address;
  private final String instruction;

  public EvaluationException (Throwable exception, RReilAddr address, String instruction) {
    super(exception);
    this.address = address;
    this.instruction = instruction;
  }

  public RReilAddr getAddress () {
    return address;
  }

  public String getInstruction () {
    return instruction;
  }

  @Override public String getMessage () {
    StringBuilder builder = new StringBuilder();
    builder.append("Cause: " + getWrappedException() + "\n");
    builder.append("Location: ");
    builder.append(" while evaluating the transfer function for ");
    builder.append(getAddress() + ": ");
    builder.append(getInstruction());
    return builder.toString();
  }

}
