package bindead.domains.segments.basics;

import rreil.lang.RReilAddr;
import bindead.domainnetwork.interfaces.MemoryDomain;

public class JumpTarget<D extends MemoryDomain<D>> {
  public final RReilAddr address;
  public final SegmentWithState<D> segstate;

  public JumpTarget (RReilAddr a, SegmentWithState<D> s) {
    address = a;
    segstate = s;
  }
}
