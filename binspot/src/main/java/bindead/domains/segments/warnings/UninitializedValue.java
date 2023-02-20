package bindead.domains.segments.warnings;

import bindead.data.NumVar;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * Denotes a pointer that would be too imprecise.
 * @author Bogdan Mihaila
 */
public class UninitializedValue extends StateRestrictionWarning {
  private static final String fmt = "Tried to access uninitialized value %s";
  private final NumVar ptr;

  public UninitializedValue (NumVar v) {
    this.ptr = v;
  }

  @Override public String message () {
    return String.format(fmt, ptr);
  }

}
