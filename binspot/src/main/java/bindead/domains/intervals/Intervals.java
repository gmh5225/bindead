package bindead.domains.intervals;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.term;

import java.util.Iterator;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import rreil.lang.MemVar;
import rreil.lang.util.Type;
import bindead.abstractsyntax.zeno.Zeno;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.debug.DomainStringBuilder;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.ZenoHeadDomain;
import bindead.exceptions.DomainStateException.VariableSupportSetException;
import bindead.exceptions.Unreachable;

import com.jamesmurty.utils.XMLBuilder;

/**
 * A simple interval domain that tracks for every variable {@code x} an interval [l, u]
 */
public class Intervals extends ZenoHeadDomain<Intervals> {
  private final boolean DEBUGSUBSETOREQUAL = IntervalProperties.INSTANCE.debugSubsetOrEqual.isTrue();
  private final boolean DEBUGASSIGN = IntervalProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean DEBUGBINARIES = IntervalProperties.INSTANCE.debugBinaryOperations.isTrue();
  private final boolean DEBUGWIDENING = IntervalProperties.INSTANCE.debugWidening.isTrue();
  private final boolean DEBUGTESTS = IntervalProperties.INSTANCE.debugTests.isTrue();
  private final boolean DEBUGOTHER = IntervalProperties.INSTANCE.debugOther.isTrue();

  public static final String NAME = "INTERVALS";
  private final AVLMap<NumVar, Interval> intervals;
  private final SynthChannel channel;

  public Intervals () {
    this(AVLMap.<NumVar, Interval>empty(), new SynthChannel(), AnalysisCtx.unknown());
  }

  private Intervals (AVLMap<NumVar, Interval> intervals, SynthChannel channel, AnalysisCtx ctx) {
    super(NAME, ctx);
    this.intervals = intervals;
    this.channel = channel;
  }

  private Intervals build (AVLMap<NumVar, Interval> intervals, SynthChannel channel) {
    return new Intervals(intervals, channel, getContext());
  }

  private Intervals build (AVLMap<NumVar, Interval> intervals) {
    return build(intervals, new SynthChannel());
  }

  @Override public Intervals setContext (AnalysisCtx ctx) {
    return new Intervals(intervals, new SynthChannel(), ctx);
  }

  @Override public Intervals eval (Zeno.Assign stmt) {
    Interval valueApproximation = evaluate(stmt.getRhs());
    VarSet equalities = VarSet.empty();
    if (valueApproximation.isConstant()) {
//      Option<Interval> oldValue = intervals.get(stmt.getLhs().getId());
      //&& oldValue.isSome() && !oldValue.get().isEqualTo(valueApproximation)) { // XXX: would be too strict and throws away the new equalities on cumulative application of tests, assignments by parent domains
      // if executing a sequence of transfer functions which set the same value, e.g. x < 0; x = 0;
      equalities = equalities.add(stmt.getLhs().getId());
    }
    AVLMap<NumVar, Interval> newIntervals = intervals.bind(stmt.getLhs().getId(), valueApproximation);
    SynthChannel synth = new SynthChannel();
    for (NumVar var : equalities)
      synth.addEquation(linear(newIntervals.get(var).get().getConstant().negate(), term(var)).toEquality());
    if (!synth.getEquations().isEmpty())
      if (AnalysisProperties.INSTANCE.debugTests.isTrue())
        System.out.println("Synth: " + synth);
    Intervals result = build(newIntervals, synth);
    if (DEBUGASSIGN) {
      System.out.println(NAME + ":");
      System.out.println("  after " + stmt + " that is " + valueApproximation);
      System.out.println("  values: " + result);
    }
    return result;
  }

