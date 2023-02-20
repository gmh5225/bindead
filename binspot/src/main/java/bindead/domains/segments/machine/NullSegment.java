package bindead.domains.segments.machine;

import java.util.LinkedList;
import java.util.List;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.Range;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Lin;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.data.MemVarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domains.segments.basics.RegionAccess;
import bindead.domains.segments.basics.SegCompatibleState;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.domains.segments.warnings.IllegalNumericPointerTarget;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.Unreachable;

/**
 * A set of fixed-sized segments backed by some data blob. Each segment is either located at a fixed, absolute address or
 * it is relative to some symbolic address.
 *
 * @author Axel Simon
 */
public class NullSegment<D extends MemoryDomain<D>> extends Segment<D> {
  public static final String NAME = "Null";

  public NullSegment () {
  }

  @Override public P3<List<MemVar>, Boolean, D> initialize (D state) {
    return P3.<List<MemVar>, Boolean, D>tuple3(new LinkedList<MemVar>(), Boolean.FALSE, state);
  }

  @Override public SegmentWithState<D> triggerAssignment (Lhs lhs, Rhs rhs, D state) {
    throw new InvariantViolationException();
  }

  @Override public List<P2<RReilAddr, SegmentWithState<D>>> resolveJump (Lin target, AbstractMemPointer location, D state) {
    warn(state, location.getOffsetRange((QueryChannel) state));
    return null;
  }

  /**
   * flag an error for accesses at numeric locations
   *
   * @return null as it contains no targets
   * */
  @Override public List<RegionAccess<D>> dereference (Lin sourcePointerValue, AbstractPointer pointer, D state) {
    if (pointer.isAbsolute()) {
      try {
        state = state.assumePointsToAndConcretize(sourcePointerValue, null, null);
        warn(state, pointer.getExplicitOffset(state));
      } catch (Unreachable _) {
      }
    }
    return null;
  }

  private void warn (D state, Range accessRange) {
    state.getContext().addWarning(new IllegalNumericPointerTarget(accessRange));
  }

  @Override public SegCompatibleState<D> makeCompatible (Segment<D> otherRaw, D state, D otherState) {
    return new SegCompatibleState<D>(this, state, otherState);
  }

  @Override public SegmentWithState<D> tryPrimitive (PrimOp prim, D state) {
    return null;
  }

  @Override public NullSegment<D> introduceRegion (MemVar region, ContentCtx ctx) {
    return new NullSegment<D>();
  }

  @Override public String toString () {
    return NAME + ": --";
  }

  @Override public MemVarSet getChildSupportSet () {
    return MemVarSet.empty();
  }

  @Override public void toCompactString (StringBuilder builder, PrettyDomain childDomain) {
  }

}
