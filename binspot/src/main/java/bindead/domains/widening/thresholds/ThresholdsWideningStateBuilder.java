package bindead.domains.widening.thresholds;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.Bound;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.combinators.ZenoStateBuilder;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Substitution;
import bindead.domains.widening.thresholds.ThresholdsWideningState.MutableState;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.Unreachable;

/**
 * Helper for modifying (and building) the widening thresholds domain state.
 */
class ThresholdsWideningStateBuilder<D extends ZenoDomain<D>> extends ZenoStateBuilder {
  private static ZenoFactory zeno = ZenoFactory.getInstance();
  private MutableState thresholds;

  public ThresholdsWideningStateBuilder (ThresholdsWideningState state) {
    thresholds = state.mutableCopy();
  }

  public ThresholdsWideningState build () {
    return thresholds.immutableCopy();
  }

  /**
   * Adds the predicates from this test as widening thresholds if the test is redundant.
   *
   * @return The resulting state after applying the test. This is done for performance reasons as to not have to apply
   *         the test again if it already was applied here as a side-effect of testing the redundancy.
   */
  protected D collectThresholds (Option<ProgramPoint> currentLocation, Test test, D state) {
    if (ZenoTestHelper.isTautologyReportUnreachable(test)) {
      if (AnalysisProperties.INSTANCE.debugTests.isTrue())
        System.out.println("Thresholds swallows test: " + test);
      return state;
    }
    if (test.getOperator() == ZenoTestOp.NotEqualToZero || test.getOperator() == ZenoTestOp.EqualToZero) {
      if (AnalysisProperties.INSTANCE.debugTests.isTrue())
        System.out.println("Thresholds splits test: " + test);
      P2<Test, Test> tests = test.splitEquality();
      collect(currentLocation, tests._1(), state);
      collect(currentLocation, tests._2(), state);
      // we definitely need to apply the original test again here as a (dis-)equality that is split up
      // in two lower-equal tests is not as strong as the original test or then we would need to
      // do a meet of both results instead of the join and a meet operation we do not have
      return state.eval(test);
    } else {
      if (AnalysisProperties.INSTANCE.debugTests.isTrue())
        System.out.println("Thresholds test: " + test);
      D newState = collect(currentLocation, test, state);
      if (newState != null)
        return newState;
      return state.eval(test);
    }
  }

  /**
   * If a test is only on flag variables it is not interesting as a threshold.
   * This way we can avoid collecting lots of threshold on points to flags that
   * never are of any use.
   */
  private static boolean isInterestingAsThreshold (Test test) {
    for (NumVar var : test.getVars()) {
      if (!var.isFlag())
        return true;
    }
    return false;
  }

  private D collect (Option<ProgramPoint> currentLocation, Test test, D state) {
    assert test.getOperator() != ZenoTestOp.NotEqualToZero && test.getOperator() != ZenoTestOp.EqualToZero;
    if (!isInterestingAsThreshold(test))
      return null;
    Threshold threshold;
    if (currentLocation.isSome()) // FIXME: this does not matter, so remove!
      threshold = new Threshold(currentLocation.get(), test);
    else
      threshold = new Threshold(ThresholdsWideningState.zeroPoint, test);
    // not precise enough on some tests
//    if (ZenoTestHelper.isAlwaysSatisfiable(test, state))
//      thresholds.add(threshold);
//    return null;
    P2<Boolean, D> result = isRedundant(test, state);
    // should not make a difference if we collect all thresholds here as the join later on anyway removes non-redundant ones
    // However, we can limit the amount of thresholds that need to be propagated by removing the non-redundant already here.
    if (result._1())
      thresholds.add(threshold); // replaces any transformed threshold
    return result._2();
  }

  /**
   * See if a test is redundant, i.e. has no effect on the child state.
   *
   * @return a tuple saying if the state was redundant and the resulting state itself.
   *         If the test caused an unreachable exception then the returned result state will be null.
   */
  private P2<Boolean, D> isRedundant (Test test, D state) {
    try {
      D newState = state.eval(test);
      // if the test did not modify the state (make it smaller) then the original state is still subset-equal to it
      boolean redundant = state.subsetOrEqual(newState);
      return P2.tuple2(redundant, newState);
    } catch (Unreachable _) {
      return P2.tuple2(false, null);
    }
  }

