package bindead.domains.undef;

import java.util.Collection;
import java.util.LinkedList;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLSet;
import rreil.lang.ComparisonOp;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Cmp;
import bindead.abstractsyntax.finite.Finite.FiniteRangeRhs;
import bindead.abstractsyntax.finite.Finite.Lhs;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.abstractsyntax.finite.util.VarExtractor;
import bindead.data.Linear;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.NumVar.FlagVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.combinators.FiniteFunctor;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.segments.warnings.UninitializedValue;
import bindead.exceptions.Unreachable;

public final class Undef<D extends FiniteDomain<D>> extends FiniteFunctor<UndefState, D, Undef<D>> {
  public static final String NAME = "UNDEF";
  private static final FiniteFactory finite = FiniteFactory.getInstance();

  public Undef (final D child) {
    super(NAME, UndefState.empty(), child);
  }

  private Undef (UndefState state, final D child) {
    super(NAME, state, child);
  }

  @Override public Undef<D> build (UndefState state, D childState) {
    return new Undef<D>(state, childState);
  }

  @Override public Undef<D> eval (Assign stmt) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    NumVar lhsVar = stmt.getLhs().getId();
    Rhs rhs = stmt.getRhs();
    VarSet rhsVars = VarExtractor.get(rhs);

    D newChildState = childState;
    if (rhs instanceof FiniteRangeRhs && ((FiniteRangeRhs) rhs).getRange().isTop()) {
      // assigning TOP to lhs will make it undefined
      builder.project(lhsVar);
      builder.addUndefined(lhsVar);
      newChildState = builder.applyChildOps(newChildState);
    } else if (builder.hasUndefined(rhsVars)) {
      // if one of the vars in rhs is undefined -> lhs will be undefined
      builder.project(lhsVar);
      builder.addUndefined(lhsVar);
      newChildState = builder.applyChildOps(newChildState);
    } else {
      // the vars in rhs are either all defined or some depend on flags
      VarSet rhsFlags = builder.getFlagsFor(rhsVars);
      if (rhsFlags.isEmpty()) {
        // all vars in rhs are defined -> lhs will be defined
        builder.promoteToChild(lhsVar); // if lhs is undefined it will be put in a new partition here
        builder.removeFromPartition(lhsVar); // if lhs is in a partition, then make it defined
        newChildState = builder.applyChildOps(newChildState);
        newChildState = newChildState.eval(stmt);
      } else if (rhsFlags.size() == 1) {
        // all vars with flags in rhs are in the same partition -> add lhs to the same partition
        FlagVar rhsFlag = (FlagVar) rhsFlags.first();
        builder.promoteToChild(lhsVar);
        VarSet lhsFlag = builder.getFlagsFor(lhsVar);
        // do not remove the partition of lhs if we have an invertible assignment, e.g. x = x + 1
        if (!lhsFlag.isEmpty() && !lhsFlag.first().equalTo(rhsFlag))
          builder.removeFromPartition(lhsVar);

        builder.addToPartition(rhsFlag, lhsVar);
        newChildState = builder.applyChildOps(newChildState);
        newChildState = newChildState.eval(stmt);
      } else {
        // not all vars in rhs are defined -> flag of lhs is 1 only if all flags in rhs are 1
        // we express this by applying the assignment "lhsFlag = sumOf(rhsFlags) == |rhsFlags|" on the child
        builder.promoteToChild(lhsVar); // if lhs is undefined it will be put in a new partition here
        // as lhs might be itself part of rhs and in a partition we cannot move it over to the
        // new partition before we express the relation between the flags in the child.
        // This is because removing lhs from a partition where it is the only member
        // would remove the partition flag in the child, too.
        FlagVar newLhsFlag = builder.addEmptyPartition();
        newChildState = builder.applyChildOps(newChildState);
        int size = 1;
        Rlin sumOfFlags = finite.linear(size, buildSumOfVars(rhsFlags));
        Rlin numberOfFlags = finite.literal(size, BigInt.of(rhsFlags.size()));
        Cmp cmp = finite.comparison(sumOfFlags, ComparisonOp.Cmpeq, numberOfFlags);
        Lhs lhs = finite.variable(size, newLhsFlag);
        newChildState = newChildState.eval(finite.assign(lhs, cmp));
        // we might already know the valuation of the new partition flag, so reducing Undef is possible
        // NOTE: the synth channel is too unreliable, thus using the query method below
//        builder.reduceFromNewEqualities(newChildState.getSynthChannel().getEquations());
        builder.reduceWithQuery(newChildState, rhsFlags);
        // now it is safe to move lhs over to the new partition if it still exists
        builder.removeFromPartition(lhsVar);
        if (builder.isFlag(newLhsFlag))
          builder.addToPartition(newLhsFlag, lhsVar);
        newChildState = builder.applyChildOps(newChildState);
        newChildState = newChildState.eval(stmt);
      }
    }
    return build(builder.build(), newChildState);
  }

  private static Linear buildSumOfVars (VarSet variables) {
    Linear sum = Linear.ZERO;
    for (NumVar var : variables) {
      sum = sum.add(var);
    }
    return sum;
  }

  @Override public Undef<D> eval (Test stmt) {
    VarSet vars = VarExtractor.get(stmt);
    UndefStateBuilder builder = new UndefStateBuilder(state);
    D newChildState = childState;
    VarSet flags = builder.getFlagsFor(vars);
    if (!someMayBeZero(flags) && !builder.hasUndefined(vars)) {
      newChildState = newChildState.eval(stmt);
      // NOTE: the synth channel is too unreliable, thus using the query method below
//      builder.reduceFromNewEqualities(newChildState.getSynthChannel().getEquations());
      builder.reduceWithQuery(newChildState, flags);
      newChildState = builder.applyChildOps(newChildState);
    } else {
      builder.promoteToChild(vars);
      newChildState = builder.applyChildOps(newChildState);
      int size = 1;
      Linear sumOfFlags = buildSumOfVars(flags);
      Linear numberOfFlags = Linear.linear(BigInt.of(flags.size()));

      D allVarsDefinedChildState = newChildState;
      try { // the test applied on the state if all flags would be 1 -> "sumOf(varsFlags) = size(varsFlags)"
        // execute the test on child
        allVarsDefinedChildState = allVarsDefinedChildState.eval(stmt);
        // apply the condition for the flag
        Test test = finite.equalTo(size, sumOfFlags, numberOfFlags);
        allVarsDefinedChildState = allVarsDefinedChildState.eval(test);
      } catch (Unreachable _) {
        allVarsDefinedChildState = null;
      }
      D someVarsDefinedChildState = newChildState;
      try { // the test applied on the state where some flags would be 0 -> "sumOf(varsFlags) < size(varsFlags)"
        // apply the condition for the flag
        Test test = finite.unsignedLessThan(size, sumOfFlags, numberOfFlags);
        someVarsDefinedChildState = someVarsDefinedChildState.eval(test);
      } catch (Unreachable _) {
        someVarsDefinedChildState = null;
      }
      newChildState = joinNullables(allVarsDefinedChildState, someVarsDefinedChildState);
      if (newChildState == null)
        throw new Unreachable();
      // NOTE: the synth channel is too unreliable, thus using the query method below
//      builder.reduceFromNewEqualities(newChildState.getSynthChannel().getEquations());
      builder.reduceWithQuery(newChildState, flags);
      newChildState = builder.applyChildOps(newChildState);
    }
    return build(builder.build(), newChildState);
  }

  @Override public P3<UndefState, D, D> makeCompatible (Undef<D> other, boolean isWideningPoint) {
    UndefStateBuilder thisBuilder = new UndefStateBuilder(state);
    UndefStateBuilder otherBuilder = new UndefStateBuilder(other.state);
    thisBuilder.makeCompatible(childState, otherBuilder, other.childState);
    D newChildStateOfFst = thisBuilder.applyChildOps(childState);
    D newChildStateOfSnd = otherBuilder.applyChildOps(other.childState);
    UndefState newState = thisBuilder.build();
    return P3.<UndefState, D, D>tuple3(newState, newChildStateOfFst, newChildStateOfSnd);
  }

  @Override public Undef<D> introduce (NumVar numericVariable, Type type, Option<BigInt> value) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    D newChildState = childState;
    if (value.isNone() && type != Type.Address)
      builder.addUndefined(numericVariable);
    else
      newChildState = newChildState.introduce(numericVariable, type, value);
    return build(builder.build(), newChildState);
  }

  @Override public Undef<D> project (NumVar variable) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    builder.project(variable);
    D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Undef<D> substitute (NumVar x, NumVar y) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    builder.substitute(x, y);
    D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Undef<D> foldNG (ListVarPair vars) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    ListVarPair childVars = builder.foldNG(vars);
    builder.getChildOps().addFoldNG(childVars);
    D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Undef<D> foldNG (AddrVar p, AddrVar e, ListVarPair vars) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    ListVarPair childVars = builder.foldNG(vars);
    builder.getChildOps().addFoldNG(p, e, childVars);
    D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Undef<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts,
      VarSet ptc) {
    return build(state, childState.bendGhostEdgesNG(summary, concrete, svs, cvs, pts, ptc));
  }

  @Override public Undef<D> bendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    // XXX do we need to do anything here?
    /*for (VarPair vp : vps) {
      if (state.undefined.contains(c))
        assert !state.undefined.contains(vp.getPermanent());
      assert !state.undefined.contains(vp.getEphemeral());
    }*/
    return build(state, childState.bendBackGhostEdgesNG(s, c, svs, cvs, pts, ptc));
  }

  @Override public Undef<D> copyAndPaste (VarSet vars, Undef<D> from) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    builder.copyAndPaste(vars, from.state, from.childState);
    D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Range queryRange (Linear linear) {
    VarSet vars = linear.getVars();
    UndefStateBuilder builder = new UndefStateBuilder(state);
    if (builder.hasUndefined(vars))
      return Range.from(Interval.TOP);
    VarSet flags = builder.getFlagsFor(vars);
    if (someMayBeZero(flags))
      return Range.from(Interval.TOP);
    return childState.queryRange(linear);
  }

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    if (builder.isUndefined(variable))
      return SetOfEquations.empty();
    return childState.queryEqualities(variable);
  }

  @SuppressWarnings("unused") private boolean allFlagsTrue (VarSet flags) {
    int size = 1;
    Linear sumOfFlags = buildSumOfVars(flags);
    Linear numberOfFlags = Linear.linear(BigInt.of(flags.size()));
    try {
      Test test = finite.equalTo(size, sumOfFlags, numberOfFlags);
      childState.eval(test);
    } catch (Unreachable _) {
      return false;
    }
    return true;
  }

  @Override public VarSet localSubset (VarSet toTest) {
    return state.getFlagVars(toTest);
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    if (state.undefined.contains(var))
      builder.append("T");
    else {
      shudf(builder, var);
      childState.varToCompactString(builder, var);
    }
  }

  private void shudf (StringBuilder builder, NumVar var) {
    Option<FlagVar> r = state.reverse.get(var);
    if (r.isSome()) {
      childState.varToCompactString(builder, r.get());
      builder.append("→");
    }
  }

  @Override public Undef<D> copyVariable (NumVar to, NumVar from) {
    UndefStateBuilder builder1 = new UndefStateBuilder(state);
    D newChildState = childState;
    if (builder1.isUndefined(from)) {
      // if rhs is undefined -> lhs will be undefined
      builder1.addUndefined(to);
    } else {
      // the vars in rhs are either all defined or some depend on flags
      Option<FlagVar> rhsFlags = builder1.getFlagFor(from);
      if (rhsFlags.isNone()) {
        // all vars in rhs are defined thus lhs is defined
        // just eval the assignment on the child
        newChildState = newChildState.copyVariable(to, from);
      } else if (rhsFlags.isSome()) {
        // all vars with flags in rhs are in the same partition -> add lhs to the same partition
        builder1.addToPartition(rhsFlags.get(), to);
        newChildState = newChildState.copyVariable(to, from);
      }
    }
    return build(builder1.build(), newChildState);
  }

  @Override public P2<AVLSet<AddrVar>, Undef<D>> deref (Rlin ptr) throws Unreachable {
    assertDefined(ptr);
    P2<AVLSet<AddrVar>, D> p2 = childState.deref(ptr);
    return new P2<AVLSet<AddrVar>, Undef<D>>(p2._1(), build(state, p2._2()));
  }

  // XXX this check may also be required for other operations inherited from FiniteFunctor;
  // you will know when mysterious support set errors occur.
  //
  // TODO continue with "safe" substate in case of warning
  private void assertDefined (Rlin ptr) {
    VarSet vars = ptr.getVars();
    for (NumVar v : vars)
      assertDefined(v);
  }

  private void assertDefined (NumVar var) {
    if (mayBeUndefined(var)) {
      getContext().addWarning(new UninitializedValue(var));
      throw new Unreachable();
    }
  }

  private boolean mayBeUndefined (NumVar var) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    if (builder.isUndefined(var))
      return true;
    VarSet flags = builder.getFlagsFor(var);
    return someMayBeZero(flags);
  }

  /**
   * If the SynthChannel was more trustworthy, we could assume that all
   * flags that evaluate to constant value one are reduced, so that
   * the existence of flags indicates that the variables may be undefined.
   *
   * In this case, we might just<br>
   *
   * return !flags.isEmpty();
   *
   * <br>
   * instead of testing all ranges
   * */
  private boolean someMayBeZero (VarSet flags) {
    for (NumVar flag : flags) {
      if (!childState.queryRange(flag).isOne())
        return true;
    }
    return false;
  }

  @Override public Undef<D> assumeEdgeNG (Rlin pointerVar, AddrVar targetAddr) {
    assertDefined(pointerVar);
    return build(state, childState.assumeEdgeNG(pointerVar, targetAddr));
  }

  @Override public Undef<D> expandNG (ListVarPair nvps) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    builder.expandNG(nvps);
    D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Undef<D> expandNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    UndefStateBuilder builder = new UndefStateBuilder(state);
    builder.expandNG(p, e, nvps);
    D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Undef<D> concretizeAndDisconnectNG (AddrVar s, VarSet cs) {
    return build(state, childState.concretizeAndDisconnectNG(s, cs.difference(state.undefined)));
  }

  @Override public Undef<D> assumeVarsAreEqual (int size, NumVar fst, NumVar snd) {
    VarSet vars = VarSet.of(fst, snd);
    UndefStateBuilder builder = new UndefStateBuilder(state);
    D newChildState = childState;
    VarSet flags = builder.getFlagsFor(vars);
    if (!someMayBeZero(flags) && !builder.hasUndefined(vars)) {
      newChildState = newChildState.assumeVarsAreEqual(size, fst, snd);
      builder.reduceFromNewEqualities(newChildState.getSynthChannel().getEquations());
      newChildState = builder.applyChildOps(newChildState);
    } else {
      builder.promoteToChild(vars);
      newChildState = builder.applyChildOps(newChildState);
      int size1 = 1;
      Linear sumOfFlags = buildSumOfVars(flags);
      Linear numberOfFlags = Linear.linear(BigInt.of(flags.size()));

      D allVarsDefinedChildState = newChildState;
      try { // the test applied on the state if all flags would be 1 -> "sumOf(varsFlags) = size(varsFlags)"
        // execute the test on child
        allVarsDefinedChildState = allVarsDefinedChildState.assumeVarsAreEqual(size1, fst, snd);
        // apply the condition for the flag
        Test test = finite.equalTo(size1, sumOfFlags, numberOfFlags);
        allVarsDefinedChildState = allVarsDefinedChildState.eval(test);
      } catch (Unreachable _) {
        allVarsDefinedChildState = null;
      }
      D someVarsDefinedChildState = newChildState;
      try { // the test applied on the state where some flags would be 0 -> "sumOf(varsFlags) < size(varsFlags)"
        // apply the condition for the flag
        Test test = finite.unsignedLessThan(size1, sumOfFlags, numberOfFlags);
        someVarsDefinedChildState = someVarsDefinedChildState.eval(test);
      } catch (Unreachable _) {
        someVarsDefinedChildState = null;
      }
      newChildState = joinNullables(allVarsDefinedChildState, someVarsDefinedChildState);
      if (newChildState == null)
        throw new Unreachable();
      builder.reduceFromNewEqualities(newChildState.getSynthChannel().getEquations());
      newChildState = builder.applyChildOps(newChildState);
    }
    return build(builder.build(), newChildState);
  }

  /**
   * Returns a set of possible (abstract) target addresses without restricting the numeric state.
   */
  @Override public Collection<AddrVar> findPossiblePointerTargets (NumVar id) throws Unreachable {
    if (state.undefined.contains(id))
      return new LinkedList<AddrVar>();
    return childState.findPossiblePointerTargets(id);
  }
}
