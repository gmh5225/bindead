package bindead.domains.affine;

import static bindead.data.Linear.linear;
import static bindead.debug.StringHelpers.indentMultiline;

import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Range;
import rreil.lang.util.Type;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.abstractsyntax.zeno.util.ZenoExprSimplifier;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.Linear.Divisor;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.exceptions.DomainStateException.VariableSupportSetException;
import bindead.exceptions.Unreachable;

/**
 * The affine domain functor. This domain does not try to keep its child maximally reduced but stores all variables
 * also in the child. Hence, the name redundant.
 *
 * @param <D> The type of the child domain.
 */
public class RedundantAffine<D extends ZenoDomain<D>> extends ZenoFunctor<AffineState, D, RedundantAffine<D>> {
  public static final String NAME = "REDUNDANTAFFINE";
  private static final ZenoFactory zeno = ZenoFactory.getInstance();
  private final boolean DEBUGOTHER = AffineProperties.INSTANCE.debugOther.isTrue();
  private final boolean DEBUGASSIGN = AffineProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean DEBUGBINARIES = AffineProperties.INSTANCE.debugBinaryOperations.isTrue();
  private final boolean DEBUGTESTS = AffineProperties.INSTANCE.debugTests.isTrue();
  private final boolean DEBUGSUBSETOREQUAL = AffineProperties.INSTANCE.debugSubsetOrEqual.isTrue();

  public RedundantAffine (D child) {
    super(NAME, AffineState.EMPTY, child);
  }

