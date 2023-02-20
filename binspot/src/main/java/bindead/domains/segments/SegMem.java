package bindead.domains.segments;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.fn.Fn;
import javalx.mutablecollections.CollectionHelpers;
import javalx.persistentcollections.AVLSet;
import rreil.lang.MemVar;
import rreil.lang.RReil.Assign;
import rreil.lang.RReil.Branch;
import rreil.lang.RReil.Load;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Store;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Lin;
import rreil.lang.Test;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.data.Linear;
import bindead.data.MemVarSet;
import bindead.data.NumVar.AddrVar;
import bindead.domainnetwork.combinators.RootMemoryFunctor;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.segments.basics.RegionAccess;
import bindead.domains.segments.basics.SegCompatibleState;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.basics.SegmentAccess;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.exceptions.Unreachable;

/**
 * This domain partitions the programs state space into segments.
 *
 * @author Axel Simon
 */
public class SegMem<D extends MemoryDomain<D>> extends RootMemoryFunctor<SegMemState<D>, D, SegMem<D>> {
  public static final String NAME = "SEGMEM";

  @Deprecated
  static Map<MemVar, Integer> triggers;

  public SegMem (D child, Segment<D>... segs) {
    this(init(child, segs));
  }

  private SegMem (P2<SegMemState<D>, D> state) {
    this(state._1(), state._2());
  }

  public SegMem (SegMemState<D> ctx, D child) {
    super(NAME, ctx, child);
  }

  private static <D extends MemoryDomain<D>> P2<SegMemState<D>, D> init (D child, Segment<D>... segs) {
    final SegMemState<D> state = new SegMemState<D>(segs, MemVarSet.empty());
    triggers = new HashMap<MemVar, Integer>();
    child = state.initialize(triggers, child);
    return P2.tuple2(state, child);
  }


  /* ***********
   * Operations from class FunctorDomain:
   *********** */

  @Override public SegMem<D> build (SegMemState<D> state, D childState) {
    checkSupport(state, childState);
    assert state.edgenodesAreSane();
    return new SegMem<D>(state, childState);
  }

  private static <D extends MemoryDomain<D>> void checkSupport (SegMemState<D> state, D childState) {
    MemVarSet supp1 = state.getChildSupportSet();
    MemVarSet supp2 = childState.getSupportSet();
    MemVarSet onlyInState = supp1.difference(supp2);
    MemVarSet onlyInChild = supp2.difference(supp1);
    assert sameAs(supp1, supp2) : "state has " + onlyInState + " and child has " + onlyInChild + " exclusively";
  }

  private static boolean sameAs (MemVarSet supp1, MemVarSet supp2) {
    // hsi: hack that neutralized another hack - hooray, we're getting self-referential!
    for (MemVar t : supp1)
      if (!supp2.contains(t))
        if (!t.getName().equals("stack"))
          if (!t.getName().equals("sp"))
            return false;
    for (MemVar t : supp2)
      if (!supp1.contains(t))
        if (!t.getName().equals("stack"))
          if (!t.getName().equals("sp"))
            return false;
    return true;
  }


  @Override public P3<SegMemState<D>, D, D> makeCompatible (SegMem<D> newState, boolean isWideningPoint) {
    // msgs("making compatible");
    // newState.msgs("new state");
    if (isWideningPoint) {
      newState = newState.summarizeHeap();
      // msg("summarized to " + newState);
    }
    int numberOfSegments = state.size();
    assert numberOfSegments == newState.state.size();
    SegMemState<D> rstate = state;
    D child = childState;
    D otherChild = newState.childState;
    for (int i = 0; i < numberOfSegments; i++) {
      final SegCompatibleState<D> triple = state.get(i).makeCompatible(newState.state.get(i), child, otherChild);
      rstate = rstate.setSegmentAt(i, triple.segment);
      child = triple.leftState;
      otherChild = triple.rightState;
    }
    return P3.tuple3(rstate, child, otherChild);
  }

  private SegMem<D> summarizeHeap () {
    final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(this);
    builder.summarizeHeap();
    return build(builder);
  }

  private SegMem<D> build (final SegMemStateBuilder<D> builder) {
    return build(builder.build(), builder.childState);
  }

  /* ***********
   * Operations from interface RootDomain:
   *********** */

