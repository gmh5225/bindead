package bindead.domains.widening.thresholds;


import static bindead.debug.StringHelpers.indentMultiline;

import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.data.FoldMap;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.ZenoDomain;

/**
 * A domain that tracks redundant tests and uses them as thresholds for widening.
 */
public class ThresholdsWidening<D extends ZenoDomain<D>> extends
    ZenoFunctor<ThresholdsWideningState, D, ThresholdsWidening<D>> {
  public static final String NAME = "THRESHOLDSWIDENING";
  private final boolean DEBUGBINARIES = ThresholdsWideningProperties.INSTANCE.debugBinaryOperations.isTrue();
  private final boolean DEBUGWIDENING = ThresholdsWideningProperties.INSTANCE.debugWidening.isTrue();
  private final boolean DEBUGASSIGN = ThresholdsWideningProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean DEBUGTESTS = ThresholdsWideningProperties.INSTANCE.debugTests.isTrue();
  private final boolean DEBUGOTHER = ThresholdsWideningProperties.INSTANCE.debugOther.isTrue();

  public ThresholdsWidening (D child) {
    super(NAME, ThresholdsWideningState.EMPTY, child);
  }

  private ThresholdsWidening (ThresholdsWideningState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public ThresholdsWidening<D> build (ThresholdsWideningState state, D childState) {
    return new ThresholdsWidening<D>(state, childState);
  }

  @Override public ThresholdsWidening<D> eval (Assign stmt) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    Option<ProgramPoint> location = getContext().getLocation();
    // only transform thresholds if we can associate them with a location where it happened
    if (stmt.getRhs() instanceof Rlin && location.isSome()) {
      Rlin rhs = (Rlin) stmt.getRhs();
      builder.applyAffineTransformation(stmt.getLhs().getId(), rhs, childState, location);
    } else {
      builder.removeThresholdsContaining(stmt.getLhs().getId(), childState, location);
    }
    if (DEBUGASSIGN) {
      System.out.println();
      System.out.println(name + ":");
      System.out.println("  evaluating assign: " + stmt);
      System.out.println("  before: " + state);
      System.out.println("  after: " + builder.build());
    }
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.eval(stmt);
    return build(builder.build(), newChildState);
  }

  @Override public ThresholdsWidening<D> eval (Test stmt) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    // only tests that are invariants in this state should be collected,
    // thus collect the redundant tests before applying them to the child domain
    Option<ProgramPoint> location = getContext().getLocation();
    D newChildState = builder.collectThresholds(location, stmt, childState);
    if (DEBUGTESTS) {
      System.out.println();
      System.out.println(name + ": ");
      System.out.println("  evaluating test: " + stmt);
      System.out.println("  before: " + state);
      System.out.println("  after: " + builder.build());
    }
    return build(builder.build(), newChildState);
  }

  @Override public ThresholdsWidening<D> project (VarSet vars) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    Option<ProgramPoint> location = getContext().getLocation();
    for (NumVar variable : vars)
      builder.removeThresholdsContaining(variable, childState, location);
    if (DEBUGOTHER) {
      System.out.println();
      System.out.println(name + ":");
      System.out.println("  project: " + vars);
      System.out.println("  before: " + state);
      System.out.println("  after: " + builder.build());
    }
    D newChildState = childState.project(vars);
    return build(builder.build(), newChildState);
  }

  @Override public ThresholdsWidening<D> substitute (NumVar x, NumVar y) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    builder.substitute(x, y);
    D newChildState = childState.substitute(x, y);
    return build(builder.build(), newChildState);
  }

  @Override public ThresholdsWidening<D> expand (FoldMap vars) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    builder.expand(vars);
    D newChildState = childState.expand(vars);
    return build(builder.build(), newChildState);
  }

  @Override public ThresholdsWidening<D> fold (FoldMap vars) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    builder.fold(vars);
    D newChildState = childState.fold(vars);
    return build(builder.build(), newChildState);
  }

  @Override public ThresholdsWidening<D> copyAndPaste (VarSet vars, ThresholdsWidening<D> from) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    D newChildState = childState.copyAndPaste(vars, from.childState);
    builder.copyAndPaste(vars, from.state);
    return build(builder.build(), newChildState);
  }

  @Override public ThresholdsWidening<D> widen (ThresholdsWidening<D> other) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    ThresholdsWideningStateBuilder<D> otherBuilder = new ThresholdsWideningStateBuilder<D>(other.state);
    builder.mergeThresholds(childState, other.childState, otherBuilder);
    D widenedChildState = childState.widen(other.childState);
    // widening happens at the program location of the new state (other)
    Option<ProgramPoint> location = other.getContext().getLocation();
    P2<D, Set<Threshold>> result = builder.applyNarrowing(widenedChildState, location);
    D narrowedChildState = result._1();
    if (DEBUGWIDENING) {
      System.out.println();
      System.out.println(name + " (widening)@" + location + ": ");
      System.out.println(indentMultiline("  widened-child:  ", widenedChildState.toString()) + "\n");
      System.out.println("  applied narrowing tests:  " + result._2());
      System.out.println(indentMultiline("  narrowed-child: ", narrowedChildState.toString()) + "\n");
    }
    builder.removeNonRedundantThresholds(narrowedChildState);
    return build(builder.build(), narrowedChildState);
  }

  @Override public ThresholdsWidening<D> join (ThresholdsWidening<D> other) {
    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
    ThresholdsWideningStateBuilder<D> otherBuilder = new ThresholdsWideningStateBuilder<D>(other.state);
    builder.mergeThresholds(childState, other.childState, otherBuilder);
    if (DEBUGBINARIES) {
      System.out.println();
      System.out.println(name + " (join): ");
      System.out.println("  fst: " + state);
      System.out.println("  snd: " + other.state);
      System.out.println("  join: " + builder.build());
    }
    D newChildState = childState.join(other.childState);
    builder.removeNonRedundantThresholds(newChildState);
    return build(builder.build(), newChildState);
  }

  @Override public boolean subsetOrEqual (ThresholdsWidening<D> other) {
    // seem to not be needed!
//    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
//    ThresholdsWideningStateBuilder<D> otherBuilder = new ThresholdsWideningStateBuilder<D>(other.state);
//    if (!builder.subsetOrEqual(childState, otherBuilder))
//      return false;
    return childState.subsetOrEqual(other.childState);
  }

}
