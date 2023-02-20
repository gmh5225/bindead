package bindead.domains.segments;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javalx.mutablecollections.CollectionHelpers;
import javalx.numeric.Range;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Assign;
import rreil.lang.RReil.Load;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Store;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Test;
import rreil.lang.util.RhsFactory;
import rreil.lang.util.RvarExtractor;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.data.MemVarSet;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.domains.segments.warnings.PrimitiveNotFound;
import bindead.exceptions.Unreachable;

/**
 * @author Axel Simon
 */
class SegMemStateBuilder<D extends MemoryDomain<D>> {
  private static final RhsFactory imp = RhsFactory.getInstance();

  final Segment<D>[] segments;
  D childState;
  private MemVarSet explicitlyIntroducedRegions;

  SegMemStateBuilder (SegMem<D> state) {
    this.segments = state.state.cloneSegments();
    this.childState = state.childState;
    this.explicitlyIntroducedRegions = state.state.explicitlyIntroducedRegions;
  }

  @Override public String toString () {
    return Arrays.toString(segments);
  }

  SegMemState<D> build () {
    return new SegMemState<D>(segments, explicitlyIntroducedRegions);
  }

  void evalAssign (Assign stmt, Integer triggerIdx) {
    introduceRegisters(stmt);
    Rhs rhs = stmt.getRhs();
    Lhs lhs = stmt.getLhs();
    informAboutAssign(lhs.getRegionId(), Range.from(lhs.getOffset()), rhs.getRegionOrNull(), rhs.getOffsetOrTop());
    childState = childState.evalAssign(lhs, rhs);
    if (triggerIdx != null)
      triggerN(triggerIdx, lhs, rhs);
  }

  void evalLoadFromLocation (Load stmt, AbstractMemPointer location) {
    introduceRegisters(stmt);
    Lhs lhs = stmt.getLhs();
    build().edgenodesAreSane();
    informAboutAssign(lhs.getRegionId(), Range.from(lhs.getOffset()), location.region,
        location.getOffsetRange(childState));
    build().edgenodesAreSane();
    childState = childState.evalLoad(lhs, location);
  }

  void evalLoadTop (Load stmt) {
    introduceRegisters(stmt);
    Lhs lhs = stmt.getLhs();
    informAboutAssign(lhs.getRegionId(), Range.from(lhs.getOffset()), null, Range.ZERO);
    childState = childState.evalAssign(stmt.getLhs(), imp.top(stmt.getLhs().getSize()));
  }

  void evalPrimop (PrimOp prim) {
    AnalysisCtx context = childState.getContext();
    introduceRegisters(prim.getOutArgs());
    tryPrimitiveOnAll(prim);
    if (childState == null) { // FIXME: it does not work this way, use a return value from tryPrimitiveOnAll()
      context.addWarning(new PrimitiveNotFound(prim));
      throw new Unreachable();
    }
  }

  private void tryPrimitiveOnAll (PrimOp prim) {
    for (int i = 0; i < segments.length; i++) {
      final SegmentWithState<D> res = segments[i].tryPrimitive(prim, childState);
      if (res != null) {
        segments[i] = res.segment;
        childState = res.state;
        return; // shouldn't this be done on every segment? FIXME: yep should be done on all
      }
    }
  }

  void evalStore (AbstractMemPointer target, Store storeStmt) {
    introduceRegisters(storeStmt);
    // rhs is in store, lhs in accLoc
    Range ofs = target.getOffsetRange(childState);
    informAboutAssign(target.region, ofs, storeStmt.getRhs().getRegionOrNull(), storeStmt.getRhs().getOffsetOrTop());
    childState = childState.evalStore(target, storeStmt.getRhs());
  }


  void evalTest (Test test) {
    introduceRegisters(test);
    childState = childState.eval(test);
  }

  void introduceRegion (MemVar region, RegionCtx ctx) {
    if (ctx.getSegment().isSome()) {
      assert !explicitlyIntroducedRegions.contains(region);
      explicitlyIntroducedRegions = explicitlyIntroducedRegions.insert(region);
      for (int i = 0; i < segments.length; i++) {
        Segment<D> seg = segments[i];
        seg = seg.introduceRegion(region, ctx.getSegment().get());
        if (seg != null)
          segments[i] = seg;
      }
    }
    childState = childState.introduceRegion(region, ctx);
  }

  void summarizeHeap () {
    for (int i = 0; i < segments.length; i++) {
      final SegmentWithState<D> summarized = segments[i].summarizeHeap(childState);
      segments[i] = summarized.segment;
      childState = summarized.state;
    }
  }




  // ********************


  void introduceRegister (Lin value) {
    List<Rvar> all = RvarExtractor.fromRhs(value);
    final List<Rval> inArgs = CollectionHelpers.cast(all);
    if (!inArgs.isEmpty())
      addRegisters(null, inArgs);
  }

  void introduceRegister (Rval variable) {
    List<Rval> inArgs = Collections.singletonList(variable);
    addRegisters(null, inArgs);
  }

  private void introduceRegisters (List<Lhs> args) {
    if (!args.isEmpty())
      addRegisters(args, null);
  }

  private void introduceRegisters (RReil insn) {
    List<Rvar> all = RvarExtractor.getAll(insn);
    final List<Rval> inArgs = CollectionHelpers.cast(all);
    if (!inArgs.isEmpty())
      addRegisters(null, inArgs);
  }

  private void introduceRegisters (Test insn) {
    List<Rvar> all = RvarExtractor.fromRhs(insn.getComparison());
    final List<Rval> inArgs = CollectionHelpers.cast(all);
    if (!inArgs.isEmpty())
      addRegisters(null, inArgs);
  }

  private void addRegisters (List<Lhs> args, final List<Rval> inArgs) {
    tryPrimitiveOnAll(new PrimOp("addRegisters", args, inArgs));
  }

  /**
   * @param fromRegion copied region or null if there is no single source region
   */
  private void informAboutAssign (MemVar toRegion, Range toOffset, MemVar memVar, Range fromRange) {
    for (int i = 0; i < segments.length; i++) {
      SegmentWithState<D> p2 = segments[i].informAboutAssign(childState, toRegion, toOffset, memVar, fromRange);
      segments[i] = p2.segment;
      childState = p2.state;
    }
  }

  private void triggerN (Integer n, Lhs lhs, Rhs rhs) {
    final Segment<D> seg = segments[n];
    final SegmentWithState<D> res = seg.triggerAssignment(lhs, rhs, childState);
    segments[n] = res.segment;
    childState = res.state;
  }

}