  @Override public SegMem<D> eval (PrimOp prim) {
    if (prim.is("coredump", 0, 0)) {
      StringBuilder builder = new StringBuilder();
      toCompactString(builder);
      System.out.println(builder.toString());
      return this;
    }

    final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(this);
    builder.evalPrimop(prim);
    return build(builder);
  }

  @Override public SegMem<D> eval (Assign stmt) {
    final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(this);
    final Integer triggerIdx = triggers.get(stmt.getLhs().getRegionId());
    builder.evalAssign(stmt, triggerIdx);
    return build(builder);
  }

  @Override public SegMem<D> eval (Load stmt) {
    final List<SegmentAccess<D>> sources = findPointerTargets(stmt.getReadAddress());
    if (sources.isEmpty())
      return evalLoadWithNoAccess(stmt);
    // msg("warned by access " + getContext().getWarningsChannel());
    // msg("load with access " + sources);
    SegmentAccess<D> first = sources.remove(0);
    SegMem<D> current = evalLoadOnAccess(stmt, first);
    for (final SegmentAccess<D> tuple : sources) {
      SegMem<D> result = evalLoadOnAccess(stmt, tuple);
      current = current.join(result);
    }
    return current;
  }

  // if the pointer could not be resolved we assign TOP to the lhs and continue
  // XXX hsi: this reanimates unreachable states. do we really want to do this?
  private SegMem<D> evalLoadWithNoAccess (Load stmt) {
    final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(this);
    builder.evalLoadTop(stmt);
    return build(builder);
  }

  private SegMem<D> evalLoadOnAccess (Load stmt, SegmentAccess<D> access) {
    final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(access.state);
    builder.evalLoadFromLocation(stmt, access.location);
    return build(builder);
  }

  @Override public SegMem<D> eval (Store stmt) {
    final List<SegmentAccess<D>> targets = findPointerTargets(stmt.getWriteAddress());
    // msg("eval(Store) " + targets);
    // msg("eval(Store) " + this);
    if (targets.isEmpty()) {
      // if the pointer could not be resolved we assume that the right warnings have been emitted during the dereference
      // and continue without modifying the state
      return this;
    }
    SegmentAccess<D> segmentAccess = targets.remove(0);
    // msg("eval(Store) " + segmentAccess);
    // msg("eval(Store) " + this);
    SegMem<D> current = evalStoreOnAccess(stmt, segmentAccess);
    for (final SegmentAccess<D> tuple : targets) {
      current = current.join(evalStoreOnAccess(stmt, tuple));
    }
    return current;
  }

  private SegMem<D> evalStoreOnAccess (Store stmt, SegmentAccess<D> segmentAccess) {
    final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(segmentAccess.state);
    builder.evalStore(segmentAccess.location, stmt);
    return build(builder);
  }

  private List<SegmentAccess<D>> findPointerTargets (Lin value) {
    try {
      final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(this);
      builder.introduceRegister(value);
      D newChildState = builder.childState;
      final P3<AVLSet<AddrVar>, D, Rlin> newDereferenced = newChildState.findPointerTargets(value);
      final AVLSet<AddrVar> addresses = newDereferenced._1();
      final D child = newDereferenced._2();
      final Linear offset = newDereferenced._3().getLinearTerm();
      // msg("processing target NULL");
      List<SegmentAccess<D>> targets = dereference(value, AbstractPointer.absolute(offset), child);
      for (final AddrVar addr : addresses) {
        // msg("processing target " + addr);
        List<SegmentAccess<D>> dereferenced = dereference(value, AbstractPointer.relativeTo(offset, addr), child);
        targets.addAll(dereferenced);
      }
      // hsi: The list of targets is empty when the heap domain recognizes that there is no
      // concrete instance of the abstract heap. Happens all the time.
      return targets;
    } catch (Unreachable _) {
      return new LinkedList<SegmentAccess<D>>();
    }
  }

  // collect a set of targets that we jump to; each MemDeref might have several offsets, so for each we generate a set
  // of target addresses
  @Override public List<P2<RReilAddr, SegMem<D>>> eval (Branch branch, ProgramPoint current, ProgramPoint next) {
    Lin target = branch.getTarget();
    final List<P2<RReilAddr, SegMem<D>>> result = new LinkedList<>();
    List<SegmentAccess<D>> pointerTargets = findPointerTargets(target);
    for (final SegmentAccess<D> targets : pointerTargets) {
      List<P2<RReilAddr, SegMem<D>>> withJumpsResolved = resolveJumpTargets(target, targets.location);
      List<P2<RReilAddr, SegMem<D>>> withJumpsExecuted = targets.state.evalJumpTargets(current, next, withJumpsResolved);
      result.addAll(withJumpsExecuted);
    }
    return result;
  }

