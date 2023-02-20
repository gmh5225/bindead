package bindead.domains.gauge;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import rreil.lang.util.Type;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.util.ZenoExprSimplifier;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.exceptions.Unreachable;

/**
 * @author sgreben
 */
public class Gauge<D extends ZenoDomain<D>> extends ZenoFunctor<GaugeState, D, Gauge<D>> {
  @SuppressWarnings("unused")
  private static final boolean DEBUGBINARIES = GaugeProperties.INSTANCE.debugBinaryOperations.isTrue();
  private static final boolean DEBUGSUBSETOREQUAL = GaugeProperties.INSTANCE.debugSubsetOrEqual.isTrue();
  private static final boolean DEBUGWIDENING = GaugeProperties.INSTANCE.debugWidening.isTrue();
  private static final boolean DEBUGASSIGN = GaugeProperties.INSTANCE.debugAssignments.isTrue();
  private static final boolean DEBUGTESTS = GaugeProperties.INSTANCE.debugTests.isTrue();

  public static final String NAME = "GAUGE";

  public Gauge (D childState) {
    this(GaugeState.EMPTY, childState);
  }

  private Gauge (GaugeState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public Gauge<D> join (Gauge<D> other) {
    GaugeStateBuilder fst = state.getBuilder();
    GaugeStateBuilder snd = other.state.getBuilder();
    fst.join(childState, snd);
    return build(fst.build(), childState.join(other.childState));
  }

  @Override public Gauge<D> widen (Gauge<D> other) {
    GaugeStateBuilder fst = state.getBuilder();
    GaugeStateBuilder snd = other.state.getBuilder();
    if (DEBUGWIDENING) {
      System.out.println(name + ":");
      System.out.println("  widening@" + other.getContext().getLocation());
      System.out.println("  this: " + state);
      System.out.println("  other: " + other.state);
    }
    fst.widen(childState, snd, other.childState);
    if (DEBUGWIDENING) {
      System.out.println("  result: " + fst.build().toString());
    }
    return build(fst.build(), childState.widen(other.childState));
  }

  @Override public boolean subsetOrEqual (Gauge<D> other) {
    GaugeStateBuilder fst = state.getBuilder();
    GaugeStateBuilder snd = other.state.getBuilder();
    if (DEBUGSUBSETOREQUAL) {
      System.out.println(name + ":");
      System.out.println("  subsumption test");
      System.out.println("  this: " + state);
      System.out.println("  other: " + other);
      System.out.println("  result: " + fst.subsetOrEqualTo(snd));
    }
    return fst.subsetOrEqualTo(snd);
  }

  @Override public Gauge<D> build (GaugeState state, D childState) {
    return new Gauge<D>(state, childState);
  }

  /**
   * TODO -s from discussion on 08/11/12:
   * - check for constant assignment via child's synth channel (DONE)
   * - fix reduction (DONE)
   * */
  @Override public Gauge<D> eval (Assign stmt) {
    stmt = ZenoExprSimplifier.run(stmt, childState);
    GaugeStateBuilder builder = state.getBuilder();
    builder.assign(childState, stmt);
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.eval(stmt);
    if (DEBUGASSIGN) {
      System.out.println(name + ":");
      System.out.println("  assign: " + stmt);
      System.out.println("  this: " + state);
      System.out.println("  after: " + builder.build().toString());
    }
    return build(builder.build(), newChildState);
  }

  @Override public Gauge<D> eval (Test test) throws Unreachable {
    if (DEBUGTESTS) {
      System.out.println(name + ":");
      System.out.println("  evaluating test: " + test);
      System.out.println("  this: " + state);
    }
    if (ZenoTestHelper.isTautologyReportUnreachable(test))
      return this;
    if (test.getOperator().equals(ZenoTestOp.NotEqualToZero)) {
      P2<Test, Test> tuple = test.splitEquality();
      Gauge<D> childTested = build(state, childState.eval(test));
      Gauge<D> lessThanMinusOne, greaterThanOne;
      try {
        lessThanMinusOne =
          childTested.eval(tuple._1());
      } catch (Unreachable u) {
        greaterThanOne =
          childTested.eval(tuple._2());
        return greaterThanOne;
      } // <= -1 !unreachable
      try {
        greaterThanOne =
          childTested.eval(tuple._2());
        return lessThanMinusOne.join(greaterThanOne);
      } catch (Unreachable u) {
        return lessThanMinusOne;
      }
    }

    if (test.getOperator().equals(ZenoTestOp.EqualToZero)) {
      P2<Test, Test> tuple = test.splitEquality();
      Gauge<D> childTested = build(state, childState.eval(test));
      Gauge<D> leqZero, geqZero;
      try {
        geqZero =
          childTested.eval(tuple._1());
      } catch (Unreachable u) {
        leqZero =
          childTested.eval(tuple._2());
        return leqZero;
      } // <= -1 !unreachable
      try {
        leqZero =
          childTested.eval(tuple._2());
        return geqZero.join(leqZero);
      } catch (Unreachable u) {
        return geqZero;
      }
    }

    GaugeStateBuilder builder = state.getBuilder();
    D newChildState = childState.eval(test);

    newChildState = builder.reduce(newChildState, test);

    return build(builder.build(), newChildState);
  }


  @Override public Gauge<D> project (VarSet vars) {
    GaugeStateBuilder builder = state.getBuilder();
    for (NumVar variable : vars)
      builder.project(childState, variable);
    D newChildState = childState.project(vars);
    return build(builder.build(), newChildState);
  }

  @Override public Gauge<D> substitute (NumVar x, NumVar y) {
    GaugeStateBuilder builder = state.getBuilder();
    builder.substitute(x, y);
    D newChildState = childState.substitute(x, y);
    return build(builder.build(), newChildState);
  }

  @Override public Gauge<D> introduce (NumVar variable, Type type, Option<BigInt> value) {
    GaugeStateBuilder builder = state.getBuilder();
    builder.introduce(variable, value);
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.introduce(variable, type, value);
    return build(builder.build(), newChildState);
  }

}