  /**
   * See if a test is redundant, i.e. has no effect on the child state.
   * Unfortunately this is less precise on some tests.
   */
  @SuppressWarnings("unused") private P2<Boolean, D> isRedundantwithQuery (Test test, D state) {
    if (ZenoTestHelper.isAlwaysSatisfiable(test, state))
      return P2.tuple2(true, state.eval(test));
    return P2.tuple2(false, null);
  }

  /**
   * See if a test is redundant (has no effect on child state) by applying the negated test on the child and
   * seeing if it is unreachable.
   * Unfortunately that is less precise and would flag tests redundant/non-redundant although they are not.
   * This happens as the the child domain is not precise.
   * An example: testing the redundancy of "j - 1 <= i" in the state {i = [0, 1], j = [0, 2]}
   * applying the negated test "i <= j - 2" does not yield bottom but applying the original test
   * will not change the state.
   */
  @SuppressWarnings("unused") private P2<Boolean, D> isRedundantWithNegatedTest (Test test, D state) {
    Test opposingTest = test.not();
    try {
      state.eval(opposingTest);
    } catch (Unreachable _) {
      D newState = state.eval(test);
      // the opposing test is unsatisfiable, thus, the original test is redundant
      return P2.tuple2(true, newState);
    }
    return P2.tuple2(false, null);
  }

  protected void expand (FoldMap pairs) {
    for (Threshold threshold : thresholds.getTresholdsContainingAnyOf(pairs.getAllVars())) {
      Test test = threshold.getTest();
      Linear lin = test.getExpr();
      Linear renamed = lin.renameVar(pairs);
      if (renamed != null) {
        Test newTest = zeno.comparison((renamed), test.getOperator());
        thresholds.add(threshold.withTest(newTest), thresholds.getTransformations(threshold));
      }
    }
  }

  protected void fold (FoldMap pairs) {
    Set<Threshold> permanentSet = new HashSet<Threshold>();
    Set<Threshold> ephemeralSet = new HashSet<Threshold>();
    // also save the state of the transformed thresholds to be able to add them back again
    MutableState addedSet = ThresholdsWideningState.EMPTY.mutableCopy();
    Set<Threshold> removeSet = new HashSet<Threshold>();
    for (Threshold threshold : thresholds.getTresholdsContainingAnyOf(pairs.getAllVars())) {
      Test test = threshold.getTest();
      Linear lin = test.getExpr();
      VarSet vars = lin.getVars();
      boolean hasEphemeral = false;
      boolean hasPermanent = false;
      for (VarPair pair : pairs) {
        if (vars.contains(pair.getEphemeral()))
          hasEphemeral = true;
        if (vars.contains(pair.getPermanent()))
          hasPermanent = true;
      }
      // remove this test as soon as we find that it contains one ephemeral or permanent variable
      if (hasEphemeral || hasPermanent)
        removeSet.add(threshold);
      if (hasEphemeral && hasPermanent)
        continue;
      if (hasPermanent) {
        permanentSet.add(threshold);
        addedSet.add(threshold, thresholds.getTransformations(threshold));
      }
      if (hasEphemeral) {
        Test newTest = zeno.comparison((test.getExpr().renameVar(pairs)), test.getOperator());
        Threshold newThreshold = threshold.withTest(newTest);
        ephemeralSet.add(newThreshold);
        addedSet.add(newThreshold, thresholds.getTransformations(threshold));
      }
    }
    for (Threshold threshold : removeSet) {
      thresholds.remove(threshold);
    }
    // calculate the intersection of the two sets
    permanentSet.retainAll(ephemeralSet);
    for (Threshold threshold : permanentSet) {
      thresholds.add(threshold, addedSet.getTransformations(threshold));
    }
  }

  protected void copyAndPaste (VarSet vars, ThresholdsWideningState otherState) {
    ThresholdsWideningStateBuilder<D> otherBuilder = new ThresholdsWideningStateBuilder<D>(otherState);
    for (Threshold threshold : otherBuilder.thresholds.getTresholdsContainingAnyOf(vars)) {
      Test test = threshold.getTest();
      if (vars.containsAll(test.getExpr().getVars()))
        this.thresholds.add(threshold, otherBuilder.thresholds.getTransformations(threshold));
    }
  }

