package bindead.domains.syntacticstripes;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.data.Linear;
import bindead.data.Linear.Divisor;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.syntacticstripes.StripeStateBuilder.Constraint;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The stripe domain's data-structure.
 */
final class StripeState extends FunctorState {
  private static final boolean CHECK = StripeProperties.INSTANCE.checkState.isTrue();
  // REFACTOR use class SetOfEquations instead of AVLSet<Linear>
  static final AVLSet<Linear> EMPTYSET = AVLSet.empty();
  static final AVLMap<NumVar, Constraint> EMPTYCONSTRAINTS = AVLMap.<NumVar, Constraint>empty();
  static final StripeState EMPTY = empty();
  final AVLMap<Linear, Stripe> stripes;
  final AVLMap<NumVar, AVLSet<Linear>> reverse;
  final AVLMap<NumVar, Constraint> narrowingconstraints;
  final VarSet specials;

  StripeState (AVLMap<Linear, Stripe> stripes, AVLMap<NumVar, AVLSet<Linear>> reverse, VarSet specials) {
    this.stripes = stripes;
    this.reverse = reverse;
    this.specials = specials;
    this.narrowingconstraints = EMPTYCONSTRAINTS;
    if (CHECK)
      check();
  }

  StripeState (AVLMap<Linear, Stripe> stripes, AVLMap<NumVar, AVLSet<Linear>> reverse, VarSet specials,
      AVLMap<NumVar, Constraint> narrowingpredicates) {
    this.stripes = stripes;
    this.reverse = reverse;
    this.specials = specials;
    this.narrowingconstraints = narrowingpredicates;
    if (CHECK)
      check();
  }

  private static StripeState empty () {
    return new StripeState(
      AVLMap.<Linear, Stripe>empty(),
      AVLMap.<NumVar, AVLSet<Linear>>empty(),
      VarSet.empty());
  }

  boolean notInSupport (NumVar var) {
    return !inSupport(var);
  }

  boolean inSupport (NumVar var) {
    return inReverseMapping(var);
  }

  private boolean inReverseMapping (NumVar var) {
    Option<AVLSet<Linear>> usedIn = reverse.get(var);
    if (usedIn.isNone())
      return false;
    return usedIn.get().size() > 0;
  }

  @Override public String toString () {
    Iterator<P2<Linear, Stripe>> iterator = stripes.iterator();
    if (!iterator.hasNext())
      return "{}";
    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      P2<Linear, Stripe> element = iterator.next();
      Linear key = element._1();
      Stripe value = element._2();
      builder.append(value);
      builder.append(": ");
      builder.append(key);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    XMLBuilder xml = builder;
    for (P2<Linear, Stripe> idAndLinear : stripes) {
      // TODO: maybe render special variable as well.
      idAndLinear._1().toXML(builder);
    }
    return xml;
  }

  Linear inlineSyntactically (Linear linear) {
    Divisor gcd = Divisor.one();
    Linear t = linear.dropConstant().lowestForm(gcd);
    if (stripes.contains(t)) {
      Stripe stripe = stripes.get(t).get();
      return stripe.asNegatedLinear(gcd, linear.getConstant());
    } else if (stripes.contains(t.negate())) {
      Stripe stripe = stripes.get(t.negate()).get();
      return stripe.asLinear(gcd, linear.getConstant());
    } else
      return linear;
  }

  Linear inlineSyntacticallyForTest (Linear linear) {
    Divisor gcd = Divisor.one();
    Linear t = linear.dropConstant().lowestForm(gcd);
    if (stripes.contains(t)) {
      Stripe stripe = stripes.get(t).get();
      return linear.add(stripe.asLinear());
    } else if (stripes.contains(t.negate())) {
      Stripe stripe = stripes.get(t.negate()).get();
      return linear.sub(stripe.asLinear());
    } else
      return linear;
  }

  <D extends ZenoDomain<D>> P2<D, SynthChannel> applyStripes (Test stmt, D childState) {
    D newChildState = childState;
    // Collect all possibly affected stripes
    VarSet vs = stmt.getExpr().getVars();
    AVLSet<Linear> affectedStripes = EMPTYSET;
    for (NumVar x : vs)
      affectedStripes =
        affectedStripes.union(reverse.get(x).getOrElse(EMPTYSET));
    SynthChannel channel = newChildState.getSynthChannel();
    if (Stripes.DEBUGTESTS) {
      System.out.println("  affected stripes: " + affectedStripes);
      System.out.println("  child: " + newChildState);
    }
    for (Linear lin : affectedStripes) {
      Stripe s = stripes.get(lin).get();
      Test t = s.asEquality(lin);
      if (Stripes.DEBUGTESTS)
        System.out.println("  evaluating test: " + t);
      newChildState = newChildState.eval(t);
      if (Stripes.DEBUGTESTS)
        System.out.println("  child: " + newChildState);
      channel = channel.intersect(newChildState.getSynthChannel());
    }
    return P2.tuple2(newChildState, channel);
  }

  <D extends ZenoDomain<D>> P2<D, SynthChannel> applyNarrowingConstraints
      (Test test, D childState, SynthChannel channel) {
    D newChildState = childState;
    if (narrowingconstraints.isEmpty())
      return P2.tuple2(newChildState, channel);
    // TODO: this might apply a given constraint more than once.
    for (NumVar x : test.getVars()) {
      Constraint c = narrowingconstraints.getOrNull(x);
      if (c != null) {
        Test t = c.asTestOrNull(childState);
        if (t != null) {
          if (Stripes.DEBUGTESTS)
            System.out.println("  evaluating constraint: " + c + " as: " + t);
          newChildState = newChildState.eval(t);
          if (Stripes.DEBUGTESTS)
            System.out.println("  child: " + newChildState);
          channel = channel.intersect(newChildState.getSynthChannel());
        }
      }
    }
    return P2.tuple2(newChildState, channel);
  }

  VarSet getLocalVariables () {
    VarSet locals = VarSet.empty();
    for (NumVar x : specials) {
      locals = locals.add(x);
    }
    return locals;
  }

  private void check () {
    for (P2<NumVar, AVLSet<Linear>> pair : reverse) {
      AVLSet<Linear> vs = pair._2();
      NumVar var = pair._1();
      for (Linear lin : vs) {
        assert stripes.contains(lin);
        Stripe special = stripes.getOrNull(lin);
        if (var != special.special)
          assert lin.getVars().contains(var);
      }
    }
    for (P2<Linear, Stripe> pair : stripes) {
      Stripe stripe = pair._2();
      assert pair._1().getVars().size() >= 2;
      assert pair._1().getConstant().isZero();
      assert reverse.get(stripe.special).get().size() == 1;
      for (Term t : pair._1()) {
        assert reverse.get(t.getId()).get().contains(pair._1());
      }
    }
    assert stripes.size() == specials.size();
  }
}
