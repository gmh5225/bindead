package bindead.domainnetwork.combinators;

import java.util.Collection;
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
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.FunctorDomain;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.exceptions.Unreachable;

/**
 * Abstract base class for {@link bindead.domainnetwork.interfaces.numeric.FiniteDomain}s used as
 * cardinal power domains (functor domains).
 */
public abstract class FiniteFunctor<S extends FunctorState, C extends FiniteDomain<C>, D extends FiniteFunctor<S, C, D>>
    extends FunctorDomain<S, C, D>
    implements FiniteDomain<D> {

  protected FiniteFunctor (String name, S state, C childState) {
    super(name, state, childState);
  }

  /* ***********
   * Operations from interface FiniteDomain:
   *********** */

  @SuppressWarnings("deprecation") @Override public List<P2<NumVar.AddrVar, D>> deprecatedDeref (
      Rlin ptr, VarSet summaries) throws Unreachable {
    return wrapChildDerefsWithState(childState.deprecatedDeref(ptr, summaries));
  }

  private List<P2<NumVar.AddrVar, D>> wrapChildDerefsWithState (List<P2<NumVar.AddrVar, C>> newChildStates) {
    List<P2<NumVar.AddrVar, D>> res = new LinkedList<P2<NumVar.AddrVar, D>>();
    for (P2<NumVar.AddrVar, C> tuple : newChildStates)
      res.add(new P2<NumVar.AddrVar, D>(tuple._1(), build(state, tuple._2())));
    return res;
  }

  @Override public D introduce (NumVar variable, Type type, Option<BigInt> value) {
    C newChildState = childState.introduce(variable, type, value);
    return build(state, newChildState);
  }

  @Override public D assumeConcrete (NumVar var) {
    final C newChildState = childState.assumeConcrete(var);
    return build(state, newChildState);
  }

  @Override public D assumePointsToAndConcretize (Rlin refVar, AddrVar target, VarSet contents) {
    final C newChildState = childState.assumePointsToAndConcretize(refVar, target, contents);
    return build(state, newChildState);
  }

  @Override public P2<AVLSet<AddrVar>, D> deref (Rlin ptr)
      throws Unreachable {
    P2<AVLSet<AddrVar>, C> p2 = childState.deref(ptr);
    return new P2<AVLSet<AddrVar>, D>(p2._1(), build(state, p2._2()));
  }

  @Override public Range queryEdgeFlag (NumVar src, AddrVar tgt) {
    return childState.queryEdgeFlag(src, tgt);
  }


  @Override public D assumeEdgeFlag (NumVar refVar, AddrVar target, BigInt value) {
    final C newChildState = childState.assumeEdgeFlag(refVar, target, value);
    return build(state, newChildState);
  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // TODO implement in PrettyDomain
    throw new UnimplementedException();

  }

  final protected D testEqual (int size, NumVar fst, NumVar snd) {
    bindead.abstractsyntax.finite.Finite.Test t = FiniteFactory.getInstance().equalTo(size, fst, snd);
    return eval(t);
  }

  /**
   * Returns a set of possible (abstract) target addresses without restricting the numeric state.
   */
  @Override public Collection<AddrVar> findPossiblePointerTargets (NumVar id) throws Unreachable {
//    throw new UnimplementedException();
    return childState.findPossiblePointerTargets(id);
  }

}
