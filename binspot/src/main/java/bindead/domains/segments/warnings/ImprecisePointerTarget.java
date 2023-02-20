package bindead.domains.segments.warnings;

import javalx.numeric.Range;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * Denotes a pointer that would be too imprecise.
 * @author Bogdan Mihaila
 */
public class ImprecisePointerTarget extends StateRestrictionWarning {
  private static final String fmt = "Pointer to %s has imprecise target addresses %s";
  private final AbstractPointer ptr;
  private final Range range;

  public ImprecisePointerTarget (AbstractPointer ptr, Range range) {
    this.ptr = ptr;
    this.range = range;
  }

  @Override public String message () {
    return String.format(fmt, ptr, range);
  }

}
