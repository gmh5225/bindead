package bindead.domains.widening.delayed;

import static bindead.data.Linear.linear;
import javalx.data.Option;
import javalx.numeric.BigInt;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.abstractsyntax.zeno.ZenoVisitor;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.VoidDomainState;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.exceptions.Unreachable;

/**
 * A domain that translates non-invertible assignments x = e to tests x - e = 0 such that the widening thresholds domain
 * can use these to not lose precision on widening for variables that have been assigned constants inside a loop.
 */
public class DelayedWideningWithThresholds<D extends ZenoDomain<D>> extends
    ZenoFunctor<VoidDomainState, D, DelayedWideningWithThresholds<D>> {
  public static final String NAME = "DELAYEDWIDENING(Thresholds)";
  private final boolean DEBUGASSIGN = DelayedWideningProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean SEMANTICCONSTANTS = DelayedWideningProperties.INSTANCE.semanticConstants.isTrue();

  ZenoVisitor<Option<BigInt>, Void> rhsEvaluator = new ZenoRhsVisitorSkeleton<Option<BigInt>, Void>() {
    @Override public Option<BigInt> visit (Bin expr, Void data) {
      return Option.none();
    }

    @Override public Option<BigInt> visit (RangeRhs expr, Void data) {
      return Option.none();
      // NOTE: this would already be semantic
//      javalx.numeric.Range range = expr.getRange();
//      if (range.isConstant())
//        return Option.some(linear(range.getConstantOrNull()));
//      else
//        return Option.none(); // TODO: could generate two tests here for each bound if there exist bounds
    }

    @Override public Option<BigInt> visit (Rlin expr, Void data) {
      if (!expr.getDivisor().isOne())
        throw new IllegalStateException();
      return Option.fromNullable(expr.getConstantOrNull());
    }
  };

  public DelayedWideningWithThresholds (D childState) {
    this(VoidDomainState.empty(), childState);
  }

  private DelayedWideningWithThresholds (VoidDomainState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public DelayedWideningWithThresholds<D> build (VoidDomainState state, D childState) {
    return new DelayedWideningWithThresholds<D>(state, childState);
  }

  @Override public boolean subsetOrEqual (DelayedWideningWithThresholds<D> other) {
    return childState.subsetOrEqual(other.childState);
  }

  @Override public DelayedWideningWithThresholds<D> join (DelayedWideningWithThresholds<D> other) {
    D newChildState = childState.join(other.childState);
    return build(state, newChildState);
  }

  @Override public DelayedWideningWithThresholds<D> widen (DelayedWideningWithThresholds<D> other) {
    D newChildState = childState.widen(other.childState);
    return build(other.state, newChildState);
  }

  @Override public DelayedWideningWithThresholds<D> eval (Assign stmt) {
    ZenoFactory zeno = ZenoFactory.getInstance();
    D newChildState = childState.eval(stmt);    // evaluate on child as result is needed later on for constants test
    Option<BigInt> constantRhs = getConstant(newChildState, stmt);
    if (constantRhs.isSome() && !isInvertibleAssignment(stmt)) {
      NumVar lhs = stmt.getLhs().getId();
      // invertible assignments e.g. x = x + e would lead to false tests TODO: see why again!
      Linear linear = linear(lhs).sub(constantRhs.get());
      Test test = zeno.comparison(linear, ZenoTestOp.EqualToZero);
      newChildState = newChildState.eval(test);
      if (DEBUGASSIGN) {
        Option<ProgramPoint> location = getContext().getLocation();
        System.out.println(name + " @" + location + ": ");
        System.out.println("  evaluating assign: " + stmt);
        System.out.println("  adding threshold: " + test);
      }
    }
    return build(state, newChildState);
  }

  private static boolean isInvertibleAssignment (Assign stmt) {
    NumVar lhs = stmt.getLhs().getId();
    Rhs rhs = stmt.getRhs();
    return rhs.getVars().contains(lhs);
  }

  private Option<BigInt> getConstant (D newChildState, Assign stmt) {
    if (SEMANTICCONSTANTS)  // semantic constants test
      return Option.fromNullable(newChildState.queryRange(stmt.getLhs().getId()).getConstantOrNull());
    else
      return stmt.getRhs().accept(rhsEvaluator, null); // syntactic constants test
  }

  @Override public DelayedWideningWithThresholds<D> eval (Test test) throws Unreachable {
    D newChildState = childState.eval(test);
    return build(state, newChildState);
  }

  @Override public DelayedWideningWithThresholds<D> project (VarSet vars) {
    D newChildState = childState.project(vars);
    return build(state, newChildState);
  }

  @Override public DelayedWideningWithThresholds<D> substitute (NumVar x, NumVar y) {
    D newChildState = childState.substitute(x, y);
    return build(state, newChildState);
  }

  @Override final public String toString () {
    // the domain does not have a state itself so ignore it in the output
    return childState.toString();
  }

}
