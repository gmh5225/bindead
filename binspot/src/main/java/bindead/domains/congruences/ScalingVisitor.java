package bindead.domains.congruences;

import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Congruence;
import javalx.persistentcollections.AVLMap;
import bindead.abstractsyntax.zeno.Zeno;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.data.Linear;
import bindead.data.Linear.Divisor;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.exceptions.Unreachable;

class ScalingVisitor extends ZenoRhsVisitorSkeleton<Rhs, CongruenceStateBuilder> {
  private static final ZenoFactory zeno = ZenoFactory.getInstance();
  private static final ScalingVisitor visitor = new ScalingVisitor();

  public static void run (Assign ass, CongruenceStateBuilder sys) {
    Rhs newRhs = ass.getRhs().accept(visitor, sys);
    sys.getChildOps().addAssignment(zeno.assign(ass.getLhs(), newRhs));
    Congruence approximation = RhsVisitor.run(ass.getRhs(), sys);
    sys.state = sys.state.bind(ass.getLhs().getId(), approximation);
    if (approximation.getScale().isGreaterThan(Bound.ONE))
      sys.getChildOps().addScaleDown(ass.getLhs().getId(), approximation);
  }

  public static void run (Test test, CongruenceStateBuilder sys) {
    // adding the test as a child op has to happen before we modify the congruence state
    Linear le = test.getExpr();
    Linear.Divisor d = Divisor.one();
    Linear res = sys.inlineIntoLinear(le, d);
    Test inlinedTest = zeno.comparison(zeno.linear(res, d.get()), test.getOperator());
    if (!ZenoTestHelper.isTautologyReportUnreachable(inlinedTest))
      sys.getChildOps().addTest(inlinedTest);

    boolean DEBUGTESTS = CongruenceProperties.INSTANCE.debugTests.isTrue();
    Linear lin = test.getExpr();
    AVLMap<NumVar, Congruence> newState = sys.state;
    boolean allReachable = true;

    if (lin.isConstantOnly()) {
      BigInt constant = lin.getConstant();
      switch (test.getOperator()) {
      case EqualToZero:
        allReachable = constant.isZero();
        break;
      case NotEqualToZero:
        allReachable = !constant.isZero();
        break;
      case LessThanOrEqualToZero:
        allReachable = !constant.isPositive();
        break;
      default:
        throw new IllegalStateException();
      }
    } else {
      switch (test.getOperator()) {
      case EqualToZero:
        for (Term term : lin) {
          NumVar var = term.getId();
          Congruence currentVariableCongruence = sys.state.get(var).get();
          Linear remaining = lin.dropTerm(var).negate(); // bring it to the right hand side
          BigInt varCoeff = term.getCoeff();
          Congruence remainingTermsCongruence = Congruences.evaluateCongruences(remaining, sys.state);
          if (remainingTermsCongruence.isConstantOnly() && !varCoeff.abs().isEqualTo(Bound.ONE))
            continue; // cannot divide constants as congruences
          remainingTermsCongruence = remainingTermsCongruence.div(varCoeff.abs());
          // division does not take sign into account
          remainingTermsCongruence = remainingTermsCongruence.mul(BigInt.of(varCoeff.sign()));
          Option<Congruence> meet = currentVariableCongruence.meet(remainingTermsCongruence);
          if (meet.isSome()) {
            Congruence newVariableCongruence = meet.get();
            if (newVariableCongruence.equals(currentVariableCongruence))
              continue; // nothing new was inferred
            newState = newState.bind(var, newVariableCongruence);
            if (newVariableCongruence.isConstantOnly()) {
              // refine the child as a wrong constant might get stored by applying the test only
              Assign assign = zeno.assign(zeno.variable(var), zeno.literal(newVariableCongruence.getOffset()));
              sys.getChildOps().addAssignment(assign);
            } else { // we need a scaling in the child as the congruence has changed
              sys.getChildOps().addScale(var, currentVariableCongruence, newVariableCongruence);
            }
          } else {
            allReachable = false;
            break;
          }
        }
        break;
      case NotEqualToZero:
        // cannot really say anything about the unsatisfiability of the test by looking only at the congruences.
        break;
      case LessThanOrEqualToZero:
        // needs to be evaluated for unsatisfiability in child domain
        break;
      default:
        throw new IllegalStateException();
      }
    }

    if (!allReachable) {
      if (DEBUGTESTS) {
        System.out.println(Congruences.NAME + ": ");
        System.out.println("  evaluating test: " + test);
        System.out.println("  before: " + sys.state);
        System.out.println("  after: <unreachable>");
      }
      throw new Unreachable();
    }
    if (DEBUGTESTS) {
      System.out.println(Congruences.NAME + ": ");
      System.out.println("  evaluating test: " + test);
      System.out.println("  before: " + sys.state);
      System.out.println("  after: " + newState);
    }
    sys.state = newState;
  }

