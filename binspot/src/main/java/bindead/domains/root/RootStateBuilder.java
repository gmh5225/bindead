package bindead.domains.root;

import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.FiniteRange;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import javalx.persistentcollections.tree.FiniteRangeTree;
import javalx.persistentcollections.tree.OverlappingRanges;
import rreil.lang.MemVar;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.Type;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.abstractsyntax.memderef.RootRegionAccess;
import bindead.abstractsyntax.memderef.SymbolicOffset;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.MemoryChildOp;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.fields.messages.ReadFromUnknownPointerInfo;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.DomainStateException.VariableSupportSetException;

class RootStateBuilder {
  private MemVarSet regions;
  private AVLMap<MemVar, NumVar> regionToAddress;
  private AVLMap<NumVar, MemVar> addressToRegion;
  protected AVLMap<MemVar, RegionCtx> contexts;
  private FiniteRangeTree<ContentCtx> segments;
  protected AVLMap<BigInt, MemVar> concreteAddresses;

  RootStateBuilder (RootState state) {
    regions = state.regions;
    regionToAddress = state.regionToAddress;
    addressToRegion = state.addressToRegion;
    contexts = state.contexts;
    segments = state.segments;
    concreteAddresses = state.concreteAddresses;
  }

  /**
   * Return the address variable for a region. Might introduce a new address variable if the region has none.
   *
   * @param regionId The ID of a region.
   * @return The ID of the corresponding address variable.
   */
  public NumVar addressOf (MemVar regionId) {
    Option<NumVar> address = regionToAddress.get(regionId);
    if (address.isSome())
      return address.get();
    // XXX: Ad-hoc creation of symbolic addresses...
    NumVar symbolicAddress = NumVar.freshAddress();
    childOps.addIntro(symbolicAddress, Type.Address);
    setAddressOf(regionId, symbolicAddress);
    return symbolicAddress;
  }

  /**
   * Return a region for an address variable ID.
   *
   * @param addressVariableId The ID of an address variable
   * @return The ID of the corresponding region.
   */
  private MemVar regionOf (NumVar addressVariableId) {
    return addressToRegion.get(addressVariableId).get();
  }

  /**
   * Return a region for a memory access.
   *
   * @param accessRange A range in memory.
   * @return The ID for the region that contains the given range
   */
  private MemVar regionOf (Interval accessRange) {
    OverlappingRanges<ContentCtx> overlaps = segments.searchOverlaps(accessRange);
    if (overlaps.size() != 1)
      return null;
    ContentCtx segment = overlaps.iterator().next()._2();
    return concreteAddresses.get(segment.getAddress()).get();
  }

  public RootStateBuilder addSegmentCtx (ContentCtx ctx) {
    int BYTESIZE = 8;
    BigInt startAddress = ctx.getAddress().mul(BYTESIZE);
    FiniteRange segmentRange = FiniteRange.of(startAddress, (ctx.getSize() - 1) * BYTESIZE + 1);
    OverlappingRanges<ContentCtx> overlappingSegments = segments.searchOverlaps(segmentRange);
    if (!overlappingSegments.isEmpty())
      throw new InvariantViolationException();
    segments = segments.bind(segmentRange, ctx);
    return this;
  }

  /**
   * Adds a register-region to this builder. If the region is already known
   * this builder is returned unchanged.
   *
   * @param region The region/register identifier.
   * @return The updated builder.
   */
  public RootStateBuilder addRegister (MemVar region) {
    if (regions.contains(region))
      return this;
    regions = regions.insert(region);
    contexts = contexts.bind(region, RegionCtx.EMPTYSTICKY);
    childOps.addRegionIntro(region, RegionCtx.EMPTYSTICKY);
    return this;
  }

  public void setAddressOf (MemVar regionId, NumVar addressVariable) {
    regionToAddress = regionToAddress.bind(regionId, addressVariable);
    addressToRegion = addressToRegion.bind(addressVariable, regionId);
  }

  public void introduce (MemVar region, RegionCtx regionCtx) {
    if (contexts.contains(region))
      throw new VariableSupportSetException();
    contexts = contexts.bind(region, regionCtx);
    regions = regions.insert(region);
  }

  void makeCompatible (RootStateBuilder other) {
    mergeRegionsAndContexts(other);
    mergeAddresses(other);
    mergeSegments(other);
  }

  private void mergeRegionsAndContexts (RootStateBuilder other) {
    MemVarSet inBoth = regions.intersection(other.regions);
    MemVarSet onlyInFst = regions.difference(inBoth);
    MemVarSet onlyInSnd = other.regions.difference(inBoth);
    for (MemVar r : onlyInFst) {
      RegionCtx ctx = contexts.get(r).get();
      other.contexts = other.contexts.bind(r, ctx);
      other.regions = other.regions.insert(r);
      other.childOps.addRegionIntro(r, ctx);
    }
    for (MemVar r : onlyInSnd) {
      RegionCtx ctx = other.contexts.get(r).get();
      contexts = contexts.bind(r, ctx);
      regions = regions.insert(r);
      childOps.addRegionIntro(r, ctx);
    }
  }

