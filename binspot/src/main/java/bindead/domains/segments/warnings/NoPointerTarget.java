package bindead.domains.segments.warnings;

import rreil.lang.Rhs.Rval;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * This warning is emitted when no target for a pointer is found,
 * which is conceptually broken, because the DataSegment domain
 * should tell NULL pointers from constant accesses
 *
 * @author Axel Simon
 *
 */
public class NoPointerTarget extends StateRestrictionWarning {
  private static final String fmt = "Expression %s does not refer to any known target.";
  private final Rval ptr;

  public NoPointerTarget (Rval value) {
    this.ptr = value;
  }

  @Override public String message () {
    return String.format(fmt, ptr);
  }

}
