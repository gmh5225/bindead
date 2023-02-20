/**
 *
 */
package bindead.domains.gauge;


import static bindead.data.Linear.linear;
import static bindead.data.Linear.mulAdd;
import static bindead.data.Linear.term;
import static javalx.data.Option.none;
import static javalx.data.Option.some;
import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.ZenoStateBuilder;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Substitution;
import bindead.exceptions.DomainStateException.VariableSupportSetException;

/**
 * @author sgreben;
 */
class GaugeStateBuilder extends ZenoStateBuilder {
  private final MutableGaugeStateData data;

  public GaugeStateBuilder (MutableGaugeStateData data) {
    this.data = data;
  }

  public GaugeState build () {
    return new GaugeState(data.getImmutableCopy());
  }

  private <D extends ZenoDomain<D>> void joinCounters (D thisChild, GaugeStateBuilder other) {
    VarSet joinedCandidates = (data.getCandidates().intersection(other.data.getCandidates()));
    VarSet joinedCounters =
      (data.getCandidates().union(data.getCounters())).intersection(other.data.getCounters())
          .union
          (other.data.getCandidates().union(other.data.getCounters())).intersection(data.getCounters());
    for (NumVar lambda : data.getCounters().difference(joinedCounters))
      forgetCounter(lambda, thisChild.queryRange(lambda));
    // for(NumVar newCounter:joinedCounters.delete(data.getCounters()))
    // System.out.println("[JOIN] NEW COUNTER: "+newCounter);
    joinedCandidates = joinedCandidates.difference(joinedCounters);
    data.setCandidates(joinedCandidates);
    data.setCounters(joinedCounters);
  }

  private <D extends ZenoDomain<D>> void inferCounters (D thisChild, GaugeStateBuilder other) {
    VarSet inferredCounters =
      (data.getCandidates().intersection(other.data.getCandidates()))
          .union
          (other.data.getCounters().union(data.getCounters()));
    // for(NumVar newCounter:inferredCounters.delete(data.getCounters()))
    // System.out.println("[INFER] NEW COUNTER: "+newCounter);
    data.setCandidates(VarSet.empty());
    data.setCounters(inferredCounters);
  }

  private void forgetCounter (NumVar lambda, Range lambda_range) {
    for (NumVar x : data.getOccurences(lambda)) {
      data.setWedge(x, data.getWedge(x).forgetCounter(lambda, lambda_range));
    }
  }

  private void deleteCounter (NumVar lambda, Range lambda_range) {
    forgetCounter(lambda, lambda_range);
    data.setCounters(data.getCounters().remove(lambda));
  }

  public <D extends ZenoDomain<D>> void join (D thisChild, GaugeStateBuilder other) {
    // Join candidates and counters:
    joinCounters(thisChild, other);

    // Join wedges:
    ThreeWaySplit<AVLMap<NumVar, Wedge>> diff = data.getWedges().split(other.data.getWedges());

    AVLMap<NumVar, Wedge> onlyInThis = diff.onlyInFirst();
    AVLMap<NumVar, Wedge> onlyInOther = diff.onlyInSecond();
    AVLMap<NumVar, Wedge> differentValues = diff.inBothButDiffering();

    for (P2<NumVar, Wedge> entry : onlyInOther)
      data.setWedge(entry._1(), Wedge.FULL);
    for (P2<NumVar, Wedge> entry : onlyInThis)
      data.setWedge(entry._1(), Wedge.FULL);
    for (P2<NumVar, Wedge> entry : differentValues)
      data.setWedge(entry._1(), entry._2().join(other.data.getWedge(entry._1())));
  }

  private Option<BigInt> toZT (Range range) {
    if (range.isConstant())
      return some(range.getConstantOrNull());
    else
      return none();
  }

  private Wedge widenLI (Wedge left, Wedge right, NumVar lambda, Range left_range, Range right_range) {
    return left.widenLI(right, lambda, toZT(left_range), toZT(right_range));
  }


