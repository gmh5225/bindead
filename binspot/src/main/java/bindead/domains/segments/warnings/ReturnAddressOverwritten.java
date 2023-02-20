package bindead.domains.segments.warnings;

import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * A warning about overwriting the return address of a stack frame with an array access.
 *
 * @author Axel Simon
 */
public class ReturnAddressOverwritten extends StateRestrictionWarning {
  private static final String fmt = "Buffer overflow: access to %s overwrites return address on previous stack frame.";
  private final AbstractPointer ptr;

  public ReturnAddressOverwritten (AbstractPointer ptr) {
    this.ptr = ptr;
  }

  @Override public String message () {
    return String.format(fmt, ptr);
  }
}
