package bindead.domains.root;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import rreil.RReilGrammarException;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil.Assign;
import rreil.lang.RReil.Branch;
import rreil.lang.RReil.Load;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Store;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Test;
import rreil.lang.util.RvarExtractor;
import rreil.lang.util.Type;
import bindead.data.NumVar;
import bindead.domainnetwork.combinators.RootMemoryFunctor;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.fields.messages.ReadFromUnknownPointerInfo;
import bindead.domains.segments.warnings.PrimitiveNotFound;
import bindead.exceptions.Unreachable;

public class Root<D extends MemoryDomain<D>> extends RootMemoryFunctor<RootState, D, Root<D>> {
  public static final String NAME = "ROOT";

  public Root (D child) {
    this(RootState.EMPTY, child);
  }

  private Root (RootState ctx, D child) {
    super(NAME, ctx, child);
  }

  /* ***********
   * Operations from class FunctorDomain:
   *********** */

  @Override public Root<D> build (RootState state, D childState) {
    return new Root<D>(state, childState);
  }

  @Override public P3<RootState, D, D> makeCompatible (Root<D> other, boolean isWideningPoint) {
    if (this == other)
      return P3.tuple3(state, childState, childState);
    RootStateBuilder fst = new RootStateBuilder(state);
    RootStateBuilder snd = new RootStateBuilder(other.state);
    fst.makeCompatible(snd);
    D newChildStateOfFst = fst.applyChildOps(childState);
    D newChildStateOfSnd = snd.applyChildOps(other.childState);
    return P3.tuple3(fst.build(), newChildStateOfFst, newChildStateOfSnd);
  }


  /* ***********
   * Operations from interface RootDomain
   *********** */

  @Override public Root<D> eval (Assign assign) {
    RootStateBuilder builder = new RootStateBuilder(state);
    Lhs lhs = assign.getLhs();
    builder.addRegister(lhs.getRegionId());
    Rhs rhs = assign.getRhs();
    for (Rvar var : RvarExtractor.fromRhs(rhs)) {
      builder.addRegister(var.getRegionId());
    }
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.evalAssign(lhs, rhs);
    return build(builder.build(), newChildState);
  }

  @Override public Root<D> eval (Load load) {
//    RootStateBuilder builder = new RootStateBuilder(state);
//    Lhs lhs1 = load.getLhs();
//    builder.addRegister(lhs1.getRegionId());
//    Rval rhs = load.getReadAddress();
//    for (Rvar var : RvarExtractor.fromRhs(rhs)) {
//      builder.addRegister(var.getRegionId());
//    }
//    RootRegionAccess xref = builder.resolveReference(rhs, childState);
//    D newChildState = builder.applyChildOps(childState);
//    if (xref == null) {
//      // the reference could not be resolved thus assign top to the lhs
//      RhsFactory imp = RhsFactory.getInstance();
//      Lhs lhs = load.getLhs();
//      newChildState = newChildState.evalAssign(lhs, imp.arbitrary(lhs.getSize()));
//    } else {
//      newChildState = newChildState.evalLoad(lhs1, xref.location);
//    }
//    return build(builder.build(), newChildState);
    /**
     * FIXME: New RReil grammar
     */
    throw new RReilGrammarException();
  }

  @Override public Root<D> eval (Store store) {
//    RootStateBuilder builder = new RootStateBuilder(state);
//    Rval addr = store.getWriteAddress();
//    for (Rvar var : RvarExtractor.fromRval(addr)) {
//      builder.addRegister(var.getRegionId());
//    }
//    Rval rhs = store.getRhs();
//    for (Rvar var : RvarExtractor.fromRhs(rhs)) {
//      builder.addRegister(var.getRegionId());
//    }
//    RootRegionAccess xref = builder.resolveReference(addr, childState);
//    if (xref == null)
//      throw new UnimplementedMethodException();
//    D newChildState = builder.applyChildOps(childState);
//    newChildState = newChildState.evalStore(xref.location, rhs);
//    return build(builder.build(), newChildState);
    /**
     * FIXME: New RReil grammar
     */
    throw new RReilGrammarException();
  }

  @Override public Root<D> eval (Test test) {
    RootStateBuilder builder = new RootStateBuilder(state);
    for (Rvar var : RvarExtractor.fromRhs(test.getComparison())) {
      builder.addRegister(var.getRegionId());
    }
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.eval(test);
    return build(builder.build(), newChildState);
  }

