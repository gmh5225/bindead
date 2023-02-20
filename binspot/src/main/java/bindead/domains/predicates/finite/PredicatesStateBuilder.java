package bindead.domains.predicates.finite;

import static bindead.domains.predicates.finite.PredicatesState.MutableState.getEntailed;
import static bindead.domains.predicates.finite.PredicatesState.MutableState.isEntailed;

import java.util.List;

import javalx.fn.Predicate;
import javalx.mutablecollections.CollectionHelpers;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.util.FiniteTestSimplifier;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.combinators.FiniteStateBuilder;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.affine.Substitution;
import bindead.domains.predicates.finite.PredicatesState.Flag;
import bindead.domains.predicates.finite.PredicatesState.MutableState;

/**
 * Perform bulk operations on a predicates state.
 *
 * @author Bogdan Mihaila
 */
public class PredicatesStateBuilder extends FiniteStateBuilder {
  private final MutableState state;
  private final QueryChannel childState;

  public PredicatesStateBuilder (PredicatesState state, QueryChannel childState) {
    this.state = state.toMutable();
    this.childState = childState;
  }

  public PredicatesState build () {
    return state.toImmutable();
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Childops: ");
    builder.append(getChildOps());
    builder.append('\n');
    builder.append(state);
    return builder.toString();
  }

  protected boolean isFlag (NumVar flag) {
    return state.isFlag(flag);
  }

  protected boolean isPredicatesVar (NumVar var) {
    return state.isPredicatesVar(var);
  }

  /**
   * Get the predicates implied by the flag.
   */
  protected AVLSet<Test> getPredicates (NumVar flag, boolean flagValuation) {
    return state.getPredicates(flag, flagValuation);
  }

  /**
   * Get the predicates implied by the flag.
   */
  protected AVLSet<Test> getPredicates (Flag flag) {
    return state.getPredicates(flag);
  }

  /**
   * Return the flags that imply predicates that contain any of the passed in variables.
   */
  protected AVLSet<Flag> getOccurrencesOf (VarSet predicateVars) {
    AVLSet<Flag> occurences = AVLSet.empty();
    for (NumVar var : predicateVars) {
      occurences = occurences.union(state.getOccurrencesOf(var));
    }
    return occurences;
  }

  /**
   * Return the flags that contain one of the variables.
   */
  protected AVLSet<Flag> getFlagsFromVars (VarSet vars) {
    return state.getFlagsFromVars(vars);
  }

  protected void assignPredicateToFlag (NumVar flag, Test test) {
    // we separate the flags from the predicates, i.e. the flag vars cannot be part of a predicate
    removePredicatesContaining(flag);
    VarSet predicateVars = test.getVars();
    removeFlags(predicateVars);
    if (predicateVars.contains(flag)) {
      // the flag is overwritten but we cannot store it as it also occurs in the predicate
      projectFlag(flag, predicateVars);
      return;
    }
    projectFlag(flag, predicateVars);
    state.addPredicate(flag, true, test);
    state.addPredicate(flag, false, test.not());
  }

  protected void assignPredicatesToFlag (NumVar flag, AVLSet<Test> truePredicates, AVLSet<Test> falsePredicates) {
    VarSet predicatesVars =
      PredicatesState.getVariablesInPredicates(truePredicates).union(
          PredicatesState.getVariablesInPredicates(falsePredicates));
    // we separate the flags from the predicates, i.e. the flag vars cannot be part of a predicate
    removePredicatesContaining(flag);
    removeFlags(predicatesVars);
    if (predicatesVars.contains(flag)) {
      // the flag is overwritten but we cannot store it as it also occurs in the predicate
      projectFlag(flag, predicatesVars);
      return;
    }
    projectFlag(flag, predicatesVars);
    for (Test test : truePredicates) {
      state.addPredicate(flag, true, test);
    }
    for (Test test : falsePredicates) {
      state.addPredicate(flag, false, test);
    }
  }

  protected void assignCopy (NumVar targetFlag, NumVar sourceFlag) {
    if (targetFlag.equalTo(sourceFlag))
      return;
    projectFlag(targetFlag, VarSet.of(sourceFlag));
    state.copyFlagPredicates(targetFlag, sourceFlag);
  }

  protected void assignNegatedCopy (NumVar targetFlag, NumVar sourceFlag) {
    if (targetFlag.equalTo(sourceFlag)) {
      state.negateFlag(sourceFlag);
      return;
    }
    project(targetFlag, VarSet.of(sourceFlag));
    state.copyFlagPredicatesNegated(targetFlag, sourceFlag);
  }

