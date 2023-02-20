package bindead.domains.widening.delayed;

import javalx.data.Option;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.abstractsyntax.zeno.ZenoVisitor;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.exceptions.Unreachable;

/**
 * A domain that tracks the program points of assignments where the right-hand-side is a constant. On widening points
 * the domain can suppress the widening (using a join instead) on its child if the domain itself is not yet stable.
 * That means if new paths with constant assignments have been found then do not widen yet.
 */
public class DelayedWidening<D extends ZenoDomain<D>> extends ZenoFunctor<DelayedWideningState, D, DelayedWidening<D>> {
  public static final String NAME = "DELAYEDWIDENING";
  private final boolean DEBUGBINARIES = DelayedWideningProperties.INSTANCE.debugBinaryOperations.isTrue();
  private final boolean DEBUGWIDENING = DelayedWideningProperties.INSTANCE.debugWidening.isTrue();
  private final boolean DEBUGASSIGN = DelayedWideningProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean SEMANTICCONSTANTS = DelayedWideningProperties.INSTANCE.semanticConstants.isTrue();

  ZenoVisitor<Boolean, Void> syntacticConstantExprTest = new ZenoRhsVisitorSkeleton<Boolean, Void>() {
    @Override public Boolean visit (Bin expr, Void data) {
      return false;
    }

    @Override public Boolean visit (RangeRhs expr, Void data) {
      return false;
      // NOTE: this would already be semantic:
      // return expr.getRange().isConstant();
    }

    @Override public Boolean visit (Rlin expr, Void data) {
      return expr.isConstantOnly();
    }
  };


  public DelayedWidening (D childState) {
    this(DelayedWideningState.EMPTY, childState);
  }

  private DelayedWidening (DelayedWideningState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public DelayedWidening<D> build (DelayedWideningState state, D childState) {
    return new DelayedWidening<D>(state, childState);
  }

  @Override public boolean subsetOrEqual (DelayedWidening<D> other) {
    return childState.subsetOrEqual(other.childState);
  }

  @Override public DelayedWidening<D> join (DelayedWidening<D> other) {
    DelayedWideningState newState = new DelayedWideningState(state.assignments.union(other.state.assignments));
    if (DEBUGBINARIES) {
      System.out.println(name + " (join): ");
      System.out.println("  fst:  " + state.assignments);
      System.out.println("  snd:  " + other.state.assignments);
      System.out.println("  join: " + newState.assignments);
    }
    D newChildState = childState.join(other.childState);
    return build(newState, newChildState);
  }

  @Override public DelayedWidening<D> widen (DelayedWidening<D> other) {
    // other.state is the join of this and the new state
    D newChildState;
    AVLSet<ProgramPoint> newConstAssignments = other.state.assignments.difference(state.assignments);
    if (newConstAssignments.isEmpty())
      newChildState = childState.widen(other.childState);
    else
      newChildState = childState.join(other.childState);
    if (DEBUGWIDENING) {
      String message =
        newConstAssignments.isEmpty() ? "applying widening" : "widening suppressed because of "
          + newConstAssignments.size() + " new constant assignments from: " + newConstAssignments;
      Option<ProgramPoint> location = getContext().getLocation();
      System.out.println(name + " (widening)@" + location + ": " + message);
    }
    return build(other.state, newChildState);
  }

  @Override public DelayedWidening<D> eval (Assign stmt) {
    Option<ProgramPoint> location = getContext().getLocation();
    D newChildState = childState.eval(stmt);    // evaluate on child as result is needed later on for constants test
    DelayedWideningState newState = state;
    if (location.isSome() && !state.assignments.contains(location.get())) {
      if (isConstant(newChildState, stmt))
        newState = new DelayedWideningState(state.assignments.add(location.get()));
    }
    if (DEBUGASSIGN) {
      System.out.println(name + " @" + location + ": ");
      System.out.println("  evaluating assign: " + stmt);
      System.out.println("  before: " + state.assignments);
      System.out.println("  after: " + newState.assignments);
    }
    return build(newState, newChildState);
  }

  private boolean isConstant (D childState, Assign stmt) {
    if (SEMANTICCONSTANTS)
      return childState.queryRange(stmt.getLhs().getId()).isConstant();   // semantic constants test
    else
      return stmt.getRhs().accept(syntacticConstantExprTest, null); // syntactic constants test
  }

  @Override public DelayedWidening<D> eval (Test test) throws Unreachable {
    D newChildState = childState.eval(test);
    return build(state, newChildState);
  }

  @Override public DelayedWidening<D> project (VarSet vars) {
    D newChildState = childState.project(vars);
    return build(state, newChildState);
  }

  @Override public DelayedWidening<D> substitute (NumVar x, NumVar y) {
    D newChildState = childState.substitute(x, y);
    return build(state, newChildState);
  }

}
