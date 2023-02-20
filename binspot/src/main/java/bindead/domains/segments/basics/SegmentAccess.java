package bindead.domains.segments.basics;

import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domains.segments.SegMem;

public class SegmentAccess<D extends MemoryDomain<D>> {
  public final AbstractMemPointer location;
  public final SegMem<D> state;

  public SegmentAccess (AbstractMemPointer location, SegMem<D> state) {
    this.location = location;
    this.state = state;
  }

  @Override public String toString () {
    return "Access<" + location + ">\ninState: " + state;
  }
}