  /**
   * Remove a flag. Tries to substitute it with other variables
   * that are equal. One can provide a set of variables that are not allowed to be used as substitutions.
   * This is useful when assigning new predicates to a flag where the flag needs to be projected first but
   * should not be substituted with a variable from its new predicates.
   */
  private void projectFlag (NumVar flag, VarSet forbiddenSubstitutionVariables) {
    assert !isPredicatesVar(flag);
    if (!isFlag(flag))
      return;
    // always disallow the substitution of flags by variables from their predicates as we need to keep
    // the two sets separate
    forbiddenSubstitutionVariables = forbiddenSubstitutionVariables.union(state.getVariablesInPredicatesOf(flag));
    tryFlagSubstitution(flag, forbiddenSubstitutionVariables);
    removeFlag(flag); // safeguard if we could not find a substitution
  }

  /**
   * Remove a variable from the flags or predicates. Tries to substitute it with other variables
   * that are equal. One can provide a set of variables that are not allowed to be used as substitutions.
   * This is useful when assigning new predicates to a flag where the flag needs to be projected first but
   * should not be substituted with a variable from its new predicates.
   */
  protected void project (NumVar var, VarSet forbiddenSubstitutionVariables) {
    if (isFlag(var)) {
      tryFlagSubstitution(var, forbiddenSubstitutionVariables);
      removeFlag(var); // safeguard if we could not find a substitution
    } else if (isPredicatesVar(var)) {
      tryPredicatesVarSubstitution(var);
      removePredicatesContaining(var); // safeguard if not all the occurrences could be substituted
    }
  }

  /**
   * Remove a variable from the flags or predicates. Tries to substitute it with other variables
   * that are equal.
   */
  protected void project (NumVar var) {
    project(var, VarSet.empty());
  }

  protected void substitute (NumVar x, NumVar y) {
    // sometimes y is from another domain (e.g. during the join) so it does not exist here
    // thus we can substitute a flag if y is either a variable or not a predicate
    if (isFlag(x) && (isFlag(y) || !isPredicatesVar(y))) {
      // just replace the variable in the maps
      state.substituteFlag(x, y);
      return;
    }
    if (isPredicatesVar(x) && isPredicatesVar(y)) {
      tryPredicatesVarSubstitution(x, y);
      removePredicatesContaining(x); // safeguard if we could not remove all occurrences of x by substitutions
      return;
    }
    // if the variables are not in the same camp we just give up and remove any known information
    // XXX: one could try to keep some information in this case
    removeFlag(x);
    removeFlag(y);
    removePredicatesContaining(x);
    removePredicatesContaining(y);
  }

  protected void copyAndPaste (VarSet vars, PredicatesStateBuilder from) {
    assert !state.getAllFlagVars().union(state.getAllPredicateVars()).containsAny(vars);
    for (NumVar varToCopy : vars) {
      AVLSet<Test> truePredicatesToCopy = from.getPredicates(varToCopy, true);
      truePredicatesToCopy = filterContaining(truePredicatesToCopy, vars);
      AVLSet<Test> falsePredicatesToCopy = from.getPredicates(varToCopy, false);
      falsePredicatesToCopy = filterContaining(falsePredicatesToCopy, vars);
      assignPredicatesToFlag(varToCopy, truePredicatesToCopy, falsePredicatesToCopy);
    }
  }

  private static AVLSet<Test> filterContaining (AVLSet<Test> predicates, final VarSet vars) {
    return CollectionHelpers.filter(predicates, new Predicate<Test>() {
      @Override public Boolean apply (Test test) {
        return vars.containsAll(test.getVars());
      }
    });
  }

  protected void intersect (PredicatesStateBuilder other) {
    state.intersect(other.state);
  }

  protected <D extends FiniteDomain<D>> boolean isSubset (D thisChildState, PredicatesStateBuilder other) {
    return isEntailed(other.state, state, thisChildState);
  }

  protected <D extends FiniteDomain<D>> PredicatesStateBuilder join (PredicatesStateBuilder other,
      D thisChildState, D otherChildState) {
    MutableState entailedThisInOther = getEntailed(state, other.state, otherChildState);
    MutableState entailedOtherInThis = getEntailed(other.state, state, thisChildState);
    entailedThisInOther.union(entailedOtherInThis);
    return new PredicatesStateBuilder(entailedThisInOther.toImmutable(), thisChildState);
  }

  protected void renameFold (FoldMap pairs) {
    for (VarPair pair : pairs) {
      rename(pair.getEphemeral(), pair.getPermanent());
    }
  }

