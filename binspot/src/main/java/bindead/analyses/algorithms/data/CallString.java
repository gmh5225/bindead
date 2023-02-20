package bindead.analyses.algorithms.data;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import rreil.lang.RReilAddr;
import bindead.exceptions.DomainStateException.InvariantViolationException;

/**
 * Represents a sequence of calls that describe the path how a procedure was reached. A Call-String can be limited in
 * that only a certain number of calls are significant to identify the path to a procedure and the others are ignored. If
 * the Call-String is limited by k then the last k-calls are the significant ones.
 */
public class CallString implements Cloneable {
  private static final int defaultCallStringLength = 50;
  private final int maxSignificantLength;
  private final LinkedList<Transition> backLogString = new LinkedList<Transition>();
  private final LinkedList<Transition> significantString = new LinkedList<Transition>();

  /**
   * Build a new Call-String object bounded by a maximum length for the significant string part.
   *
   * @param k The maximum length of the right-most significant string part
   * @return A new Call-String object
   */
  private static CallString withMaxSignificantLength (int k) {
    return new CallString(k);
  }

  /**
   * Build a new Call-String object bounded by a maximum length for the significant string part.
   *
   * @param k The maximum length of the right-most significant string part
   * @return A new Call-String object
   */
  public static CallString root (int k) {
    return withMaxSignificantLength(k);
  }

  /**
   * Build a new Call-String object bounded by a default maximum length for the significant string part.
   *
   * @return A new Call-String object
   */
  public static CallString root () {
    return withMaxSignificantLength(defaultCallStringLength);
  }

  private CallString (int maxSignificantLength) {
    if (maxSignificantLength < 0)
      maxSignificantLength = 0;
    this.maxSignificantLength = maxSignificantLength;
  }

  @Override protected CallString clone () {
    return new CallString(this);
  }

  private CallString (CallString other) {
    this.maxSignificantLength = other.maxSignificantLength;
    this.significantString.addAll(other.significantString);
    this.backLogString.addAll(other.backLogString);
  }

  public CallString pop (Transition transition) {
    CallString newCallString = clone();
    assert newCallString.size() > 0;
    Transition lastTransition;
    if (maxSignificantLength == 0) {
      lastTransition = newCallString.backLogString.removeLast();
    } else {
      if (newCallString.backLogString.size() > 0) {
        assert newCallString.significantString.size() == maxSignificantLength;
        lastTransition = newCallString.significantString.removeLast();
        Transition leastSignificantCall = newCallString.backLogString.removeLast();
        newCallString.significantString.addFirst(leastSignificantCall);
      } else {
        lastTransition = newCallString.significantString.removeLast();
      }
    }

    if (!lastTransition.equals(transition))
      throw new InvariantViolationException();
    return newCallString;
  }

  public CallString unsafePop () {
    CallString newCallString = clone();
    assert newCallString.size() > 0;
    if (maxSignificantLength == 0) {
      newCallString.backLogString.removeLast();
    } else {
      if (newCallString.backLogString.size() > 0) {
        assert newCallString.significantString.size() == maxSignificantLength;
        newCallString.significantString.removeLast();
        Transition leastSignificantCall = newCallString.backLogString.removeLast();
        newCallString.significantString.addFirst(leastSignificantCall);
      } else {
        newCallString.significantString.removeLast();
      }
    }
    return newCallString;
  }

  public Transition peek () {
    return significantString.peekLast();
  }

  public CallString push (Transition transition) {
    CallString newCallString = clone();
    if (maxSignificantLength == 0) {
      newCallString.backLogString.addLast(transition);
    } else {
      if (newCallString.size() > maxSignificantLength) {
        Transition leastSignificantCall = newCallString.significantString.removeFirst();
        newCallString.backLogString.addLast(leastSignificantCall);
      }
      newCallString.significantString.addLast(transition);
    }
    return newCallString;
  }

  public int size () {
    return significantString.size() + backLogString.size();
  }

  public boolean isRoot () {
    return size() == 0;
  }

  public List<Transition> getSignificantTransitions () {
    return Collections.unmodifiableList(significantString);
  }

  public List<Transition> getAllTransitions () {
    List<Transition> all = new LinkedList<Transition>();
    all.addAll(backLogString);
    all.addAll(significantString);
    return Collections.unmodifiableList(all);
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + (significantString == null ? 0 : significantString.hashCode());
    result = prime * result + maxSignificantLength;
    return result;
  }

  /**
   * This Call-String equals another Call-String if their significant calls are the same.
   *
   * @param obj The other Call-String object to be compared to
   * @return {@code true} if this and the other object are equal
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
    return significantString.toString();
  }

  /**
   * Print a pretty version of this Call-String.
   *
   * @param maxDisplayableCalls How many calls should be displayed. The rest will be displayed as "...".
   * @return A prettier version of the call paths in this Call-String.
   */
  public String pretty (int maxDisplayableCalls) {
    StringBuilder builder = new StringBuilder();
    if (getSignificantTransitions().isEmpty()) {
      return "";
    }
    List<Transition> transitions = getSignificantTransitions();
    if (transitions.size() > maxDisplayableCalls) {
      int callsSize = transitions.size();
      transitions = transitions.subList(callsSize - maxDisplayableCalls, callsSize);
      builder.append("(" + (callsSize - maxDisplayableCalls) + " more) [...] ");
    }
    for (Transition transition : transitions) {
      builder.append(transition.getTarget());
      builder.append("» ");
    }
    builder.setLength(builder.length() - 2); // remove trailing separator
    return builder.toString();
  }

  public static class Transition {
    private final RReilAddr source;
    private final RReilAddr target;

    public Transition (RReilAddr a, RReilAddr b) {
      this.source = a;
      this.target = b;
    }

    public RReilAddr getSource () {
      return source;
    }

    public RReilAddr getTarget () {
      return target;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      return builder.append('<').append(source).append("»").append(target).append('>').toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((source == null) ? 0 : source.hashCode());
      result = prime * result + ((target == null) ? 0 : target.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Transition))
        return false;
      Transition other = (Transition) obj;
      if (source == null) {
        if (other.source != null)
          return false;
      } else if (!source.equals(other.source))
        return false;
      if (target == null) {
        if (other.target != null)
          return false;
      } else if (!target.equals(other.target))
        return false;
      return true;
    }

  }
}