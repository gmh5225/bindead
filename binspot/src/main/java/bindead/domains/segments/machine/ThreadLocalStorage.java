package bindead.domains.segments.machine;

import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.Rhs.Lin;
import rreil.lang.util.Type;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.abstractsyntax.memderef.SymbolicOffset;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.segments.basics.RegionAccess;
import bindead.domains.segments.basics.SegCompatibleState;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.exceptions.DomainStateException.InvariantViolationException;

/**
 * A single segment that represents thread-local storage.
 *
 * @author Axel Simon
 */
public class ThreadLocalStorage<D extends MemoryDomain<D>> extends Segment<D> {
  private final MemVar tlsRegion;
  private final AddrVar tlsAddr;

  public static final String NAME = "ThreadLocalStorage";

  public ThreadLocalStorage () {
    this.tlsRegion = MemVar.fresh("thread-local storage");
    this.tlsAddr = NumVar.freshAddress("thread-local storage");
  }

  @Override public P3<List<MemVar>, Boolean, D> initialize (D state) {
    state = state.introduceRegion(tlsRegion, RegionCtx.EMPTYSTICKY);
    state = state.introduce(tlsAddr, Type.Address, Option.<BigInt>none());
    return P3.<List<MemVar>, Boolean, D>tuple3(new LinkedList<MemVar>(), Boolean.FALSE, state);
  }

  @Override public SegmentWithState<D> triggerAssignment (Lhs lhs, rreil.lang.Rhs rhs, D state) {
    throw new InvariantViolationException();
  }

  @Override public SegCompatibleState<D> makeCompatible (Segment<D> otherRaw, D state, D otherState) {
    ThreadLocalStorage<D> other = (ThreadLocalStorage<D>) otherRaw;
    assert tlsRegion.equals(other.tlsRegion);
    assert tlsAddr.equalTo(other.tlsAddr);
    return new SegCompatibleState<D>(this, state, otherState);
  }

  @Override public List<RegionAccess<D>> dereference (Lin sourcePointerValue, AbstractPointer dRef, D state) {
    List<RegionAccess<D>> res = null;
    if (!dRef.isAbsolute() && dRef.address.equalTo(tlsAddr)) {
      res = new LinkedList<RegionAccess<D>>();
      res.add(new RegionAccess<D>(new AbstractMemPointer(tlsRegion, new SymbolicOffset(dRef.offset)),
        new SegmentWithState<D>(this, state)));
    }
    return res;
  }

  @Override public MemVarSet getChildSupportSet () {
    return MemVarSet.of(tlsRegion);
  }

  @Override public void toCompactString (StringBuilder builder, PrettyDomain childDomain) {
    throw new UnimplementedException();
  }

}
