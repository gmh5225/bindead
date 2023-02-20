package bindead.exceptions;

import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rval;
import bindead.data.NumVar;

/**
 * Base exception class for domain operations.
 */
public abstract class DomainStateException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DomainStateException (String message) {
    super(message);
  }

  public static class UnimplementedMethodException extends DomainStateException {
    private static final long serialVersionUID = 1L;

    public UnimplementedMethodException () {
      super("Unimplemented method");
    }

    public UnimplementedMethodException (String what) {
      super("Unimplemented method: " + what);
    }

  }

  public static class InvariantViolationException extends DomainStateException {
    private static final long serialVersionUID = 1L;

    public InvariantViolationException () {
      this("");
    }

    public InvariantViolationException (String message) {
      super("Invariant violated: " + message);
    }
  }

  public static class VariableSupportSetException extends DomainStateException {

    private static final long serialVersionUID = 1L;

    public VariableSupportSetException () {
      this("Inconsistent variable support set detected");
    }

    public VariableSupportSetException (Rval variable) {
      this("Unkown variable " + variable);
    }

    public VariableSupportSetException (NumVar variable) {
      this("Unkown variable " + variable);
    }

    public VariableSupportSetException (Lin target) {
      this("Some unkown variable " + target);
    }

    public VariableSupportSetException (String string) {
      super(string);
    }
  }

}
