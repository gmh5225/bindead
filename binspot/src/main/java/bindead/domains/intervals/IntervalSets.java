package bindead.domains.intervals;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.numeric.IntervalSet;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import rreil.lang.util.Type;
import bindead.abstractsyntax.zeno.Zeno;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.debug.DomainStringBuilder;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.ZenoHeadDomain;
import bindead.exceptions.DomainStateException.VariableSupportSetException;
import bindead.exceptions.Unreachable;

import com.jamesmurty.utils.XMLBuilder;

public class IntervalSets extends ZenoHeadDomain<IntervalSets> {
  private static final boolean DEBUGSUBSETOREQUAL = IntervalProperties.INSTANCE.debugSubsetOrEqual.isTrue();
  private static final boolean DEBUGASSIGN = IntervalProperties.INSTANCE.debugAssignments.isTrue();
  private static final boolean DEBUGBINARIES = IntervalProperties.INSTANCE.debugBinaryOperations.isTrue();
  private static final boolean DEBUGTESTS = IntervalProperties.INSTANCE.debugTests.isTrue();
  private static final boolean DEBUGOTHER = IntervalProperties.INSTANCE.debugOther.isTrue();

  public static final String NAME = "INTERVALSETS";

  private final AVLMap<NumVar, IntervalSet> intervalSets;
  private final VarSet equalities;


  public IntervalSets () {
    super(NAME, AnalysisCtx.unknown());
    this.intervalSets = AVLMap.<NumVar, IntervalSet>empty();
    this.equalities = VarSet.empty();
  }

  private IntervalSets (AVLMap<NumVar, IntervalSet> intervalSets, VarSet equalities, AnalysisCtx ctx) {
    super(NAME, ctx);
    this.intervalSets = intervalSets;
    this.equalities = equalities;
  }

  @Override public IntervalSets setContext (AnalysisCtx ctx) {
    return new IntervalSets(intervalSets, VarSet.empty(), ctx);
  }

  @Override public IntervalSets eval (Assign stmt) {
    final IntervalSet approx = evaluate(stmt.getRhs());
    VarSet equalities = VarSet.empty();
    if (approx.isConstant()) {
      equalities = equalities.add(stmt.getLhs().getId());
    }
    final AVLMap<NumVar, IntervalSet> newIntervalSets = intervalSets.bind(stmt.getLhs().getId(), approx);
    final IntervalSets result = build(newIntervalSets, equalities);
    if (DEBUGASSIGN) {
      System.out.println(NAME + ":");
      System.out.println("  after " + stmt + " that is " + approx);
      System.out.println("  values: " + result);
    }
    return result;
  }