  @Override public Rhs visit (RangeRhs expr, CongruenceStateBuilder sys) {
    return expr;
  }

  @Override public Rhs visit (Bin expr, CongruenceStateBuilder sys) {
    Rlin l = (Rlin) expr.getLeft().accept(this, sys);
    Rlin r = (Rlin) expr.getRight().accept(this, sys);
    if (l.equals(expr.getLeft()) && r.equals(expr.getRight()))
      return expr;
    return zeno.binary(l, expr.getOp(), r);
  }

  @Override public Rhs visit (Rlin expr, CongruenceStateBuilder sys) {
    Linear le = expr.getLinearTerm();
    Linear.Divisor d = new Linear.Divisor(expr.getDivisor());
    Linear res = sys.inlineIntoLinear(le, d);
    if (le.equals(res) && d.get().isEqualTo(expr.getDivisor()))
      return expr;
    return zeno.linear(res, d.get());
  }

  private static final class RhsVisitor extends ZenoRhsVisitorSkeleton<Congruence, CongruenceStateBuilder> {
    private static final RhsVisitor $ = new RhsVisitor();

    public static Congruence run (Rhs rhs, CongruenceStateBuilder congruences) {
      return rhs.accept($, congruences);
    }

    @Override public Congruence visit (Zeno.Bin stmt, final CongruenceStateBuilder congruences) {
      Congruence left = stmt.getLeft().accept(this, congruences);
      Congruence right = stmt.getRight().accept(this, congruences);
      switch (stmt.getOp()) {
      case Mul:
        return left.mul(right);
      case Div:
        // only defined division "congruence / constant", others have to be performed in child
        if (!left.isConstantOnly() && right.isConstantOnly()) {
          if (right.getOffset().isZero())
            return Congruence.ZERO; // ARM semantics allow division by zero
          return left.div(right.getOffset());
        }
        break;
      case Shl:
        if (right.isConstantOnly())
          return left.shl(right.getOffset());
        break;
      case Shr:
        if (right.isConstantOnly())
          return left.shr(right.getOffset());
        break;
      default:
      }
      return Congruence.ONE; // return * 1 + 0 congruence for non-implemented operations
    }

    /**
     * Calculate a congruence for the given variable assignments.
     */
    @Override public Congruence visit (Zeno.Rlin stmt, final CongruenceStateBuilder congruences) {
      Congruence res = Congruences.evaluateCongruences(stmt.getLinearTerm(), congruences.state);
      if (res.isConstantOnly()) {
        BigInt constant = res.getOffset();
        BigInt[] divResult = constant.divideAndRemainder(stmt.getDivisor());
        if (!divResult[1].isZero())
          throw new Unreachable();
        return new Congruence(Bound.ZERO, divResult[0]);
      }
      return res.div(stmt.getDivisor());
    }

    @Override public Congruence visit (Zeno.RangeRhs range, final CongruenceStateBuilder congruences) {
      return range.getRange().getCongruence();
    }
  }

}
