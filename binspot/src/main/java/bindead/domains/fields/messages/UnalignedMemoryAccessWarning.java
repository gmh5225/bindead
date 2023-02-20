package bindead.domains.fields.messages;

import rreil.lang.Field;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domains.fields.Fields;

/**
 * A warning issued by the {@link Fields} domain indicating read-access
 * to a given region at an address that was not byte aligned.
 */
public class UnalignedMemoryAccessWarning extends WarningMessage.Warning {
  private static final String fmt = "Non byte aligned memory access for field: %s";
  private final Field field;

  public UnalignedMemoryAccessWarning (Field field) {
    this.field = field;
  }

  @Override public String message () {
    return String.format(fmt, field);
  }
}
