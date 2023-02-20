package bindead.domains.widening.oldthresholds;


import static bindead.debug.StringHelpers.indentMultiline;

import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P3;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.data.FoldMap;
import bindead.data.Linear.Divisor;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.widening.thresholds.ThresholdsWideningProperties;

/**
 * A domain that tracks redundant tests and uses them as thresholds for widening.
 */
public class ThresholdsWidening<D extends ZenoDomain<D>> extends
    ZenoFunctor<ThresholdsWideningState, D, ThresholdsWidening<D>> {
  public static final String NAME = "THRESHOLDSWIDENING(OLD)";
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
    if (stmt.getRhs() instanceof Rlin) {
      Rlin rhs = (Rlin) stmt.getRhs();
      Divisor d = new Divisor(rhs.getDivisor());
      builder.applyAffineTransformation(d, stmt.getLhs().getId(), rhs.getLinearTerm(), childState);
    } else {
      builder.removeThresholdsContaining(stmt.getLhs().getId(), childState);
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
    for (NumVar variable : vars)
      builder.removeThresholdsContaining(variable, childState);
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
    P3<D, Threshold, Set<Threshold>> result = builder.applyNarrowing(widenedChildState, location);
    D narrowedChildState = result._1();
    if (DEBUGWIDENING) {
      System.out.println();
      System.out.println(name + " (widening)@" + location + ": ");
      System.out.println(indentMultiline("  widened-child:  ", widenedChildState.toString()) + "\n");
      System.out.println("  consumed test: " + result._2() + ";   applied narrowing tests:  " + result._3());
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
    // NOTE: we do not need such hacks as thresholds should be removed whenever they become non-redundant
    // during join and widening.
//    ThresholdsWideningStateBuilder<D> builder = new ThresholdsWideningStateBuilder<D>(state);
//    ThresholdsWideningStateBuilder<D> otherBuilder = new ThresholdsWideningStateBuilder<D>(other.state);
//    if (!builder.subsetOrEqual(otherBuilder))
//      return false;
    return childState.subsetOrEqual(other.childState);
  }

}