  @Override public Intervals eval (Test test) {
    if (test.getOperator().equals(Zeno.ZenoTestOp.NotEqualToZero)) {
      // special handling of disequalities because we want to generate synthesized implications
      // from the precision loss in the convex approximation. Intervals mostly cannot express disequalities.
      if (AnalysisProperties.INSTANCE.debugTests.isTrue())
        System.out.println("Intervals splits test: " + test);
      P2<Test, Test> tests = test.splitEquality();
      Intervals firstState = null;
      Intervals secondState = null;
      try {
        firstState = eval(tests._1());
      } catch (Unreachable _) {
      }
      try {
        secondState = eval(tests._2());
      } catch (Unreachable _) {
      }
      Intervals result = joinNullables(firstState, secondState);
      if (result == null)
        throw new Unreachable();
      return result;
    }

    if (AnalysisProperties.INSTANCE.debugTests.isTrue())
      System.out.println("Numeric test: " + test);
    AVLMap<NumVar, Interval> newIntervals = intervals;
    VarSet newEqualities = VarSet.empty();
    Linear expr = test.getExpr();
    boolean allReachable = true;
    if (expr.isConstantOnly()) {
      BigInt lhs = expr.getConstant();
      switch (test.getOperator()) {
      case EqualToZero:
        allReachable = lhs.isZero();
        break;
      case NotEqualToZero:
        allReachable = !lhs.isZero();
        break;
      case LessThanOrEqualToZero:
        allReachable = !lhs.isPositive();
        break;
      default:
        throw new IllegalStateException();
      }
    } else {
      for (Term term : expr) {
        Option<Interval> approxLhsOption = intervals.get(term.getId());
        if (approxLhsOption.isNone())
          throw new VariableSupportSetException();
        Interval approxLhs = approxLhsOption.get();
        Linear equation = expr.dropTerm(term.getId());
        Interval approxRhs = evaluate(equation).negate();
        Option<Interval> meet = applyMeet(test.getOperator(), term.getCoeff(), approxLhs, approxRhs);
        if (meet.isNone()) {
          allReachable = false;
          break;
        } else {
          Interval newValue = meet.get();
          if (newValue.isConstant()) {
//            Option<Interval> oldValue = intervals.get(t.getId());
            // && oldValue.isSome() && !oldValue.get().isEqualTo(newValue)) { // XXX: would be too strict and throws away the new equalities on cumulative application of tests, assignments by parent domains
            // if executing a sequence of transfer functions which set the same value, e.g. x < 0; x = 0;
            newEqualities = newEqualities.add(term.getId());
          }
          newIntervals = newIntervals.bind(term.getId(), newValue);
        }
      }
    }

    if (!allReachable) {
      if (DEBUGTESTS) {
        System.out.println(NAME + ": ");
        System.out.println("  evaluating test: " + test);
        System.out.println("  before: " + this);
        System.out.println("  after: unreachable");
      }
      throw new Unreachable();
    }
    SynthChannel synth = new SynthChannel();
    for (NumVar var : newEqualities) {
      synth.addEquation(linear(newIntervals.get(var).get().getConstant().negate(), term(var)).toEquality());
    }
    if (!synth.getEquations().isEmpty())
      if (AnalysisProperties.INSTANCE.debugTests.isTrue())
        System.out.println("Synth: " + synth);
    Intervals result = build(newIntervals, synth);
    if (DEBUGTESTS) {
      System.out.println(NAME + ": ");
      System.out.println("  evaluating test: " + test);
      System.out.println("  before: " + this);
      System.out.println("  after: " + result);
    }
    return result;
  }

  private Interval evaluate (Rhs rhs) {
    return rhs.accept(RhsEvaluator.instance, this);
  }

  /**
   * Calculate an interval for the given variable assignments of the linear expression.
   */
  private Interval evaluate (Linear linear) {
    Interval res = Interval.ZERO;
    for (Term term : linear) { // iterate only over the non-constant terms
      Interval value = intervals.get(term.getId()).getOrElse(Interval.TOP).mul(term.getCoeff());
      res = res.add(value);
    }
    res = res.add(linear.getConstant());
    return res;
  }

