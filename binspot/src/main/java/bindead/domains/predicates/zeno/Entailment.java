package bindead.domains.predicates.zeno;

import javalx.data.products.P2;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.MultiMap;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.data.Linear;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.exceptions.Unreachable;

/**
 * See if a predicate entails other predicates or if a predicate is entailed in a state.
 *
 * @author Bogdan Mihaila
 */
class Entailment {
  private static final ZenoFactory zeno = ZenoFactory.getInstance();

  /**
   * From a set of equalities build a set of tests for that equalities.
   */
  public static AVLSet<Test> equalitiesToTests (SetOfEquations equalities) {
    AVLSet<Test> result = AVLSet.empty();
    for (Linear equality : equalities) {
      Test test = zeno.comparison(equality, ZenoTestOp.EqualToZero);
      result = result.add(test);
    }
    return result;
  }

  /**
   * For all implications p → q that we track, if the test t ⊦ p we infer q and if t ⊦ ¬q
   * we infer ¬p (from the equivalence: p→q ≡ ¬p∨q ≡ ¬q→¬p).
   *
   * @param test A new fact/predicate that holds.
   * @param builder The state (mapping of predicates to the implied predicates).
   * @return A set of implied predicates that are entailed by the test.
   */
  public static AVLSet<Test> getSyntacticallyEntailed (Test test, PredicatesStateBuilder builder) {
    AVLSet<Test> result = AVLSet.empty();
    AVLSet<Test> entailed1 = getSyntacticallyForwardEntailed(test, builder);
    result = result.union(entailed1);
    AVLSet<Test> entailed2 = getSyntacticallyBackwardEntailed(test, builder);
    result = result.union(entailed2);
    return result;
  }

  /**
   * For each p → q where t tests variables from p it tests for t ⊦ p and if it holds returns p.
   */
  private static AVLSet<Test> getSyntacticallyForwardEntailed (Test test, PredicatesStateBuilder builder) {
    AVLSet<Test> result = AVLSet.empty();
    VarSet testVars = test.getVars();
    Iterable<Test> predicates = builder.getAllLhsContaining(testVars);
    for (Test predicate : predicates) {
      if (ZenoTestHelper.isSyntacticallyEntailedBy(predicate, test))
        result = result.union(builder.getConsequences(predicate));
    }
    return result;
  }

  /**
   * For each p → q where t tests variables from q it tests t ⊦ ¬q and if it holds returns ¬p.
   */
  private static AVLSet<Test> getSyntacticallyBackwardEntailed (Test test, PredicatesStateBuilder builder) {
    AVLSet<Test> result = AVLSet.empty();
    VarSet testVars = test.getVars();
    Iterable<Test> predicates = builder.getAllRhsContaining(testVars);
    for (Test predicate : predicates) {
      if (ZenoTestHelper.isSyntacticallyEntailedBy(predicate.not(), test))
        result = result.union(ZenoTestHelper.negateTests(builder.getPremises(predicate)));
    }
    return result;
  }


  /**
   * For all implications p → q that we track, if the child state s ⊧ p we infer q and if s ⊧ ¬q
   * we infer ¬p (from the equivalence: p→q ≡ ¬p∨q ≡ ¬q→¬p).
   *
   * @param childState A state where the test t has already been applied on.
   * @param modifiedVars A set of variables that changed their values while obtaining the child state.
   *          This is used as an optimization hint to test only predicates for entailment in the state
   *          if they speak about any of the given variables. Can thus be empty.
   * @param builder The state (mapping of flags to the implied predicates).
   * @return A set of implied predicates that are entailed by the effect of the test in the child state.
   */
  public static <D extends ZenoDomain<D>> AVLSet<Test> getSemanticallyEntailed (D childState, VarSet modifiedVars,
      PredicatesStateBuilder builder) {
    AVLSet<Test> result = AVLSet.empty();
    AVLSet<Test> entailed1 = getSemanticallyForwardEntailed(childState, modifiedVars, builder);
    result = result.union(entailed1);
    AVLSet<Test> entailed2 = getSemanticallyBackwardEntailed(childState, modifiedVars, builder);
    result = result.union(entailed2);
    return result;
  }

