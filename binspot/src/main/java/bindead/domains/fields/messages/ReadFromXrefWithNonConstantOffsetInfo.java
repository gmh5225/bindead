package bindead.domains.fields.messages;

import javalx.numeric.Range;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domains.fields.Fields;

/**
 * A warning issued by the {@link Fields} domain indicating read-access to a given region with non-constant memory
 * offset.
 */
public class ReadFromXrefWithNonConstantOffsetInfo extends WarningMessage.Info {
  private static final String fmt = "Read from xref: %s with non-constant offset: %s";
  private final AbstractMemPointer location;
  private final Range offset;

  public ReadFromXrefWithNonConstantOffsetInfo (AbstractMemPointer location, Range offset) {
    this.location = location;
    this.offset = offset;
  }

  /**
   * {@inheritDoc}
   */
  @Override public String message () {
    return String.format(fmt, location, offset);
  }
}
