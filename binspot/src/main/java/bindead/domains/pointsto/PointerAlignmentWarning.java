package bindead.domains.pointsto;

import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.domainnetwork.channels.WarningMessage;
import javalx.numeric.BigInt;

/**
 * A warning raised when pointer alignment operations are assumed.
 */
public class PointerAlignmentWarning extends WarningMessage.StateRestrictionWarning {
  private static final String fmt =
    "Assuming that pointer expression <%s> is aligned with respect to the %d bits mask given by '%s'";
  private final Rlin ptr;
  private final String mask;
  private final int size;

  public PointerAlignmentWarning (Rlin pointer, Rlin mask) {
    this.ptr = pointer;
    BigInt constant = mask.getLinearTerm().getConstant();
    this.size = mask.getSize();
    this.mask = constant.toBinaryStringUnsigned(size);
  }

  @Override public String message () {
    return String.format(fmt, ptr, size, mask);
  }
}
