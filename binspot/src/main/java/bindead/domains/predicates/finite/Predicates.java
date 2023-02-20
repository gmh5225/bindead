package bindead.domains.predicates.finite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.FoldMap;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.combinators.FiniteFunctor;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.predicates.PredicatesProperties;

/**
 * The predicates tracking domain.
 *
 * @param <D> The type of the child domain.
 *
 * @author Bogdan Mihaila
 */
public class Predicates<D extends FiniteDomain<D>> extends FiniteFunctor<PredicatesState, D, Predicates<D>> {
  public static final String NAME = "PREDICATES(F)";
  private final boolean DEBUGASSIGN = PredicatesProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean DEBUGBINARIES = PredicatesProperties.INSTANCE.debugBinaryOperations.isTrue();
  private final boolean DEBUGSUBSET = PredicatesProperties.INSTANCE.debugSubsetOrEqual.isTrue();
  private final boolean DEBUGOTHER = PredicatesProperties.INSTANCE.debugOther.isTrue();
  private final SetOfEquations newEqualities;

  public Predicates (D child) {
    this(PredicatesState.EMPTY, child, SetOfEquations.empty());
  }

  private Predicates (PredicatesState state, D childState, SetOfEquations newEqualities) {
    super(NAME, state, childState);
    this.newEqualities = newEqualities;
  }

  private Predicates<D> build (PredicatesState state, D childState, SetOfEquations newEqualities) {
    return new Predicates<D>(state, childState, newEqualities);
  }

  @Override public Predicates<D> build (PredicatesState state, D childState) {
    return build(state, childState, SetOfEquations.empty());
  }

  @Override public P3<PredicatesState, D, D> makeCompatible (Predicates<D> other, boolean isWideningPoint) {
    // intersect the flags and their associated predicates set
    PredicatesStateBuilder thisBuilder = new PredicatesStateBuilder(state, childState);
    PredicatesStateBuilder otherBuilder = new PredicatesStateBuilder(other.state, other.childState);
    PredicatesStateBuilder joinedBuilder = otherBuilder.join(thisBuilder, other.childState, childState);
    return P3.tuple3(joinedBuilder.build(), this.childState, other.childState);
  }

  @Override public Predicates<D> addToState (Predicates<D> newState, boolean isWideningPoint) {
//    Predicates<D> result = super.addToState(newState, isWideningPoint);
    // XXX: copied the code from super and modified it to be able to implement the isSubset test for this domain
    if (newState == this) {
      debugSubsetOrEqual(isWideningPoint, true);
      return null;
    }
    PredicatesStateBuilder thisBuilder = new PredicatesStateBuilder(state, childState);
    PredicatesStateBuilder otherBuilder = new PredicatesStateBuilder(newState.state, newState.childState);
    // NOTE: as the direction is reversed as compared to the isSubset() method in the Zeno domains
    // this is the old state and we should test if new state is subset of this state
    // i.e. newState.isSubset(thisState)
    Predicates<D> result;
    boolean isSubset = otherBuilder.isSubset(newState.childState, thisBuilder);
    if (DEBUGSUBSET) {
      System.out.println(NAME + " (subset): " + isSubset);
      System.out.println("    fst: " + state);
      System.out.println("    other: " + newState.state);
    }

    D collectedChild = childState.addToState(newState.childState, isWideningPoint);
    if (collectedChild == null) { // new child state is a subset of this child
      if (isSubset)
        return null;
      else
        collectedChild = this.childState;
    }
    PredicatesStateBuilder joinedBuilder = otherBuilder.join(thisBuilder, newState.childState, childState);
    result = build(joinedBuilder.build(), collectedChild);
    if (DEBUGBINARIES) {
      System.out.println(NAME + " (join):");
      System.out.println("    fst: " + state);
      System.out.println("    other: " + newState.state);
      System.out.println("  join: " + result);
    }
    return result;
  }

