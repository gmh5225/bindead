package bindead.domains.sat;

import javalx.data.Option;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.data.FoldMap;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.FiniteFunctor;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.exceptions.Unreachable;

public final class Sat<D extends FiniteDomain<D>>
    extends FiniteFunctor<SatState, D, Sat<D>> {
  public static final String NAME = "SAT";

  public Sat (final D child) {
    super(NAME, SatState.empty(), child);
  }

  private Sat (SatState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public Sat<D> build (SatState state, D childState) {
    return new Sat<D>(state, childState);
  }

  @Override public Sat<D> eval (Assign stmt) {
    System.out.println("sat.eval(" + stmt + ")");
    final SatStateBuilder builder = new SatStateBuilder(state);
    // D newChildState = builder.applyChildOps(childState);
    builder.eval(stmt);
    SatState b = builder.build();
    D c = childState.eval(stmt);
    return build(b, c);
  }

  @Override public Sat<D> eval (Test test)
      throws Unreachable {
    final SatStateBuilder builder = new SatStateBuilder(state);
    // D newChildState = builder.applyChildOps(childState);
    builder.eval(test);
    return build(builder.build(), childState.eval(test));
  }

  @Override public Sat<D> join (Sat<D> other) {
    final SatStateBuilder l = new SatStateBuilder(state);
    final SatStateBuilder r = new SatStateBuilder(other.state);
    l.joinWith(r);
    return build(l.build(), childState.join(other.childState));
  }

  @Override public Sat<D> widen (Sat<D> other) {
    final SatStateBuilder l = new SatStateBuilder(state);
    final SatStateBuilder r = new SatStateBuilder(other.state);
    l.widenWith(r);
    return build(l.build(), childState.widen(other.childState));
  }

  @Override public boolean subsetOrEqual (Sat<D> other) {
    final SatStateBuilder builder = new SatStateBuilder(state);
    boolean thissse = builder
        .subsetOrEqual(new SatStateBuilder(other.state));
    boolean childsse = childState.subsetOrEqual(other.childState);
    return thissse || childsse;
  }

  @Override public P3<SatState, D, D> makeCompatible (Sat<D> other,
      boolean isWideningPoint) {
    // TODO hsi implement
    throw new UnimplementedException();
  }


  @Override public Sat<D> copyAndPaste (VarSet vars, Sat<D> from) {
    SatStateBuilder builder = new SatStateBuilder(state);
    builder.copyAndPaste(vars, from.state);
    return build(builder.build(),
        childState.copyAndPaste(vars, from.childState));
  }

  @Override public Sat<D> project (NumVar variable) {
    SatStateBuilder builder = new SatStateBuilder(state);
    builder.remove(variable);
    return build(builder.build(), childState.project(variable));
  }

  @Override public Sat<D> introduce (NumVar variable, Type type, Option<BigInt> value) {
    System.out.println("introduce: " + variable + " :: " + type + " = "
      + value);
    SatStateBuilder builder = new SatStateBuilder(state);
    // D newChildState = childState.introduce(variable, type, value);
    if (type == Type.Bool) {
      builder.introduce(variable, value);
    }
    return build(builder.build(),
        childState.introduce(variable, type, value));
  }

  @Override public Sat<D> substitute (NumVar y, NumVar x) {
    final SatStateBuilder builder = new SatStateBuilder(state);
    builder.substitute(y, x);
    // D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), childState.substitute(y, x));
  }


  @Override public Sat<D> copyVariable (NumVar to, NumVar from) {
    // TODO implement in FiniteDomain<Sat<D>>
    throw new UnimplementedException();

  }

  @Override public Sat<D> assumeEdgeNG (Rlin pointerVar, AddrVar targetAddr) {
    // TODO implement in FiniteDomain<Sat<D>>
    throw new UnimplementedException();

  }

  @Override public Sat<D> concretizeAndDisconnectNG (AddrVar s, VarSet cs) {
    // TODO implement in FiniteDomain<Sat<D>>
    throw new UnimplementedException();

  }

  @Override public Sat<D> bendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    // TODO implement in FiniteDomain<Sat<D>>
    throw new UnimplementedException();

  }

  @Override public Sat<D> expandNG (ListVarPair nvps) {
    SatStateBuilder builder = new SatStateBuilder(state);
    builder.expand(FoldMap.fromList(nvps));
    return build(builder.build(), childState.expandNG(nvps));
  }

  @Override public Sat<D> expandNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    SatStateBuilder builder = new SatStateBuilder(state);
    builder.expand(FoldMap.fromList(nvps));
    return build(builder.build(), childState.expandNG(p,e,nvps));
  }

  @Override public Sat<D> foldNG (ListVarPair nvps) {
    SatStateBuilder builder = new SatStateBuilder(state);
    builder.fold(FoldMap.fromList(nvps));
    return build(builder.build(), childState.foldNG(nvps));

  }

  @Override public Sat<D> foldNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    SatStateBuilder builder = new SatStateBuilder(state);
    builder.fold(FoldMap.fromList(nvps));
    return build(builder.build(), childState.foldNG(p, e, nvps));
  }

  @Override public Sat<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    // TODO implement in FiniteDomain<Sat<D>>
    throw new UnimplementedException();

  }

  @Override public Sat<D> assumeVarsAreEqual (int size, NumVar fst, NumVar snd) {
    // TODO implement in FiniteDomain<Sat<D>>
    throw new UnimplementedException();

  }
}
