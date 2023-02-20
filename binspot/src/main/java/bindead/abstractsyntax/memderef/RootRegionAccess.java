package bindead.abstractsyntax.memderef;

import javalx.numeric.Range;
import rreil.lang.MemVar;
import bindead.data.NumVar.AddrVar;

public class RootRegionAccess {

  public final AbstractMemPointer location;
  public final AddrVar addr;

  public RootRegionAccess (AddrVar addr, AbstractMemPointer location) {
    this.addr = addr;
    assert location != null;
    this.location = location;
  }

  public MemVar getRegion () {
    return location.region;
  }

  /**
   * Create a memory dereference object for a pointer that is accessed at explicitly given offsets
   *
   * @param region the region that is being accessed
   * @param offset the explicit offset
   * @return a memory dereference object
   */
  public static RootRegionAccess explicit (MemVar region, Range offset) {
    return new RootRegionAccess(null, new AbstractMemPointer(region, new ExplicitOffset(offset)));
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append(location.toString());
    if (addr != null)
      builder.append("@").append(addr.toString());
    return builder.toString();
  }


}
