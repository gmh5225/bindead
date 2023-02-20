package bindead.domains.segments.warnings;

import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * A warning about an array access that goes beyond the first stack frame of the program.
 *
 * @author Axel Simon
 */
public class StackUnderflow extends StateRestrictionWarning {
  private static final String fmt = "Stack underflow: access to %s goes beyond the beginning of the first stack frame.";
  private final AbstractPointer ptr;

  public StackUnderflow (AbstractPointer ptr) {
    this.ptr = ptr;
  }

  @Override public String message () {
    return String.format(fmt, ptr);
  }

}
