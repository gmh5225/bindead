package bindead.domains.segments.machine;

import java.util.LinkedList;
import java.util.List;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.FiniteRange;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.tree.FiniteRangeTree;
import javalx.persistentcollections.tree.OverlappingRanges;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Lin;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.abstractsyntax.memderef.DerefOffset;
import bindead.abstractsyntax.memderef.ExplicitOffset;
import bindead.abstractsyntax.memderef.SymbolicOffset;
import bindead.data.Linear;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domains.segments.basics.RegionAccess;
import bindead.domains.segments.basics.SegCompatibleState;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.domains.segments.warnings.IllegalNumericPointerTarget;
import bindead.domains.segments.warnings.ImpreciseCallTarget;
import bindead.domains.segments.warnings.OutOfRegionBoundsAccess;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.Unreachable;

/**
 * A set of fixed-sized segments backed by some data blob. Each segment is either located at a fixed, absolute address or
 * it is relative to some symbolic address.
 *
 * @author Axel Simon
 */
public class DataSegments<D extends MemoryDomain<D>> extends Segment<D> {
  private static final int maxJumpTargets = 100;
  public static final String NAME = "Data";
  private static final FiniteFactory fin = FiniteFactory.getInstance();

  /**
   * A map from symbolic addresses to pairs of region size (in bytes) and region id.
   */
  // REFACTOR store proper objects in collections: P2<BigInt, MemVar> => DataSegmentInfo
  private final AVLMap<AddrVar, P2<BigInt, MemVar>> symbolic;
  /**
   * Absolute regions with the intervals, representing valid addresses (in bytes) of the region.
   */
  private final AbsoluteAddresses absolute;

  /**
   * A map of regions that hasn't been used either as address nor as absolute region.
   */
  private final AVLMap<MemVar, ContentCtx> pending;

  public DataSegments () {
    symbolic = AVLMap.empty();
    absolute = AbsoluteAddresses.empty();
    pending = AVLMap.empty();
  }

  private DataSegments (AVLMap<AddrVar, P2<BigInt, MemVar>> addresses,
      AbsoluteAddresses absolute, AVLMap<MemVar, ContentCtx> pending) {
    this.symbolic = addresses;
    this.absolute = absolute;
    this.pending = pending;
  }

  @Override public P3<List<MemVar>, Boolean, D> initialize (D state) {
    return P3.<List<MemVar>, Boolean, D>tuple3(new LinkedList<MemVar>(), Boolean.FALSE, state);
  }

  @Override public SegmentWithState<D> triggerAssignment (Lhs lhs, Rhs rhs, D state) {
    throw new InvariantViolationException();
  }

  @Override public List<P2<RReilAddr, SegmentWithState<D>>> resolveJump (Lin target, AbstractMemPointer location,
      D state) {
    final List<P2<RReilAddr, SegmentWithState<D>>> targets = new LinkedList<>();
    final Range offsets = location.getOffsetRange(state);
    // FIXME: see if we can express the non-convex approximation for jumps in the intervals/sets datatype
    // and thus use it as a criteria for resolving jumps here
    // if (offsets.isNonConvexApproximation() && offsets.numberOfDiscreteValues().isLessThan(maxJumpTargets)) {
    if (offsets.numberOfDiscreteValues().isLessThan(BigInt.of(maxJumpTargets))) {
      assert absolute.contains(location.region) : "Code must be in absolute regions for now.";
      BigInt baseAddress = absolute.startAddressOf(location.region);
      for (BigInt offset : offsets) {
        BigInt absoluteAddress = baseAddress.add(offset);
        D jumpedToState = state;
        // FIXME: introduce Tests in RREIL that contain expressions instead of Rvars and then
        // apply the that test here by using the Lin from above
//        if (target instanceof Rvar) {
//          // restrict the variable value to the chosen target address
//          FiniteRange access = FiniteRange.of(0, target.getSize() - 1);
//          Option<NumVar> targetVariable = state.pickSpecificField(((Rvar) target).getRegionId(), access);
//          if (targetVariable.isSome()) {
//            FiniteFactory finite = FiniteFactory.getInstance();
//            Test test = finite.equalTo(target.getSize(), linear(targetVariable.get()), linear(absoluteAddress));
//            jumpedToState = state.eval(test);
//          }
//        }
        SegmentWithState<D> inState = new SegmentWithState<>(this, jumpedToState);
        targets.add(P2.<RReilAddr, SegmentWithState<D>>tuple2(RReilAddr.valueOf(absoluteAddress), inState));
      }
    } else {
      state.getContext().addWarning(new ImpreciseCallTarget(location, offsets));
    }
    return targets;
  }

