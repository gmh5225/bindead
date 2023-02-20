package bindead.domainnetwork.combinators;

import javalx.data.Option;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import rreil.lang.MemVar;
import rreil.lang.util.Type;
import bindead.data.FoldMap;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.FunctorDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.ZenoDomain;

/**
 * Abstract base class for {@link bindead.domainnetwork.interfaces.numeric.ZenoDomain}s
 * used as cardinal power domains.
 *
 * @param <S> the type of the state for this domain
 * @param <C> the type of the child domain
 * @param <D> The self-type of the domain.
 */
public abstract class ZenoFunctor<S extends FunctorState, C extends ZenoDomain<C>, D extends ZenoFunctor<S, C, D>>
    extends FunctorDomain<S, C, D> implements ZenoDomain<D> {

  protected ZenoFunctor (String name, S state, C childState) {
    super(name, state, childState);
  }

  @Override public D introduce (NumVar variable, Type type, Option<BigInt> value) {
    C newChildState = childState.introduce(variable, type, value);
    return build(state, newChildState);
  }

  @Override public D expand (FoldMap vars) {
    C newChildState = childState.expand(vars);
    return build(state, newChildState);
  }

  @Override public D fold (FoldMap vars) {
    C newChildState = childState.fold(vars);
    return build(state, newChildState);
  }

  @Override public D copyAndPaste (VarSet vars, D other) {
    C newChildState = childState.copyAndPaste(vars, other.childState);
    return build(state, newChildState);
  }

  /**
   * Currently, this is a no-op on all Zeno domains. Should never be called as it is
   * caught in FiniteZenoFunctor.
   */
  @Override public D assumeConcrete (NumVar var) {
    throw new UnimplementedException("should never be called");
  }

  @Override public P3<S, C, C> makeCompatible (D other, boolean isWideningPoint) {
    // not sensible for Zeno domains, bypassed via addToStateWithLatticeOps
    throw new UnimplementedException();
  }

  @Override public D addToState (D newState, boolean isWideningPoint) {
    return addToStateWithLatticeOps(newState, isWideningPoint);
  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // not sensible for Zeno domains, should be caught by Fields domain
    throw new UnimplementedException();

  }
}