  // resolve the pointer into jump targets in the code segment
  private List<P2<RReilAddr, SegMem<D>>> resolveJumpTargets (Lin target, AbstractMemPointer ptr) {
    // usually this should be handled by the code (data) segment
    for (int codeSegmentIndex = 0; codeSegmentIndex < state.size(); codeSegmentIndex++) {
      Segment<D> segment = state.get(codeSegmentIndex);
      List<P2<RReilAddr, SegmentWithState<D>>> result = segment.resolveJump(target, ptr, childState);
      if (result != null)
        return mutateStateAt(result, codeSegmentIndex);
    }
    return Collections.emptyList();
  }

  private List<P2<RReilAddr, SegMem<D>>> mutateStateAt (List<P2<RReilAddr, SegmentWithState<D>>> addressStates,
      final int index) {
    return CollectionHelpers.map(addressStates, new Fn<P2<RReilAddr, SegmentWithState<D>>, P2<RReilAddr, SegMem<D>>>() {
      @Override public P2<RReilAddr, SegMem<D>> apply (P2<RReilAddr, SegmentWithState<D>> a) {
        return P2.tuple2(a._1(), build(state.setSegmentAt(index, a._2().segment), a._2().state));
      }
    });
  }

  // execute the jump to the targets
  private List<P2<RReilAddr, SegMem<D>>> evalJumpTargets (ProgramPoint current, ProgramPoint next,
      List<P2<RReilAddr, SegMem<D>>> resolvedJumps) {
    // then execute the jump
    List<P2<RReilAddr, SegMem<D>>> finalResult = new LinkedList<>();
    for (P2<RReilAddr, SegMem<D>> resolvedTarget : resolvedJumps) {
      SegMemState<D> resolvedState = resolvedTarget._2().state;
      // usually this should be handled by the stack segment
      for (int stackSegmentIndex = 0; stackSegmentIndex < resolvedState.size(); stackSegmentIndex++) {
        Segment<D> segment = resolvedState.get(stackSegmentIndex);
        SegmentWithState<D> finalSegment = segment.evalJump(resolvedTarget._1(), childState, current, next);
        if (finalSegment != null) {
          SegMemState<D> finalState = resolvedState.setSegmentAt(stackSegmentIndex, finalSegment.segment);
          finalResult.add(P2.tuple2(resolvedTarget._1(), build(finalState, finalSegment.state)));
        }
      }
    }
    return finalResult;
  }

  @Override public SegMem<D> eval (Test test) throws Unreachable {
    final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(this);
    builder.evalTest(test);
    return build(builder);
  }

  @Override public SegMem<D> introduceRegion (MemVar region, RegionCtx ctx) {
    final SegMemStateBuilder<D> builder = new SegMemStateBuilder<D>(this);
    builder.introduceRegion(region, ctx);
    return build(builder);
  }

  private List<SegmentAccess<D>> dereference (Lin pointerValue, final AbstractPointer target, D child) {
    final List<SegmentAccess<D>> result = new LinkedList<SegmentAccess<D>>();
    for (int i = 0; i < state.size(); i++) {
      Segment<D> segment = state.get(i);
      final List<RegionAccess<D>> res = segment.dereference(pointerValue, target, child);
      if (res != null) {
        for (final RegionAccess<D> deref : res) {
          SegMemState<D> newstate = state.setSegmentAt(i, deref.segstate.segment);
          SegMem<D> built = build(newstate, deref.segstate.state);
          result.add(new SegmentAccess<D>(deref.location, built));
        }
      }
    }
    return result;
  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // TODO implement in PrettyDomain
    throw new UnimplementedException();

  }

  @Override public void toCompactString (StringBuilder builder) {
    List<D> cs = childState.enumerateAlternatives();
    for (int i = 0; i < cs.size(); i++) {
      D c = cs.get(i);
      builder.append("Alternative " + i + ":\n");
      state.toCompactString(name, builder, c);
      c.toCompactString(builder);
      builder.append('\n');
    }
  }

}
