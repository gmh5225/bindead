package bindead.analyses.algorithms.data;

import javalx.data.Option;
import rreil.lang.RReilAddr;
import bindead.domainnetwork.interfaces.ProgramPoint;

/**
 * A program point distinguished by the address of the current instruction and the callstring of the analysis.
 */
public class ProgramCtx implements ProgramPoint {
  private final CallString callString;
  private final RReilAddr address;

  public ProgramCtx (CallString callString, RReilAddr address) {
    this.callString = callString;
    this.address = address;
  }

  public CallString getCallString () {
    return callString;
  }

  @Override public RReilAddr getAddress () {
    return address;
  }

  @Override public ProgramPoint withAddress (RReilAddr newAddress) {
    return new ProgramCtx(callString, newAddress);
  }

  @Override public int compareTo (ProgramPoint other) {
    int cmp = this.getAddress().compareTo(other.getAddress());
    if (!(other instanceof ProgramCtx))
      return cmp;
    ProgramCtx otherPoint = (ProgramCtx) other;
    return cmp != 0 ? cmp : this.getCallString().size() - otherPoint.getCallString().size();
  }

  public static Option<ProgramPoint> none () {
    return ProgramPoint.nowhere;
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((address == null) ? 0 : address.hashCode());
    result = prime * result + ((callString == null) ? 0 : callString.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof ProgramCtx))
      return false;
    ProgramCtx other = (ProgramCtx) obj;
    return this.compareTo(other) == 0;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    if (!callString.isRoot())
      builder.append(callString + ", ");
    builder.append(address.toShortString());
    builder.append(")");
    return builder.toString();
  }
}