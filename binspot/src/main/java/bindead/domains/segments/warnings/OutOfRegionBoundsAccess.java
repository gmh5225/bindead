package bindead.domains.segments.warnings;

import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.domainnetwork.channels.WarningMessage.StateRestrictionWarning;

/**
 * A warning about an access that goes beyond the bounds of a region.
 *
 * @author Bogdan Mihaila
 */
public abstract class OutOfRegionBoundsAccess extends StateRestrictionWarning {
  protected AbstractMemPointer location;

  public OutOfRegionBoundsAccess (AbstractMemPointer location) {
    this.location = location ;
  }

  public static class Below extends OutOfRegionBoundsAccess {
    private static final String fmt = "Access at %s goes before the beginning of the region.";

    public Below (AbstractMemPointer location) {
      super(location);
    }

    @Override public String message () {
      return String.format(fmt, location);
    }
  }

  public static class Above extends OutOfRegionBoundsAccess {
    private static final String fmt = "Access at %s goes beyond the end of the region.";

    public Above (AbstractMemPointer location) {
      super(location);
    }

    @Override public String message () {
      return String.format(fmt, location);
    }
  }
}
