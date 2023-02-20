package bindead.domains.segments.warnings;

import rreil.lang.RReilAddr;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

public class UnknownStackFrame extends StateRestrictionWarning {
  private static final String fmt = "The jump target %s does not belong to an already known stack frame.";
  private final RReilAddr target;

  public UnknownStackFrame (RReilAddr target) {
    this.target = target;
  }

  @Override public String message () {
    return String.format(fmt, target);
  }

}
