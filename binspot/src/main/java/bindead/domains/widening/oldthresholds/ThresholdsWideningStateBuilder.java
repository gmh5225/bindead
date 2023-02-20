package bindead.domains.widening.oldthresholds;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.Linear.Divisor;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.combinators.ZenoStateBuilder;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Substitution;
import bindead.domains.widening.oldthresholds.ThresholdsWideningState.MutableState;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.Unreachable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
    if (currentLocation.isNone()) // tests without an origin are not of interest to us
      return state.eval(test);
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
    Threshold threshold = new Threshold(currentLocation.get(), test);
    // TODO: not sure why we needed this conditions as it now works even without.
    // keep (possibly transformed) threshold if it has already been applied for narrowing
    // i.e. do not overwrite it with the not transformed one.
    // If the tracked threshold has not been applied anywhere yet then we overwrite by collecting the original again.
    if (thresholds.contains(threshold.getOrigin()) && thresholds.isApplied(threshold.getOrigin())
      || thresholds.containsAnyAppliedFrom(currentLocation.get()))
      return null;
// XXX: not yet feasible as it is less precise. Need to find out where and why before enabling it by default.
//    if (ZenoTestHelper.isAlwaysSatisfiable(test, state))
//      thresholds.add(threshold);
//    return null;

    P2<Boolean, D> result = isRedundant(test, state);
    if (result._1())
      thresholds.add(threshold);
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
   * See if a test is redundant (has no effect on child state) by applying the negated test on the child and
   * seeing if it is unreachable.
   * Unfortunately that is less precise and would flag tests redundant although they are not when the child domain
   * is not precise.
   */
  @SuppressWarnings("unused") private boolean isRedundantOld (Test test, D state) {
    Test opposingTest = test.not();
    try {
      state.eval(opposingTest);
    } catch (Unreachable _) {
      // the opposing test is unsatisfiable, thus, the original test is redundant
      return true;
    }
    return false;
  }

  protected void removeThresholdsContaining (NumVar var, D state) {
    // TODO: enabling substitutions comes with a big runtime cost as we keep many more thresholds
    // see if this can be improved somehow or enable only when necessary
    AVLSet<Substitution> subsititions = generateSubstitutionsFor(var, state);
    if (!subsititions.isEmpty()) // try to keep the threshold by using a substitution
      applySubstitution(var, subsititions.getMin().get());

    // FIXME: use reverse map!
    for (Threshold threshold : thresholds) {
      Test test = threshold.getTest();
      Linear lin = test.getExpr();
      VarSet vars = lin.getVars();
      if (vars.contains(var))
        thresholds.remove(threshold);
    }
  }

  protected void expand (FoldMap pairs) {
    // FIXME: use reverse map!
    for (Threshold threshold : thresholds) {
      Test test = threshold.getTest();
      Linear lin = test.getExpr();
      Linear renamed = lin.renameVar(pairs);
      if (renamed != null) {
        Test newTest = zeno.comparison((renamed), test.getOperator());
        thresholds.add(threshold.withTest(newTest), thresholds.getAppliedPoints(threshold));
      }
    }
  }

  protected void fold (FoldMap pairs) {
    Set<Threshold> permanentSet = new HashSet<Threshold>();
    Set<Threshold> ephemeralSet = new HashSet<Threshold>();
    // also save the state of the transformed thresholds to be able to add them back again
    MutableState addedSet = ThresholdsWideningState.EMPTY.mutableCopy();
    Set<Threshold> removeSet = new HashSet<Threshold>();
    for (Threshold threshold : thresholds) {
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
        addedSet.add(threshold, thresholds.getAppliedPoints(threshold));
      }
      if (hasEphemeral) {
        Test newTest = zeno.comparison((test.getExpr().renameVar(pairs)), test.getOperator());
        Threshold newThreshold = threshold.withTest(newTest);
        ephemeralSet.add(newThreshold);
        addedSet.add(newThreshold, thresholds.getAppliedPoints(threshold));
      }
    }
    for (Threshold threshold : removeSet) {
      thresholds.remove(threshold);
    }
    // calculate the intersection of the two sets
    permanentSet.retainAll(ephemeralSet);
    for (Threshold threshold : permanentSet) {
      thresholds.add(threshold, addedSet.getAppliedPoints(threshold));
    }
  }

  protected void copyAndPaste (VarSet vars, ThresholdsWideningState otherState) {
    ThresholdsWideningStateBuilder<D> otherBuilder = new ThresholdsWideningStateBuilder<D>(otherState);
    for (Threshold threshold : otherBuilder.thresholds) {
      Test test = threshold.getTest();
      if (vars.containsAll(test.getExpr().getVars()))
        this.thresholds.add(threshold, otherBuilder.thresholds.getAppliedPoints(threshold));
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
    applySubstitution(x, sigma);
  }

  private void applySubstitution (NumVar var, Substitution sigma) {
    // FIXME: introduce a reverse map, cause this can get quite expensive
    // then one can immediately see which threshold contains some of the var in sigma
    for (Threshold threshold : thresholds) {
      Test test = threshold.getTest();
      VarSet vars = test.getVars();
      if (vars.contains(var)) {
        Test newTest = test.applySubstitution(sigma);
        if (ZenoTestHelper.isTautology(newTest))
          thresholds.remove(threshold);
        else
          thresholds.substitute(threshold, threshold.withTest(newTest));
      }
    }
  }

  /**
   * Perform an affine transformation on the equality system such that {@code d*var = rhs} holds. Creates up to two
   * substitutions and up to one variable that needs projecting out.
   *
   * @param d the factor in front of the variable, represented by a {@link bindead.data.numeric.Linear.Divisor} object
   * @param var the variable
   * @param rhs the new value of the scaled variable, may only contain parameter variables
   */
  protected void applyAffineTransformation (Divisor d, NumVar var, Linear rhs, D state) {
    if (!rhs.getCoeff(var).isZero()) {
      // this is an invertible substitution
      Substitution subst = Substitution.invertingSubstitution(rhs, d.get(), var);
      applySubstitution(var, subst);
    } else {
      // this is a non-invertible substitution thus remove information on var
      Linear newEq = rhs.subTerm(d.get(), var);
      newEq = newEq.toEquality();
      removeThresholdsContaining(var, state);
      generateNewPredicates(newEq); // some tests seem to need it but some even seem to be more precise (ChaouchNested)
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

  private void generateNewPredicates (Linear newEq) {
    // FIXME: introduce a reverse map, cause this can get quite expensive
    // then one can immediately see which threshold contains some of the vars in newEq
    for (Threshold threshold : thresholds) {
      Test test = threshold.getTest();
      Linear lin = test.getExpr();
      VarSet vars = lin.getVars();
      for (NumVar var : vars) {
        Substitution sigma = newEq.genSubstitutionOrNull(var);
        if (sigma != null) {
          Test newTest = zeno.comparison((lin.applySubstitution(sigma)), test.getOperator());
          if (thresholds.contains(threshold)) { // threshold may has been already removed earlier for a different variable
            thresholds.substitute(threshold, threshold.withTest(newTest));
          }
        }
      }
    }
  }

  /**
   * Apply all non-redundant and unconsumed predicates to the child.
   *
   * @return The narrowed child state and the smallest applied test.
   */
  protected P3<D, Threshold, Set<Threshold>> applyNarrowing (D state, Option<ProgramPoint> point) {
    // we could just apply all predicates, i.e. redundant and non-redundant but applying only the non-redundant
    // thresholds improves precision as we do not mark the redundant ones as applied here
    Set<Threshold> nonRedundant = getNonRedundantThresholds(state);
    Set<Threshold> unconsumed = getUnconsumedThresholds(nonRedundant, point);
    if (unconsumed.isEmpty())
      return P3.<D, Threshold, Set<Threshold>>tuple3(state, null, Collections.<Threshold>emptySet());
    D narrowedState = applyThresholds(unconsumed, state);
    Threshold smallestTest = getSmallestThreshold(unconsumed, narrowedState, point);
    markApplied(smallestTest, point);
    removeNonRedundantThresholds(narrowedState);
    return P3.<D, Threshold, Set<Threshold>>tuple3(narrowedState, smallestTest, unconsumed);
  }

  /**
   * Returns the threshold that is closest to the given state.
   * If there are more than one then it randomly chooses one of them.
   */
  private Threshold getSmallestThreshold (Set<Threshold> candidates, D state, Option<ProgramPoint> point) {
    assert !candidates.isEmpty();
    // find distance of every test to the state
    // find the minimal distance
    // return the tests with the minimal distance
    Multimap<BigInt, Threshold> distances = HashMultimap.create();
    BigInt minDistance = null;
    for (Threshold threshold : candidates) {
      Test test = threshold.getTest();
      assert test.getOperator() != ZenoTestOp.NotEqualToZero && test.getOperator() != ZenoTestOp.EqualToZero;
      Bound distanceOfTestToState = state.queryRange(test.getExpr()).convexHull().high();
      if (!distanceOfTestToState.isFinite()) {
        // tests with unbounded distance did not have an effect on the state thus can be marked as applied to remove them
        markApplied(threshold, point);
        continue;
      }
      // the following distance is usually negative, but positive values may occur if queryRange approximates the result
      BigInt distance = distanceOfTestToState.asInteger().abs();
      if (minDistance == null)
        minDistance = distance;
      else
        minDistance = minDistance.min(distance);
      if (distance.equals(minDistance)) // we want the minimum only, so no need to track bigger ones
        distances.put(distance, threshold);
    }
    if (distances.isEmpty()) // it can happen because of impreciseness that a test is unbounded on query but has and effect on applying it
      return candidates.iterator().next();
    else
      return distances.get(minDistance).iterator().next();
  }

  private Set<Threshold> getUnconsumedThresholds (Set<Threshold> candidates, Option<ProgramPoint> point) {
    // for termination if we do not know the program point then all the tests are consumed here
    if (point.isNone() || candidates.isEmpty())
      return Collections.emptySet();
    Set<Threshold> unconsumed = new HashSet<Threshold>();
    for (Threshold threshold : candidates) {
      if (!thresholds.isAppliedAt(threshold, point.get()))
        unconsumed.add(threshold);
    }
    return unconsumed;
  }

  private void markApplied (Threshold smallestTest, Option<ProgramPoint> point) {
    thresholds.markAppliedAt(smallestTest, point.getOrNull());
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

  /**
   * Sees if in {@code this} we have some thresholds that are marked as applied at more program points
   * than they are marked in {@code other}.
   */
  protected boolean subsetOrEqual (ThresholdsWideningStateBuilder<D> otherBuilder) {
    return thresholds.hasAppliedThresholdsNotIn(otherBuilder.thresholds);
  }

  protected void mergeThresholds (D thisChildState, D otherChildState, ThresholdsWideningStateBuilder<D> otherBuilder) {
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
      if (redundantTest != null)
        thresholds.add(threshold.withTest(redundantTest), candidates.getAppliedPoints(threshold));
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

  @Override public String toString () {
    return build().toString();
  }

}
