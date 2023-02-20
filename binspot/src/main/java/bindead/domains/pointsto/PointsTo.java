package bindead.domains.pointsto;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLSet;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.DebugChannel;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.combinators.FiniteFunctor;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.finitesupportset.FiniteSupportSet;
import bindead.domains.pointsto.PointsToSet.PointsToEntry;
import bindead.domains.segments.warnings.IllegalPointerArithmetics;
import bindead.exceptions.Unreachable;

/**
 * Implementation of the points-to domain.
 */
public final class PointsTo<D extends FiniteDomain<D>> extends FiniteFunctor<PointsToState, D, PointsTo<D>> {
  protected final boolean DEBUGOTHER = PointsToProperties.INSTANCE.debugOther.isTrue();
  public final static String NAME = "POINTSTO";
  private final static FiniteFactory fin = FiniteFactory.getInstance();
  private static final boolean checkSupport = false;

  public PointsTo (final D child) {
    super(NAME, PointsToState.EMPTY, child);
  }

  private PointsTo (PointsToState state, D childState) {
    super(NAME, state, childState);
  }

  protected void msg (String msg) {
    if (DEBUGOTHER) {
      System.out.println("\n" + name + ": " + msg);
    }
  }

  @Override public P3<PointsToState, D, D> makeCompatible (PointsTo<D> other, boolean isWideningPoint) {
    MakeCompatibleWorker<D> worker = new MakeCompatibleWorker<D>(state, other.state);
    return worker.makeCompatible(childState, other.childState);
  }

  private D assumeConst (NumVar numVar, D childDomain, BigInt val) {
    return childDomain.eval(fin.equalTo(0, Linear.linear(numVar), Linear.linear(val)));
  }

  private D assumePtsIsNull (PointsToSet targets, D newChildState) {
    if (!targets.sumOfFlags.isConstantZero())
      assumeConst(targets.sumOfFlags.numVar, newChildState, Bound.ZERO);
    for (PointsToEntry p : targets) {
      newChildState = assumeConst(p.flag, newChildState, Bound.ZERO);
    }
    return newChildState;
  }

  @Override public PointsTo<D> build (PointsToState s, D cs) {
    checkSupportSet(s, cs);
    // clean up zero entries from points-to sets
    VarSet zeros = VarSet.empty();
    for (Linear x : getSynthChannel().getEquations())
      if (x.isSingleTerm() && x.getConstant().isZero()) {
        zeros = zeros.add(x.getKey());
        // System.out.println("equation " + zeros);
      }
    for (PointsToSet pts : s.getPointsToMap()) {
      for (P2<AddrVar, PointsToEntry> entry : pts.entryMap) {
        NumVar flag = entry._2().flag;
        boolean isZero = zeros.contains(flag) || cs.queryRange(flag).isZero();
        if (isZero) {
          cs = cs.project(flag);
          s = s.removePtsEntry(pts.var, entry._1());
        }
      }
    }
    return new PointsTo<D>(s, cs);
  }

  private static <D extends FiniteDomain<D>> void checkSupportSet (PointsToState s, D cs) {
    if (checkSupport)
      if (cs instanceof FiniteSupportSet) {
        @SuppressWarnings("unchecked")
        VarSet allChildVars = ((FiniteSupportSet<D>) cs).state.getSupportSet();
        VarSet allStateVars = s.varsInChildDomain();
        VarSet d1 = allChildVars.difference(allStateVars);
        VarSet d2 = allStateVars.difference(allChildVars);
        assert d1.isEmpty() : "pts domain does not have : " + d1;
        assert d2.isEmpty() : "inner domain does not have " + d2;
      }
  }

  private PointsTo<D> build (PointsToStateBuilder<D> builder) {
    D child = builder.applyChildOps(childState);
    return build(builder, child);
  }

  private PointsTo<D> build (PointsToStateBuilder<D> builder, D cs) {
    return build(builder.build(), cs);
  }

