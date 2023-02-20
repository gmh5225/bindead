package bindead.domainnetwork.combinators;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.persistentcollections.AVLSet;
import rreil.lang.MemVar;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.FunctorDomain;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.exceptions.Unreachable;

/**
 * Abstract base class for domains that reside at the interface from finite to zeno domains.
 */
public abstract class FiniteZenoFunctor<S extends FunctorState, C extends ZenoDomain<C>, D extends FiniteZenoFunctor<S, C, D>>
    extends FunctorDomain<S, C, D>
    implements FiniteDomain<D> {

  protected FiniteZenoFunctor (String name, S state, C childState) {
    super(name, state, childState);
  }

  /* ***********
   * Operations from interface FiniteDomain:
   *
   * (A ZenoFunctor is only responsible for wrapping values, so everything except
   * test and assign operations is simply delegated to the child domain)
   *
   *********** */

  @Override public final D introduce (NumVar variable, Type type, Option<BigInt> value) {
    return build(state, childState.introduce(variable, type, value));
  }

  @Override public final D project (NumVar x) {
    return build(state, childState.project(VarSet.of(x)));
  }

  @Override public final D substitute (NumVar x, NumVar y) {
    C newChildState = childState.substitute(x, y);
    return build(state, newChildState);
  }

  @Override public final D copyAndPaste (VarSet vars, D from) {
    return build(state, childState.copyAndPaste(vars, from.childState));
  }

  @Override public final D assumeConcrete (NumVar var) {
    throw new UnimplementedException("this call should be caught by the PointsTo domain");
  }

  @Override public final List<P2<AddrVar, D>>
      deprecatedDeref (Rlin ptr, VarSet summaries) {
    throw new UnimplementedException("This call has to be caught by the points-to domain");
  }

  @Override public final P2<AVLSet<AddrVar>, D> deref (Rlin ptr)
      throws Unreachable {
    throw new UnimplementedException("This call has to be caught by the points-to domain");
  }

  @Override public final D assumePointsToAndConcretize (Rlin refVar, AddrVar target, VarSet contents) {
    throw new UnimplementedException("This call has to be caught by the points-to domain");
  }

  @Override public final List<AddrVar> findPossiblePointerTargets (NumVar id) throws Unreachable {
    throw new UnimplementedException("This call has to be caught by the points-to domain");
  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // TODO implement in PrettyDomain
    throw new UnimplementedException();

  }
}