  private void assertAccessInBounds (BigInt lower, BigInt upper, AbstractMemPointer location, final D state) {
    Linear belowBound = location.offset.upperBound();
    Linear aboveBound = location.offset.lowerBound();
    try {
      if (belowBound != null)
        state.eval(fin.unsignedLessThan(0, belowBound, Linear.linear(lower)));
      state.getContext().addWarning(new OutOfRegionBoundsAccess.Below(location));
    } catch (Unreachable _) {
    }
    try {
      if (aboveBound != null)
        state.eval(fin.unsignedLessThanOrEqualTo(0, Linear.linear(upper), aboveBound));
      state.getContext().addWarning(new OutOfRegionBoundsAccess.Above(location));
    } catch (Unreachable _) {
    }
  }

  /**
   * @return null if this segment finds no target for pointer
   * */
  @Override public List<RegionAccess<D>> dereference (Lin sourcePointerValue, AbstractPointer pointer, D state) {
    try {
      if (pointer.isAbsolute())
        return resolveAbsolute(sourcePointerValue, pointer, state);
      else
        return resolveSymbolic(sourcePointerValue, pointer, state);
    } catch (Unreachable _) {
      return null;
    }
  }

  private List<RegionAccess<D>> resolveAbsolute (Lin sourcePointerValue, AbstractPointer pointer, D state) {
    state = state.assumePointsToAndConcretize(sourcePointerValue, null, null);
    Range accessRange = pointer.getExplicitOffset(state);
    OverlappingRanges<MemVar> overlapping = absolute.searchOverlaps(accessRange.convexHull());
    if (overlapping.size() != 1 || !accessRange.isFinite()) {
      state.getContext().addWarning(new IllegalNumericPointerTarget(accessRange));
      return null;
    }
    P2<FiniteRange, MemVar> overlap = overlapping.getFirst();
    FiniteRange regionRange = overlap._1();
    BigInt regionStartAddress = regionRange.low().asInteger();
    MemVar region = overlap._2();
    Range accessOffset = accessRange.sub(regionStartAddress);
    DerefOffset offset = new ExplicitOffset(accessOffset);
    BigInt regionSize = regionRange.getSpan();
    return singleAccess(state, region, offset, regionSize);
  }

  private List<RegionAccess<D>> resolveSymbolic (Lin sourcePointerValue, AbstractPointer pointer, D state) {
    if (!symbolic.contains(pointer.address))
      return null;
    state = state.assumePointsToAndConcretize(sourcePointerValue, pointer.address, null);
    P2<BigInt, MemVar> tuple = symbolic.get(pointer.address).get();
    MemVar region = tuple._2();
    DerefOffset offset = new SymbolicOffset(pointer.offset);
    BigInt regionSize = tuple._1();
    return singleAccess(state, region, offset, regionSize);
  }

  private List<RegionAccess<D>> singleAccess (D state, MemVar region, DerefOffset offset, BigInt regionSize) {
    AbstractMemPointer location = new AbstractMemPointer(region, offset);
    assertAccessInBounds(Bound.ZERO, regionSize, location, state);
    RegionAccess<D> access = new RegionAccess<D>(location, new SegmentWithState<D>(this, state));
    return singleton(access);
  }

  private List<RegionAccess<D>> singleton (RegionAccess<D> access) {
    List<RegionAccess<D>> result = new LinkedList<RegionAccess<D>>();
    result.add(access);
    return result;
  }

  @Override public SegCompatibleState<D> makeCompatible (Segment<D> otherRaw, D state, D otherState) {
    DataSegments<D> other = (DataSegments<D>) otherRaw;
    // if all data segments are created up-front, this suffices
    assert symbolic == other.symbolic;
    assert absolute == other.absolute;
    assert pending == other.pending;
    return new SegCompatibleState<D>(this, state, otherState);
  }