  protected RedundantAffine (AffineState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public RedundantAffine<D> build (AffineState state, D childState) {
    return new RedundantAffine<D>(state, childState);
  }

  @Override public RedundantAffine<D> eval (Assign assign) {
    Assign stmt = InliningVisitor.run(assign, state);
    stmt = ZenoExprSimplifier.run(stmt, childState);
    AffineStateBuilder sys = new AffineStateBuilder(state);
    // if the right-hand side is a linear expression, we perform the assignment on the affine domain;
    // otherwise the child domain has to indicate that an equality has arisen during evaluation
    Rhs rhs = stmt.getRhs();
    if (rhs instanceof Rlin) {
      Rlin rlin = (Rlin) rhs;
      // the right-hand side is a linear expression, perform this operation on the affine domain
      Linear le = rlin.getLinearTerm();
      Linear.Divisor d = new Linear.Divisor(rlin.getDivisor());
      sys.affineTrans(d, stmt.getLhs().getId(), le);
      if (DEBUGASSIGN) {
        System.out.println(name + ":");
        System.out.println("  after " + assign + " that is inlined to " + stmt);
        System.out.println("  on child: " + sys.getChildOps());
        System.out.println("  equations: " + sys.build().toString());
      }
    } else {
      sys.promoteVariable(stmt.getLhs().getId());
    }
    AffineState newState = sys.build();

    D newChildState = childState;
    // this is a heuristic to know which assignment yields more precision when applied on the child
    // as because of the inlining we might get a statement that is more precise when executed on
    // a child that is imprecise on linear operations as e.g. the intervals domain
    if (stmt.getRhs().getVars().size() < assign.getRhs().getVars().size())
      assign = stmt;
    newChildState = newChildState.eval(assign); // use the inlined version of the assignment for better precision
    newChildState = refineChildWithEqualities(newState.newEqualities, newState, newChildState);
    return build(newState, newChildState);
  }

  @Override public RedundantAffine<D> eval (Test test) {
    Test stmt = InliningVisitor.run(test, state);
    // The following visitor will throw an Unreachable exception if the affine domain itself is sufficient to tell
    // that the test renders the domain empty.
    if (AnalysisProperties.INSTANCE.debugTests.isTrue())
      System.out.println("Affine test: " + test);
    if (ZenoTestHelper.isTautologyReportUnreachable(stmt)) { // the test was redundant
      if (AnalysisProperties.INSTANCE.debugTests.isTrue())
        System.out.println("Affine swallowed test: " + test);
//      return stripSynthChannel();
      // XXX: actually a bug to return the unchanged domain as we also return the old synthChannel with all the new
      // equalities that were inferred by an older transfer function. As the test was redundant (does not change
      // the state) after inlining it we can return the same state. Although the new equalities do not come
      // from this transfer function this HACK is necessary as sequences of tests are performed sometimes in parent
      // domains and we lose the equalities that were inferred in previous transfer functions. This maintains them
      // as the test was redundant and the equalities are still valid.
      // Nevertheless, it is better to implement a join in the synthChannel and join all the channels where ever
      // a sequence of transfer functions is performed, e.g. FiniteBuilder, ZenoBuilder and PointsTo
      return this;
    }
    // We first need to tell the child about the intersection since it might thereby find out about other equalities
    // that hold. The child will communicate these over the refinement channel. (For any additional equalities to
    // be found, the child domain probably needs to be somehow relational.) Given the equalities from the child
    // and the one in stmt (if any), we add all of them to the affine domain which will modify the child domain
    // accordingly.
    D newChildState = childState.eval(test);
    if (test != stmt)
      newChildState = newChildState.eval(stmt);
    newChildState = refineWithInlinedTests(stmt, newChildState);

    // extract the equalities that are propagated up in the broadcast channel
    SynthChannel synthChannel = newChildState.getSynthChannel();
    SetOfEquations newChildEqualities = synthChannel.getEquations();
    if (stmt.getOperator() == ZenoTestOp.EqualToZero)
      newChildEqualities = newChildEqualities.add(stmt.getExpr().toEquality());
    if (newChildEqualities.isEmpty()) {
      if (DEBUGTESTS) {
        System.out.println(name + ": ");
        System.out.println("  evaluating test: " + test);
        System.out.println("  this: " + state);
        System.out.println(indentMultiline("  before: ", childState.toString()));
        System.out.println(indentMultiline("  after:  ", newChildState.toString()));
      }
      return build(state, newChildState);
    } else {
      AffineStateBuilder sys = new AffineStateBuilder(state);
      for (Linear equality : newChildEqualities) {
        equality = sys.inlineIntoLinear(equality, Linear.Divisor.one());
        sys.intersectWithLinear(equality);
      }
      AffineState newState = sys.build();
      // see if the equality came from the child then we don't need to eval it there again!
      VarSet newConstantVars = newState.newEqualities.difference(synthChannel.getVariables());
      newChildState = refineChildWithEqualities(newConstantVars, newState, newChildState);
      if (DEBUGTESTS) {
        System.out.println(name + ": ");
        System.out.println("  evaluating test: " + stmt);
        System.out.println("  this (before): " + state);
        System.out.println("  this (after):  " + newState);
        System.out.println(indentMultiline("  before: ", childState.toString()));
        System.out.println(indentMultiline("  after:  ", newChildState.toString()));
      }
      return build(newState, newChildState);
    }
  }

  @SuppressWarnings("unused") private RedundantAffine<D> stripSynthChannel () {
    AffineState newState = state.withoutEqualities();
    // HACK: to rebuild the child domain without any old synthchannel info (not reliable as it is not official semantic)
    D newChildState = childState.setContext(getContext());
    return build(newState, newChildState);
  }

  /**
   * Apply the equalities known in the affine to the child for the given vars.
   */
  private D refineChildWithEqualities (VarSet equalityVars, AffineState state, D childState) {
    for (NumVar var : equalityVars) {
      // this lookup should not fail since only leading variables are inserted
      // into newEqualities and because a leading variable cannot be removed by
      // intersecting with other equalities over parameter variables
      Linear newEquality = state.affine.get(var).get().toEquality();
      Test newEqualityTest = zeno.comparison(zeno.linear(newEquality), ZenoTestOp.EqualToZero);
      try {
        childState = childState.eval(newEqualityTest);
      } catch (Unreachable e) {
        // The inlined tests can actually throw unreachable if the child domain was not properly reduced
        // earlier. Propagate this up as the domain should have been bottom already earlier.
        Range value = childState.queryRange(newEquality);
        getContext().addWarning(new UnreducedChildInfo(newEqualityTest, value));
        throw e;
      }
    }
    return childState;
  }

  /**
   * Apply all the tests that can be generated by inlining all known affine equalities for the variables in the given
   * test. This is needed to perform a transitive closure as the redundant affine domain does not project out
   * variables in the child if they are defined through an affine equality. It can thus happen that just applying
   * the original test does not reduce all the variables in the transitive closure of the test variables.
   */
  private D refineWithInlinedTests (Test test, D childState) {
    D newChildState = childState;
    Linear testTerm = test.getExpr();
    for (NumVar var : testTerm.getVars()) {
      for (Linear equality : state.getConstraints(var)) {
        Test newTest = zeno.comparison(equality, ZenoTestOp.EqualToZero);
        newChildState = newChildState.eval(newTest);
      }
    }
    return newChildState;
  }

  @Override public RedundantAffine<D> introduce (NumVar variable, Type type, Option<BigInt> value) {
    if (state.inSupport(variable))
      throw new VariableSupportSetException();
    AffineStateBuilder sys = new AffineStateBuilder(state);
    if (value.isSome()) {
      sys.getChildOps().addIntro(variable);
      sys.affineTrans(Linear.Divisor.one(), variable, Linear.linear(value.get()));
    }
    D newChildState = childState.introduce(variable, type, value);
    return build(sys.build(), newChildState);
  }

  @Override public RedundantAffine<D> project (VarSet vars) {
    AffineStateBuilder sys = new AffineStateBuilder(state);
    for (NumVar variable : vars) {
      sys.removeVariable(variable);
      if (DEBUGOTHER) {
        System.out.println(name + ":");
        System.out.println("  project: " + vars);
        System.out.println("  on child: " + sys.getChildOps());
        System.out.println("  equations: " + sys.build().toString());
        AffineState newCtx = sys.build();
        assert newCtx.notInSupport(variable);
      }
    }
    D newChildState = childState.project(vars);
    AffineState newCtx = sys.build();
    return build(newCtx, newChildState);
  }

  @Override public RedundantAffine<D> substitute (NumVar x, NumVar y) {
    if (state.inSupport(y))
      throw new VariableSupportSetException();
    Linear newEq = state.inlineIntoLinear(Linear.linear(x), Divisor.one());
    AffineStateBuilder sys = new AffineStateBuilder(state);
    sys.affineTrans(Divisor.one(), y, newEq);
    sys.removeVariable(x);
    D newChildState = childState.substitute(x, y);
    return build(sys.build(), newChildState);
  }

  @Override public RedundantAffine<D> expand (FoldMap pairs) {
    List<VarPair> leadingPairs = new LinkedList<VarPair>();
    List<VarPair> parameterPairs = new LinkedList<VarPair>();
    for (VarPair pair : pairs) {
      if (state.affine.contains(pair.getPermanent())) {
        leadingPairs.add(pair);
      } else {
        parameterPairs.add(pair);
      }
    }
    // perform expansion on child
    D newChildState = childState.expand(pairs);

    // infer all lhs variables that contain variables of the tuples
    VarSet lhsVars = VarSet.empty();
    for (VarPair pair : parameterPairs) {
      lhsVars = lhsVars.union(state.reverse.get(pair.getPermanent()).getOrElse(VarSet.empty()));
    }
    for (VarPair pair : leadingPairs) {
      lhsVars = lhsVars.add(pair.getPermanent());
    }

    // intersect the affine domain with the renaming of each equality
    if (lhsVars.size() == 0)
      return build(state, newChildState);

    AffineStateBuilder builder = new AffineStateBuilder(state);
    for (NumVar var : lhsVars) {
      Linear eq = state.affine.get(var).get();
      eq = eq.renameVar(pairs);
      eq = builder.inlineIntoLinear(eq, Linear.Divisor.one());
      builder.intersectWithLinear(eq);
    }
    return build(builder.build(), newChildState);
  }

  @Override public RedundantAffine<D> fold (FoldMap pairs) {
    AffineStateBuilder builder = new AffineStateBuilder(state);
    builder.fold(pairs);
    D newChildState = childState.fold(pairs);
    AffineState built = builder.build();
    return build(built, newChildState);
  }

  @Override public RedundantAffine<D> copyAndPaste (VarSet vars, RedundantAffine<D> from) {
    VarSet paramVars = vars;
    List<Linear> eqsToCopy = new LinkedList<Linear>();
    for (NumVar var : paramVars) {
      Option<Linear> oEq = from.state.affine.get(var);
      if (oEq.isSome()) {
        paramVars = paramVars.remove(var);
        eqsToCopy.add(oEq.get());
      }
    }
    D newChildState = childState.copyAndPaste(vars, from.childState);
    AffineStateBuilder builder = new AffineStateBuilder(state);
    for (Linear eq : eqsToCopy) {
      NumVar key = eq.getKey();
      VarSet varsInEq = eq.getVars().remove(key);
      VarSet varsNotCopied = varsInEq.difference(paramVars);
      if (varsNotCopied.size() == 0) {
        // we're lucky: all variables in the equality are copied, so we can
        // simply insert the equality in the other domain
        builder.insertLinear(eq);
      } else {
        // we need to remove varsNotCopied from eq since they do not exist in
        // this domain; thus, calculate an interval for these variables, set
        // the key var to this interval in builder and then perform an
        // invertible assignment key = eq + key
        Linear queryEq = linear(eq.getConstant());
        eq = eq.dropConstant();
        for (NumVar var : varsNotCopied) {
          queryEq = queryEq.addTerm(eq.getCoeff(var), var);
          eq = eq.dropTerm(var);
        }
        BigInt d = eq.getCoeff(key);
        if (!d.isOne())
          eq = eq.dropTerm(key).addTerm(Bound.ONE, key);
        Range range = from.queryRange(queryEq);
        // set bounds for the equation by applying test with the interval bounds to the child
        if (range.isFinite()) {
          if (range.isConstant()) {
            eq = eq.add(range.getConstantOrNull());
            newChildState =
              newChildState.eval(zeno.comparison(zeno.linear(eq, d), ZenoTestOp.EqualToZero));
          } else {
            Linear upperBoundEquation = eq.add(range.getMax()).negate();
            newChildState =
              newChildState.eval(zeno.comparison(zeno.linear(upperBoundEquation, d), ZenoTestOp.LessThanOrEqualToZero));
            Linear lowerBoundEquation = eq.add(range.getMin());
            newChildState =
              newChildState.eval(zeno.comparison(zeno.linear(lowerBoundEquation, d), ZenoTestOp.LessThanOrEqualToZero));
          }
        }
      }
    }
    return build(builder.build(), newChildState);
  }

  @Override public boolean subsetOrEqual (RedundantAffine<D> other) {
    AffineStateBuilder fst = new AffineStateBuilder(state);
    AffineStateBuilder snd = new AffineStateBuilder(other.state);
    boolean isSubset = fst.makeCompatible(snd, true);
    if (DEBUGSUBSETOREQUAL) {
      System.out.println(name + ":");
      System.out.println("  subset-or-equal: " + isSubset);
      System.out.println("  fst: " + state);
      System.out.println("    on child: " + fst.getChildOps());
      System.out.println("  snd: " + other.state);
      System.out.println("    on child: " + snd.getChildOps());
    }
    // if the affine domain is not subset then there is no need to ask the child
    if (!isSubset)
      return false;
    return childState.subsetOrEqual(other.childState);
  }

  @Override public RedundantAffine<D> join (RedundantAffine<D> other) {
    AffineStateBuilder fst = new AffineStateBuilder(state);
    AffineStateBuilder snd = new AffineStateBuilder(other.state);
    fst.makeCompatible(snd, false);
    if (DEBUGBINARIES) {
      System.out.println(name + ":");
      System.out.println("  fst: " + state);
      System.out.println("    on child: " + fst.getChildOps());
      System.out.println("  snd: " + other.state);
      System.out.println("    on child: " + snd.getChildOps());
    }
    D newChildState = childState.join(other.childState);
    return build(fst.build(), newChildState);
  }

  @Override public RedundantAffine<D> widen (RedundantAffine<D> other) {
    AffineStateBuilder fst = new AffineStateBuilder(state);
    AffineStateBuilder snd = new AffineStateBuilder(other.state);
    fst.makeCompatible(snd, false);
    D newChildState = childState.widen(other.childState);
    // apply any equalities of zeno variables with flags on the child as flags are not widened in intervals
    // the equality must be enforced after widening to still hold
    AffineState newState = fst.build();
    VarSet equalitiesWithFlags = VarSet.empty();
    for (P2<NumVar, Linear> tuple : newState.affine) {
      if (containsFlag(tuple._2()))
        equalitiesWithFlags = equalitiesWithFlags.add(tuple._1());
    }
    newChildState = refineChildWithEqualities(equalitiesWithFlags, newState, newChildState);
    return build(newState, newChildState);
  }

  private static boolean containsFlag (Linear equality) {
    for (NumVar variable : equality.getVars()) {
      if (variable.isFlag())
        return true;
    }
    return false;
  }

  @Override public Range queryRange (Linear expr) {
    Linear.Divisor d = Linear.Divisor.one();
    Linear inlined = AffineState.inlineIntoLinear(expr, d, state.affine);
    if (inlined.isConstantOnly() && d.get().isEqualTo(Bound.ONE))
      return Range.from(inlined.getConstant());
    // Range result = childState.queryRange(inlined).div(d.get());
    // bm XXX: we query the child with the expr and not the inlined one because we are mostly more precise with that
    // should we use both and do a meet on the results?
    Range result = childState.queryRange(expr);
    if (DEBUGOTHER) {
      System.out.println(name + ":");
      System.out.println("  query: " + expr + " that is: " + inlined);
      System.out.println("  equations: " + state);
      System.out.println("  divisor: " + d.get());
      System.out.println("  result: " + result);
    }
    return result;
  }

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    SetOfEquations equalities = SetOfEquations.empty();
    Linear eq = state.affine.get(variable).getOrNull();
    // `variable` is a key
    if (eq != null)
      equalities = equalities.add(eq);
    // `variable` is not a key
    VarSet usedIn = state.reverse.get(variable).getOrElse(VarSet.empty());
    for (NumVar x : usedIn)
      equalities = equalities.add(state.affine.get(x).get());
    return equalities;
  }

  @Override public SynthChannel getSynthChannel () {
    SynthChannel synth = super.getSynthChannel().clone(); // get child channel with local vars removed
    for (NumVar var : state.newEqualities) {
      // this lookup should not fail since only leading variables are inserted
      // into newEqualities and because a leading variable cannot be removed by
      // intersecting with other equalities over parameter variables
      synth.addEquation(state.affine.get(var).get());
    }
    return synth;
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    state.appendVar(builder, var, childState);
  }

}