  private static Option<Interval> applyMeet (ZenoTestOp testOp, BigInt lhsCoeff, Interval lhs, Interval rhs) {
    switch (testOp) {
    case EqualToZero: // c*lhs == rhs
      rhs = rhs.divRoundInvards(lhsCoeff);
      if (rhs == null) // no integral solutions for lhs
        return Option.none();
      else
        return lhs.meet(rhs);
    case NotEqualToZero: // c*lhs != rhs
      // split disequality c*a != b into c*a <= b - 1 and -c*a <= -b - 1 and then do a join of the results
      assert false : "Disequalities should be split up on test application in eval(Test)";
      Option<Interval> leftPart =
        applyMeet(ZenoTestOp.LessThanOrEqualToZero, lhsCoeff, lhs, rhs.sub(Bound.ONE));
      Option<Interval> rightPart =
        applyMeet(ZenoTestOp.LessThanOrEqualToZero, lhsCoeff.negate(), lhs, rhs.negate().sub(Bound.ONE));
      return join(leftPart, rightPart);
    case LessThanOrEqualToZero: // c*lhs <= rhs
      rhs = Interval.downFrom(rhs.high()).divRoundInvards(lhsCoeff);
      return lhs.meet(rhs);
    default:
      throw new IllegalStateException();
    }
  }

  private static Option<Interval> join (Option<Interval> first, Option<Interval> second) {
    if (first.isSome() && second.isSome())
      return Option.some(first.get().join(second.get()));
    if (first.isNone() && second.isNone())
      return Option.none();
    if (first.isSome())
      return first;
    else
      return second;
  }

  @Override public Intervals join (Intervals other) {
    AVLMap<NumVar, Interval> differing = inBothButDifferingOnly(other);
    SynthChannel synth = new SynthChannel();
    synth.setImplications(SynthesizedPredicatesBuilder.generateImplications(differing, other.intervals));
    AVLMap<NumVar, Interval> joined = AVLMap.empty();
    for (P2<NumVar, Interval> inThis : differing) {
      NumVar var = inThis._1();
      Interval thisValue = inThis._2();
      Interval otherValue = other.intervals.get(var).get();
      Interval resultValue = thisValue.join(otherValue);
      joined = joined.bind(var, resultValue);
    }
    Intervals result = build(joined.union(intervals), synth);

    if (DEBUGBINARIES) {
      System.out.println(NAME + ":");
      System.out.println("  differing:");
      System.out.println("    from fst: " + build(intervals.intersection(differing)));
      System.out.println("    from snd: " + build(other.intervals.intersection(differing)));
      System.out.println("  join: " + result);
    }
    return result;
  }

  @Override public Intervals widen (Intervals other) {
    AVLMap<NumVar, Interval> differing = inBothButDifferingOnly(other);
    SynthChannel synth = new SynthChannel();
    synth.setImplications(SynthesizedPredicatesBuilder.generateImplications(differing, other.intervals));
    AVLMap<NumVar, Interval> widened = AVLMap.empty();
    for (P2<NumVar, Interval> inThis : differing) {
      NumVar var = inThis._1();
      Interval thisValue = inThis._2();
      Interval otherValue = other.intervals.get(var).get();
      Interval resultValue;
      if (var.isFlag()) {
        // flags are boolean variables thus not widening them here removes the need to later wrap them to size 1.
        // as wrapping destroys affine relations this improves the precision.
        // especially points-to flags rely on not widening the flags.
        resultValue = thisValue.join(otherValue);
        // the widening is suppressed only for value that are in the boolean range [0, 1]
        if (!resultValue.subsetOrEqual(Interval.BOOLEANTOP))
          resultValue = thisValue.widen(otherValue);
      } else {
        resultValue = thisValue.widen(otherValue);
      }
      widened = widened.bind(var, resultValue);
    }
    Intervals result = build(widened.union(intervals), synth);
    if (DEBUGWIDENING || DEBUGBINARIES) {
      System.out.println(NAME + ":");
      System.out.println("  differing:");
      System.out.println("    values in 1st: " + build(intervals.intersection(differing)));
      System.out.println("    values in 2nd: " + build(other.intervals.intersection(differing)));
      System.out.println("  widened values: " + build(result.intervals.intersection(differing)));
    }
    return result;
  }


  private AVLMap<NumVar, Interval> inBothButDifferingOnly (Intervals other) {
    ThreeWaySplit<AVLMap<NumVar, Interval>> split = intervals.split(other.intervals);
    if (!split.onlyInFirst().isEmpty())
      throw new VariableSupportSetException();
    if (!split.onlyInSecond().isEmpty())
      throw new VariableSupportSetException();
    return split.inBothButDiffering();
  }

