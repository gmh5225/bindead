package bindead.domains.fields.messages;

import rreil.lang.MemVar;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domains.fields.Fields;

/**
 * A warning issued by the {@link Fields} domain indicating write-access to a given region that is not writable.
 */
public class WriteToReadOnlySegmentWarning extends WarningMessage.StateRestrictionWarning {
  private static final String fmt = "Write to read-only region for field: %s";
  private final MemVar region;

  public WriteToReadOnlySegmentWarning (MemVar region) {
    this.region = region;
  }

  @Override public String message () {
    return String.format(fmt, region.toString());
  }
}
