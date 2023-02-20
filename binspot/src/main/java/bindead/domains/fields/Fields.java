package bindead.domains.fields;

import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.FiniteRange;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.ThreeWaySplit;
import javalx.persistentcollections.tree.FiniteRangeTree;
import javalx.persistentcollections.tree.OverlappingRanges;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Test;
import rreil.lang.util.RhsFactory;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.data.MemVarPair;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.DebugChannel;
import bindead.domainnetwork.combinators.MemoryFiniteFunctor;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.fields.FieldGraph.Partitioning;
import bindead.domains.fields.messages.ReadFromXrefWithNonConstantOffsetInfo;
import bindead.domains.fields.messages.WriteToReadOnlySegmentWarning;
import bindead.domains.fields.messages.WriteToXrefWithNonConstantOffsetWarning;
import bindead.domains.segments.heap.PathString;
import bindead.exceptions.Unreachable;

public class Fields<D extends FiniteDomain<D>> extends MemoryFiniteFunctor<FieldState, D, Fields<D>> {
  private static final RhsFactory imp = RhsFactory.getInstance();
  private static final FiniteFactory fin = FiniteFactory.getInstance();
  public static final String NAME = "FIELDS";

  public Fields (D child) {
    super(NAME, FieldState.EMPTY, child);
  }

  private Fields (FieldState state, D childState) {
    super(NAME, state, childState);
  }

  /* ***********
   * Operations from class FunctorDomain:
   *********** */

  @Override public Fields<D> build (FieldState state, D childState) {
    return new Fields<D>(state, childState);
  }

  @Override public P3<FieldState, D, D> makeCompatible (Fields<D> other, boolean isWideningPoint) {
    FieldStateBuilder<D> fst = new FieldStateBuilder<D>(state);
    FieldStateBuilder<D> snd = new FieldStateBuilder<D>(other.state);
    fst.makeCompatible(snd);
    D newChildStateOfFst = fst.applyReorderedChildOps(childState);
    D newChildStateOfSnd = snd.applyReorderedChildOps(other.childState);
    return P3.<FieldState, D, D>tuple3(fst.build(), newChildStateOfFst, newChildStateOfSnd);
  }

  /* ***********
   * Operations from interface MemoryDomain:
   *********** */