  @Override public Predicates<D> eval (Assign assign) {
    // * collect tests from comparisons as predicates associated with flags (the lhs of a comparison)
    // * split a test into t and ¬t and assign them to f and ¬f accordingly: f → t; ¬f → ¬t
    // * remove predicates when overwriting a flag
    // * transform predicates on linear transformations of a variable
    // * substitute flags and predicates when they are overwritten
    // * transform the rhs of the assignment if it is a comparison as we do not evaluate the test yet
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, childState);
    Assign stmt = PredicatesAssignmentVisitor.run(assign, builder);
    if (DEBUGASSIGN) {
      System.out.println(name + ":");
      System.out.println("  evaluating: " + assign + " that is: " + stmt);
      System.out.println("  before: " + state);
      System.out.println("  after: " + builder.build());
    }
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.eval(stmt);
    return build(builder.build(), newChildState);
  }

  @Override public Predicates<D> eval (Test stmt) {
    // * lookup predicates for a flag, if a flag is tested then also apply the associated predicates
    // * see if new equalities after transfer function could have set a flag to 0 or 1 and apply its predicates
    // * more fancy additional reduction: if the variable in the test occurs in a predicate
    // we can infer here the value for the flag, e.g. ¬f → {x <= 5} and f → {x >= 10} and test is x >= 6
    // then f == 0 follows through the equality of p→q = ¬q→¬p. We execute all the predicates where the test
    // variable is part of the predicate and if any of them is unreachable we know that the tests imply that the flag
    // is not feasible (from second implication above).
    // In the above example {x <= 5} will be unreachable and thus we can infer ¬¬f which means f = 1 holds and all
    // the predicates for f are true. Then we apply all of the predicates for f.
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, childState);
    P2<D, SetOfEquations> result = fixApply(stmt, childState, builder, new HashSet<Test>());
    D newChildState = result._1();
    SetOfEquations newEqualities = result._2();
    return build(builder.build(), newChildState, newEqualities);
  }

  private P2<D, SetOfEquations> fixApply (Test test, D state, PredicatesStateBuilder builder,
      Set<Test> alreadyAppliedTests) {
    if (AnalysisProperties.INSTANCE.debugTests.isTrue())
      System.out.println("Predicates test: " + test);
    D newChildState = state.eval(test);
    alreadyAppliedTests.add(test);
    AVLSet<Test> newImpliedTests = AVLSet.empty();
    newImpliedTests = newImpliedTests.union(Entailment.getSyntacticallyEntailed(test, builder));
    SetOfEquations newEqualities = newChildState.getSynthChannel().getEquations();
    for (Test equalityTest : Entailment.equalitiesToTests(newEqualities)) {
      // NOTE: the test below does not always work as equalitiesToTests() produces Tests with size 0 (from Zeno tests)
      // and they are not syntactically equal to a test coming from finite domains above
      if (alreadyAppliedTests.contains(equalityTest))
        continue;
      newImpliedTests = newImpliedTests.union(Entailment.getSyntacticallyEntailed(equalityTest, builder));
    }
    for (Test impliedTest : newImpliedTests) {
      if (alreadyAppliedTests.contains(impliedTest))
        continue;
      P2<D, SetOfEquations> result = fixApply(impliedTest, newChildState, builder, alreadyAppliedTests);
      newChildState = result._1();
      // accumulate the new equalities from all the test applications to pass on all of them to parent domains.
      // For example the UNDEF domain needs the equalities to reduce flag values to true, false.
      newEqualities = newEqualities.union(result._2());
    }
    return P2.tuple2(newChildState, newEqualities);
  }

  @Override public Predicates<D> project (NumVar variable) {
    // * remove all the predicates containing var or try to substitute the variable to keep the tests
    // * if var is a flag then remove it from state
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, childState);
    builder.project(variable);
    if (DEBUGOTHER) {
      System.out.println(name + ":");
      System.out.println("  projecting: " + variable);
      System.out.println("  before: " + state);
      System.out.println("  after: " + builder.build());
    }
    D newChildState = childState.project(variable);
    return build(builder.build(), newChildState);
  }

  @Override public Predicates<D> substitute (NumVar x, NumVar y) {
    // * perform substitution either on flags if both are flags or update all the preds for the substituted vars
    // what happens if we substitute f/var or the other way round?
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, childState);
    builder.substitute(x, y);
    if (DEBUGOTHER) {
      System.out.println(name + ":");
      System.out.println("  substitute: [" + x + "\\" + y + "]");
      System.out.println("  before: " + state);
      System.out.println("  after: " + builder.build());
    }
    D newChildState = childState.substitute(x, y);
    return build(builder.build(), newChildState);
  }

  @Override public Predicates<D> copyAndPaste (VarSet vars, Predicates<D> from) {
    // * c&p in child
    // * intersect vars with predsOfVarsToCopy and c&p the result over to here;
    // or project in "from" everything out that is not in vars and then do a join with this.
    //   basically it means only copy a predicate if all its variables are also in the c&p set
    D newChildState = childState.copyAndPaste(vars, from.childState);
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, newChildState);
    builder.copyAndPaste(vars, new PredicatesStateBuilder(from.state, from.childState));
    return build(builder.build(), newChildState);
  }

  @Override public SynthChannel getSynthChannel () {
    SynthChannel synth = super.getSynthChannel(); // get child channel with local vars removed in Functor superclass
    synth.addEquations(newEqualities);
    return synth;
  }

  /*@Override public Predicates<D> copyWholeVariable (NumVar to, NumVar from) {
    Predicates<D> d = introduce(to, Type.Zeno, Option.<BigInt>none());
    Assign stmt = finite.assign(finite.variable(0, to), finite.linear(0, from));
    d = d.eval(stmt);
    return d;
  }*/

  @Override public Predicates<D> copyVariable (NumVar to, NumVar from) {
    PredicatesState built = copyPredicatesOfVariable(to, from);
    D newChildState = childState.copyVariable(to, from);
    return build(built, newChildState);
  }

  private PredicatesState copyPredicatesOfVariable (NumVar to, NumVar from) {
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, childState);
    if (builder.isFlag(from))
      builder.assignCopy(to, from);
    // by returning expr in this case instead of TOP we maintain the affine relation between f0 and f1
    else
      builder.project(to);
    PredicatesState built = builder.build();
    return built;
  }

  @Override public Predicates<D> assumeEdgeNG (Rlin pointerVar, AddrVar targetAddr) {
    return build(state, childState.assumeEdgeNG(pointerVar, targetAddr));
  }

  @Override public Predicates<D> expandNG (ListVarPair nvps) {
    // expand on child
    D newChildState = childState.expandNG(nvps);
    PredicatesState built = expandOnState(nvps, newChildState);
    return build(built, newChildState);
  }

  @Override public Predicates<D> expandNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    // expand on child
    D newChildState = childState.expandNG(p, e, nvps);
    PredicatesState built = expandOnState(nvps, newChildState);
    return build(built, newChildState);
  }

  @Override public Predicates<D> foldNG (ListVarPair nvps) {
    // expand on child
    D newChildState = childState.foldNG(nvps);
    PredicatesState built = foldOnState(nvps, newChildState);
    return build(built, newChildState);
  }

  @Override public Predicates<D> foldNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    D newChildState = childState.foldNG(p, e, nvps);
    PredicatesState built = foldOnState(nvps, newChildState);
    return build(built, newChildState);
  }

  private PredicatesState foldOnState (List<VarPair> nvps, D newChildState) {
    // * fold on child
    // * rename vars according to foldmap and intersect resulting preds with current state
    //   improve by looking which flags are mentioned etc.
    FoldMap pairs = FoldMap.fromList(nvps);
    PredicatesStateBuilder oldStateBuilder = new PredicatesStateBuilder(state, newChildState);
    PredicatesStateBuilder newStateBuilder = new PredicatesStateBuilder(state, newChildState);
    newStateBuilder.renameFold(pairs);
    newStateBuilder.intersect(oldStateBuilder);
    return newStateBuilder.build();
  }

  private PredicatesState expandOnState (List<VarPair> nvps, D newChildState) {
    // * expand flags ∈ pairs and duplicate predicates
    // * in expanded flags use the expanded variables instead of the normal ones
    // or simpler just rename all the variables everywhere and then re-add (join) this to the state.
    // By this if no flags get expanded then the preds set for a flag will grow.
    // e.g. expand ((f,f'), (u,u')) in state: f -> {u < 1, v > 2} leads to the
    // state: f -> {u < 1, v > 2}; f' -> {u' < 1, v > 2}
    PredicatesStateBuilder oldStateBuilder = new PredicatesStateBuilder(state, newChildState);
    PredicatesStateBuilder newStateBuilder = new PredicatesStateBuilder(state, newChildState);
    newStateBuilder.renameExpand(nvps);
    newStateBuilder.union(oldStateBuilder);
    PredicatesState built = newStateBuilder.build();
    return built;
  }

  @Override public Predicates<D> concretizeAndDisconnectNG (AddrVar s, VarSet cs) {
    return build(state, childState.concretizeAndDisconnectNG(s, cs));
  }

  @Override public Predicates<D> bendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts,
      VarSet ptc) {
    return build(state, childState.bendBackGhostEdgesNG(s, c, svs, cvs, pts, ptc));
  }

  @Override public Predicates<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs,
      VarSet pts, VarSet ptc) {
    return build(state, childState.bendGhostEdgesNG(summary, concrete, svs, cvs, pts, ptc));
  }

  @Override public Predicates<D> assumeVarsAreEqual (int size, NumVar fst, NumVar snd) {
    D newChildState = childState.assumeVarsAreEqual(size, fst, snd);
    return build(state, newChildState);
  }

}
