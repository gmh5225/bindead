package bindead.domains.finitesupportset;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.Range;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.util.VarExtractor;
import bindead.data.Linear;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.combinators.FiniteFunctor;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.pointsto.PointsTo;
import bindead.exceptions.Unreachable;


/**
 * Support set tracking domain for the finite domains. Insert between domains to track variables and report
 * invariant violations during the domain operations.
 */
public final class FiniteSupportSet<D extends FiniteDomain<D>>
    extends FiniteFunctor<SupportSetState, D, FiniteSupportSet<D>> {

  public static final String NAME = "SUPPORTSET";

  public FiniteSupportSet (final D child) {
    super(NAME, SupportSetState.empty(), child);
  }

  public FiniteSupportSet (SupportSetState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public FiniteSupportSet<D> build (SupportSetState s, D cs) {
    // ugly, but necessary for now:
    if (cs instanceof PointsTo<?>) {
      PointsTo<?> cs2 = (PointsTo<?>) cs;
      VarSet x = cs2.state.getSupport();
      VarSet supp = s.getSupportSet();
      VarSet onlyInChild = x.difference(supp);
      VarSet onlyInParent = supp.difference(x);
      assert onlyInChild.isEmpty() : "only in child: " + onlyInChild;
      assert onlyInParent.isEmpty() : "only in parent: " + onlyInParent;
    }
    return new FiniteSupportSet<D>(s, cs);
  }

  @Override public FiniteSupportSet<D> copyAndPaste (VarSet vars, FiniteSupportSet<D> from) {
    state.haveNot(vars);
    from.state.have(vars);
    return build(state.with(vars), childState.copyAndPaste(vars, from.childState));
  }

  @SuppressWarnings("deprecation") @Override public List<P2<NumVar.AddrVar, FiniteSupportSet<D>>> deprecatedDeref (
      Rlin ptr, VarSet summaries) throws Unreachable {
    state.have(ptr);
    state.have(summaries);
    List<P2<AddrVar, FiniteSupportSet<D>>> alts =
      new LinkedList<P2<AddrVar, FiniteSupportSet<D>>>();
    for (P2<AddrVar, D> ca : childState.deprecatedDeref(ptr, summaries)) {
      AddrVar adr = ca._1();
      alts.add(new P2<AddrVar, FiniteSupportSet<D>>(adr, build(state, ca._2())));
    }
    return alts;
  }

  @Override public FiniteSupportSet<D> eval (Assign stmt) {
    state.have(VarExtractor.get(stmt));
    return build(state, childState.eval(stmt));
  }

  @Override public FiniteSupportSet<D> eval (Test test) {
    state.have(test.getVars());
    return build(state, childState.eval(test));
  }

  @Override public FiniteSupportSet<D> introduce (NumVar var, Type type, Option<BigInt> value) {
    state.haveNot(var);
    return build(state.with(var), childState.introduce(var, type, value));
  }

  @Override public FiniteSupportSet<D> project (NumVar var) {
    state.have(var);
    return build(state.without(var), childState.project(var));
  }

  @Override public SetOfEquations queryEqualities (NumVar var) {
    state.have(var);
    return childState.queryEqualities(var);
  }

  @Override public Range queryRange (Linear lin) {
    state.have(lin);
    return childState.queryRange(lin);
  }

  @Override public P3<SupportSetState, D, D> makeCompatible (FiniteSupportSet<D> other, boolean isWideningPoint) {
    state.sameAs(other.state);
    return P3.<SupportSetState, D, D>tuple3(state, childState, other.childState);
  }

  @Override public FiniteSupportSet<D> substitute (NumVar from, NumVar to) {
    state.have(from);
    state.haveNot(to);
    return build(state.without(from).with(to), childState.substitute(from, to));
  }

  @Override public FiniteSupportSet<D> copyVariable (NumVar to, NumVar from) {
    state.haveNot(to);
    state.have(from);
    return build(state.with(to), childState.copyVariable(to, from));
  }

  @Override public FiniteSupportSet<D> assumeEdgeNG (Rlin pointerVar, AddrVar targetAddr) {
    state.have(pointerVar);
    state.have(targetAddr);
    return build(state, childState.assumeEdgeNG(pointerVar, targetAddr));
  }

  @Override public FiniteSupportSet<D> expandNG (ListVarPair nvps) {
    SupportSetState s = expandOnState(nvps);
    D cs = childState.expandNG(nvps);
    return build(s, cs);
  }

  @Override public FiniteSupportSet<D> expandNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    state.have(p);
    SupportSetState s = expandOnState(nvps).with(e);
    D cs = childState.expandNG(p, e, nvps);
    return build(s, cs);
  }

  private SupportSetState expandOnState (List<VarPair> nvps) {
    SupportSetState s = state;
    for (VarPair vp : nvps) {
      s.have(vp.getPermanent());
      s.haveNot(vp.getEphemeral());
      s = s.with(vp.getEphemeral());
    }
    return s;
  }

  @Override public FiniteSupportSet<D> concretizeAndDisconnectNG (AddrVar s, VarSet cs) {
    D chs = childState.concretizeAndDisconnectNG(s, cs);
    return build(state, chs);
  }

  @Override public FiniteSupportSet<D> bendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts,
      VarSet ptc) {
    D cs = childState.bendBackGhostEdgesNG(s, c, svs, cvs, pts, ptc);
    return build(state, cs);

  }

  @Override public FiniteSupportSet<D> foldNG (ListVarPair nvps) {
    SupportSetState newState = state;
    for (VarPair vp : nvps) {
      state.have(vp.getPermanent());
      newState = newState.without(vp.getEphemeral());
    }
    return build(newState, childState.foldNG(nvps));
  }

  @Override public FiniteSupportSet<D> foldNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    state.have(p);
    SupportSetState newState = state.without(e);
    for (VarPair vp : nvps) {
      state.have(vp.getPermanent());
      newState = newState.without(vp.getEphemeral());
    }
    D newChild = childState.foldNG(p, e, nvps);
    return build(newState, newChild);

  }

  @Override public FiniteSupportSet<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs,
      VarSet pts, VarSet ptc) {
    D chs = childState.bendGhostEdgesNG(summary, concrete, svs, cvs, pts, ptc);
    return build(state, chs);
  }

  @Override public FiniteSupportSet<D> assumeVarsAreEqual (int size, NumVar fst, NumVar snd) {
    state.have(fst);
    state.have(snd);
    return build(state, childState.assumeVarsAreEqual(size, fst, snd));
  }

  /**
   * Returns a set of possible (abstract) target addresses without restricting the numeric state.
   */
  @Override public Collection<AddrVar> findPossiblePointerTargets (NumVar id) throws Unreachable {
    state.have(id);
    return childState.findPossiblePointerTargets(id);
  }

}
