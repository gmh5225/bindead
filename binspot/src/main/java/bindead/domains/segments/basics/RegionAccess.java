package bindead.domains.segments.basics;

import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.data.Linear;
import bindead.domainnetwork.interfaces.MemoryDomain;

/**
 * <s resolved access to a memory region. Fields (addr, offset) correspond to the abstract pointer, region is the region
 * that has been found for the abstract address addr.
 *
 * */
public class RegionAccess<D extends MemoryDomain<D>> {

  public final AbstractMemPointer location;
  public final SegmentWithState<D> segstate;

  public RegionAccess (AbstractMemPointer location, SegmentWithState<D> ss) {
    assert location != null;
    this.location = location;
    assert ss != null;
    this.segstate = ss;
  }

  public final RegionAccess<D> addOffset (Linear ofs) {
    return new RegionAccess<D>(location.addOfs(ofs), segstate);
  }

  @Override public String toString () {
    return location.toString();
  }
}
