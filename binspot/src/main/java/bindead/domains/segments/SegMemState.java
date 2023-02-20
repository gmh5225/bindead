package bindead.domains.segments;

import java.util.List;
import java.util.Map;

import javalx.data.products.P3;
import rreil.lang.MemVar;
import bindead.data.MemVarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domains.segments.basics.Segment;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Store the segment sets that comprise the state of the program.
 *
 * @author Axel Simon
 */
public class SegMemState<D extends MemoryDomain<D>> extends FunctorState {

  private final Segment<D>[] segments;

  final MemVarSet explicitlyIntroducedRegions;

  public SegMemState (Segment<D>[] segments, MemVarSet explicitlyIntroducedRegions) {
    this.segments = segments;
    this.explicitlyIntroducedRegions = explicitlyIntroducedRegions;
    assert explicitlyIntroducedRegions != null;
  }

  public D initialize (Map<MemVar, Integer> triggers, D state) {
    for (int i = 0; i < segments.length; i++) {
      final P3<List<MemVar>, Boolean, D> triple = segments[i].initialize(state);
      state = triple._3();
      for (final MemVar v : triple._1()) {
        triggers.put(v, i);
      }
    }
    return state;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    return builder;
  }

  @Override public String toString () {
    final StringBuilder builder = new StringBuilder();
    for (final Segment<D> segment : segments) {
      builder.append(segment).append("\n");
    }
    builder.setLength(builder.length() - 1); // remove last newline
    return builder.toString();
  }

  Segment<D>[] cloneSegments () {
    return segments.clone();
  }

  MemVarSet getChildSupportSet () {
    MemVarSet css = explicitlyIntroducedRegions;
    for (Segment<D> s : segments)
      css = css.insertAll(s.getChildSupportSet());
    return MemVarSet.from(css);
  }

  int size () {
    return segments.length;
  }

  /**
   * Return a new segment state with the given segment replaced by a new one.
   */
  SegMemState<D> setSegmentAt (int i, Segment<D> segment) {
    Segment<D>[] segments = cloneSegments();
    segments[i] = segment;
    return new SegMemState<D>(segments, explicitlyIntroducedRegions);
  }

  Segment<D> get (int i) {
    return segments[i];
  }

  public boolean edgenodesAreSane () {
    for (Segment<D> s : segments)
      if (!s.connectorsAreSane())
        return false;
    return true;
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    for (Segment<?> seg : segments) {
      seg.toCompactString(builder, childDomain);
    }
  }
}
