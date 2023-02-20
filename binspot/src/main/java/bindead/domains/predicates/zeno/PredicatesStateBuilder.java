package bindead.domains.predicates.zeno;

import static bindead.domains.predicates.zeno.PredicatesState.MutableState.getEntailed;
import static bindead.domains.predicates.zeno.PredicatesState.MutableState.isEntailed;
import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.combinators.ZenoStateBuilder;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Substitution;
import bindead.domains.predicates.zeno.PredicatesState.MutableState;

/**
 * Perform bulk operations on a predicates state.
 *
 * @author Bogdan Mihaila
 */
class PredicatesStateBuilder extends ZenoStateBuilder {
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

  public boolean contains (NumVar variable) {
    return state.contains(variable);
  }

  /**
   * Return all the premises (left-hand-sides) of the tracked implications.
   */
  public Iterable<Test> getAllLhs () {
    return state.getAllLhs();
  }

  /**
   * Return all the premises (left-hand-sides) of the tracked implications
   * that contain any of the passed in variables.
   */
  public Iterable<Test> getAllLhsContaining (VarSet vars) {
    return state.getAllLhsContaining(vars);
  }

  /**
   * Return all the consequences (right-hand-sides) of the tracked implications.
   */
  public Iterable<Test> getAllRhs () {
    return state.getAllRhs();
  }

  /**
   * Return all the consequences (right-hand-sides) of the tracked implications
   * that contain any of the passed in variables.
   */
  public Iterable<Test> getAllRhsContaining (VarSet vars) {
    return state.getAllRhsContaining(vars);
  }

  /**
   * Returns all the implied predicates for the given predicate, i.e. all the right-hand-sides
   * of implications containing the passed in predicate as a premise.
   */
  public AVLSet<Test> getConsequences (Test predicate) {
    AVLSet<Test> consequences = state.getConsequences(predicate);
    // usually this is called for predicates known to be in the state, so empty is an error
    assert !consequences.isEmpty();
    return consequences;
  }

  /**
   * Returns all the predicates that imply the passed in predicate, i.e. the left-hand-sides
   * of implications containing the passed in predicate.
   */
  public AVLSet<Test> getPremises (Test predicate) {
    AVLSet<Test> premises = state.getPremises(predicate);
    // usually this is called for predicates known to be in the state, so empty is an error
    assert !premises.isEmpty();
    return premises;
  }

  protected void addImplication (Test premise, Test consequence) {
    state.addImplication(premise, consequence);
  }

  protected void addImplications (Test premise, AVLSet<Test> consequences) {
    for (Test consequence : consequences) {
      state.addImplication(premise, consequence);
    }
  }

  protected void project (NumVar variable) {
    if (!contains(variable))
      return;
    Option<Substitution> substitution = getSubstitutionFor(variable);
    if (substitution.isSome())
      state.substituteInPredicates(substitution.get());
    state.removePredicatesContaining(variable);
  }

  protected void substitute (NumVar x, NumVar y) {
    assert !x.equalTo(y);
    if (!contains(x))
      return;
    Substitution substitution = new Substitution(x, Linear.linear(y), BigInt.ONE);
    substitute(substitution);
  }

  protected void substitute (Substitution substitution) {
    state.substituteInPredicates(substitution);
  }

  private Option<Substitution> getSubstitutionFor (NumVar variable) {
    if (childState == null)
      return Option.none();
    SetOfEquations equalities = childState.queryEqualities(variable);
    if (equalities.isEmpty())
      return Option.none();
    Substitution substitution = equalities.iterator().next().genSubstitution(variable);
    return Option.some(substitution);
  }

  protected void renameFold (FoldMap pairs) {
    for (VarPair pair : pairs) {
      substitute(pair.getEphemeral(), pair.getPermanent());
    }
  }

  protected void renameExpand (FoldMap pairs) {
    for (VarPair pair : pairs) {
      substitute(pair.getPermanent(), pair.getEphemeral());
    }
  }

  protected void copyAndPaste (VarSet vars, PredicatesStateBuilder other) {
    other.state.removePredicatesNotContaining(vars);
    union(other);
  }

  protected void union (PredicatesStateBuilder other) {
    state.union(other.state);
  }

  protected void intersect (PredicatesStateBuilder other) {
    state.intersect(other.state);
  }

  @SuppressWarnings("unchecked") protected <D extends ZenoDomain<D>> boolean isSubset (
      PredicatesStateBuilder otherBuilder) {
    return isEntailed(otherBuilder.state, state, (D) childState);
  }

  @SuppressWarnings("unchecked") protected <D extends ZenoDomain<D>> PredicatesStateBuilder join (
      PredicatesStateBuilder otherBuilder) {
    MutableState entailedThisInOther = getEntailed(state, otherBuilder.state, (D) otherBuilder.childState);
    MutableState entailedOtherInThis = getEntailed(otherBuilder.state, state, (D) childState);
    entailedThisInOther.union(entailedOtherInThis);
    return new PredicatesStateBuilder(entailedThisInOther.toImmutable(), childState);
  }

}