  @Override public PointsTo<D> copyAndPaste (VarSet vars, PointsTo<D> from) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.copyAndPaste(vars, from);
    return build(builder);
  }

  @Override public P2<AVLSet<AddrVar>, PointsTo<D>> deref (Rlin ptr)
      throws Unreachable {
    D cs = childState;
    HyperPointsToSet hyperPtr =
      new RhsTranslator(0, state, getContext().getWarningsChannel()).run(ptr);

    Linear toRestrict = hyperPtr.sumOfFlags;
    String msg = hyperPtr.toString();
    cs = restrictToZeroOneInterval(cs, toRestrict, msg);

    for (@SuppressWarnings("unused")
    P2<AddrVar, Linear> entry : hyperPtr) {
      // TODO HSI test for _2 < 0 => flag error
      // TODO HSI test for _2 > 1 => flag error
      // TODO HSI trim _2 to [0,1]
    }
    AVLSet<AddrVar> targets = AVLSet.<AddrVar>empty();
    for (P2<AddrVar, Linear> x : hyperPtr.getTerms()) {
      targets = targets.add(x._1());
    }
    return new P2<AVLSet<AddrVar>, PointsTo<D>>(targets, build(state, cs));
  }

  private D restrictToZeroOneInterval (D cs, Linear toRestrict, String msg) {
    Range sumRange = cs.queryRange(toRestrict);
    if (!sumRange.isFinite() || sumRange.getMax().isGreaterThan(BigInt.ONE)) {
      getContext().addWarning(new IllegalPointerArithmetics(msg));
      Test t = fin.unsignedLessThanOrEqualTo(0, toRestrict, Linear.ONE);
      cs = cs.eval(t);
    }
    if (!sumRange.isFinite() || sumRange.getMin().isLessThan(BigInt.ZERO)) {
      getContext().addWarning(new IllegalPointerArithmetics(msg));
      Test t = fin.unsignedLessThanOrEqualTo(0, Linear.ZERO, toRestrict);
      cs = cs.eval(t);
    }
    return cs;
  }

  @Deprecated @Override public List<P2<NumVar.AddrVar, PointsTo<D>>> deprecatedDeref (Rlin ptr, VarSet summaries)
      throws Unreachable {
    List<P2<AddrVar, PointsTo<D>>> res =
      new LinkedList<P2<NumVar.AddrVar, PointsTo<D>>>();
    try {
      res.add(derefNullAlternative(ptr));
    } catch (Unreachable e) {
      // ignore bottom
    }
    HyperPointsToSet hyperPtr =
      new RhsTranslator(0, state, getContext().getWarningsChannel()).run(ptr);
    Test testSumOfFlags = fin.equalTo(0, hyperPtr.sumOfFlags, Linear.ONE);
    for (P2<AddrVar, Linear> term : hyperPtr.getTerms()) {
      try {
        Linear flagCoeff = term._2();
        PointsToStateBuilder<D> altBuilder = new PointsToStateBuilder<D>(state);
        AddrVar targetAddress = term._1();
        PointsTo<D> altState = build(altBuilder.build(), altBuilder.applyChildOps(childState));
        res.add(new P2<NumVar.AddrVar, PointsTo<D>>(targetAddress, altState));
        altBuilder.getChildOps().addTest(testSumOfFlags);
        if (!summaries.contains(targetAddress)) {
          Test test = fin.equalTo(0, flagCoeff, Linear.ONE);
          altBuilder.getChildOps().addTest(test);
          // msg("found non-summary " + targetAddress);
        } else {
          // msg("found summary " + targetAddress);
        }
      } catch (Unreachable e) {
        // ignore bottom
      }
    }
    return res;
  }

  private P2<AddrVar, PointsTo<D>> derefNullAlternative (Rlin ptr) {
    D newChildState = childState;
    Term[] terms = ptr.getLinearTerm().getTerms();
    for (Term term : terms) {
      NumVar varId = term.getId();
      PointsToSet targets = state.getPts(varId);
      newChildState = assumePtsIsNull(targets, newChildState);
    }
    PointsTo<D> dom = build(state, newChildState);
    // here we might remove the variables that are set to zero from the pts and from newChildState.
    return new P2<AddrVar, PointsTo<D>>(null, dom);
  }

  @Override public PointsTo<D> eval (Assign assign) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    int size = assign.getLhs().getSize();
    NumVar lhsVar = assign.getLhs().getId();
    Rhs rhs = assign.getRhs();
    assert !lhsVar.isAddress();
    HyperPointsToSet hyperRhs =
      state.translate(assign.getLhs().getSize(), rhs, childState.getContext().getWarningsChannel());
    builder.evalAssign(size, lhsVar, hyperRhs);
    return build(builder);
  }

  @Override public PointsTo<D> eval (Test test) {
    msg("test " + test);
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    TestTranslator<D> testTranslator = new TestTranslator<D>(builder, childState.getContext().getWarningsChannel());
    //msg("test " + test);
    Rlin le = fin.linear(test.getSize(), test.getLeftExpr());
    Rlin re = fin.linear(test.getSize(), test.getRightExpr());
    HyperPointsToSet left = state.translate(test.getSize(), le, getContext().getWarningsChannel());
    HyperPointsToSet right = state.translate(test.getSize(), re, getContext().getWarningsChannel());
    D cs = testTranslator.run(childState, test, left, right);
    assert builder.getChildOps().isEmpty();
    if (cs == null)
      throw new Unreachable();
    return build(builder, cs);
  }

  /**
   * If a functor domain uses the default {@link querySynth} implementation
   * then this function is called to check with of the passed-in variables are
   * local to the functor domain. A functor domain that introduces its own
   * variables must override this method by one which returns the subset of
   * the input variables that are only known within this domain. This subset
   * is then removed from all information propagated up the synthesized
   * channel.
   *
   * @param vars
   *          the variable set in the synthesized channel
   * @return the subset of {@code toTest} that are owned by this functor
   *         domain
   */
  @Override public VarSet localSubset (VarSet vars) {
    VarSet res = VarSet.empty();
    for (NumVar var : vars)
      if (state.isLocal(var)) {
        res.add(var);
      }
    return res;
  }

  @Override public PointsTo<D> introduce (NumVar variable, Type type,
      Option<BigInt> value) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.introduce(variable, type);
    PointsTo<D> res = build(builder.build(), childState.introduce(variable, type, value));
    return res;
  }

  @Override public PointsTo<D> project (NumVar var) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.project(var, childState);
    return build(builder);
  }

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    if (!state.isScalar(variable))
      return SetOfEquations.empty();
    VarSet toFilter = state.varsNotExportingEqualities().union(state.getNonScalars());
    SetOfEquations eqs = childState.queryEqualities(variable);
    eqs = eqs.removeVariables(toFilter);
    return eqs;
  }

  @Override public SynthChannel getSynthChannel () {
    SynthChannel channel = childState.getSynthChannel();
    VarSet toFilter = localSubset(channel.getVariables()).union(state.getNonScalars());
    return channel.removeVariables(toFilter);
  }

  @Override public DebugChannel getDebugChannel () {
    return new DebugChannel(childState.getDebugChannel()) {
      @Override public PointsToSet queryPointsToSet (NumVar variable) {
        return queryPts(variable);
      }
    };
  }

  PointsToSet queryPts (NumVar var) {
    return state.getPts(var);
  }

  /**
   * {@inheritDoc}
   */
  @Override public Range queryRange (Linear lin) {
    HyperPointsToSet hpts = new HyperPointsToSet(0);
    Linear newLin = Linear.ZERO;
    // just for optimization, shouldn't make a difference:
    if (state.isScalar(lin))
      return childState.queryRange(lin);
    for (Term t : lin) {
      BigInt coefficient = t.getCoeff();
      NumVar termVar = t.getId();
      for (PointsToEntry p : state.getPts(termVar)) {
        // TODO HSI if one of the components is a summary, return top
        // but for that we need to know which MemVar belongs to a summary region.
        // This is not an issue as long as we do not perform address calculations
        // involving more than one abstract address
        hpts.addCoefficient(p.address, Linear.linear(coefficient, p.flag));
      }
      if (termVar.isAddress()) {
        hpts.addCoefficient((AddrVar) termVar, Linear.linear(coefficient));
      } else {
        newLin = newLin.add(Linear.linear(coefficient, termVar));
      }
    }
    for (P2<AddrVar, Linear> e : hpts.getTerms()) {
      BigInt coeff = childState.queryRange(e._2()).getConstantOrNull();
      if (coeff != null) {
        newLin = newLin.addTerm(coeff, e._1());
      } else
        return Range.from(Interval.TOP);
    }
    newLin = newLin.add(lin.getConstant());
    Range r = childState.queryRange(newLin);
    return r;
  }

  @Override public PointsTo<D> substitute (NumVar x, NumVar y) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.substitute(x, y);
    return build(builder);
  }

  @Override public PointsTo<D> assumeConcrete (NumVar var) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.assumeConcrete(var);
    return build(builder);
  }

  @Override public PointsTo<D> assumePointsToAndConcretize (Rlin pointerVar, AddrVar target, VarSet contents) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    HyperPointsToSet hyperRhs =
      state.translate(pointerVar.getSize(), pointerVar, childState.getContext().getWarningsChannel());
    builder.assumePointsTo(hyperRhs, target, contents);
    return build(builder);
  }

  @Override public Collection<AddrVar> findPossiblePointerTargets (NumVar id) throws Unreachable {
    return state.findAllPossibleEdges(id);
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    PointsToSet pts = state.getPts(var);
    pts.appendInfo(builder, childState);
  }

  @Override public Range queryEdgeFlag (NumVar src, AddrVar tgt) {
    PointsToEntry en = state.getPts(src).getEntry(tgt);
    if (en == null)
      return Range.from(0);
    return childState.queryRange(en.flag);
  }

  @Override public PointsTo<D> assumeEdgeFlag (NumVar src, AddrVar tgt, BigInt value) {
    PointsToEntry en = state.getPts(src).getEntry(tgt);
    if (en == null) {
      if (value.isZero())
        return this;
      else
        throw new Unreachable();
    }
    D cs = childState.eval(fin.equalTo(0, Linear.linear(en.flag), Linear.linear(value)));
    if (value.isZero()) {
      PointsToState s2 = state.removePtsEntry(src, tgt);
      cs = cs.project(en.flag);
      return build(s2, cs);
    }
    return build(state, cs);
  }


  @Override public PointsTo<D> copyVariable (NumVar to, NumVar from) {
    assert !to.isAddress();
    assert !from.isAddress();
    assert state.hasPts(from);
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    // XXX assign ghost nodes when creating edgenodes???

    PointsToSet fromPts = state.getPts(from);

    PointsToSet targetPts = PointsToSet.empty(to);

    if (fromPts.hasGhostNode()) {
      NumVar newGhost = NumVar.fresh();
      targetPts = targetPts.withGhostNode(newGhost);
      builder.getChildOps().addHardcopy(newGhost, fromPts.outFlagOfGhostNode);
    }

    if (!fromPts.sumOfFlags.isConstantZero()) {
      NumVar newSumFlag = NumVar.fresh();
      targetPts = targetPts.withSumVar(newSumFlag);
      builder.getChildOps().addHardcopy(newSumFlag, fromPts.sumOfFlags.numVar);
    }

    for (PointsToEntry entry : fromPts) {
      NumVar newFlag = NumVar.fresh();
      builder.getChildOps().addHardcopy(newFlag, entry.flag);
      targetPts = targetPts.bind(entry.address, newFlag);
    }

    builder.bindPts(targetPts);
    builder.getChildOps().addHardcopy(to, from);
    return build(builder);
  }

  @Override public PointsTo<D> assumeEdgeNG (Rlin pointerVar, AddrVar targetAddr) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    HyperPointsToSet hyperRhs =
      state.translate(pointerVar.getSize(), pointerVar, childState.getContext().getWarningsChannel());
    builder.assumePointsTo(hyperRhs, targetAddr);
    return build(builder);
  }

  @Override public PointsTo<D> expandNG (ListVarPair nvps) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.expandNG(nvps);
    return build(builder);
  }

  @Override public PointsTo<D> expandNG (AddrVar summary, AddrVar concrete, ListVarPair nvps) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.expandNG(summary, concrete, nvps);
    return build(builder);
  }

  @Override public PointsTo<D> concretizeAndDisconnectNG (AddrVar s, VarSet cs) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.concretizeAndDisconnectNG(s, cs);
    return build(builder);
  }

  @Override public PointsTo<D> bendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    // msg("bendbackGhostEdges " + s + " " + c + " " + svs + "," + cvs + "\n" + state);
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.bendBackGhostEdgesNG(s, c, svs, cvs, pts, ptc);
    return build(builder);
  }

  // removes all pointers between the two regions
  @Override public PointsTo<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts,
      VarSet ptc) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.bendGhostEdgesNG(summary, concrete, svs, cvs, pts, ptc);
    return build(builder);
  }

  // add compatible flags to nvps, then fold on child domain
  @Override public PointsTo<D> foldNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.foldNG(p, e, nvps);
    return build(builder);
  }

  // assuming there are no pointers between the two regions
  @Override public PointsTo<D> foldNG (ListVarPair nvps) {
    PointsToStateBuilder<D> builder = new PointsToStateBuilder<D>(state);
    builder.foldNG(nvps);
    return build(builder);
  }

  @Override public PointsTo<D> assumeVarsAreEqual (int size, NumVar fst, NumVar snd) {
    PointsToSet left = state.getPts(fst);
    PointsToSet right = state.getPts(snd);
    D cs = assumeSumsOfFlagsAreEqual(left, right, childState);
    cs = assumeGhostFlagsAreEqual(left, right, cs);

    for (PointsToEntry le : left) {
      PointsToEntry re = right.getEntry(le.address);
      if (re == null)
        cs = cs.eval(fin.equalToZero(0, le.flag));
      else
        cs = cs.assumeVarsAreEqual(0, le.flag, re.flag);
    }
    for (PointsToEntry re : right) {
      PointsToEntry le = right.getEntry(re.address);
      if (le == null)
        cs = cs.eval(fin.equalToZero(0, re.flag));
    }
    cs = cs.assumeVarsAreEqual(size, fst, snd);
    return build(state, cs);
  }


  private static <D extends FiniteDomain<D>> D assumeSumsOfFlagsAreEqual (PointsToSet left, PointsToSet right, D cs) {
    if (left.sumOfFlags.isConstantZero() && !right.sumOfFlags.isConstantZero())
      cs = cs.eval(fin.equalToZero(0, right.sumOfFlags.numVar));
    else if (!left.sumOfFlags.isConstantZero() && right.sumOfFlags.isConstantZero())
      cs = cs.eval(fin.equalToZero(0, left.sumOfFlags.numVar));
    else if (!left.sumOfFlags.isConstantZero() && !right.sumOfFlags.isConstantZero())
      cs = cs.assumeVarsAreEqual(0, left.sumOfFlags.numVar, right.sumOfFlags.numVar);
    return cs;
  }

  private static <D extends FiniteDomain<D>> D assumeGhostFlagsAreEqual (PointsToSet left, PointsToSet right, D cs) {
    if (!left.hasGhostNode() && right.hasGhostNode())
      cs = cs.eval(fin.equalToZero(0, right.outFlagOfGhostNode));
    else if (left.hasGhostNode() && !right.hasGhostNode())
      cs = cs.eval(fin.equalToZero(0, left.outFlagOfGhostNode));
    else if (left.hasGhostNode() && right.hasGhostNode())
      cs = cs.assumeVarsAreEqual(0, left.outFlagOfGhostNode, right.outFlagOfGhostNode);
    return cs;
  }
}
