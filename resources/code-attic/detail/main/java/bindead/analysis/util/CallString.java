package bindead.analysis.util;

import bindead.exceptions.DomainStateException;
import bindead.exceptions.DomainStateException.ErrObj;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import rreil.abstractsyntax.RReil.Call;

/**
 * Represents a sequence of calls that describe the path how a procedure was reached. A Call-String can be limited in
 * that only a certain number of calls are significant to identify the path to a procedure and the others are ignored. If
 * the Call-String is limited by k then the last k-calls are the significant ones.
 */
public class CallString implements Cloneable {
  private final int maxSignificantLength;
  private final LinkedList<Call> backLogString = new LinkedList<Call>();
  private final LinkedList<Call> significantString = new LinkedList<Call>();

  /**
   * Build a new Call-String object bounded by a maximum length for the significant string part.
   *
   * @param k The maximum length of the right-most significant string part
   * @return A new Call-String object
   */
  public static CallString withMaxSignificantLength (int k) {
    return new CallString(k);
  }

  protected CallString (int maxSignificantLength) {
    if (maxSignificantLength < 0)
      maxSignificantLength = 0;
    this.maxSignificantLength = maxSignificantLength;
  }

  @Override protected CallString clone () {
    return new CallString(this);
  }

  protected CallString (CallString other) {
    this.maxSignificantLength = other.maxSignificantLength;
    this.significantString.addAll(other.significantString);
    this.backLogString.addAll(other.backLogString);
  }

  public CallString pop (Call call) {
    CallString newCallString = clone();
    assert newCallString.size() > 0;
    Call lastCall;
    if (maxSignificantLength == 0) {
      lastCall = newCallString.backLogString.removeLast();
    } else {
      if (newCallString.backLogString.size() > 0) {
        assert newCallString.significantString.size() == maxSignificantLength;
        lastCall = newCallString.significantString.removeLast();
        Call leastSignificantCall = newCallString.backLogString.removeLast();
        newCallString.significantString.addFirst(leastSignificantCall);
      } else {
        lastCall = newCallString.significantString.removeLast();
      }
    }

    if (!lastCall.equals(call))
      throw new DomainStateException(ErrObj.INVARIANT_FAILURE);
    return newCallString;
  }

  public CallString push (Call call) {
    CallString newCallString = clone();
    if (maxSignificantLength == 0) {
      newCallString.backLogString.addLast(call);
    } else {
      if (newCallString.size() > maxSignificantLength) {
        Call leastSignificantCall = newCallString.significantString.removeFirst();
        newCallString.backLogString.addLast(leastSignificantCall);
      }
      newCallString.significantString.addLast(call);
    }
    return newCallString;
  }

  public int size () {
    return significantString.size() + backLogString.size();
  }

  public List<Call> getSignificantCalls () {
    return Collections.unmodifiableList(significantString);
  }

  public List<Call> getAllCalls () {
    List<Call> all = new LinkedList<Call>();
    all.addAll(backLogString);
    all.addAll(significantString);
    return Collections.unmodifiableList(all);
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((significantString == null) ? 0 : significantString.hashCode());
    result = prime * result + maxSignificantLength;
    return result;
  }

  /**
   * This Call-String equals another Call-String if their significant calls are the same.
   *
   * @param obj The other Call-String object to be compared to
   * @return <code>true</code> if this and the other object are equal
   */
  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof CallString))
      return false;
    CallString other = (CallString) obj;
    if (significantString == null) {
      if (other.significantString != null)
        return false;
    } else if (!significantString.equals(other.significantString))
      return false;
    if (maxSignificantLength != other.maxSignificantLength)
      return false;
    return true;
  }

  @Override public String toString () {
    return "Length=" + maxSignificantLength + ", " + backLogString + "||" + significantString;
  }

  /**
   * Print a pretty version of this Call-String.
   * @param maxDisplayableCalls How many calls should be displayed. The rest will be displayed as "...".
   * @return A prettier version of the call paths in this Call-String.
   */
  public String pretty (int maxDisplayableCalls) {
    StringBuilder builder = new StringBuilder();
    if (getSignificantCalls().isEmpty()) {
      builder.append("<program entry>");
      return builder.toString();
    }
    List<Call> calls = getSignificantCalls();
    if (calls.size() > maxDisplayableCalls) {
      int callsSize = calls.size();
      calls = calls.subList(callsSize - maxDisplayableCalls, callsSize);
      builder.append("(" + (callsSize - maxDisplayableCalls) + " more) [...] ");
    }
    for (Call call : calls) {
      builder.append(call.getRReilAddress());
      builder.append("» ");
    }
    builder.setLength(builder.length() - 2); // remove trailing separator
    return builder.toString();
  }
}