  /**
   * Substitute all occurrences of {@code x} with {@code y}.
   *
   * @param x the variable to replace
   * @param y the variable to add
   */
  protected void substitute (NumVar x, NumVar y) {
    Substitution sigma = new Substitution(x, Linear.linear(y), Bound.ONE);
    applySubstitutionToAll(x, sigma, ProgramPoint.nowhere); // this should definitely be allowed more than once
  }

  protected void removeThresholdsContaining (NumVar var, D state, Option<ProgramPoint> location) {
    AVLSet<Substitution> subsititions = generateSubstitutionsFor(var, state);
    if (!subsititions.isEmpty()) // try to keep the threshold by using a substitution
      applySubstitutionToAll(var, subsititions.getMin().get(), location);
    // now remove all thresholds
    for (Threshold threshold : thresholds.getTresholdsContaining(var)) {
      thresholds.remove(threshold);
    }
  }

  /**
   * Applies a substitution to all thresholds containing the variable. Note that for termination it is important
   * to not allow infinite substitutions. Thus substitutions should be allowed only finitely many times per
   * program point. If no program point is given then this method will always apply the substitution.
   * Take care to call it with a program point wherever necessary!
   */
  private void applySubstitutionToAll (NumVar var, Substitution sigma, Option<ProgramPoint> location) {
    for (Threshold threshold : thresholds.getTresholdsContaining(var)) {
      if (location.isSome() && thresholds.getTransformations(threshold).contains(location.get()))
        continue; // do not apply a substitution more than once for termination!
      Test test = threshold.getTest();
      Test newTest = test.applySubstitution(sigma);
      if (ZenoTestHelper.isTautology(newTest)) {
        thresholds.remove(threshold);
      } else {
        Threshold newThreshold = threshold.withTest(newTest);
        thresholds.substitute(threshold, newThreshold);
        if (location.isSome())
          thresholds.markTransformedAt(newThreshold, location.get());
      }
    }
  }

  /**
   * Perform an affine transformation if possible on the equality system such that {@code lhs = rhs} holds.
   */
  protected void applyAffineTransformation (NumVar lhs, Rlin rhs, D state, Option<ProgramPoint> location) {
    if (rhs.getVars().contains(lhs)) {
      // this is an invertible substitution
      Substitution subst = Substitution.invertingSubstitution(rhs.getLinearTerm(), rhs.getDivisor(), lhs);
      applySubstitutionToAll(lhs, subst, location);
    } else {
      // this is a non-invertible substitution thus remove any thresholds on lhs
      // the removal though performs substitutions to try to keep the threshold
      removeThresholdsContaining(lhs, state, location);
      // see if we can use the equality at least by generating some new thresholds from the already known ones
      // not sure if we really need this as the threshold removal above already tries to perform some substitutions
      // to keep the removed threshold by using other variables.
//      Linear newEq = rhs.getLinearTerm().subTerm(rhs.getDivisor(), lhs);
//      newEq = newEq.toEquality();
//      generateNewPredicates(newEq, location);
    }
  }

  /**
   * Generates new predicates from a new linear equality that holds. It does this by trying to substitute variables
   * in all thresholds using this new equality.
   * Very costly, thus see if really needed as the affine might just do the same job.
   */
  private void generateNewPredicates (Linear newEq, Option<ProgramPoint> location) {
    for (Threshold threshold : thresholds.getTresholdsContainingAnyOf(newEq.getVars())) {
      Test test = threshold.getTest();
      Linear testExpr = test.getExpr();
      VarSet vars = testExpr.getVars();
      for (NumVar var : vars) {
        Substitution sigma = newEq.genSubstitutionOrNull(var);
        if (sigma != null) {
          Test newTest = test.applySubstitution(sigma);
          if (thresholds.contains(threshold)) { // threshold may has been already removed earlier for a different variable
            // FIXME: why do we remove the other one here and not just add new ones?
            Threshold newThreshold = threshold.withTest(newTest);
            thresholds.substitute(threshold, newThreshold);
            if (location.isSome())
              thresholds.markTransformedAt(newThreshold, location.get());
            // FIXME: with any of below some sendmail examples do not terminate
//            thresholds.add(newThreshold);
//            thresholds.add(newThreshold, thresholds.getTransformations(threshold));
            continue;
          }
        }
      }
    }
  }

