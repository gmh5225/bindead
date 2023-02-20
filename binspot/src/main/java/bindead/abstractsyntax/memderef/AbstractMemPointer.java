package bindead.abstractsyntax.memderef;

import javalx.numeric.Range;
import rreil.lang.MemVar;
import bindead.data.Linear;
import bindead.domainnetwork.channels.QueryChannel;

public class AbstractMemPointer {
  public final MemVar region;
  public final DerefOffset offset;

  public AbstractMemPointer (MemVar region, DerefOffset offset) {
    this.region = region;
    this.offset = offset;
  }

  public AbstractMemPointer addOfs (Linear ofs) {
    return new AbstractMemPointer(region, offset.add(ofs));
  }

  @Override public String toString () {
    return "[" + region +":" + offset + "]";
  }

  public <D extends QueryChannel> Range getOffsetRange (D childState) {
    return offset.getRange(childState);
  }
}
