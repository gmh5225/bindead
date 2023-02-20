package bindead.domains.segments.warnings;

import javalx.numeric.Range;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * Denotes a pointer that would be too imprecise.
 * @author Bogdan Mihaila
 */
public class IllegalNumericPointerTarget extends StateRestrictionWarning {
  private static final String fmt = "Possible access violation at numeric address %s";
  private final Range range;

  public IllegalNumericPointerTarget (Range range) {
    this.range = range;
  }

  @Override public String message () {
    return String.format(fmt, range);
  }

}