  public void renameExpand (List<VarPair> nvps) {
    for (VarPair pair : nvps) {
      rename(pair.getPermanent(), pair.getEphemeral());
    }
  }

  private void rename (NumVar x, NumVar y) {
    if (isFlag(x)) {
      state.copyFlagPredicates(y, x);
      removeFlag(x);
    } else if (isPredicatesVar(x)) {
      Substitution substitution = new Substitution(x, Linear.linear(y), Bound.ONE);
      applyPredicatesSubstitution(substitution);
    }
  }

  protected void union (PredicatesStateBuilder other) {
    state.union(other.state);
  }

  protected void applyPredicatesSubstitution (Substitution substitution) {
    for (Flag flag : state.getOccurrencesOf(substitution.getVar())) {
      AVLSet<Test> oldPredicates = state.getPredicates(flag);
      AVLSet<Test> newPredicates = AVLSet.empty();
      for (Test oldPredicate : oldPredicates) {
        if (!oldPredicate.getVars().contains(substitution.getVar())) {
          newPredicates = newPredicates.add(oldPredicate);
          continue;
        }
        Test newPredicate = oldPredicate.applySubstitution(substitution);
        if (!oldPredicate.equals(newPredicate))
          newPredicate = FiniteTestSimplifier.normalizeTautology(newPredicate); // substitutions can produce tautologous tests
        newPredicates = newPredicates.add(newPredicate);
      }
      VarSet oldVars = PredicatesState.getVariablesInPredicates(oldPredicates);
      VarSet newVars = PredicatesState.getVariablesInPredicates(newPredicates);
      VarSet removedVars = oldVars.difference(newVars);
      for (NumVar var : removedVars) {
        state.removeVariableOccurrenceInPredicatesOf(flag, var);
      }
      VarSet addedVars = newVars.difference(oldVars);
      for (NumVar var : addedVars) {
        state.addVariableOccurrenceInPredicatesOf(flag, var);
      }
      state.setPredicates(flag, newPredicates);
    }
  }

  private void removeFlag (NumVar flag) {
    // flag can also not exist
    if (!isFlag(flag))
      return;
    state.removeFlag(flag);
  }

  private void removeFlags (VarSet flags) {
    for (NumVar flag : flags) {
      removeFlag(flag);
    }
  }

  private void removePredicatesContaining (NumVar var) {
    state.removePredicatesContaining(var);
  }

  private void tryFlagSubstitution (NumVar flag, VarSet forbiddenSubstitutionVariables) {
    AVLSet<Substitution> allSubstitutions = state.generateSubstitutionsFor(flag, childState);
    AVLSet<Substitution> usefulSubstitutions = AVLSet.empty();
    for (Substitution substitution : allSubstitutions) {
      if (substitution.isSimple()) {
        NumVar y = substitution.getExpr().getSingleVarOrNull();
        if (!forbiddenSubstitutionVariables.contains(y) && !isPredicatesVar(y)) {
          usefulSubstitutions = AVLSet.singleton(substitution);
          break; // shortcut the loop as we need only one suitable substitution
        }
      }
    }
    if (!usefulSubstitutions.isEmpty()) {
      Substitution substitution = usefulSubstitutions.getMin().get();
      assert substitution.getFac().isOne();
      NumVar y = substitution.getExpr().getSingleVarOrNull();
      assert y != null;
      state.substituteFlag(flag, y);
    }
  }

  private void tryPredicatesVarSubstitution (NumVar var) {
    AVLSet<Substitution> allSubstitutions = state.generateSubstitutionsFor(var, childState);
    AVLSet<Substitution> usefulSubstitutions = AVLSet.empty();
    for (Substitution substitution : allSubstitutions) {
      boolean isUseful = true;
      for (NumVar substituteVar : substitution.getExpr().getVars()) {
        if (isFlag(substituteVar)) {
          isUseful = false;
          break;
        }
      }
      if (isUseful) {
        usefulSubstitutions = AVLSet.singleton(substitution);
        break; // shortcut the loop as we need only one suitable substitution
      }
    }
    if (!usefulSubstitutions.isEmpty()) {
      Substitution substitution = usefulSubstitutions.getMin().get();
      applyPredicatesSubstitution(substitution);
    }
  }

  private void tryPredicatesVarSubstitution (NumVar x, NumVar y) {
    assert !x.equalTo(y);
    Substitution substitution = new Substitution(x, Linear.linear(y), BigInt.ONE);
    applyPredicatesSubstitution(substitution);
  }
}
