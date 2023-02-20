package bindead.domains.segments.warnings;

import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * Denotes a pointer that would be too imprecise.
 *
 * @author Bogdan Mihaila
 */
public class IllegalPointerArithmetics extends StateRestrictionWarning {
  private static final String fmt = "Illegal Pointer Arithmetics in %s";
  private final String msg;

  public IllegalPointerArithmetics (String string) {
    this.msg = string;
  }

  @Override public String message () {
    return String.format(fmt, msg);
  }

}