  public <D extends ZenoDomain<D>> void widen (D thisChild, GaugeStateBuilder other, D otherChild) {
    // Join candidates and counters:
    // joinCounters(thisChild, other);
    inferCounters(thisChild, other);

    // Perform wedge widening:
    VarSet Lambda = data.getCounters().union(other.data.getCounters());
    VarSet ChangedCounters = Lambda;
    VarSet Domain = data.getWedgesDomain().union(other.data.getWedgesDomain());
    for (NumVar lambda : Lambda) {
      if (toZT(thisChild.queryRange(lambda)).equals(toZT(otherChild.queryRange(lambda))))
        ChangedCounters = ChangedCounters.remove(lambda);
    }

    if (ChangedCounters.isEmpty()) {
      for (NumVar x : Domain)
        data.setWedge(x, data.getWedge(x).intervalWiden(other.data.getWedge(x)));
    } else {
      for (NumVar lambda : ChangedCounters) {
        Range u_range = thisChild.queryRange(lambda);
        Range v_range = otherChild.queryRange(lambda);
        // System.out.println("WIDEN: "+u_range+" -> "+v_range);
        for (NumVar x : Domain.remove(lambda))
          data.setWedge(x, widenLI(data.getWedge(x), other.data.getWedge(x), lambda, u_range, v_range));
      }
      // System.out.println("Post-interpolation: "+data);
      VarSet UnchangedCounters = Lambda.difference(ChangedCounters);
      for (NumVar x : Domain)
        if (!data.getWedge(x).getVars().intersection(UnchangedCounters).isEmpty())
          data.setWedge(x, data.getWedge(x).partialJoin(other.data.getWedge(x), UnchangedCounters));
    }
    // System.out.println("Post-widening: "+data);
  }


  private static Option<BigInt> getIncrementOf (NumVar c, Rhs rhs) {
    if (!(rhs instanceof Rlin))
      return none();
    Linear lin = ((Rlin) rhs).getLinearTerm();
    VarSet linVars = lin.getVars();
    if (linVars.size() == 1 &&
      linVars.contains(c) &&
      !lin.getConstant().isNegative() &&
      lin.getCoeff(c).isEqualTo(Bound.ONE))
      return some(lin.getConstant());
    else
      return none();
  }

  /**
   * Assign to a counter.
   * If the assignment is syntactically an increment operation ( x := x + k ),
   * perform counter increments. Otherwise, remove the counter.
   */
  public <D extends ZenoDomain<D>> void assignCounter (D childState, NumVar assignedToVar, Rhs rhs, Wedge rhsValue) {
    assert (data.isCounter(assignedToVar));
    Option<BigInt> increment = getIncrementOf(assignedToVar, rhs);
    if (increment.isSome())
      if (data.getWideningReference().isSome()) {
        for (NumVar x : data.getOccurences(assignedToVar))
          data.setWedge(x, data.getWedge(x).inc(assignedToVar, increment.get()));
      } else
        for (NumVar x : data.getOccurences(assignedToVar))
          data.setWedge(x, data.getWedge(x).inc(assignedToVar, increment.get()));
    else {
      deleteCounter(assignedToVar, childState.queryRange(assignedToVar));
      // assignPotentialCandidate(childState, assignedToVar, rhs, rhsValue);
    }
  }

  /**
   * Assign to a counter candidate.
   * If the assignment is neither an increment nor assigns a non-negative value,
   * remove the variable from the candidate list.
   */
  public <D extends ZenoDomain<D>> void assignCandidate (D childState, NumVar assignedToVar, Rhs rhs, Wedge rhsValue) {
    assert (data.isCandidate(assignedToVar));
    Option<BigInt> increment = getIncrementOf(assignedToVar, rhs);
    if (!increment.isSome() && !(rhsValue.hasLowerBound() && !rhsValue.lower().getConstant().isNegative()))
      data.setCandidates(data.getCandidates().remove(assignedToVar));
  }

  /**
   * Assign to a potential counter candidate.
   * If the assignment is either an increment nor assigns a non-negative value,
   * adds the variable to the candidate list.
   */
  public <D extends ZenoDomain<D>> void assignPotentialCandidate (D childState, NumVar assignedToVar, Rhs rhs,
      Wedge rhsValue) {
    assert (data.isCandidate(assignedToVar));
    Option<BigInt> increment = getIncrementOf(assignedToVar, rhs);
    if (increment.isSome() || (rhsValue.hasLowerBound() && !rhsValue.lower().getConstant().isNegative()))
      data.setCandidates(data.getCandidates().add(assignedToVar));
  }

  public <D extends ZenoDomain<D>> void assign (D childState, Assign stmt) {
    NumVar assignedToVar = stmt.getLhs().getId();
    Rhs assignedExpression = stmt.getRhs();
    Wedge assignedValue = assignedExpression.accept(WedgeEvaluator.INSTANCE, data.getWedges());

    if (data.isCounter(assignedToVar))
      assignCounter(childState, assignedToVar, assignedExpression, assignedValue);
    else if (data.isCandidate(assignedToVar))
      assignCandidate(childState, assignedToVar, assignedExpression, assignedValue);
    else
      assignPotentialCandidate(childState, assignedToVar, assignedExpression, assignedValue);
    data.setWedge(assignedToVar, assignedValue);
  }

