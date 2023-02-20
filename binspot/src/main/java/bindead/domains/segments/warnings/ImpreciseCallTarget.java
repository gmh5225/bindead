package bindead.domains.segments.warnings;

import javalx.numeric.Range;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * @author Axel Simon
 */
public class ImpreciseCallTarget extends StateRestrictionWarning {
  private static final String fmt = "branch to %s has imprecise target addresses %s";
  private final AbstractMemPointer location;
  private final Range range;

  public ImpreciseCallTarget (AbstractMemPointer location, Range range) {
    this.location = location;
    this.range = range;
  }

  @Override public String message () {
    return String.format(fmt, location, range);
  }

}