  /**
   * Returns {@code true} if all the implications are semantically entailed in the given state.
   * For an implication p → q to be entailed in a state s, the state must semantically entail q after p has been
   * applied to it, i.e. [[p]]s ⊧ q.
   */
  public static <D extends ZenoDomain<D>> boolean areAllImplicationsSemanticallyEntailed (
      Iterable<P2<Test, Test>> implications, D childState) {
    D newChildState = childState;
    for (P2<Test, Test> implication : implications) {
      Test premise = implication._1();
      Test consequence = implication._2();
      try {
        newChildState = childState.eval(premise);
        if (!isSemanticallyEntailedIn(consequence, newChildState))
          return false;
      } catch (Unreachable _) {
      }
    }
    return true;
  }

  /**
   * Returns all the implications that are semantically entailed in the given state.
   * For an implication p → q to be entailed in a state s, the state must semantically entail q after p has been
   * applied to it, i.e. [[p]]s ⊧ q.
   */
  public static <D extends ZenoDomain<D>> MultiMap<Test, Test> getAllSemanticallyEntailedImplications (
      Iterable<P2<Test, Test>> implications, D childState) {
    MultiMap<Test, Test> result = MultiMap.empty();
    D newChildState = childState;
    for (P2<Test, Test> implication : implications) {
      Test premise = implication._1();
      Test consequence = implication._2();
      boolean entailed = false;
      try {
        newChildState = childState.eval(premise);
        if (isSemanticallyEntailedIn(consequence, newChildState))
          entailed = true;
      } catch (Unreachable _) {
        entailed = true;
      }
      if (entailed)
        result = result.add(premise, consequence);
    }
    return result;
  }

  /**
   * For each p → q where t tests variables from p it tests for t ⊧ p and if it holds returns p.
   */
  private static <D extends ZenoDomain<D>> AVLSet<Test> getSemanticallyForwardEntailed (D childState,
      VarSet modifiedVars, PredicatesStateBuilder builder) {
    AVLSet<Test> result = AVLSet.empty();
    Iterable<Test> predicates;
    if (modifiedVars.isEmpty())
      predicates = builder.getAllLhs();
    else
      predicates = builder.getAllLhsContaining(modifiedVars);
    for (Test predicate : predicates) {
      if (isSemanticallyEntailedIn(predicate, childState))
        result = result.union(builder.getConsequences(predicate));
    }
    return result;
  }

  /**
   * For each p → q where t tests variables from q it tests t ⊧ ¬q and if it holds returns ¬p.
   */
  private static <D extends ZenoDomain<D>> AVLSet<Test> getSemanticallyBackwardEntailed (D childState,
      VarSet modifiedVars,
      PredicatesStateBuilder builder) {
    AVLSet<Test> result = AVLSet.empty();
    Iterable<Test> predicates;
    if (modifiedVars.isEmpty())
      predicates = builder.getAllRhs();
    else
      predicates = builder.getAllRhsContaining(modifiedVars);
    for (Test predicate : predicates) {
      if (isNegationSemanticallyEntailedIn(predicate, childState))
        result = result.union(ZenoTestHelper.negateTests(builder.getPremises(predicate)));
    }
    return result;
  }

  private static <D extends ZenoDomain<D>> boolean isSemanticallyEntailedIn (Test test, D childState) {
    return isNegationSemanticallyEntailedIn(test.not(), childState);
  }

  private static <D extends ZenoDomain<D>> boolean isNegationSemanticallyEntailedIn (Test test, D childState) {
    try {
      childState.eval(test);
    } catch (Unreachable _) {
      return true;
    }
    return false;
  }

}