  @Override public SegmentWithState<D> tryPrimitive (PrimOp prim, D state) {
    if (prim.is("fixAtConstantAddress", 0, 1)) { // set a region to have an absolute address
      MemVar region = null;
      Rhs rval = prim.getInArg(0);
      if (rval instanceof Rhs.Rvar)
        region = ((Rhs.Rvar) rval).getRegionId();
      if (region != null) {
        assert pending.contains(region); // only regions that are not assigned yet an address are allowed
        ContentCtx ctx = pending.get(region).get();
        long r = ctx.getSize();
        assert r > 0;
        FiniteRange rangeInBytes = ctx.addressableSpaceInBytes();
        if (absolute.hasOverlaps(rangeInBytes))
          throw new InvariantViolationException();
        AbsoluteAddresses newAbsolute = absolute.bind(region, rangeInBytes);
        return new SegmentWithState<D>(new DataSegments<D>(symbolic, newAbsolute, pending.remove(region)), state);
      }
    } else if (prim.is("addressOf", 1, 1)) { // assign the address variable of a region to a variable
      MemVar region = null;
      Rhs rval = prim.getInArg(0);
      if (rval instanceof Rhs.Rvar)
        region = ((Rhs.Rvar) rval).getRegionId();
      if (region != null) {
        if (pending.contains(region)) {
          ContentCtx ctx = pending.get(region).get();
          AddrVar address = NumVar.freshAddress("&" + region.toString());
          AVLMap<AddrVar, P2<BigInt, MemVar>> newSymbolic =
            symbolic.bind(address, P2.tuple2(BigInt.of(ctx.getSize()), region));
          Lhs var = prim.getOutArg(0);
          state = state.assignSymbolicAddressOf(var, address);
          return new SegmentWithState<D>(new DataSegments<D>(newSymbolic, absolute, pending.remove(region)), state);
        } else {
          // REFACTOR: use a reverse map
          for (P2<AddrVar, P2<BigInt, MemVar>> pair : symbolic)
            if (pair._2()._2().equals(region)) {
              Lhs var = prim.getOutArg(0);
              state = state.assignSymbolicAddressOf(var, pair._1());
              return new SegmentWithState<D>(new DataSegments<D>(symbolic, absolute, pending), state);
            }
        }
      }
    }
    return null;
  }

  @Override public DataSegments<D> introduceRegion (MemVar region, ContentCtx ctx) {
    return new DataSegments<D>(symbolic, absolute, pending.bind(region, ctx));
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    if (!absolute.isEmpty()) {
      builder.append("Absolute: ").append(absolute);
    }
    if (!symbolic.isEmpty()) {
      if (builder.length() > 0)
        builder.append("\n");
      builder.append("Symbolic: ").append(symbolic);
    }
    return StringHelpers.indentMultiline(NAME + ": ", builder.toString());
  }

  static class AbsoluteAddresses {
    private static final AbsoluteAddresses EMPTY = new AbsoluteAddresses();
    /**
     * The start and end addresses (in bytes) of each region.
     */
    private final AVLMap<MemVar, FiniteRange> regions;
    /**
     * A map from intervals, representing valid addresses (in bytes) of a region, to the region id.
     */
    private final FiniteRangeTree<MemVar> ranges;

    private AbsoluteAddresses (AVLMap<MemVar, FiniteRange> regions, FiniteRangeTree<MemVar> ranges) {
      this.regions = regions;
      this.ranges = ranges;
    }

    private AbsoluteAddresses () {
      ranges = FiniteRangeTree.empty();
      regions = AVLMap.empty();
    }

    public static AbsoluteAddresses empty () {
      return EMPTY;
    }

    public AbsoluteAddresses bind (MemVar region, FiniteRange range) {
      return new AbsoluteAddresses(regions.bind(region, range), ranges.bind(range, region));
    }

    public OverlappingRanges<MemVar> searchOverlaps (Interval range) {
      return ranges.searchOverlaps(range);
    }

    public BigInt startAddressOf (MemVar region) {
      return regions.get(region).get().low().asInteger();
    }

    public boolean isEmpty () {
      return ranges.isEmpty();
    }

    public boolean contains (MemVar region) {
      return regions.contains(region);
    }

    @Override public String toString () {
      return ranges.toString();
    }

    public MemVarSet getChildSupportSet () {
      MemVarSet css = MemVarSet.empty();
      for (P2<MemVar, FiniteRange> x : regions)
        css = css.insert(x._1());
      return css;
    }

    boolean hasOverlaps (FiniteRange rangeInBytes) {
      return ranges.hasOverlaps(rangeInBytes);
    }
  }

  @Override public MemVarSet getChildSupportSet () {
    MemVarSet css = MemVarSet.empty();
    for (P2<AddrVar, P2<BigInt, MemVar>> x : symbolic)
      css = css.insert(x._2()._2());
    css = css.insertAll(absolute.getChildSupportSet());
    for (P2<MemVar, ContentCtx> x : pending)
      css = css.insert(x._1());
    return css;
  }

  @Override public void toCompactString (StringBuilder builder, PrettyDomain childDomain) {
    // throw new UnimplementedException();
  }

}
