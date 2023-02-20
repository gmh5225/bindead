package bindead.domainnetwork.channels;

/**
 * Base class for all warnings signaled during the analysis.
 */
public abstract class WarningMessage {
  private static final String $InfoPrefix = "#II";
  private static final String $WarningPrefix = "#WW";
  private static final String $StateRestrictionWarningPrefix = "#EE";

  private WarningMessage () {
  }

  public abstract String message ();

  public abstract String detailedMessage ();

  @Override public String toString () {
    return detailedMessage();
  }

  /**
   * Class of purely informative messages. Continuing the analysis after an
   * info-message has no influence on soundness or what so ever.
   */
  public static abstract class Info extends WarningMessage {
    @Override public String detailedMessage () {
      return $InfoPrefix + ": " + message();
    }
  }

  /**
   * Class of warning messages. A {@code Warning} is raised if unexpected
   * behavior was encountered, e.g. the return address was modified by the
   * callee but still points to a valid code-location. Continuing the analysis
   * after a warning message has no influence on soundness of the analysis.
   */
  public static abstract class Warning extends WarningMessage {
    @Override public String detailedMessage () {
      return $WarningPrefix + ": " + message();
    }
  }

  /**
   * Class of warning messages which indicate that for the analysis to continue
   * certain assumptions had to be made. For example the interval domain may
   * raise a division-by-zero warning if the operation `z = a / b` with I(b) = [-1, 1]
   * is to be evaluated. Continuing the analysis without issuing a
   * {@code StateRestrictionWarninig} would render the analysis unsound. In
   * this example the analysis would progresses by applying the assumption `b != 0.`
   */
  public static abstract class StateRestrictionWarning extends WarningMessage {
    @Override public String detailedMessage () {
      return $StateRestrictionWarningPrefix + ": " + message();
    }
  }
}
