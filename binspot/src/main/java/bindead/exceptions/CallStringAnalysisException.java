package bindead.exceptions;

import javalx.exceptions.UncheckedExceptionWrapper;
import rreil.lang.RReilAddr;
import bindead.analyses.Analysis;
import bindead.analyses.algorithms.data.CallString;

/**
 * An exception that occurred during the call-string analysis. The exception carries the analysis results so far and some
 * hints at what point and with what state of the analysis the exception occurred.
 */
public class CallStringAnalysisException extends UncheckedExceptionWrapper {
  private final Analysis<?> analysis;
  private final CallString callString;
  private final RReilAddr address;

  public CallStringAnalysisException (Throwable cause, Analysis<?> analysis, CallString callString,
      RReilAddr point) {
    super(cause);
    this.analysis = analysis;
    this.callString = callString;
    this.address = point;
  }

  @Override public String getMessage () {
    StringBuilder builder = new StringBuilder();
    builder.append("BAM! at ");
    builder.append(address);
    builder.append(" caused by:\n");
    builder.append(getWrappedException().toString());
    return builder.toString();
  }

  public Analysis<?> getAnalysis () {
    return analysis;
  }

  public CallString getCallString () {
    return callString;
  }

  public RReilAddr getAddress () {
    return address;
  }

  @Override public String toString () {
    return getMessage();
  }

}
