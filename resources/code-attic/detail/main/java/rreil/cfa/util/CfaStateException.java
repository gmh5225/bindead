package rreil.cfa.util;

/**
 * Thrown to indicate that the CFA was in an unexpected or inconsistent state while trying to perform an operation on it
 * or that the CFA did not withhold a necessary invariant.
 * @author Bogdan Mihaila
 */
public class CfaStateException extends RuntimeException {
  private final ErrObj errorClass;
  private final String errorMessage;

  public CfaStateException (ErrObj errorClass) {
    this(errorClass, null);
  }

  public CfaStateException (ErrObj errorClass, String additionalErrorMessage) {
    this.errorClass = errorClass;
    this.errorMessage = additionalErrorMessage;
  }

  @Override public String getMessage () {
    if (errorMessage != null)
      return errorClass.toString() + ": " + errorMessage;
    else
      return errorClass.toString();
  }

  public static enum ErrObj {
    EMPTY_BLOCK,
    INVARIANT_FAILURE,
    MISSING_JUMP_TARGET,
    TOO_MANY_JUMP_TARGETS
  }
}
