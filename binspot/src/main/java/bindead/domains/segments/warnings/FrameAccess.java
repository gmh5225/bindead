package bindead.domains.segments.warnings;

import bindead.data.NumVar;
import bindead.domainnetwork.channels.WarningMessage.Warning;

/**
 * Warns when dereferencing a predecessor frame and we could not set the
 * flag that points to it. Thus accessing variables from that frame afterwards may result in
 * overapproximated or undefined values as it was not possible to restrict the state to the certain predecessor frame.
 *
 * @author Bogdan Mihaila
 */
public class FrameAccess extends Warning {
  private static final String fmt = "Trying to access the predecessor frame by setting the flag %s yielded ⊥";
  private final NumVar flag;

  public FrameAccess (NumVar flag) {
    this.flag = flag;
  }

  @Override public String message () {
    return String.format(fmt, flag);
  }

}