  @Override public Fields<D> evalAssign (Lhs lhs, Rhs rhs) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.runAssign(childState, lhs, rhs);
    return finish(builder);
  }

  @Override public Fields<D> evalLoad (Lhs value, AbstractMemPointer location) {
    int accessSize = value.getSize();
    Range offset = location.getOffsetRange(childState);
    Rhs rhs;
    if (offset.isConstant()) {
      rhs = varFromRegion(accessSize, offset, location.region);
    } else {
      // read from region with non-constant offset -> set lhs to [-oo, +oo]
      getContext().addWarning(new ReadFromXrefWithNonConstantOffsetInfo(location, offset));
      rhs = imp.range(accessSize, Interval.TOP);
    }

    return evalAssign(value, rhs);
  }

  @Override public Fields<D> evalStore (AbstractMemPointer location, Lin value) {
    Range offset = location.getOffsetRange(childState);
    int accessSize = value.getSize();
    if (!state.canWriteTo(location.region)) {
      getContext().addWarning(new WriteToReadOnlySegmentWarning(location.region));
      return this; // write is ignored
    }
    if (offset.isConstant()) {
      Rvar varFromXref = varFromRegion(accessSize, offset, location.region);
      return evalAssign(varFromXref.asLhs(), value);
    } else {
      FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
      // Write to region with non-constant offset
      getContext().addWarning(new WriteToXrefWithNonConstantOffsetWarning(location, offset));
      Interval fieldFromRange = offset.convexHull().mul(Bound.EIGHT).add(Interval.of(0, accessSize - 1));
      builder.removeOverlapping(location.region, fieldFromRange);
      return finish(builder);
    }
  }

  private static Rvar varFromRegion (int accessSize, Range offset, MemVar region) {
    int effectiveOffset = offset.getMin().mul(Bound.EIGHT).intValue();
    return imp.variable(accessSize, effectiveOffset, region);
  }

  @Override public Fields<D> eval (Test test) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.runTest(childState, test);
    return finish(builder);
  }

  @Override public P3<AVLSet<AddrVar>, Fields<D>, Rlin> findPointerTargets (Lin ptr) throws Unreachable {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    Finite.Rlin effectiveRhs = (Rlin) builder.resolveRhs(ptr);
    D newChildState = builder.applyChildOps(childState);
    P2<AVLSet<AddrVar>, D> newChildren = newChildState.deref(effectiveRhs);
    FieldState newState = builder.build();
    Fields<D> s = build(newState, newChildren._2());
    return new P3<AVLSet<AddrVar>, Fields<D>, Rlin>(newChildren._1(), s, effectiveRhs);
  }

  @Deprecated @Override public List<P2<AbstractPointer, Fields<D>>> deprecatedDeref (int size, Rval ptr,
      VarSet summaries) throws Unreachable {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    Finite.Rlin effectiveRhs = builder.resolveRval(ptr);
    D newChildState = builder.applyChildOps(childState);
    List<P2<AbstractPointer, Fields<D>>> res = new LinkedList<P2<AbstractPointer, Fields<D>>>();
    List<P2<AddrVar, D>> newChildren = newChildState.deprecatedDeref(effectiveRhs, summaries);
    FieldState newState = builder.build();
    for (P2<AddrVar, D> tuple : newChildren) {
      // will not work if effectiveRhs contains temp variables?!?
      assert builder.hasNoTempVars();
      D localCS = builder.applyDelayedKillOps(tuple._2());
      AbstractPointer md = AbstractPointer.createMemDeref(effectiveRhs, tuple._1());
      // Range r = md.offset.getRange(childState);
      // msg("deref returns offset " + effectiveRhs + " = " + r);
      res.add(new P2<AbstractPointer, Fields<D>>(md, build(newState, localCS)));
    }
    return res;
  }

  @Override public Fields<D> introduceRegion (MemVar region, RegionCtx regionCtx) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.introduce(region, regionCtx);
    return finish(builder);
  }

  @Override public Fields<D> substituteRegion (MemVar from, MemVar to) {
    if (from == to)
      return this;
    assert state.containsRegion(from);
    assert !state.containsRegion(to);
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.rename(from, to);
    return finish(builder);
  }

  @Override public Fields<D> assignSymbolicAddressOf (Lhs lhs, NumVar symbolicAddress) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    assert state.regions.contains(lhs.getRegionId()); // we do not want to introduce a new region here
    Finite.Lhs finiteLhs = builder.resolveLhs(lhs);
    Finite.Rhs finiteRhs = fin.linear(lhs.getSize(), symbolicAddress);
    builder.getChildOps().addAssignment(finiteLhs, finiteRhs);
    return finish(builder);
  }


  @Override public Fields<D> projectRegion (MemVar region) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.projectRegion(region);
    return finish(builder);
  }

  private Fields<D> finish (FieldStateBuilder<D> builder) {
    D newChildState = builder.applyReorderedChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Fields<D> assumePointsToAndConcretize (Lin pointerValue, AddrVar target, MemVar region) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.assumePointsTo(pointerValue, target, region);
    D newChildState = builder.applyReorderedChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public List<P2<PathString, AddrVar>> findPossiblePointerTargets (MemVar id) throws Unreachable {
    return state.findPossiblePointerTargets(childState, id);
  }

  @Override public Fields<D> copyMemRegion (MemVar fromVar, MemVar toVar) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.copyMemRegion(fromVar, toVar);
    D newChildState = builder.applyReorderedChildOps(childState);
    return build(builder.build(), newChildState);
  }

  private final Range tryLinearization (OverlappingRanges<VariableCtx> overlapping, FiniteRange key) {
    FieldGraph g = FieldGraph.build(overlapping);
    Partitioning path = g.findPartitioning(key.low(), key.high());
    Option<FiniteRange> span = path.span();
    if (span.isNone() || !key.isEqualTo(span.get()))
      return null;
    return linearize(path, key.getSpan());
  }

  private final Range linearize (Partitioning path, BigInt size) {
    Range value = null;
    assert !path.isEmpty();
    int baseOffset = path.get(0)._1().low().asInteger().intValue();
    for (P2<FiniteRange, VariableCtx> ctx : path) {
      FiniteRange intervalKey = ctx._1();
      NumVar variable = ctx._2().getVariable();
      Range variableRange = queryRange(variable);
      int currentOffset = intervalKey.low().asInteger().intValue();
      BigInt coeff = BigInt.powerOfTwo(currentOffset - baseOffset);
      // TODO: need to convert queried values to the right size
      // XXX bm: this happens in FieldsBuilder, why not here?
      // why do we have here BigInts for the size and ints in the builder?
      if (value == null)
        value = variableRange.mul(coeff);
      else
        value = value.add(variableRange.mul(coeff));
    }
    return value;
  }

  @Override public final Range queryRange (Rval value) {
    if (value instanceof Rlit) {
      BigInt c = ((Rlit) value).getValue();
      return Range.from(c);
    } else if (value instanceof Address) {
      RReilAddr address = ((Address) value).getAddress();
      if (address.offset() != 0)
        throw new IllegalArgumentException("Not possible to translate a RREIL address with offset to a range: " + address);
      return Range.from(address.base());
    }
    Rvar var = (Rvar) value;
    return queryRange(var.getRegionId(), var.bitsRange());
  }

  /**
   * Query the range valuation of interval-field {@code key} contained in {@code region}.
   *
   * @param region The region in which to search for the field.
   * @param key The interval-field.
   * @return The range valuation of the field or null if the field cannot be resolved.
   */
  @Override public final Range queryRange (MemVar region, FiniteRange key) {
    OverlappingRanges<VariableCtx> overlapping = state.queryOverlappingFields(region, key);
    if (overlapping.isEmpty())
      return null;
    overlapping.sortByFiniteRangeKey();
    return tryLinearization(overlapping, key);
  }

  @Override public Range queryRange (Lin value) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    if (value instanceof LinRval)
      return queryRange(((LinRval) value).getRval());
    bindead.abstractsyntax.finite.Finite.Rhs resolved = builder.resolveRhs(value);
    if (resolved instanceof Rlin) {
      Rlin linear = (Rlin)resolved;
      // TODO: would we need to wrap here to the size?
      return childState.queryRange(linear.getLinearTerm());
    }
    return null;
  }

  @Override public DebugChannel getDebugChannel () {
    return new DebugChannel(childState.getDebugChannel()) {
      @Override public Rlin resolve (Rvar value) {
        FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
        return builder.resolve(value);
      }
    };
  }

  @Override public Option<NumVar> pickSpecificField (MemVar region, FiniteRange access) {
    OverlappingRanges<VariableCtx> overlapping = state.queryOverlappingFields(region, access);
    for (P2<FiniteRange, VariableCtx> field : overlapping)
      if (field._1().isEqualTo(access))
        return Option.some(field._2().getVariable());
    return Option.none();
  }

  @Override public Fields<D> copyAndPaste (MemVarSet vars, Fields<D> other) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.copyAndPaste(vars, other);
    D newChildState = builder.applyReorderedChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public MemVarSet getSupportSet () {
    return state.getSupportSet();
  }

  @Override public Fields<D> assumeEdgeNG (Lin fieldThatPoints, AddrVar address) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.assumeEdgeNG(fieldThatPoints, address);
    D newChildState = builder.applyReorderedChildOps(childState);
    return build(builder.build(), newChildState);

  }

  @Override public Fields<D> expandNG (List<MemVarPair> mvps) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.expandNG(mvps);
    return finish(builder);
  }

  @Override public Fields<D> expandNG (AddrVar address, AddrVar address2, List<MemVarPair> mvps) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.expandNG(address, address2, mvps);
    return finish(builder);
  }

  @Override public Fields<D> concretizeAndDisconnectNG (AddrVar summary, AVLSet<MemVar> concreteNodes) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.concretizeAndDisconnectNG(summary, concreteNodes);
    D newChildState = builder.applyReorderedChildOps(childState);
    return build(builder.build(), newChildState);

  }

  @Override public Fields<D> bendBackGhostEdgesNG (AddrVar summary, AddrVar concrete, MemVarSet sContents,
      MemVarSet cContents, MemVarSet pointingToSummary, MemVarSet pointingToConcrete) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.bendBackGhostEdgesNG(concrete, summary, sContents, cContents, pointingToSummary, pointingToConcrete);
    D newChildState = builder.applyReorderedChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Fields<D> foldNG (AddrVar address, AddrVar address2, List<MemVarPair> mvps) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.foldNG(address, address2, mvps);
    return finish(builder);
  }

  @Override public Fields<D> foldNG (List<MemVarPair> mvps) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.foldNG(mvps);
    return finish(builder);
  }

  @Override public Fields<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, MemVarSet sContents,
      MemVarSet cContents, MemVarSet pointingToSummary, MemVarSet pointingToConcrete) {
    FieldStateBuilder<D> builder = new FieldStateBuilder<D>(state);
    builder.bendGhostEdgesNG(concrete, summary, sContents, cContents, pointingToSummary, pointingToConcrete);
    D newChildState = builder.applyReorderedChildOps(childState);
    return build(builder.build(), newChildState);

  }

  @Override public Range queryPtsEdge (MemVar from, BigInt offset, int size, AddrVar to) {
    MemVar region = from;
    FiniteRange key = FiniteRange.of(offset, size);
    OverlappingRanges<VariableCtx> overlapping = state.queryOverlappingFields(region, key);
    if (!(overlapping.size() == 1))
      return Range.top();
    NumVar fromVar = overlapping.getFirst()._2().getVariable();
    Range queryEdgeFlag = childState.queryEdgeFlag(fromVar, to);
    return queryEdgeFlag;
  }

  @Override public Fields<D> assumeRegionsAreEqual (MemVar first, MemVar second) {
    Region r1 = state.regions.get(first);
    Region r2 = state.regions.get(second);
    ThreeWaySplit<FiniteRangeTree<VariableCtx>> split = r1.fields.split(r2.fields);
    // when they differ, some write operation has invalidated the connector
    assert split.onlyInFirst().isEmpty();
    assert split.onlyInSecond().isEmpty();
    D cs = childState;
    for (P2<FiniteRange, VariableCtx> x : split.inBothButDiffering()) {
      NumVar fst = x.snd.getVariable();
      NumVar snd = r2.fields.get(x._1()).get().getVariable();
      int size = x._1().getSpan().intValue();
      cs = cs.assumeVarsAreEqual(size, fst, snd);
    }
    return build(state, cs);
  }



  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    //builder.append(var+" = ");
    state.regions.get(var).appendInfo(builder, childState);
    //builder.append(string);
  }
}
