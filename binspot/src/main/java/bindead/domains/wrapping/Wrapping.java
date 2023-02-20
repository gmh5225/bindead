package bindead.domains.wrapping;

import javalx.data.Option;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Range;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.FoldMap;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.FiniteZenoFunctor;
import bindead.domainnetwork.combinators.VoidDomainState;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.exceptions.Unreachable;

/**
 * A domain that performs the translation between finite and infinite integer arithmetic by wrapping around values
 * of variables that do not fit in the range of their associated size.
 *
 * @param <D> The type of the child domain
 */
public class Wrapping<D extends ZenoDomain<D>> extends FiniteZenoFunctor<VoidDomainState, D, Wrapping<D>> {
  public static final String NAME = "WRAPPING";
  private final static FiniteFactory fin = FiniteFactory.getInstance();

  public Wrapping (D child) {
    super(NAME, VoidDomainState.empty(), child);
  }

  private Wrapping (VoidDomainState state, D child) {
    super(NAME, state, child);
  }

  /* ***********
   * Operations from class FunctorDomain:
   *********** */

  @Override public Wrapping<D> build (VoidDomainState ctx, D childState) {
    return new Wrapping<D>(ctx, childState);
  }

  @Override public P3<VoidDomainState, D, D> makeCompatible (Wrapping<D> other, boolean isWideningPoint) {
    return P3.<VoidDomainState, D, D>tuple3(state, childState, other.childState);
  }

  /* ***********
   * Operations from interface FiniteDomain:
   *********** */

  @Override public Wrapping<D> eval (Finite.Assign stmt) {
    WrappingStateBuilder builder = new WrappingStateBuilder(childState);
    builder.runAssign(stmt, builder);
    D newChildState = builder.applyChildOps(childState);
    return build(state, newChildState);
  }

  @Override public Wrapping<D> eval (Test test) throws Unreachable {
    WrappingStateBuilder builder = new WrappingStateBuilder(childState);
    builder.runTest(test);
    D newChildState = builder.applyChildOps(childState);
    return build(state, newChildState);
  }

  @Override final public String toString () {
    // wrapping does not have a state itself so ignore it in the output
    return childState.toString();
  }

  @Override public Range queryEdgeFlag (NumVar src, AddrVar tgt) {
    throw new UnimplementedException("should be caught in PointsTo domain");
  }

  @Override public Wrapping<D> assumeEdgeFlag (NumVar refVar, AddrVar target, BigInt value) {
    throw new UnimplementedException("should be caught in PointsTo domain");
  }

  @Override public Wrapping<D> copyVariable (NumVar to, NumVar from) {
    Wrapping<D> d = introduce(to, Type.Zeno, Option.<BigInt>none());
    Assign stmt = fin.assign(fin.variable(0, to), fin.linear(0, from));
    d = d.eval(stmt);
    return d;
  }

  @Override public Wrapping<D> assumeEdgeNG (Rlin pointerVar, AddrVar targetAddr) {
    throw new UnimplementedException("must be caught in PointsTo domain");
  }

  @Override public Wrapping<D> expandNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    ListVarPair withAddresses = new ListVarPair(nvps);
    withAddresses.add(new VarPair(p, e));
    return expandNG(withAddresses);
  }

  @Override public Wrapping<D> expandNG (ListVarPair nvps) {
    D newChildState = childState.expand(FoldMap.fromList(nvps));
    return build(state, newChildState);
  }

  @Override public Wrapping<D> concretizeAndDisconnectNG (AddrVar s, VarSet cs) {
    // should be caught in PointsTo
    throw new UnimplementedException();
  }

  @Override public Wrapping<D> bendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    throw new UnimplementedException("should be caught in PointsTo");

  }


  @Override public final Wrapping<D> foldNG (ListVarPair nvps) {
    D folded = childState.fold(FoldMap.fromList(nvps));
    return build(state, folded);
  }


  @Override public Wrapping<D> foldNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    FoldMap fromList = FoldMap.fromList(nvps);
    fromList.add(p, e);
    D folded = childState.fold(fromList);
    return build(state, folded);

  }

  @Override public Wrapping<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts,
      VarSet ptc) {
    throw new UnimplementedException("should be caught in PointsTo domain");
  }

  @Override public Wrapping<D> assumeVarsAreEqual (int size, NumVar fst, NumVar snd) {
    bindead.abstractsyntax.finite.Finite.Test t = FiniteFactory.getInstance().equalTo(size, fst, snd);
    return eval(t);
  }
}