  public Wedge approximate (Rhs rhs) {
    return rhs.accept(WedgeEvaluator.INSTANCE, data.getWedges());
  }

  public Wedge approximate (Linear lin) {
    return (new Rlin(lin)).accept(WedgeEvaluator.INSTANCE, data.getWedges());
  }

  public boolean subsetOrEqualTo (GaugeStateBuilder other) {
    VarSet Domain = data.getWedgesDomain().union(other.data.getWedgesDomain());
    for (NumVar x : Domain)
      if (!data.getWedge(x).subsetOrEqualTo(other.data.getWedge(x)))
        return false;
    return true;
  }

  public <D extends ZenoDomain<D>> void project (D child, NumVar x) {
    if (data.isCounter(x))
      deleteCounter(x, child.queryRange(x));
    if (data.isCandidate(x))
      data.setCandidates(data.getCandidates().remove(x));
    data.removeWedge(x);
  }

  public void introduce (NumVar x, Option<BigInt> value) {
    if (data.getWedgesDomain().contains(x))
      throw new IllegalArgumentException("GAUGE: Cannot introduce a variable that is already present.");
    else {
      data.setWedge(x, Wedge.FULL);
      if (value.isSome())
        data.setWedge(x, Wedge.singleton(value.get()));
    }
  }

  public void substitute (NumVar x, NumVar y) {
    Option<Wedge> Wx = data.getWedgeOption(x);
    if (Wx.isSome()) {
      if (data.isCounter(x))
        data.substituteCounter(x, y);
      data.setWedge(y, Wx.get());
    } else
      throw new VariableSupportSetException();
  }

  private static Test leq0 (Linear expr) {
    return ZenoFactory.getInstance().comparison((expr), ZenoTestOp.LessThanOrEqualToZero);
  }

  // For each test operation, performs a simple one pass, two-part reduction
  // The first part reduces *non*-counters against *their own* wedges, while the
  // second part reduces *counters* against wedges they *occur in*.
  public <D extends ZenoDomain<D>> D reduce (D childState, Test test) {
    Linear expr = test.getExpr();
    // Wedge approx = approximate(expr);

    VarSet exprVars = expr.getVars();
    VarSet exprCounters = exprVars.intersection(data.getCounters());
    VarSet exprNonCounters = exprVars.difference(data.getCounters());
    VarSet relevantVars = VarSet.empty();
    for (NumVar v : exprCounters)
      relevantVars = relevantVars.union(data.getOccurences(v));

    switch (test.getOperator()) {
    case LessThanOrEqualToZero:
      // if(approx.hasLowerBound()) {
      // System.out.println("APPROX:"+approx);
      // childState = childState.eval(leq0(approx.lower()), pcOption);
      // }
      for (NumVar v : exprNonCounters) {
        // Eliminate v in the test, substitute the result in its wedge, reduce
        Substitution sigma = expr.genSubstitution(v);
        Wedge W = data.getWedge(v);
        if (expr.getCoeff(v).compareTo(Bound.ZERO) < 0 && W.hasUpperBound()) {
          // test -vwedge.upper() + elim(expr,xk) <=0
          Linear testExpr = mulAdd(Bound.MINUSONE, W.upper(), sigma.getFac(), sigma.getExpr());
          childState = childState.eval(leq0(testExpr));
        }
        if (expr.getCoeff(v).compareTo(Bound.ZERO) > 0 && W.hasLowerBound()) {
          // test vwedge.lower() - elim(expr,xk) <= 0
          Linear testExpr = mulAdd(Bound.ONE, W.lower(), sigma.getFac().negate(), sigma.getExpr());
          childState = childState.eval(leq0(testExpr));
        }
      }
      // Eliminate lambda in the test, substitute the result in affected wedges, reduce
      for (NumVar c : exprCounters) {
        Substitution sigma = expr.genSubstitution(c);
        for (NumVar v : relevantVars.remove(c)) {
          Wedge W = data.getWedge(v);
          Wedge Wsubst = W.applySubstitution(sigma);
          if (expr.getCoeff(c).isNegative() && W.hasLowerBound()) {
            Linear testExpr = mulAdd(Bound.MINUSONE, linear(term(v)), Bound.ONE, Wsubst.lower());
            childState = childState.eval(leq0(testExpr));
          }
          if (expr.getCoeff(c).isPositive() && W.hasUpperBound()) {
            Linear testExpr = mulAdd(Bound.ONE, linear(term(v)), Bound.MINUSONE, Wsubst.upper());
            childState = childState.eval(leq0(testExpr));
          }
        }
      }
      break;
    default:
      throw new IllegalArgumentException();
    }
    return childState;
  }

}
