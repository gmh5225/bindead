package bindead.domains.affine;

import static bindead.data.Linear.linear;

import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
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
 * The affine domain functor.
 *
 * @param <D> The type of the child domain.
 */
public class Affine<D extends ZenoDomain<D>> extends ZenoFunctor<AffineState, D, Affine<D>> {
  public static final String NAME = "AFFINE";
  private static final ZenoFactory zeno = ZenoFactory.getInstance();
  private final boolean DEBUGASSIGN = AffineProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean DEBUGBINARIES = AffineProperties.INSTANCE.debugBinaryOperations.isTrue();
  private final boolean DEBUGTESTS = AffineProperties.INSTANCE.debugTests.isTrue();
  private final boolean DEBUGOTHER = AffineProperties.INSTANCE.debugOther.isTrue();
  private final boolean DEBUGSUBSETOREQUAL = AffineProperties.INSTANCE.debugSubsetOrEqual.isTrue();

  public Affine (D child) {
    super(NAME, AffineState.EMPTY, child);
  }

  private Affine (AffineState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public Affine<D> build (AffineState state, D childState) {
    return new Affine<D>(state, childState);
  }

  @Override public Affine<D> eval (Assign assign) {
    Assign stmt = InliningVisitor.run(assign, state);
    stmt = ZenoExprSimplifier.run(stmt, childState);
    AffineStateBuilder sys = new AffineStateBuilder(state);
    // if the right-hand side is a linear expression, we perform the assignment on the affine domain;
    // otherwise the child domain has to indicate that an equality has arisen during evaluation
    Rhs rhs = stmt.getRhs();
    D newChildState = childState;
    if (rhs instanceof Rlin) {
      Rlin rlin = (Rlin) rhs;
      // the right-hand side is a linear expression, perform this operation on the affine domain
      Linear le = rlin.getLinearTerm();
      Linear.Divisor d = new Linear.Divisor(rlin.getDivisor());
      sys.affineTrans(d, stmt.getLhs().getId(), le);
      if (DEBUGASSIGN) {
        System.out.println(name + ":");
        System.out.println("  after " + assign + " that is " + stmt);
        System.out.println("  on child: " + sys.getChildOps());
        System.out.println("  equations: " + sys.build().toString());
      }
      newChildState = sys.applyChildOps(newChildState);
    } else {
      sys.promoteVariable(stmt.getLhs().getId());
      try { // FIXME: see if it is possible to execute and simplify the binary operation in the affine domain
        newChildState = sys.applyChildOps(newChildState);
      } catch (Unreachable e) {
        stmt = zeno.assign(stmt.getLhs(), zeno.range(Interval.TOP));
        throw e;
      }
      newChildState = newChildState.eval(stmt);
    }
    return build(sys.build(), newChildState);
  }

  @Override public Affine<D> eval (Test test) {
    Test stmt = InliningVisitor.run(test, state);
    // The following visitor will throw an Unreachable exception if the affine domain itself is sufficient to tell
    // that the test renders the domain empty.
    if (ZenoTestHelper.isTautologyReportUnreachable(stmt)) { // the test was redundant
//    return stripSynthChannel();
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
    D newChildState = childState.eval(stmt);
    if (DEBUGTESTS) {
      System.out.println(name + ": ");
      System.out.println("  evaluating test: " + stmt);
      System.out.println("  this: " + state);
      System.out.println("  before: " + childState);
      System.out.println("  after: " + newChildState);
    }
    // extract the equalities that are propagated up in the broadcast channel
    SetOfEquations equalities = newChildState.getSynthChannel().getEquations();
    if (stmt.getOperator() == ZenoTestOp.EqualToZero)
      equalities = equalities.add(stmt.getExpr().toEquality());
    if (equalities.isEmpty())
      return build(state, newChildState);
    AffineStateBuilder sys = new AffineStateBuilder(state);
    for (Linear eq : equalities) {
      eq = sys.inlineIntoLinear(eq, Linear.Divisor.one());
      sys.intersectWithLinear(eq);
    }
    newChildState = sys.applyChildOps(newChildState);
    return build(sys.build(), newChildState);
  }

  private Affine<D> stripSynthChannel () {
    AffineState newState = state.withoutEqualities();
    // HACK: to rebuild the child domain without any old synthchannel info (not reliable as it is not official semantic)
    D newChildState = childState.setContext(getContext());
    return build(newState, newChildState);
  }

  @Override public Affine<D> introduce (NumVar variable, Type type, Option<BigInt> value) {
    if (state.inSupport(variable))
      throw new VariableSupportSetException();
    if (value.isSome()) {
      AffineStateBuilder sys = new AffineStateBuilder(state);
      sys.getChildOps().addIntro(variable);
      sys.affineTrans(Linear.Divisor.one(), variable, Linear.linear(value.get()));
      D newChildState = sys.applyChildOps(childState);
      return build(sys.build(), newChildState);
    } else {
      D newChildState = childState.introduce(variable, type, value);
      return build(state, newChildState);
    }
  }

  @Override public Affine<D> project (VarSet vars) {
    AffineStateBuilder sys = new AffineStateBuilder(state);
    for (NumVar variable : vars)
    {
      sys.removeVariable(variable);
      if (DEBUGOTHER) {
        System.out.println(name + ":");
        System.out.println("  project: " + variable);
        System.out.println("  on child: " + sys.getChildOps());
        System.out.println("  equations: " + sys.build().toString());
      }
      AffineState newCtx = sys.build();
      assert newCtx.notInSupport(variable);
    }
    D newChildState = sys.applyChildOps(childState);
    AffineState newCtx = sys.build();
    return build(newCtx, newChildState);
  }

  @Override public Affine<D> substitute (NumVar x, NumVar y) {
    if (state.inSupport(y))
      throw new VariableSupportSetException();
    Linear newEq = state.inlineIntoLinear(Linear.linear(x), Divisor.one());
    AffineStateBuilder sys = new AffineStateBuilder(state);
    sys.getChildOps().addIntro(y);
    sys.affineTrans(Divisor.one(), y, newEq);
    sys.removeVariable(x);
    D newChildState = sys.applyChildOps(childState);
    return build(sys.build(), newChildState);
  }

  @Override public Affine<D> expand (FoldMap pairs) {
    FoldMap leadingPairs = new FoldMap();
    FoldMap parameterPairs = new FoldMap();
    for (VarPair pair : pairs) {
      if (state.affine.get(pair.getPermanent()).isSome()) {
        leadingPairs.add(pair);
      } else {
        parameterPairs.add(pair);
      }
    }
    // perform expansion on child
    D newChildState = childState.expand(parameterPairs);

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
    for (VarPair pair : leadingPairs)
      builder.getChildOps().addIntro(pair.getEphemeral());

    for (NumVar var : lhsVars) {
      Linear eq = state.affine.get(var).get();
      eq = eq.renameVar(pairs);
      eq = builder.inlineIntoLinear(eq, Linear.Divisor.one());
      builder.getChildOps().addTest(zeno.comparison(eq, ZenoTestOp.EqualToZero));
      builder.intersectWithLinear(eq);
    }
    newChildState = builder.applyChildOps(newChildState);
    return build(builder.build(), newChildState);
  }

  @Override public Affine<D> fold (FoldMap pairs) {
    AffineStateBuilder builder = new AffineStateBuilder(state);
    pairs = builder.fold(pairs);
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.fold(pairs);
    return build(builder.build(), newChildState);
  }

  @Override public Affine<D> copyAndPaste (VarSet vars, Affine<D> from) {
    VarSet paramVars = vars;
    List<Linear> eqsToCopy = new LinkedList<Linear>();
    for (NumVar var : paramVars) {
      Option<Linear> oEq = from.state.affine.get(var);
      if (oEq.isSome()) {
        paramVars = paramVars.remove(var);
        eqsToCopy.add(oEq.get());
      }
    }
    D newChildState = childState.copyAndPaste(paramVars, from.childState);
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
        builder.getChildOps().addIntro(key);
        builder.getChildOps().addAssignment(zeno.assign(zeno.variable(key), zeno.range(range)));
        builder.getChildOps().addAssignment(zeno.assign(zeno.variable(key), zeno.linear(eq, d)));
      }
    }
    newChildState = builder.applyChildOps(newChildState);
    return build(builder.build(), newChildState);
  }

