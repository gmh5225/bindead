package bindead.domains.segments.basics;

import bindead.domainnetwork.interfaces.MemoryDomain;

public class SegmentWithState<D extends MemoryDomain<D>> {
  public final Segment<D> segment;
  public final D state;

  public SegmentWithState (Segment<D> seg, D child) {
    segment = seg;
    state = child;
  }

  @Override public String toString () {
    return toCompactString();
//    return "***Segment: " + segment + "\n***State: " + state;
  }

  public String toCompactString() {
    StringBuilder builder = new StringBuilder();
    segment.toCompactString(builder, state);
    state.toCompactString(builder);
    return builder.toString();
  }
}