  @Override public boolean subsetOrEqual (Intervals other) {
    AVLMap<NumVar, Interval> split = inBothButDifferingOnly(other);
    if (DEBUGSUBSETOREQUAL) {
      System.out.println(NAME + ":");
      System.out.println(" subset-or-equal:");
      System.out.println("  differing:");
      System.out.println("    from fst: " + build(intervals.intersection(split)));
      System.out.println("    from snd: " + build(other.intervals.intersection(split)));
    }
    for (P2<NumVar, Interval> entry : split) {
      NumVar variable = entry._1();
      Interval a = entry._2();
      Interval b = other.intervals.get(variable).get();
      if (!a.subsetOrEqual(b))
        return false;
    }
    return true;
  }

  @Override public Intervals introduce (NumVar variable, Type type, Option<BigInt> value) {
    AVLMap<NumVar, Interval> updatedIntervals = intervals;
    Interval initial = Interval.top();
    switch (type) {
    case Bool:
      initial = Interval.BOOLEANTOP;
      break;
    case Zeno:
      initial = Interval.TOP;
      break;
    case Address:
      initial = Interval.GREATER_THAN_OR_EQUAL_TO_ZERO;
      break;
    }
    if (value.isSome())
      initial = Interval.of(value.get());
    updatedIntervals = updatedIntervals.bind(variable, initial);
    return build(updatedIntervals);
  }

  @Override public Intervals project (VarSet vars) {
    AVLMap<NumVar, Interval> removed = intervals;
    for (NumVar var : vars)
      removed = removed.remove(var);
    Intervals result = build(removed);
    if (DEBUGOTHER) {
      System.out.println(NAME + ":");
      System.out.println("  project: " + vars);
      System.out.println("  values: " + result);
    }
    return result;
  }

  @Override public Intervals substitute (NumVar x, NumVar y) {
    AVLMap<NumVar, Interval> updatedIntervals = intervals;
    Option<Interval> valueOfXOption = updatedIntervals.get(x);
    if (valueOfXOption.isNone())
      throw new VariableSupportSetException();
    Interval valueOfX = valueOfXOption.get();
    updatedIntervals = updatedIntervals.remove(x);
    updatedIntervals = updatedIntervals.bind(y, valueOfX);
    return build(updatedIntervals);
  }

  @Override public Intervals expand (FoldMap vars) {
    AVLMap<NumVar, Interval> updatedIntervals = intervals;
    for (VarPair vp : vars) {
      Option<Interval> valueOfPermanentOption = updatedIntervals.get(vp.getPermanent());
      if (valueOfPermanentOption.isNone())
        throw new VariableSupportSetException();
      Interval valueOfPermanent = valueOfPermanentOption.get();
      updatedIntervals = updatedIntervals.bind(vp.getEphemeral(), valueOfPermanent);
    }
    return build(updatedIntervals);
  }

  @Override public Intervals fold (FoldMap vars) {
    AVLMap<NumVar, Interval> updatedIntervals = intervals;
    for (VarPair vp : vars) {
      Option<Interval> valueOfPermanentOption = updatedIntervals.get(vp.getPermanent());
      if (valueOfPermanentOption.isNone())
        throw new VariableSupportSetException();
      Interval valueOfPermanent = valueOfPermanentOption.get();
      Option<Interval> valueOfEphemeralOption = updatedIntervals.get(vp.getEphemeral());
      if (valueOfEphemeralOption.isNone())
        throw new VariableSupportSetException();
      Interval valueOfEphemeral = valueOfEphemeralOption.get();
      updatedIntervals = updatedIntervals.remove(vp.getEphemeral());
      updatedIntervals = updatedIntervals.bind(vp.getPermanent(), valueOfPermanent.join(valueOfEphemeral));
    }
    return build(updatedIntervals);
  }

  @Override public Intervals copyAndPaste (VarSet vars, Intervals from) {
    AVLMap<NumVar, Interval> updatedIntervals = intervals;
    for (NumVar var : vars) {
      Interval value = from.intervals.get(var).getOrNull();
      if (value == null)
        throw new VariableSupportSetException();
      updatedIntervals = updatedIntervals.bind(var, value);
    }
    return build(updatedIntervals);
  }