  @Override public Affine<D> join (Affine<D> other) {
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
    D newChildStateOfFst = fst.applyChildOps(childState);
    D newChildStateOfSnd = snd.applyChildOps(other.childState);
    D newChildState = newChildStateOfFst.join(newChildStateOfSnd);
    return build(fst.build(), newChildState);
  }

  @Override public boolean subsetOrEqual (Affine<D> other) {
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
    // there is no need to make the child domain compatible if the affine
    // domain is not even a subset since in this case the child won't even be
    // asked to check for subset
    if (!isSubset)
      return false;
    D newChildStateOfFst = fst.applyChildOps(childState);
    D newChildStateOfSnd = snd.applyChildOps(other.childState);
    return newChildStateOfFst.subsetOrEqual(newChildStateOfSnd);
  }

  @Override public Affine<D> widen (Affine<D> other) {
    AffineStateBuilder fst = new AffineStateBuilder(state);
    AffineStateBuilder snd = new AffineStateBuilder(other.state);
    fst.makeCompatible(snd, false);
    D newChildStateOfFst = fst.applyChildOps(childState);
    D newChildStateOfSnd = snd.applyChildOps(other.childState);
    D newChildState;
    newChildState = newChildStateOfFst.widen(newChildStateOfSnd);
    return build(fst.build(), newChildState);
  }

  @Override public Range queryRange (Linear linear) {
    Linear.Divisor d = Linear.Divisor.one();
    Linear inlined = AffineState.inlineIntoLinear(linear, d, state.affine);
    Range result = childState.queryRange(inlined).divRoundInvards(d.get());
    if (DEBUGOTHER) {
      System.out.println(name + ":");
      System.out.println("  query: " + linear + " that is: " + inlined);
      System.out.println("  equations: " + state);
      System.out.println("  divisor: " + d.get());
      System.out.println("  result: " + result);
    }
    if (result == null) // there was no integral solution to the rounding
      throw new Unreachable();
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
    SynthChannel syn = childState.getSynthChannel().clone();
    for (NumVar var : state.newEqualities) {
      // this lookup should not fail since only leading variables are inserted
      // into newEqualities and because a leading variable cannot be removed by
      // intersecting with other equalities over parameter variables
      syn.addEquation(state.affine.get(var).get());
    }
    return syn;
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    state.appendVar(builder, var, childState);
  }
}