  @Override public List<P2<RReilAddr, Root<D>>> eval (Branch branch, ProgramPoint current, ProgramPoint next) {
    Range targets = childState.queryRange(branch.getTarget());
    if (targets == null)
      targets = Range.from(Interval.top());
    Root<D> thisState = build(state, childState);
    if (targets.isConstant()) { // handle single target case for performance reasons
      P2<RReilAddr, Root<D>> singleTarget =
        P2.tuple2(RReilAddr.valueOf(targets.getConstantOrNull()), thisState);
      return Collections.singletonList(singleTarget);
    }
    int maxTargets = 100;
    BigInt r = targets.numberOfDiscreteValues(); // maximum number of branch targets that we want to allow before
                                                 // flagging it as an error
    if (targets.isFinite() && r.isLessThan(BigInt.of(maxTargets))) {
      List<P2<RReilAddr, Root<D>>> result =
        new LinkedList<P2<RReilAddr, Root<D>>>();
      for (BigInt value : targets) {
        result.add(P2.tuple2(RReilAddr.valueOf(value), thisState));
      }
      return result;
    }
    getContext().addWarning(new ReadFromUnknownPointerInfo(targets));
    throw new Unreachable();
  }

  @Override public Root<D> eval (PrimOp prim) {
    RootStateBuilder builder = new RootStateBuilder(state);
    if (prim.is("currentStackFrameAddress", 1, 0)) {
      NumVar symbolicAddress = builder.addressOf(MemVar.getVarOrFresh("stack"));
      D newChildState = builder.applyChildOps(childState);
      newChildState = newChildState.assignSymbolicAddressOf(prim.getOutArgs().get(0), symbolicAddress);
      return build(builder.build(), newChildState);
    } else if (prim.is("fixAtConstantAddress", 0, 1)) {
      MemVar region = null;
      Rhs rval = prim.getInArgs().get(0);
      if (rval instanceof Rvar)
        region = ((Rvar) rval).getRegionId();
      if (region != null && builder.contexts.contains(region)) {
        RegionCtx rCtx = builder.contexts.getOrNull(region);
        builder.addSegmentCtx(rCtx.getSegment().get());
        return build(builder.build(), childState);
      }
    } else if (prim.is("addressOf", 1, 1)) {
      MemVar region = null;
      Rhs rval = prim.getInArgs().get(0);
      if (rval instanceof Rvar)
        region = ((Rvar) rval).getRegionId();
      if (region != null) {
        NumVar symbolicAddress = builder.addressOf(region);
        D newChildState = builder.applyChildOps(childState);
        newChildState = newChildState.assignSymbolicAddressOf(prim.getOutArgs().get(0), symbolicAddress);
        return build(builder.build(), newChildState);
      }
    } else if (prim.is("addRegisters", 0, Integer.MAX_VALUE)) {
      D newChildState = childState;
      for (int i = 0; i < prim.getInArgs().size(); i++) {
        Rvar variable = (Rvar) prim.getInArg(i);
        MemVar id = variable.getRegionId();
        builder.addRegister(id);
        newChildState = newChildState.introduceRegion(id, RegionCtx.EMPTYSTICKY);
      }
      return build(builder.build(), newChildState);
    } else {
      getContext().addWarning(new PrimitiveNotFound(prim));
      throw new Unreachable();
    }
    return null;
  }

  @Override public Root<D> introduceRegion (MemVar region, RegionCtx regionCtx) {
    RootStateBuilder builder = new RootStateBuilder(state);
    D newChildState = childState.introduceRegion(region, regionCtx);
    if (regionCtx.isAddressable()) {
      NumVar addressVar = NumVar.freshAddress();
      Option<BigInt> concreteAddress = regionCtx.getAddress();
      if (concreteAddress.isSome())
        builder.concreteAddresses = builder.concreteAddresses.bind(concreteAddress.get(), region);
      newChildState = newChildState.introduce(addressVar, Type.Address, concreteAddress);
      builder.setAddressOf(region, addressVar);
    }
    builder.introduce(region, regionCtx);
    return build(builder.build(), newChildState);
  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // TODO implement in PrettyDomain
    throw new UnimplementedException();

  }
}