  @Override public Range queryRange (Linear lin) {
    Range result = Range.from(evaluate(lin));
    if (DEBUGOTHER) {
      System.out.println(NAME + ":");
      System.out.println("  query: " + lin);
      System.out.println("  values: " + this);
      System.out.println("  result: " + result);
    }
    return result;
  }

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    return SetOfEquations.empty();
  }

  @Override public SynthChannel getSynthChannel () {
    return channel;
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    Interval interval = intervals.get(var).get();
    if (interval.isConstant())
      builder.append(interval.toString());
    else
      builder.append(var.toString());
  }

  @Override public void toCompactString (StringBuilder builder) {
    builder.append(NAME + ": #" + intervals.size() + " ");
    builder.append("{");
    for (P2<NumVar, Interval> i : intervals) {
      NumVar var = i._1();
      Interval interval = i._2();
      if (interval.isConstant()) // shown in inlined value
        continue;
      if (interval.isTop()) // don't show
        continue;
      builder.append(var + "=");
      interval.toStringDotted(builder);
      builder.append(", ");
    }
    builder.setLength(builder.length() - 2);
    builder.append("}");
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(Intervals.NAME);
    for (P2<NumVar, Interval> varAndValue : intervals) {
      Interval value = varAndValue._2();
      builder = builder.e("Entry")
          .a("type", "interval")
          .e("Variable")
          .t(varAndValue._1().toString())
          .up()
          .e("Value")
          .e("lowerBound")
          .t(value.low().toString())
          .up()
          .e("upperBound")
          .t(value.high().toString())
          .up()
          .up()
          .up();
    }
    return builder.up();
  }

  @Override public void toString (DomainStringBuilder builder) {
    builder.append(NAME, toString());
  }

  @Override public String toString () {
    return NAME + ": #" + intervals.size() + " " + toLexicallySortedString();
  }

  private String toLexicallySortedString () {
    StringBuilder builder = new StringBuilder();
    Set<NumVar> sorted = StringHelpers.sortLexically(intervals.keys());
    Iterator<NumVar> iterator = sorted.iterator();
    builder.append('{');
    while (iterator.hasNext()) {
      NumVar key = iterator.next();
      Interval value = intervals.getOrNull(key);
      builder.append(key);
      builder.append('=');
      builder.append(value);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  private final static class RhsEvaluator extends ZenoRhsVisitorSkeleton<Interval, Intervals> {
    public static RhsEvaluator instance = new RhsEvaluator();

    @Override public Interval visit (Zeno.Bin stmt, Intervals intervals) {
      Interval left = stmt.getLeft().accept(this, intervals);
      Interval right = stmt.getRight().accept(this, intervals);
      if (left.isConstant() && right.isConstant())
        return Interval.of(applyToConstants(stmt.getOp(), left.getConstant(), right.getConstant()));
      Interval approximation;
      switch (stmt.getOp()) {
      case Mul:
        approximation = left.mul(right);
        break;
      case Div:
        approximation = left.divRoundZero(right);
        break;
      case Shl:
        approximation = left.shl(right);
        break;
      case Shr:
        approximation = left.shr(right);
        break;
      case Mod:
        approximation = Interval.top();
        break;
      default:
        approximation = Interval.top();
        break;
      }
      return approximation;
    }

    private static BigInt applyToConstants (Zeno.ZenoBinOp op, BigInt left, BigInt right) {
      switch (op) {
      case Div:
        if (right.isZero()) // ARM semantics
          return Bound.ZERO;
        return left.divRoundZero(right);
      case Mod:
        return left.mod(right);
      case Mul:
        return left.mul(right);
      case Shl:
        return left.shl(right);
      case Shr:
        return left.shr(right);
      default:
        throw new IllegalArgumentException();
      }
    }

    /**
     * Calculate an interval for the given variable assignments. Can return {@code null} if the division of the result
     * by the divisor of the Rlin does not contain integral solutions.
     */
    @Override public Interval visit (Zeno.Rlin stmt, Intervals intervals) {
      Interval res = intervals.evaluate(stmt.getLinearTerm());
      return res.divRoundInvards(stmt.getDivisor());
    }

    @Override public Interval visit (Zeno.RangeRhs range, Intervals intervals) {
      return range.getRange().convexHull();
    }
  }


  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // TODO implement in PrettyDomain
    throw new UnimplementedException();
  }
}
