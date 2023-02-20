package bindead.domains.segments.warnings;

import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * A warning about an array access that straddles the dynamic and the static part of a stack frame.
 *
 * @author Axel Simon
 */
public class BufferOverflow extends StateRestrictionWarning {
  private static final String fmt = "Buffer overflow: access to %s overwrites parameters of function.";
  private final AbstractPointer ptr;

  public BufferOverflow (AbstractPointer ptr) {
    this.ptr = ptr;
  }

  @Override public String message () {
    return String.format(fmt, ptr);
  }

}