  private void mergeAddresses (RootStateBuilder other) {
    ThreeWaySplit<AVLMap<MemVar, NumVar>> regionToAddressSplit = regionToAddress.split(other.regionToAddress);
    assert regionToAddressSplit.inBothButDiffering().isEmpty();
    for (P2<MemVar, NumVar> binding : regionToAddressSplit.onlyInFirst()) {
      MemVar region = binding._1();
      NumVar symbolicAddress = binding._2();
      other.regionToAddress = other.regionToAddress.bind(region, symbolicAddress);
      other.addressToRegion = other.addressToRegion.bind(symbolicAddress, region);
      other.childOps.addIntro(symbolicAddress, Type.Address);
    }
    for (P2<MemVar, NumVar> binding : regionToAddressSplit.onlyInSecond()) {
      MemVar region = binding._1();
      NumVar symbolicAddress = binding._2();
      regionToAddress = regionToAddress.bind(region, symbolicAddress);
      addressToRegion = addressToRegion.bind(symbolicAddress, region);
      childOps.addIntro(symbolicAddress, Type.Address);
    }
    // Assuming we kept the address-of mappings consistent in both states.
    ThreeWaySplit<AVLMap<NumVar, MemVar>> addressToRegionSplit = addressToRegion.split(other.addressToRegion);
    assert addressToRegionSplit.onlyInFirst().isEmpty();
    assert addressToRegionSplit.onlyInSecond().isEmpty();
    assert addressToRegionSplit.inBothButDiffering().isEmpty();
  }

  private void mergeSegments (RootStateBuilder other) {
    ThreeWaySplit<FiniteRangeTree<ContentCtx>> split = segments.split(other.segments);
    assert split.onlyInFirst().isEmpty();
    assert split.onlyInSecond().isEmpty();
    assert split.inBothButDiffering().isEmpty();
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Childops: " + childOps);
    builder.append("\n");
    builder.append(build());
    return builder.toString();
  }

  public RootState build () {
    return new RootState(regions, regionToAddress, addressToRegion, contexts, segments, concreteAddresses);
  }

  /**
   * Resolve {@code Rval}s to {@code Xref} objects. A {@code Rval} may be
   * a variable or a constant. In case of variables, first, the points-to
   * set gets queried from the given channel. If the points-to set is
   * empty, the numeric value of the queried variable is used as base
   * address and a region is tried to be resolved from known segments.
   *
   * @param address An address value
   * @param builder A context builder to retrieve the current context for the address value
   * @return A cross reference to a memory field corresponding to the given address value
   */
  <Child extends MemoryDomain<Child>> RootRegionAccess resolveReference (Rval address, Child child) {
    if (address instanceof Rlit) {
      Rlit addressLiteral = (Rlit) address;
      BigInt addressInBytes = addressLiteral.getValue();
      BigInt addressInBits = addressInBytes.mul(Bound.EIGHT);
      Interval accessRange = Interval.of(addressInBits, addressInBits.add(BigInt.of(addressLiteral.getSize() - 1)));
      MemVar regionId = regionOf(accessRange);
      RegionCtx region = contexts.get(regionId).get();
      Range offset = Range.from(Interval.of(addressInBytes.sub(region.getAddress().get())));
      return RootRegionAccess.explicit(regionId, offset);
    } else if (address instanceof Rvar) {
      Rvar variable = (Rvar) address;
      // TODO use new dereference mechanism
      List<P2<AbstractPointer, Child>> pointsTo = child.deprecatedDeref(variable.getSize(), address, VarSet.empty());
      /*
        AVLSet<AddrVar> pointsTo2 = child.findPointerTargets(address)._1();
        assert pointsTo.isEmpty() == pointsTo2.isEmpty();
      */
      if (pointsTo.isEmpty()) {
        child.getContext().addWarning(new ReadFromUnknownPointerInfo(Range.from(Interval.TOP)));
        return null;
      }
      AbstractPointer dRef = pointsTo.get(0)._1();
      SymbolicOffset offset = new SymbolicOffset(dRef.offset);
      if (dRef.isAbsolute()) {
        // try to resolve into a segment and use that region's pointer
        Range accessRange = child.queryRange(variable);
        // regions are stored with their address in bits
        final MemVar regionId = regionOf(accessRange.convexHull().mul(BigInt.of(8)));
        if (regionId == null) {
          child.getContext().addWarning(new ReadFromUnknownPointerInfo(accessRange));
          return null;
        }
        assert regionId != null;
        return new RootRegionAccess(dRef.address, new AbstractMemPointer(regionId, offset));
      }
      MemVar regionId = regionOf(dRef.address);
      assert regionId != null;
      return new RootRegionAccess(dRef.address, new AbstractMemPointer(regionId, offset));
    }
    throw new InvariantViolationException(); // not reachable
  }

  public final MemoryChildOp.Sequence childOps = new MemoryChildOp.Sequence();

  /**
   * Apply all outstanding child operations to the given state.
   *
   * @param state The child state.
   */
  public final <D extends MemoryDomain<D>> D applyChildOps (D state) {
    // currently only called by root domain -- SegMem calls transfer functions directly.
    return childOps.apply(state);
  }

}
