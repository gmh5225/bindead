package bindead.domains.syntacticstripes;

import javalx.data.products.P2;
import javalx.numeric.Range;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.syntacticstripes.StripeStateBuilder.Transformation;
import bindead.exceptions.DomainStateException.UnimplementedMethodException;

/**
 * The {@code stripe} domain functor.
 *
 * @param <D> The type of the child domain.
 */
public class Stripes<D extends ZenoDomain<D>> extends ZenoFunctor<StripeState, D, Stripes<D>> {
  public static final String NAME = "STRIPES";
  static boolean DEBUGASSIGNMENTS = StripeProperties.INSTANCE.debugAssignments.isTrue();
  static boolean DEBUGTESTS = StripeProperties.INSTANCE.debugTests.isTrue();
  static boolean DEBUGBINARIES = StripeProperties.INSTANCE.debugBinaryOperations.isTrue();
  static boolean DEBUGWIDENING = StripeProperties.INSTANCE.debugWidening.isTrue();
  static boolean DEBUGQUERIES = StripeProperties.INSTANCE.debugQueries.isTrue();
  static boolean DEBUGSUBSETOREQUAL = StripeProperties.INSTANCE.debugSubsetOrEqual.isTrue();
  static boolean DEBUGOTHER = StripeProperties.INSTANCE.debugOther.isTrue();
  private final SynthChannel channel;

  public Stripes (D child) {
    super(NAME, StripeState.EMPTY, child);
    this.channel = null;
  }

  protected Stripes (StripeState state, D childState) {
    super(NAME, state, childState);
    channel = null;
  }

  protected Stripes (StripeState state, D childState, SynthChannel channel) {
    super(NAME, state, childState);
    this.channel = channel;
  }

  public Stripes<D> build (StripeState state, D childState, SynthChannel channel) {
    return new Stripes<D>(state, childState, channel);
  }

  @Override public Stripes<D> build (StripeState state, D childState) {
    return new Stripes<D>(state, childState);
  }

  @Override public Stripes<D> eval (Assign stmt) {
    Assign assign = StripeInliningVisitor.run(stmt, state);
    StripeStateBuilder sys = new StripeStateBuilder(state, true);
    if (stmt.getRhs() instanceof Rlin) {
      Rlin lin = (Rlin) stmt.getRhs();
      Transformation t = new Transformation(childState, stmt.getLhs(), lin);
      sys.applyAffineTransformation(t);
    } else {
      // Remove the lhs-variable and try to (re)insert a "shorter" stripe.
      sys.removeVariable(stmt.getLhs().getId());
    }
    if (DEBUGASSIGNMENTS) {
      System.out.println(name + ": ");
      System.out.println("  evaluating: " + stmt + " that is: " + assign);
      System.out.println("  on child: " + sys.getChildOps());
      System.out.println("  stripes: " + state.stripes + " |-> " + sys.stripes);
      System.out.println("  child: " + childState);
    }
    D newChildState = sys.applyChildOps(childState);
    newChildState = newChildState.eval(assign);
    if (DEBUGASSIGNMENTS) {
      System.out.println("  child: " + newChildState);
    }
    return build(sys.build(), newChildState);
  }

  @Override public Stripes<D> eval (Test stmt) {
    D newChildState = childState;
    newChildState = newChildState.eval(stmt);
    if (DEBUGTESTS) {
      System.out.println(name + ": ");
      System.out.println("  evaluating test: " + stmt);
      System.out.println("  this: " + state);
      System.out.println("  before: " + childState);
      System.out.println("  after: " + newChildState);
    }
    Linear expr = stmt.getExpr();
    Linear lin = state.inlineSyntactically(expr);
    if (lin != expr) {
      Test t = stmt.withExpr(lin);
      if (DEBUGTESTS) {
        System.out.println("  evaluating test: " + t);
        System.out.println("  child: " + state);
      }
      newChildState = newChildState.eval(t);
    }
    P2<D, SynthChannel> res = state.applyStripes(stmt, newChildState);
    // res = state.applyNarrowingConstraints(stmt, res._1(), res._2());
    newChildState = res._1();
    return build(state, newChildState, res._2());
  }

  @Override public Stripes<D> expand (FoldMap vars) {
    throw new UnimplementedMethodException();
  }

  @Override public Stripes<D> fold (FoldMap vars) {
    throw new UnimplementedMethodException();
  }

  @Override public Stripes<D> join (Stripes<D> other) {
    StripeStateBuilder fst = new StripeStateBuilder(state, true);
    StripeStateBuilder snd = new StripeStateBuilder(other.state, true);
    StripeStateBuilder.makeCompatible(fst, snd);
    if (DEBUGBINARIES) {
      System.out.println(name + ": ");
      System.out.println("  fst: " + state.stripes);
      System.out.println("    on child: " + fst.getChildOps());
      System.out.println("  snd: " + other.state.stripes);
      System.out.println("    on child: " + snd.getChildOps());
      System.out.println("  join: " + fst.stripes);
      System.out.println("  fst child: " + childState);
      System.out.println("  snd child: " + other.childState);
    }
    D newChildStateOfFst = fst.applyChildOps(childState);
    D newChildStateOfSnd = snd.applyChildOps(other.childState);
    D newChildState = newChildStateOfFst.join(newChildStateOfSnd);
    if (DEBUGBINARIES) {
      System.out.println("  child: " + newChildState);
    }
    return build(fst.build(), newChildState);
  }

