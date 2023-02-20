package bindead.domains.fields.messages;

import javalx.numeric.Range;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domains.root.Root;

/**
 * A warning issued by the {@link Root} domain indicating read-access to an unknown region.
 */
public class ReadFromUnknownPointerInfo extends WarningMessage.Info {
  private static final String fmt = "Read from unknown pointer with offset: %s";
  private final Range offset;

  public ReadFromUnknownPointerInfo (Range offset) {
    this.offset = offset;
  }

  @Override public String message () {
    return String.format(fmt, offset);
  }
}
