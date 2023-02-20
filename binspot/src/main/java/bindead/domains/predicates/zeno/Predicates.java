package bindead.domains.predicates.zeno;

import java.util.HashSet;
import java.util.Set;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.MultiMap;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.FoldMap;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.predicates.PredicatesProperties;
import bindead.exceptions.Unreachable;

/**
 * The predicates tracking domain.
 *
 * @param <D> The type of the child domain.
 *
 * @author Bogdan Mihaila
 */
public class Predicates<D extends ZenoDomain<D>> extends ZenoFunctor<PredicatesState, D, Predicates<D>> {
  public static final String NAME = "PREDICATES(Z)";
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

  @Override public Predicates<D> eval (Assign assign) {
    // * remove predicates when overwriting a variable with a binop or a range value
    // * transform predicates on linear transformations of a variable
    // * substitute variables in predicates when they are overwritten
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, childState);
    PredicatesAssignmentVisitor.run(assign, builder);
    if (DEBUGASSIGN) {
      System.out.println(name + ":");
      System.out.println("  evaluating: " + assign);
      System.out.println("  before: " + state);
      System.out.println("  after: " + builder.build());
    }
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.eval(assign);
    return build(builder.build(), newChildState);
  }

  @Override public Predicates<D> eval (Test test) throws Unreachable {
    // * lookup the predicates that the test entails, then also apply that predicates
    // * see if new equalities after transfer function could have caused new predicates to be entailed and apply those
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, childState);
    P3<D, SetOfEquations, MultiMap<Test, Test>> result = fixApply(test, childState, builder, new HashSet<Test>());
    D newChildState = result._1();
    SetOfEquations newEqualities = result._2();
    // TODO: make sure that the synth channel implications are not lost during the fixApply
    addSynthesizedPredicates(result._3(), builder);
    return build(builder.build(), newChildState, newEqualities);
  }

  private P3<D, SetOfEquations, MultiMap<Test, Test>> fixApply (Test test, D state, PredicatesStateBuilder builder,
      Set<Test> alreadyAppliedTests) {
    if (AnalysisProperties.INSTANCE.debugTests.isTrue())
      System.out.println("Predicates test: " + test);
    D newChildState = state.eval(test);
    alreadyAppliedTests.add(test);
    SynthChannel synthChannel = newChildState.getSynthChannel();
    SetOfEquations newEqualities = synthChannel.getEquations();
    MultiMap<Test, Test> newImplications = synthChannel.getImplications();
    AVLSet<Test> newImpliedTests = AVLSet.empty();
    newImpliedTests = newImpliedTests.union(Entailment.getSyntacticallyEntailed(test, builder));
    // XXX bm: inferring lots more tests here. Might be a performance problem, so uncomment if precision not necessary.
    VarSet modifiedVars = test.getVars().union(newEqualities.getVars());
    newImpliedTests = newImpliedTests.union(Entailment.getSemanticallyEntailed(newChildState, modifiedVars, builder));
    for (Test equalityTest : Entailment.equalitiesToTests(newEqualities)) {
      if (alreadyAppliedTests.contains(equalityTest))
        continue;
      newImpliedTests = newImpliedTests.union(Entailment.getSyntacticallyEntailed(equalityTest, builder));
      // XXX bm: inferring lots more tests here. Might be a performance problem, so uncomment if precision not necessary.
      newImpliedTests =
        newImpliedTests.union(Entailment.getSemanticallyEntailed(newChildState, equalityTest.getVars(), builder));
    }
    for (Test impliedTest : newImpliedTests) {
      if (alreadyAppliedTests.contains(impliedTest))
        continue;
      P3<D, SetOfEquations, MultiMap<Test, Test>> result =
        fixApply(impliedTest, newChildState, builder, alreadyAppliedTests);
      newChildState = result._1();
      // accumulate the new equalities from all the test applications to pass on all of them to parent domains.
      // For example the UNDEF domain needs the equalities to reduce flag values to true, false.
      newEqualities = newEqualities.union(result._2());
      // also accumulate any synthesized implications
      newImplications = newImplications.union(result._3());
    }
    return P3.tuple3(newChildState, newEqualities, newImplications);
  }

  @Override public Predicates<D> project (VarSet vars) {
    // * remove all the predicates containing var or try to substitute the variable to keep the tests
    PredicatesStateBuilder builder = new PredicatesStateBuilder(state, childState);
    for (NumVar variable : vars) {
      builder.project(variable);
      if (DEBUGOTHER) {
        System.out.println(name + ":");
        System.out.println("  projecting: " + variable);
        System.out.println("  before: " + state);
        System.out.println("  after: " + builder.build());
      }
    }
    D newChildState = childState.project(vars);
    return build(builder.build(), newChildState);
  }

  @Override public Predicates<D> substitute (NumVar x, NumVar y) {
    // * perform substitution by updating all the predicates with the substituted vars
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

  @Override public boolean subsetOrEqual (Predicates<D> other) {
    PredicatesStateBuilder thisBuilder = new PredicatesStateBuilder(state, childState);
    PredicatesStateBuilder otherBuilder = new PredicatesStateBuilder(other.state, other.childState);
    boolean isSubset = thisBuilder.isSubset(otherBuilder);
    if (DEBUGSUBSET) {
      System.out.println(NAME + ":");
      System.out.println(" subset-or-equal: " + isSubset);
      System.out.println("    fst: " + state);
      System.out.println("    snd: " + other.state);
    }
    // there is no need to test the child domain for subset if this
    // domain is not even a subset as they both need to be a subset
    if (!isSubset) {
      return false;
    }
    return childState.subsetOrEqual(other.childState);
  }

  @Override public Predicates<D> join (Predicates<D> other) {
    PredicatesStateBuilder thisBuilder = new PredicatesStateBuilder(state, childState);
    PredicatesStateBuilder otherBuilder = new PredicatesStateBuilder(other.state, other.childState);
    PredicatesStateBuilder joinedBuilder = thisBuilder.join(otherBuilder);
    D joinedChildState = childState.join(other.childState);
    addSynthesizedPredicates(joinedChildState.getSynthChannel().getImplications(), joinedBuilder);
    return build(joinedBuilder.build(), joinedChildState);
  }

  @Override public Predicates<D> widen (Predicates<D> other) {
    PredicatesStateBuilder thisBuilder = new PredicatesStateBuilder(state, childState);
    PredicatesStateBuilder otherBuilder = new PredicatesStateBuilder(other.state, other.childState);
    PredicatesStateBuilder joinedBuilder = thisBuilder.join(otherBuilder);
    D widenedChildState = childState.widen(other.childState);
    addSynthesizedPredicates(widenedChildState.getSynthChannel().getImplications(), joinedBuilder);
    return build(joinedBuilder.build(), widenedChildState);
  }

  /**
   * New predicates are generated to mitigate precision loss on join (convexity) in child domains;
   * e.g. in intervals when joining d1 : x=[0, 5] ⊔ d2 : x=[10, 15] we can infer the following
   * predicates during the join: 5 < x → 10 <= x and similar between different variables if there
   * are more than one, e.g. implications between x and y to maintain relational information.
   */
  private static void addSynthesizedPredicates (MultiMap<Test, Test> implications, PredicatesStateBuilder builder) {
    for (P2<Test, Test> implication : implications) {
      builder.addImplication(implication._1(), implication._2());
    }
  }

  @Override public Predicates<D> expand (FoldMap pairs) {
    // * expand on child
    // * expand flags ∈ pairs and duplicate predicates
    // * in expanded flags use the expanded variables instead of the normal ones
    // or simpler just rename all the variables everywhere and then re-add (join) this to the state.
    // By this if no flags get expanded then the preds set for a flag will grow.
    // e.g. expand ((f,f'), (u,u')) in state: f -> {u < 1, v > 2} leads to the
    // state: f -> {u < 1, v > 2}; f' -> {u' < 1, v > 2}
    D newChildState = childState.expand(pairs);
    PredicatesStateBuilder oldStateBuilder = new PredicatesStateBuilder(state, newChildState);
    PredicatesStateBuilder newStateBuilder = new PredicatesStateBuilder(state, newChildState);
    newStateBuilder.renameExpand(pairs);
    newStateBuilder.union(oldStateBuilder);
    return build(newStateBuilder.build(), newChildState);
  }

  @Override public Predicates<D> fold (FoldMap pairs) {
    // * fold on child
    // * rename vars according to foldmap and intersect resulting preds with current state
    //   improve by looking which flags are mentioned etc.
    D newChildState = childState.fold(pairs);
    PredicatesStateBuilder oldStateBuilder = new PredicatesStateBuilder(state, newChildState);
    PredicatesStateBuilder newStateBuilder = new PredicatesStateBuilder(state, newChildState);
    newStateBuilder.renameFold(pairs);
    newStateBuilder.intersect(oldStateBuilder);
    return build(newStateBuilder.build(), newChildState);
  }

  @Override public Predicates<D> copyAndPaste (VarSet vars, Predicates<D> from) {
    // * c&p in child
    // * intersect vars with predsOfVarsToCopy and c&p the result over to here;
    // or project in "from" everything out that is not in vars and then do a union with this.
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

}