  @Override public Stripes<D> widen (Stripes<D> other) {
    StripeStateBuilder fst = new StripeStateBuilder(state);
    StripeStateBuilder snd = new StripeStateBuilder(other.state);
    StripeStateBuilder.makeCompatibleForWidening(fst, snd);
    fst.findNarrowingConstraints(childState, other.childState);
    if (DEBUGWIDENING) {
      System.out.println(name + ": ");
      System.out.println("  fst: " + state.stripes);
      System.out.println("    on child: " + fst.getChildOps());
      System.out.println("  snd: " + other.state.stripes);
      System.out.println("    on child: " + snd.getChildOps());
      System.out.println("  widen: " + fst.stripes);
      System.out.println("  narrowing: " + fst.narrowingconstraints);
      System.out.println("  fst child: " + childState);
      System.out.println("  snd child: " + other.childState);
    }
    D newChildStateOfFst = fst.applyChildOps(childState);
    D newChildStateOfSnd = snd.applyChildOps(other.childState);
    D newChildState = newChildStateOfFst.widen(newChildStateOfSnd);
    if (DEBUGWIDENING) {
      System.out.println("  child: " + newChildState);
    }
    return build(fst.build(), newChildState);
  }

  @Override public boolean subsetOrEqual (Stripes<D> other) {
    StripeStateBuilder fst = new StripeStateBuilder(state);
    StripeStateBuilder snd = new StripeStateBuilder(other.state);
    StripeStateBuilder.makeCompatibleForWidening(fst, snd);
    if (DEBUGSUBSETOREQUAL) {
      System.out.println(name + ":");
      System.out.println("  subset-or-equal:");
      System.out.println("  fst: " + state.stripes + " |-> " + fst.stripes);
      System.out.println("    on child: " + fst.getChildOps());
      System.out.println("  snd: " + other.state.stripes + " |-> " + snd.stripes);
      System.out.println("    on child: " + snd.getChildOps());
    }
    D newChildStateOfFst = fst.applyChildOps(childState);
    D newChildStateOfSnd = snd.applyChildOps(other.childState);
    return newChildStateOfFst.subsetOrEqual(newChildStateOfSnd);
  }

  @Override public Stripes<D> project (VarSet vars) {
    Stripes<D> d = this;
    for (NumVar v : vars)
      d = d.project(v);
    return d;
  }


  public Stripes<D> project (NumVar variable) {
    StripeStateBuilder sys = new StripeStateBuilder(state, true);
    if (state.notInSupport(variable)) {
      D newChildState = childState.project(VarSet.of(variable));
      return build(state, newChildState);
    } else {
      sys.applyProjection(variable, childState);
      if (DEBUGOTHER) {
        System.out.println(name + ": ");
        System.out.println("  projecting: " + variable);
        System.out.println("  on child: " + sys.getChildOps());
        System.out.println("  stripes: " + state.stripes + " |-> " + sys.stripes);
        System.out.println("  child: " + childState);
      }
      D newChildState = sys.applyChildOps(childState);
      if (DEBUGOTHER) {
        System.out.println("  child: " + newChildState);
        System.out.flush();
      }
      newChildState = newChildState.project(VarSet.of(variable));
      StripeState newState = sys.build();
      assert newState.notInSupport(variable);
      return build(newState, newChildState);
    }
  }

  @Override public Stripes<D> substitute (NumVar x, NumVar y) {
    assert state.notInSupport(y);
    if (state.notInSupport(x)) {
      D newChildState = childState.substitute(x, y);
      return build(state, newChildState);
    }
    StripeStateBuilder sys = new StripeStateBuilder(state, true);
    sys.applyDirectSubstitution(x, y);
    if (DEBUGOTHER) {
      System.out.println(name + ": ");
      System.out.println("  substitute: [" + x + "\\" + y + "]");
      System.out.println("  on child: " + sys.getChildOps());
      System.out.println("  child: " + childState);
    }
    D newChildState = sys.applyChildOps(childState);
    newChildState = newChildState.substitute(x, y);
    if (DEBUGOTHER) {
      System.out.println("  stripes: " + state.stripes + " |-> " + sys.stripes);
      System.out.println("  constraints: " + state.narrowingconstraints + " |-> " + sys.narrowingconstraints);
      System.out.println("  child: " + newChildState);
      System.out.flush();
    }
    StripeState newState = sys.build();
    assert newState.notInSupport(x);
    return build(newState, newChildState);
  }

  @Override public Range queryRange (Linear lin) {
    Linear inlined = state.inlineSyntactically(lin);
    // if (inlined == lin) return proxy.get().queryRange(inlined);
    // TODO:
    // Range r1 = proxy.get().queryRange(inlined);
    // Range r2 = proxy.get().queryRange(lin);
    // return r1.meet(r2).get();
    Range range = childState.queryRange(inlined);
    if (DEBUGQUERIES) {
      System.out.println(name + ": ");
      System.out.println("  query: " + lin + " that is: " + inlined);
      System.out.println("  is in range: " + range);
      System.out.println("  stripes: " + state.stripes);
      System.out.println("  child: " + childState);
      System.out.flush();
    }
    return range;
  }

  @Override public VarSet localSubset (VarSet toTest) {
    return toTest.intersection(state.getLocalVariables());
  }

  @Override public SynthChannel getSynthChannel () {
    SynthChannel channel =
      this.channel != null ?
          this.channel :
          childState.getSynthChannel();
    return channel.removeVariables(
        localSubset(channel.getVariables()));
  }

  @Override public Stripes<D> copyAndPaste (VarSet vars, Stripes<D> from) {
    // XXX:
    D newChildState = childState.copyAndPaste(vars, from.childState);
    return build(state, newChildState);
  }

}