  @Override public IntervalSets eval (Zeno.Test test) {
    AVLMap<NumVar, IntervalSet> current = intervalSets;
    VarSet newEqualities = VarSet.empty();
    final Linear expr = test.getExpr();
    boolean allReachable = true;
    if (expr.isConstantOnly()) {
      final BigInt lhs = expr.getConstant();
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
      for (Term t : expr) {
        final Option<IntervalSet> approxLhsOption = intervalSets.get(t.getId());
        if (approxLhsOption.isNone())
          throw new VariableSupportSetException();
        final IntervalSet approxLhs = approxLhsOption.get();
        final Linear equation = expr.dropTerm(t.getId());
        final IntervalSet approxRhs = evaluate(equation).negate();
        final Option<IntervalSet> meet = applyMeet(test.getOperator(), t.getCoeff(), approxLhs, approxRhs);
        if (meet.isNone())
          allReachable = false;
        else {
          current = current.bind(t.getId(), meet.get());
          if (meet.get().isConstant())
            newEqualities = newEqualities.add(t.getId());
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
    IntervalSets result = build(current, newEqualities);
    if (DEBUGTESTS) {
      System.out.println(NAME + ": ");
      System.out.println("  evaluating test: " + test);
      System.out.println("  before: " + this);
      System.out.println("  after: " + result);
    }
    return result;
  }

  private static Option<IntervalSet> applyMeet (ZenoTestOp testOp, BigInt lhsCoeff, IntervalSet lhs,
      IntervalSet rhs) {
    switch (testOp) {
    case EqualToZero: // c*lhs == rhs
      rhs = rhs.divRoundInvards(lhsCoeff);
      return lhs.meet(rhs);
    case NotEqualToZero: // c*lhs != rhs
      // split disequality c*a != b into c*a <= b - 1 and -c*a <= -b - 1 and then do a join of the results
      final Option<IntervalSet> leftPart =
        applyMeet(ZenoTestOp.LessThanOrEqualToZero, lhsCoeff, lhs, rhs.sub(Bound.ONE));
      final Option<IntervalSet> rightPart =
        applyMeet(ZenoTestOp.LessThanOrEqualToZero, lhsCoeff.negate(), lhs, rhs.negate().sub(Bound.ONE));
      return join(leftPart, rightPart);
    case LessThanOrEqualToZero: // c*lhs <= rhs
      rhs = new IntervalSet(Interval.downFrom(rhs.high()).divRoundInvards(lhsCoeff));
      return lhs.meet(rhs);
    default:
      throw new IllegalStateException();
    }
  }

  private static Option<IntervalSet> join (Option<IntervalSet> first, Option<IntervalSet> second) {
    if (first.isSome() && second.isSome())
      return Option.some(first.get().join(second.get()));
    if (first.isNone() && second.isNone())
      return Option.none();
    if (first.isSome())
      return first;
    else
      return second;
  }


  @Override public IntervalSets join (IntervalSets other) {
    final ThreeWaySplit<AVLMap<NumVar, IntervalSet>> split = split(other);
    AVLMap<NumVar, IntervalSet> current = other.intervalSets.intersection(IntervalSet.join,
        split.inBothButDiffering());
    current = current.union(intervalSets);
    final IntervalSets result = build(current);
    if (DEBUGBINARIES) {
      System.out.println(NAME + ":");
      System.out.println("  differing:");
      System.out.println("    from fst: " + build(intervalSets.intersection(split.inBothButDiffering())));
      System.out.println("    from snd: " + build(other.intervalSets.intersection(split.inBothButDiffering())));
      System.out.println("  join: " + result);
    }
    return result;
  }


  @Override public IntervalSets widen (IntervalSets other) {
    AVLMap<NumVar, IntervalSet> differing = split(other).inBothButDiffering();
    AVLMap<NumVar, IntervalSet> widened = AVLMap.empty();
    for (P2<NumVar, IntervalSet> inThis : differing) {
      NumVar var = inThis._1();
      IntervalSet thisValue = inThis._2();
      IntervalSet otherValue = other.intervalSets.get(var).get();
      IntervalSet resultValue;
      if (var.isFlag())
        // flags are boolean variables thus not widening them here removes the need to later wrap them to size 1.
        // as wrapping destroys affine relations this improves the precision.
        resultValue = thisValue.join(otherValue);
      else
        resultValue = thisValue.widen(otherValue);
      widened = widened.bind(var, resultValue);
    }
    IntervalSets result = build(widened.union(intervalSets));
    if (DEBUGBINARIES) {
      System.out.println(NAME + ":");
      System.out.println("  differing:");
      System.out.println("    from fst: " + build(intervalSets.intersection(differing)));
      System.out.println("    from snd: " + build(other.intervalSets.intersection(differing)));
      System.out.println("  widen: " + result);
    }
    return result;
  }

  private ThreeWaySplit<AVLMap<NumVar, IntervalSet>> split (IntervalSets other) {
    ThreeWaySplit<AVLMap<NumVar, IntervalSet>> split = intervalSets.split(other.intervalSets);
    if (!split.onlyInFirst().isEmpty())
      throw new VariableSupportSetException();
    if (!split.onlyInSecond().isEmpty())
      throw new VariableSupportSetException();
    return split;
  }


  @Override public boolean subsetOrEqual (IntervalSets other) {
    final ThreeWaySplit<AVLMap<NumVar, IntervalSet>> split = split(other);
    if (DEBUGSUBSETOREQUAL) {
      System.out.println(NAME + ":");
      System.out.println(" subset-or-equal:");
      System.out.println("  differing:");
      System.out.println("    from fst: " + build(intervalSets.intersection(split.inBothButDiffering())));
      System.out.println("    from snd: " + build(other.intervalSets.intersection(split.inBothButDiffering())));
    }
    for (P2<NumVar, IntervalSet> entry : split.inBothButDiffering()) {
      NumVar variable = entry._1();
      IntervalSet a = entry._2();
      IntervalSet b = other.intervalSets.get(variable).get();
      if (!a.subsetOrEqual(b)) {
        return false;
      }
    }
    return true;
  }


  @Override public IntervalSets introduce (NumVar variable, Type type, Option<BigInt> value) {
    AVLMap<NumVar, IntervalSet> updatedIntervalSets = intervalSets;
    IntervalSet initial = IntervalSet.TOP;
    switch (type) {
    case Bool:
      initial = IntervalSet.BOOLEANTOP;
      break;

    case Zeno:
      initial = IntervalSet.TOP;
      break;

    case Address:
      initial = IntervalSet.GREATER_THAN_OR_EQUAL_TO_ZERO;
      break;
    }

    if (value.isSome()) {
      initial = IntervalSet.valueOf(value.get());
    }
    updatedIntervalSets = updatedIntervalSets.bind(variable, initial);
    return build(updatedIntervalSets);
  }


  @Override public IntervalSets project (VarSet vars) {
    AVLMap<NumVar, IntervalSet> result = intervalSets;
    for (NumVar variable : vars)
      result = result.remove(variable);
    if (DEBUGOTHER) {
      System.out.println(NAME + ":");
      System.out.println("  project: " + vars);
      System.out.println("  values: " + result);
    }
    return build(result);
  }


  @Override public IntervalSets substitute (NumVar x, NumVar y) {
    AVLMap<NumVar, IntervalSet> updatedIntervalSets = intervalSets;
    final Option<IntervalSet> valueOfXOption = updatedIntervalSets.get(x);
    if (valueOfXOption.isNone()) {
      throw new VariableSupportSetException();
    }
    final IntervalSet valueOfX = valueOfXOption.get();
    updatedIntervalSets = updatedIntervalSets.remove(x);
    updatedIntervalSets = updatedIntervalSets.bind(y, valueOfX);
    return build(updatedIntervalSets);
  }


  @Override public IntervalSets expand (FoldMap vars) {
    AVLMap<NumVar, IntervalSet> updatedIntervals = intervalSets;
    for (VarPair vp : vars) {
      final Option<IntervalSet> valueOfPermanentOption = updatedIntervals.get(vp.getPermanent());
      if (valueOfPermanentOption.isNone()) {
        throw new VariableSupportSetException();
      }
      final IntervalSet valueOfPermanent = valueOfPermanentOption.get();
      updatedIntervals = updatedIntervals.bind(vp.getEphemeral(), valueOfPermanent);
    }
    return build(updatedIntervals);
  }


  @Override public IntervalSets fold (FoldMap vars) {
    AVLMap<NumVar, IntervalSet> updatedIntervals = intervalSets;
    for (VarPair vp : vars) {
      final Option<IntervalSet> valueOfPermanentOption = updatedIntervals.get(vp.getPermanent());
      if (valueOfPermanentOption.isNone()) {
        throw new VariableSupportSetException();
      }
      final IntervalSet valueOfPermanent = valueOfPermanentOption.get();
      final Option<IntervalSet> valueOfEphemeralOption = updatedIntervals.get(vp.getEphemeral());
      if (valueOfEphemeralOption.isNone()) {
        throw new VariableSupportSetException();
      }
      final IntervalSet valueOfEphemeral = valueOfEphemeralOption.get();
      updatedIntervals = updatedIntervals.remove(vp.getEphemeral());
      updatedIntervals = updatedIntervals.bind(vp.getPermanent(), valueOfPermanent.join(valueOfEphemeral));
    }
    return build(updatedIntervals);
  }


  @Override public IntervalSets copyAndPaste (VarSet vars, IntervalSets from) {
    AVLMap<NumVar, IntervalSet> updatedIntervals = intervalSets;
    for (NumVar var : vars) {
      final IntervalSet value = from.intervalSets.get(var).getOrNull();
      if (value == null) {
        throw new VariableSupportSetException();
      }
      updatedIntervals = updatedIntervals.bind(var, value);
    }
    return build(updatedIntervals);
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(NAME);
    for (P2<NumVar, IntervalSet> idAndIntervalSet : intervalSets) {
      IntervalSet value = idAndIntervalSet._2();
      builder = builder.e("Entry")
          .a("type", "intervalSet")
          .e("Variable")
          .t(idAndIntervalSet._1().toString())
          .up();
      for (Interval interval : value) {
        builder = builder.e("Value")
            .e("lowerBound")
            .t(interval.low().toString())
            .up()
            .e("upperBound")
            .t(interval.high().toString())
            .up();
        builder = builder.up();
      }

      builder = builder.up();

    }
    builder = builder.up();
    return builder;
  }

  @Override public void toString (DomainStringBuilder builder) {
    builder.append(NAME, toString());
  }


  @Override public Range queryRange (Linear lin) {
    final IntervalSet intervalSet = evaluate(lin);
    Range result = Range.from(intervalSet);
    if (DEBUGOTHER) {
      System.out.println(NAME + ":");
      System.out.println("  query: " + lin);
      System.out.println("  values: " + this);
      System.out.println("  result: " + result);
    }
    return result;
  }


  @Override public SynthChannel getSynthChannel () {
    final SynthChannel syn = new SynthChannel();
    for (NumVar var : equalities) {
      syn.addEquation(Linear.linear(intervalSets.get(var).get().getConstantOrNull().negate(), Linear.term(var))
          .toEquality());
    }
    return syn;
  }

  private IntervalSets build (AVLMap<NumVar, IntervalSet> intervals, VarSet equalities) {
    return new IntervalSets(intervals, equalities, getContext());
  }

  private IntervalSets build (AVLMap<NumVar, IntervalSet> intervals) {
    return build(intervals, VarSet.empty());
  }

  @Override public String toString () {
    return NAME + ": #" + intervalSets.size() + " " + intervalSets.toString();
  }

  private IntervalSet evaluate (Rhs rhs) {
    return rhs.accept(RhsEvaluator.INSTANCE, this);
  }

  /**
   * Calculate an interval for the given variable assignments of the linear expression.
   */
  private IntervalSet evaluate (Linear linear) {
    IntervalSet res = IntervalSet.ZERO;
    for (Term term : linear) { // iterate only over the non-constant terms
      IntervalSet value = intervalSets.get(term.getId()).getOrElse(IntervalSet.TOP).mul(term.getCoeff());
      res = res.add(value);
    }
    res = res.add(linear.getConstant());
    return res;
  }

  private static final class RhsEvaluator extends ZenoRhsVisitorSkeleton<IntervalSet, IntervalSets> {
    private static final RhsEvaluator INSTANCE = new RhsEvaluator();

    @Override public IntervalSet visit (Zeno.Bin stmt, IntervalSets intervals) {
      IntervalSet left = stmt.getLeft().accept(this, intervals);
      IntervalSet right = stmt.getRight().accept(this, intervals);
      if (left.isConstant() && right.isConstant())
        return IntervalSet.valueOf(applyToConstants(stmt.getOp(), left.getConstantOrNull(), right.getConstantOrNull()));
      IntervalSet approximation;
      switch (stmt.getOp()) {
      case Mul:
        approximation = left.mul(right);
        break;
      case Div:
        approximation = left.divRoundZero(right);
        break;
      case Shl:
        approximation = left.shl(right);
        break; // XXX hsi: this break statement was missing.
      case Shr:
        approximation = left.shr(right);
        break;
      case Mod:
        approximation = IntervalSet.TOP;
        break;
      default:
        approximation = IntervalSet.TOP;
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
     * Calculate an interval set for the given variable assignments.
     */
    @Override public IntervalSet visit (Zeno.Rlin stmt, IntervalSets intervals) {
      IntervalSet res = intervals.evaluate(stmt.getLinearTerm());
      return res.divRoundInvards(stmt.getDivisor());
    }

    @Override public IntervalSet visit (Zeno.RangeRhs zenoRange, IntervalSets intervals) {
      Range range = zenoRange.getRange();
      return range.asSet();
    }
  }

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    return SetOfEquations.empty();
  }
}
