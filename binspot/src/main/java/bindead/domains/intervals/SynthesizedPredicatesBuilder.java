package bindead.domains.intervals;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.MultiMap;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.data.Linear;
import bindead.data.NumVar;

/**
 * Synthesizes predicates for the precision loss during the intervals join.
 *
 * @author Bogdan Mihaila
 */
class SynthesizedPredicatesBuilder {
  private static final ZenoFactory zeno = ZenoFactory.getInstance();

  /**
   * New predicates are generated to mitigate precision loss on join (convexity) in child domains;
   * e.g. in intervals when joining d1 : x=[0, 5]; y=[-5, -1] ⊔ d2 : x=[10, 15]; y=[2, 3] we can infer the following
   * implications during the join: 5 < x → 2 <= y; -1 < y → 10 <= x thus being able to recover
   * the precision loss in the join later on when restricting one of the variables.
   */
  public static MultiMap<Test, Test> generateImplications (AVLMap<NumVar, Interval> differingFromThis,
      AVLMap<NumVar, Interval> allFromOther) {
    MultiMap<Test, Test> implications = MultiMap.empty();
    if (differingFromThis.isEmpty())
      return implications;
    List<NumVar> predicateVariablesList = new LinkedList<NumVar>();
    AVLMap<NumVar, Test> boundsPredicatesInThis = AVLMap.empty();
    AVLMap<NumVar, Test> boundsPredicatesInOther = AVLMap.empty();
    for (NumVar variable : differingFromThis.keys()) {
      Interval valueInThis = differingFromThis.getOrNull(variable);
      Interval valueInOther = allFromOther.getOrNull(variable);
      // this can also separate overlapping intervals
      if (valueInThis.contains(valueInOther) || valueInOther.contains(valueInThis)) {
        // but it seems slower than just testing for overlap. Maybe cause we generate more implications.
//      if (valueInThis.overlaps(valueInOther)) {
        continue; // we do not generate predicates for overlapping values
      }

      // TODO: at least for non-containing/swallowing intervals we could generate predicates
      // but it would make the algorithm more complex as we do not have a lost bound predicate
      // in one of the states for such variables, thus cross correlation is more difficult
      // the condition for this would be the code below:
//      if (lostBoundsPredicatesInThis.size() != 1 && lostBoundsPredicatesInOther.size() != 1) {
//        // we want only variables where the lost bounds are on each side of the states
//        // otherwise one interval swallows the other or they overlap and we do not generate predicates for that
//        continue;
//      }
      Test lostBoundsPredicateInThis = createOutsideLostBoundsPredicates(variable, valueInThis, valueInOther);
      Test lostBoundsPredicateInOther = createInsideLostBoundsPredicates(variable, valueInOther, valueInThis);
      assert lostBoundsPredicateInThis != null;
      assert lostBoundsPredicateInOther != null;
      boundsPredicatesInThis = boundsPredicatesInThis.bind(variable, lostBoundsPredicateInThis);
      boundsPredicatesInOther = boundsPredicatesInOther.bind(variable, lostBoundsPredicateInOther);
      predicateVariablesList.add(variable);
    }
    if (predicateVariablesList.isEmpty())
      return implications;
    // perform a circular and shifted association as implications between the variables bounds
    implications =
      circularShiftedZip(implications, predicateVariablesList, boundsPredicatesInThis, boundsPredicatesInOther);
    return implications;
  }

  private static MultiMap<Test, Test> circularShiftedZip (MultiMap<Test, Test> implications,
      List<NumVar> predicateVariablesList,
      AVLMap<NumVar, Test> boundsPredicatesInThis, AVLMap<NumVar, Test> boundsPredicatesInOther) {
    ListIterator<NumVar> iterator = predicateVariablesList.listIterator();
    NumVar firstVariable = iterator.next();
    while (iterator.hasNext()) {
      NumVar currentVariable = iterator.previous();
      iterator.next(); // come back to where we were
      NumVar nextVariable = iterator.next();
      Test premise = boundsPredicatesInThis.get(currentVariable).get();
      Test consequence = boundsPredicatesInOther.get(nextVariable).get();
      // do not add tautologous implications, e.g. x < 4 → x < 5
      if (!ZenoTestHelper.isSyntacticallyEntailedBy(consequence, premise))
        implications = implications.add(premise, consequence);
    }
    NumVar lastVariable = iterator.previous();
    // now combine the last var with the first
    Test premise = boundsPredicatesInThis.get(lastVariable).get();
    Test consequence = boundsPredicatesInOther.get(firstVariable).get();
    // do not add tautologous implications, e.g. x < 4 → x < 5
    if (!ZenoTestHelper.isSyntacticallyEntailedBy(consequence, premise))
      implications = implications.add(premise, consequence);
    return implications;
  }

  /**
   * Generates predicates for the lower and upper bounds of this value if
   * the bounds are lost in the convex approximation of the join.
   * The predicates describe the space inside of the value of the predicate for the lost bounds.
   */
  private static Test createInsideLostBoundsPredicates (NumVar var, Interval valueInThis, Interval valueInOther) {
    Test predicate = createOutsideLostBoundsPredicates(var, valueInThis, valueInOther);
    return predicate.not();
  }

  /**
   * Generates predicates for the lower and upper bounds of this value if
   * the bounds are lost in the convex approximation of the join.
   * The predicates describe the space outside of the value of the predicate for the lost bounds.
   */
  private static Test createOutsideLostBoundsPredicates (NumVar var, Interval valueInThis, Interval valueInOther) {
    Test lowerBoundPredicate = createLowerBoundPredicate(var, valueInThis, valueInOther);
    if (lowerBoundPredicate != null)
      return lowerBoundPredicate;
    Test upperBoundPredicate = createUpperBoundPredicate(var, valueInThis, valueInOther);
    if (upperBoundPredicate != null)
      return upperBoundPredicate;
    return null;
  }

  /**
   * Generates a predicate of the form {@code var < l} if the value of var = [l, u] in this and through the join with
   * the other interval the lower bound {@code l} would disappear through the convex approximation.
   * Returns {@code null} if the lower bound will remain precise.
   */
  private static Test createLowerBoundPredicate (NumVar var, Interval valueInThis, Interval valueInOther) {
    Bound thisLow = valueInThis.low();
    Bound otherLow = valueInOther.low();
    if (otherLow.isLessThan(thisLow) && thisLow.isFinite()) {
      BigInt lostBound = thisLow.asInteger();
      Linear linear = Linear.linear(var).sub(lostBound).sub(BigInt.MINUSONE); // var < bound
      Test predicate = zeno.comparison(linear, ZenoTestOp.LessThanOrEqualToZero);
      return predicate;
    } else {
      return null;
    }
  }

  /**
   * Generates a predicate of the form {@code u < var} if the value of var = [l, u] in this and through the join with
   * the other interval the upper bound {@code u} would disappear through the convex approximation.
   * Returns {@code null} if the upper bound will remain precise.
   */
  private static Test createUpperBoundPredicate (NumVar var, Interval valueInThis, Interval valueInOther) {
    Bound thisHigh = valueInThis.high();
    Bound otherHigh = valueInOther.high();
    if (thisHigh.isLessThan(otherHigh) && thisHigh.isFinite()) {
      BigInt lostBound = thisHigh.asInteger();
      Linear linear = Linear.linear(lostBound).sub(var).add(BigInt.ONE); // var > bound
      Test predicate = zeno.comparison(linear, ZenoTestOp.LessThanOrEqualToZero);
      return predicate;
    } else {
      return null;
    }
  }

}
