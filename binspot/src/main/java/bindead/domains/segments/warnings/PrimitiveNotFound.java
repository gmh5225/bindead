package bindead.domains.segments.warnings;

import rreil.lang.RReil.PrimOp;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * @author Axel Simon
 */
public class PrimitiveNotFound extends StateRestrictionWarning {
  private final PrimOp primitive;

  public PrimitiveNotFound (PrimOp primitive) {
    this.primitive = primitive;
  }

  @Override public String message () {
    StringBuilder builder = new StringBuilder();
    builder.append("Primitive ");
    builder.append(primitive);
    builder.append(" not found.");
    return builder.toString();
  }

}