  private static AVLSet<Substitution> generateSubstitutionsFor (NumVar var, QueryChannel child) {
    if (child == null)
      return AVLSet.empty();
    SetOfEquations equalities = child.queryEqualities(var);
    return generateSubstitutionsFor(var, equalities);
  }

  private static AVLSet<Substitution> generateSubstitutionsFor (NumVar var, SetOfEquations equalities) {
    AVLSet<Substitution> substitutions = AVLSet.empty();
    if (equalities.isEmpty())
      return AVLSet.empty();
    for (Linear equality : equalities) {
      substitutions = substitutions.add(equality.genSubstitution(var));
    }
    return substitutions;
  }

  /**
   * Apply all non-redundant predicates to the child.
   *
   * @return The narrowed child state and the applied tests.
   */
  protected P2<D, Set<Threshold>> applyNarrowing (D state, Option<ProgramPoint> point) {
    if (point.isNone())
      return P2.tuple2(state, Collections.<Threshold>emptySet());
    ProgramPoint location = point.get();
    // only allow widening at a certain point for a fixed number of times
    // which is chosen to reflect the amount of nested loops and thresholds
    // that might be necessary to apply at a widening point. The +1 was necessary for some example.
    int wideningsHere = thresholds.getWideningNumber(location);
    if (wideningsHere > (thresholds.wideningPointsSize() * thresholds.thresholdsSize() + 1))
      return P2.tuple2(state, Collections.<Threshold>emptySet());
    thresholds.appliedNarrowingAt(location);
    // we could just apply all predicates, i.e. redundant and non-redundant
    // performance-wise the above seems marginally faster
    Set<Threshold> nonRedundant = toMutableSet(thresholds.asSet());
    // FIXME: below seems to be inprecise with CollectedWideningExamples.twoNestedLoopsStrict which uses the phase domain
//    Set<Threshold> nonRedundant = getNonRedundantThresholds(state);
    if (nonRedundant.isEmpty())
      return P2.<D, Set<Threshold>>tuple2(state, Collections.<Threshold>emptySet());
    D narrowedState = applyThresholds(nonRedundant, state);
    removeNonRedundantThresholds(narrowedState);
    return P2.<D, Set<Threshold>>tuple2(narrowedState, nonRedundant);
  }

  private static Set<Threshold> toMutableSet (Iterable<Threshold> set) {
    Set<Threshold> result = new HashSet<>();
    for (Threshold threshold : set) {
      result.add(threshold);
    }
    return result;
  }

  private Set<Threshold> getNonRedundantThresholds (D state) {
    Set<Threshold> nonRedundant = new HashSet<Threshold>();
    for (Threshold threshold : thresholds) {
      Test test = threshold.getTest();
      if (!isRedundant(test, state)._1())
        nonRedundant.add(threshold);
    }
    return nonRedundant;
  }

  protected void removeNonRedundantThresholds (D state) {
    Set<Threshold> nonRedundant = getNonRedundantThresholds(state);
    for (Threshold threshold : nonRedundant) {
      thresholds.remove(threshold);
    }
  }

  private D applyThresholds (Set<Threshold> thresh, D state) {
    for (Threshold threshold : thresh) {
      try {
        state = state.eval(threshold.getTest());
      } catch (Unreachable _) {
        // tests that have been redundant once (and are therefore in predicates) cannot possibly make the domain
        // unreachable since domains should grow monotonically.
        throw new InvariantViolationException();
      }
    }
    return state;
  }

  protected void mergeThresholds (D thisChildState, D otherChildState, ThresholdsWideningStateBuilder<D> otherBuilder) {
    // NOTE: this is possible, too, but would remove thresholds that require substitutions to be redundant in the other state
//    D joinedChildState = thisChildState.join(otherChildState);
//    MutableState joinedState = this.thresholds.union(otherBuilder.thresholds);
//    thresholds = joinedState;
//    removeNonRedundantThresholds(joinedChildState);
    P3<MutableState, MutableState, MutableState> split = thresholds.split(otherBuilder.thresholds);
    MutableState onlyInThis = split._1();
    MutableState inBoth = split._2();
    MutableState onlyInOther = split._3();
    assert onlyInThis.isConsistent();
    assert onlyInOther.isConsistent();
    assert inBoth.isConsistent();
    thresholds = inBoth;
    copyRedundantThresholdsFrom(onlyInThis, thisChildState, otherChildState);
    copyRedundantThresholdsFrom(onlyInOther, otherChildState, thisChildState);
  }

