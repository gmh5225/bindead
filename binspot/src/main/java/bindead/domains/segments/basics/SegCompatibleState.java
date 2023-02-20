package bindead.domains.segments.basics;

import bindead.domainnetwork.interfaces.MemoryDomain;


public class SegCompatibleState<D extends MemoryDomain<D>> {
  public final Segment<D> segment;
  public final D leftState;
  public final D rightState;

  public SegCompatibleState (Segment<D> seg, D leftState, D rightState) {
    this.segment = seg;
    this.leftState = leftState;
    this.rightState = rightState;
  }

  @Override public String toString () {
    return "\nSegCompatibleState:\nSegment:\n" + segment + "\n\nleft state:\n" + leftState + "\n\nright state: "
      + rightState;
  }
}
