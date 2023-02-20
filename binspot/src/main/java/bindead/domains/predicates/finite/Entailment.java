package bindead.domains.predicates.finite;

import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.predicates.finite.PredicatesState.Flag;
import bindead.exceptions.Unreachable;

/**
 * See if a predicate entails other predicates or if a predicate is entailed in a state.
 *
 * @author Bogdan Mihaila
 */
class Entailment {
  private static final FiniteFactory finite = FiniteFactory.getInstance();

  /**
   * From a set of equalities build a set of tests for that equalities.
   */
  public static AVLSet<Test> equalitiesToTests (SetOfEquations equalities) {
    AVLSet<Test> result = AVLSet.empty();
    for (Linear equality : equalities) {
      Test test = finite.equalToZero(finite.linear(0, equality));
      result = result.add(test);
    }
    return result;
  }

  /**
   * For all implications p → q that we track, if the test t ⊦ p we infer q and if t ⊦ ¬q
   * we infer ¬p (from the equivalence: p→q ≡ ¬p∨q ≡ ¬q→¬p).
   *
   * @param test A new fact that holds.
   * @param builder The state (mapping of flags to the implied predicates).
   * @return A set of implied predicates that are entailed by the test.
   */
  public static AVLSet<Test> getSyntacticallyEntailed (Test test, PredicatesStateBuilder builder) {
    AVLSet<Test> result = AVLSet.empty();
    AVLSet<Flag> flags = getSyntacticallyForwardEntailed(test, builder);
    for (Flag flag : flags) {
      result = result.union(builder.getPredicates(flag));
    }
    // NOTE: syntactic backwards entailment is not used anymore as it does not bring much
    // to infer flags from tests and it is flawed because of wrapping to assume a syntactic entailment
    // between two tests
    return result;
  }

