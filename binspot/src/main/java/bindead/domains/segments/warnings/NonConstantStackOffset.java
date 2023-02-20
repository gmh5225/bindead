package bindead.domains.segments.warnings;

import javalx.numeric.Range;
import bindead.data.NumVar;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * The offset of the stack pointer is not constant while some operations
 * need to be performed using the stack pointer.
 *
 * @author Bogdan Mihaila
 */
public class NonConstantStackOffset extends StateRestrictionWarning {
  private static final String fmt = "Stack pointer %s has a non-constant offset %s";
  private final NumVar sp;
  private final Range offset;

  public NonConstantStackOffset (NumVar sp, Range offset) {
    this.sp = sp;
    this.offset = offset;
  }

  @Override public String message () {
    return String.format(fmt, sp, offset);
  }

}