  private void copyRedundantThresholdsFrom (MutableState candidates, D candidatesState, D otherState) {
    for (Threshold threshold : candidates) {
      Test test = threshold.getTest();
      Test redundantTest = getRedundantTest(test, candidatesState, otherState, new HashSet<Test>());
      // TODO: we do perform here substitutions. This might introduce test transformations and lead to non-termination
      // see if we run into such a problem and then do not allow endless substitutions
      if (redundantTest != null)
        thresholds.add(threshold.withTest(redundantTest), candidates.getTransformations(threshold));
    }
  }

  /**
   * Return a test that can be derived by substitutions from the given candidate test and which is redundant
   * in the other state. If such a test does not exist it returns {@code null}.
   */
  private Test getRedundantTest (Test candidate, D candidatesState, D otherState, Set<Test> usedCandidates) {
    // for scalability and termination we limit the number of tests that we try
    // termination is not guaranteed by remembering the tests as substitutions can generate different
    // syntactic tests that are semantically the same
    if (usedCandidates.size() > 5) // TODO: what is a good value? make this configurable somewhere
      return null;
    if (isRedundant(candidate, otherState)._1()) {
      return candidate;
    } else {
      usedCandidates.add(candidate);
      // try some substitutions as we might just have the threshold
      // on a variable that does not exist in the child state and thus the threshold cannot be redundant
      AVLSet<Test> substitutes = getSubstituteTests(candidate, candidatesState, otherState);
      for (Test substitute : substitutes) {
        if (usedCandidates.contains(substitute))
          continue;
        Test redundantTest = getRedundantTest(substitute, candidatesState, otherState, usedCandidates);
        if (redundantTest != null)
          return redundantTest;
      }
    }
    return null; // none of the substitutions yielded a test that was redundant
  }

  private AVLSet<Test> getSubstituteTests (Test test, D testState, D otherState) {
    VarSet testVars = test.getVars();
    AVLSet<Substitution> substitutions = AVLSet.empty();
    for (NumVar var : testVars) {
      // NOTE: as an optimization it is only necessary to generate substitutions for equalities that
      // do not exist in the other state. The ones that exist there have been tested already on test application.
      SetOfEquations thisEqualities = testState.queryEqualities(var);
      SetOfEquations otherEqualities = otherState.queryEqualities(var);
      substitutions = substitutions.union(generateSubstitutionsFor(var, thisEqualities.difference(otherEqualities)));
    }
    AVLSet<Test> substitutes = AVLSet.empty();
    for (Substitution substitution : substitutions) {
      Test substituteTest = test.applySubstitution(substitution);
      if (!ZenoTestHelper.isTautology(substituteTest))
        substitutes = substitutes.add(substituteTest);
    }
    return substitutes;
  }

  public boolean subsetOrEqual (D thisChildState, ThresholdsWideningStateBuilder<D> other) {
    // NOTE: simple set difference does not work as we remove some thresholds as non-redundant during
    // the join and then re-add them on some program path. This seems weird but possible
    // due to wrapping adding new thresholds. Consider the threshold 2 < x being added inside the loop:
    // int x = 0; while (true) { x = x + 2; }
    // The threshold is lost at the loop header in each iteration but because x is incremented it can
    // be re-added each time as redundant again. Thus testing for subset as above would not terminate.
//    AVLSet<Threshold> thresholdsInThis = AVLSet.fromIterable(thresholds.asSet());
//    AVLSet<Threshold> thresholdsInOther = AVLSet.fromIterable(other.thresholds.asSet());
//    return thresholdsInThis.difference(thresholdsInOther).isEmpty();
//    return thresholds.thresholds.difference(other.thresholds.thresholds).isEmpty();
    // this subset is similar to the one in predicates
    // this seems to work as expected
    MutableState allFromOther = other.thresholds;
    other.removeNonRedundantThresholds(thisChildState);
    MutableState entailed = other.thresholds;
    return allFromOther.thresholds.difference(entailed.thresholds).isEmpty();
  }

  @Override public String toString () {
    return build().toString();
  }

}