  /**
   * For each p → q where t tests variables from p it tests for t ⊦ p and if it holds returns p.
   */
  private static AVLSet<Flag> getSyntacticallyForwardEntailed (Test test, PredicatesStateBuilder builder) {
    AVLSet<Flag> empty = AVLSet.empty();
    Linear linear = test.toLinear();
    if (!linear.isSingleTerm()) // only tests about one single variable are of interest
      return empty;
    NumVar variable = linear.getKey();
    if (!builder.isFlag(variable))
      return empty;
    // now infer the flags and their value according to the implications, i.e. if the tested flag is true=1 or false=0
    boolean flagValuation = true;
    BigInt constant = linear.getConstant().negate(); // bring it to the other side of the linear equation
    switch (test.getOperator()) {
    case Equal: {
      if (constant.isZero())
        flagValuation = false;
      else if (constant.isOne())
        flagValuation = true;
      else
        return empty;
      break;
    }
    // for this case we assume that a flag is a variable with a range of [0, 1] by construction.
    case NotEqual: {
      if (constant.isZero())
        flagValuation = true;
      else if (constant.isOne())
        flagValuation = false;
      else
        return empty;
      break;
    }
    // NOTE: we could also convert <=, < to f==0 or f==1 wherever possible,
    // i.e. f < 1 -> f == 0 and f > 0 -> f == 1
    // but that won't occur here because flags should be only tested for = or != by our grammar at this level.
    default:
      return empty;
    }
    return AVLSet.singleton(Flag.toFlag(variable, flagValuation));
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
  public static <D extends FiniteDomain<D>> AVLSet<Test> getSemanticallyEntailed (D childState, VarSet modifiedVars,
      PredicatesStateBuilder builder) {
    AVLSet<Test> result = AVLSet.empty();
    AVLSet<Flag> flags = getSemanticallyForwardEntailed(childState, modifiedVars, builder);
    for (Flag flag : flags) {
      result = result.union(builder.getPredicates(flag));
    }
    flags = getSemanticallyBackwardEntailed(childState, modifiedVars, builder);
    for (Flag flag : flags) {
      result = result.add(flag.asTest());
    }
    return result;
  }

  /**
   * For each p → q where t tests variables from p it tests for t ⊧ p and if it holds returns p.
   */
  private static <D extends FiniteDomain<D>> AVLSet<Flag> getSemanticallyForwardEntailed (D childState,
      VarSet modifiedVars, PredicatesStateBuilder builder) {
    AVLSet<Flag> result = AVLSet.empty();
    AVLSet<Flag> occurrences = builder.getFlagsFromVars(modifiedVars);
    for (Flag flag : occurrences) {
      Test flagTest = flag.asTest();
      if (isSemanticallyEntailedIn(flagTest, childState))
        result = result.add(flag);
    }
    return result;
  }

  /**
   * For each p → q where t tests variables from q it tests t ⊧ ¬q and if it holds returns ¬p.
   */
  private static <D extends FiniteDomain<D>> AVLSet<Flag> getSemanticallyBackwardEntailed (D childState,
      VarSet modifiedVars, PredicatesStateBuilder builder) {
    AVLSet<Flag> result = AVLSet.empty();
    AVLSet<Flag> occurrences = builder.getOccurrencesOf(modifiedVars);
    for (Flag flag : occurrences) {
      boolean negatedFlagInferred = false;
      AVLSet<Test> predicates = builder.getPredicates(flag);
      for (Test predicate : predicates) {
        if (!predicate.getVars().containsAny(modifiedVars))
          continue;
        if (isNegationSemanticallyEntailedIn(predicate, childState)) {
          negatedFlagInferred = true;
          break;
        }
      }
      if (negatedFlagInferred)
        result = result.add(flag.negate());
    }
    return result;
  }

  private static <D extends FiniteDomain<D>> boolean isSemanticallyEntailedIn (Test test, D childState) {
    return isNegationSemanticallyEntailedIn(test.not(), childState);
  }

  private static <D extends FiniteDomain<D>> boolean isNegationSemanticallyEntailedIn (Test test, D childState) {
    try {
      childState.eval(test);
    } catch (Unreachable _) {
      return true;
    }
    return false;
  }

  /**
   * Returns {@code true} if all the implications are semantically entailed in the given state.
   * For an implication p → q to be entailed in a state s, the state must semantically entail q after p has been
   * applied to it, i.e. [[p]]s ⊧ q.
   */
  public static <D extends FiniteDomain<D>> boolean areAllImplicationsSemanticallyEntailed (
      AVLMap<Flag, AVLSet<Test>> implications, D childState) {
    D newChildState = childState;
    for (P2<Flag, AVLSet<Test>> implication : implications) {
      Flag premise = implication._1();
      try {
        newChildState = childState.eval(premise.asTest());
      } catch (Unreachable _) {
        continue; // if the premise is unreachable then all the consequences are entailed
      }
      AVLSet<Test> consequences = implication._2();
      for (Test consequence : consequences) {
        if (!isSemanticallyEntailedIn(consequence, newChildState))
          return false;
      }
    }
    return true;
  }

  /**
   * Returns all the implications that are semantically entailed in the given state.
   * For an implication p → q to be entailed in a state s, the state must semantically entail q after p has been
   * applied to it, i.e. [[p]]s ⊧ q.
   */
  public static <D extends FiniteDomain<D>> AVLMap<Flag, AVLSet<Test>> getAllSemanticallyEntailedImplications (
      AVLMap<Flag, AVLSet<Test>> implications, D childState) {
    AVLMap<Flag, AVLSet<Test>> result = AVLMap.empty();
    D newChildState = childState;
    for (P2<Flag, AVLSet<Test>> implication : implications) {
      Flag premise = implication._1();
      AVLSet<Test> consequences = implication._2();
      try {
        newChildState = childState.eval(premise.asTest());
      } catch (Unreachable _) {
        // if the premise is unreachable then all the consequences are entailed
        result = result.bind(premise, consequences);
        continue;
      }
      for (Test consequence : consequences) {
        if (isSemanticallyEntailedIn(consequence, newChildState)) {
          AVLSet<Test> entailedConsequences = result.get(premise).getOrElse(AVLSet.<Test>empty());
          entailedConsequences = entailedConsequences.add(consequence);
          result = result.bind(premise, entailedConsequences);
        }
      }
    }
    return result;
  }
}
