package bindead.domains.finiteDisjunction;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLSet;
import rreil.lang.MemVar;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.data.Linear;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.debug.DomainPrintProperties;
import bindead.debug.DomainStringBuilder;
import bindead.domainnetwork.channels.DebugChannel;
import bindead.domainnetwork.channels.Domain;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.exceptions.Unreachable;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Defer joining of child domain until widening.
 */
public final class FiniteDisjunction<D extends FiniteDomain<D>>
    extends Domain<FiniteDisjunction<D>>
    implements FiniteDomain<FiniteDisjunction<D>> {

  public static final String NAME = "DISJUNCTION";

  private final boolean compactPrinting = DomainPrintProperties.INSTANCE.printCompact.isTrue();

  public final LinkedList<D> childState;


  public FiniteDisjunction (final D child) {
    super(NAME);
    this.childState = newcs();
    this.childState.add(child);
  }

  public FiniteDisjunction (final LinkedList<D> child) {
    super(NAME);
    this.childState = child;
  }

  private LinkedList<D> newcs () {
    return new LinkedList<D>();
  }

  private FiniteDisjunction<D> build (LinkedList<D> cs) {
    if (cs.isEmpty())
      throw new Unreachable();
    return new FiniteDisjunction<D>(cs);
  }

  @Override public FiniteDisjunction<D> copyAndPaste (VarSet vars, FiniteDisjunction<D> from) {
    D fromc = from.collapse();
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.copyAndPaste(vars, fromc));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  private D collapse () {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    D collected = it.next();
    while (it.hasNext()) {
      collected = collected.join(it.next());
    }
    return collected;
  }

  @SuppressWarnings("deprecation") @Override public List<P2<NumVar.AddrVar, FiniteDisjunction<D>>> deprecatedDeref (
      Rlin ptr, VarSet summaries) throws Unreachable {
    List<P2<AddrVar, FiniteDisjunction<D>>> alts =
      new LinkedList<P2<AddrVar, FiniteDisjunction<D>>>();
    for (D c : childState)
      try {
        for (P2<AddrVar, D> ca : c.deprecatedDeref(ptr, summaries)) {
          AddrVar adr = ca._1();
          alts.add(new P2<AddrVar, FiniteDisjunction<D>>(adr, new FiniteDisjunction<D>(ca._2())));
        }
      } catch (Unreachable _) {
      }

    return alts;
  }

  @Override public FiniteDisjunction<D> eval (Assign stmt) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.eval(stmt));
      } catch (Unreachable _) {
      }

    return build(cs);
  }

  @Override public FiniteDisjunction<D> eval (Test test) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.eval(test));
      } catch (Unreachable _) {
      }

    return build(cs);
  }

  @Override public FiniteDisjunction<D> introduce (NumVar var, Type type, Option<BigInt> value) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.introduce(var, type, value));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public FiniteDisjunction<D> project (NumVar var) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.project(var));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public SetOfEquations queryEqualities (NumVar var) {
    // too complicated, ignoring
    return SetOfEquations.empty();
  }

  @Override public Range queryRange (Linear lin) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    Range result = it.next().queryRange(lin);
    while (it.hasNext()) {
      result = result.union(it.next().queryRange(lin));
    }
    return result;
  }

  @Override public FiniteDisjunction<D> substitute (NumVar from, NumVar to) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.substitute(from, to));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  /* ***********
   * Operations from interface FiniteDomain:
   *********** */

  @Override public FiniteDisjunction<D> assumeConcrete (NumVar var) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.assumeConcrete(var));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public FiniteDisjunction<D> assumePointsToAndConcretize (Rlin refVar, AddrVar target, VarSet contents) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.assumePointsToAndConcretize(refVar, target, contents));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public P2<AVLSet<AddrVar>, FiniteDisjunction<D>> deref (Rlin ptr)
      throws Unreachable {

    AVLSet<AddrVar> vs = AVLSet.<AddrVar>empty();
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        P2<AVLSet<AddrVar>, D> p2 = c.deref(ptr);
        vs = vs.union(p2._1());
        cs.add(p2._2());
      } catch (Unreachable _) {
      }
    return P2.tuple2(vs, build(cs));
  }

  /**
   * Returns a set of possible (abstract) target addresses without restricting the numeric state.
   */
  @Override public Collection<AddrVar> findPossiblePointerTargets (NumVar id) throws Unreachable {
    LinkedList<AddrVar> cs = new LinkedList<AddrVar>();
    for (D c : childState)
      try {
        cs.addAll(c.findPossiblePointerTargets(id));
      } catch (Unreachable _) {
      }
    return cs;
  }

  /* ***********
   * Operations from interface SemiLattice:
   *********** */

  @Override public FiniteDisjunction<D> join (FiniteDisjunction<D> other) {
    LinkedList<D> cs = new LinkedList<D>(childState);
    cs.addAll(other.childState);
    FiniteDisjunction<D> built = build(cs);
    return built;
  }

  @Override public FiniteDisjunction<D> widen (FiniteDisjunction<D> other) {
    return new FiniteDisjunction<D>(collapse().widen(other.collapse()));
  }

  @Override public boolean subsetOrEqual (FiniteDisjunction<D> other) {
    return collapse().subsetOrEqual(other.collapse());
  }

  /**
   * The widening mechanism implemented here is a bit unconventional, as it differs
   * from good old numeric widening (which our numeric domains implement as<br>
   *
   * <pre>
   *   if(n <= s)
   *     return null
   *  else
   *     return numericWiden(s, s + n)
   * </pre>
   *
   * with + being the join operator).<br>
   * We cannot use this version, because a loop can allocate fresh regions
   * in every iteration, so that comparing the non-widened states would always fail.<br>
   *
   * Therefore, we use the following version:<br>
   *
   * <pre>
   * if(wideningPoint)
   *   n := summarizeHeap(n);
   * if(n <= s)
   *   return null // no update
   * if(!wideningPoint)
   *   return (s + n)
   * else
   *   return numericWiden(s, s + summarizeHeap(n))
   * </pre>
   */
  @Override public FiniteDisjunction<D> addToState (FiniteDisjunction<D> newState, boolean isWideningPoint) {
    if (newState == this || newState.subsetOrEqual(this))
      return null;
    if (isWideningPoint) {
      return widen(newState);
    } else {
      return join(newState);
    }
  }

  @Override public AnalysisCtx getContext () {
    // context must be the same for all children, so we just pick the first
    return childState.get(0).getContext();
  }

  @Override public FiniteDisjunction<D> setContext (AnalysisCtx ctx) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.setContext(ctx));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  /* ***********
   * Operations from interface QueryChannel:
   *********** */


  @Override public SynthChannel getSynthChannel () {
    // cannot join synth channels, so we fall back to the top channel
    return new SynthChannel();
  }

  @Override public DebugChannel getDebugChannel () {
    // cannot join debug channels, so we fall back to the top channel
    return new DebugChannel();
  }

  @Override public final XMLBuilder toXML (XMLBuilder builder) {
    assert false : "implement";
    // builder = childState.toXML(builder);
    return builder;
  }

  @Override public final void toString (DomainStringBuilder builder) {
    for (D c : childState) {
      builder.append(NAME, "\nAlternative:\n");
      c.toString(builder);
    }
  }

  // only overwritten by wrapping domain
  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    if (compactPrinting) {
      toCompactString(builder);
    } else {
      for (D c : childState) {
        builder.append("\nAlternative:\n");
        builder.append(c.toString());
      }
    }
    return builder.toString();
  }

  @Override public final void toCompactString (StringBuilder builder) {
    for (D c : childState) {
      builder.append("\nAlternative:\n");
      c.toCompactString(builder);
    }
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    D first = it.next();
    if (!it.hasNext()) {
      first.varToCompactString(builder, var);
      return;
    }
    builder.append("(");
    first.varToCompactString(builder, var);
    while (it.hasNext()) {
      builder.append(" v ");
      it.next().varToCompactString(builder, var);
    }
    builder.append(")");
  }

  @Override public Range queryEdgeFlag (NumVar src, AddrVar tgt) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    Range result = it.next().queryEdgeFlag(src, tgt);
    while (it.hasNext()) {
      result = result.union(it.next().queryEdgeFlag(src, tgt));
    }
    return result;
  }

  @Override public FiniteDisjunction<D> assumeEdgeFlag (NumVar refVar, AddrVar target, BigInt value) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.assumeEdgeFlag(refVar, target, value));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public FiniteDisjunction<D> copyVariable (NumVar to, NumVar from) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.copyVariable(to, from));
      } catch (Unreachable _) {
      }
    return build(cs);

  }

  @Override public FiniteDisjunction<D> assumeEdgeNG (Rlin pointerVar, AddrVar targetAddr) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.assumeEdgeNG(pointerVar, targetAddr));
      } catch (Unreachable _) {
      }
    return build(cs);

  }

  @Override public FiniteDisjunction<D> expandNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.expandNG(p, e, nvps));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public FiniteDisjunction<D> concretizeAndDisconnectNG (AddrVar s, VarSet cs) {
    // TODO implement in FiniteDomain<FiniteDisjunction<D>>
    throw new UnimplementedException();

  }

  @Override public FiniteDisjunction<D> bendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts,
      VarSet ptc) {
    // TODO implement in FiniteDomain<FiniteDisjunction<D>>
    throw new UnimplementedException();

  }

  @Override public FiniteDisjunction<D> expandNG (ListVarPair nvps) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.expandNG(nvps));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public FiniteDisjunction<D> foldNG (ListVarPair nvps) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.foldNG(nvps));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public FiniteDisjunction<D> foldNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.foldNG(p, e, nvps));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public FiniteDisjunction<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs,
      VarSet pts, VarSet ptc) {
    LinkedList<D> cs = newcs();
    for (D c : childState)
      try {
        cs.add(c.bendGhostEdgesNG(summary, concrete, svs, cvs, pts, ptc));
      } catch (Unreachable _) {
      }
    return build(cs);
  }

  @Override public List<FiniteDisjunction<D>> enumerateAlternatives () {
    LinkedList<FiniteDisjunction<D>> cs = new LinkedList<>();
    for (D c : childState)
      cs.add(new FiniteDisjunction<D>(c));
    return cs;
  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // TODO implement in PrettyDomain
    throw new UnimplementedException();

  }

  @Override public FiniteDisjunction<D> assumeVarsAreEqual (int size, NumVar fst, NumVar snd) {
    // TODO implement in FiniteDomain<FiniteDisjunction<D>>
    throw new UnimplementedException();

  }
}
