package bindead.domainnetwork.combinators;

import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.FiniteRange;
import rreil.lang.MemVar;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.NumVar;
import bindead.domainnetwork.channels.FunctorDomain;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.MemoryDomain;

/**
 * Abstract base class for domain functors that reside at the interface from memory to the finite domains.
 */
public abstract class MemoryFiniteFunctor<S extends FunctorState, C extends FiniteDomain<C>, D extends MemoryFiniteFunctor<S, C, D>>
    extends FunctorDomain<S, C, D> implements MemoryDomain<D> {

  private static final FiniteFactory fin = FiniteFactory.getInstance();

  protected MemoryFiniteFunctor (String name, S state, C childState) {
    super(name, state, childState);
  }

  /* ***********
   * Operations from interface MemoryDomain:
   *********** */

  @Override public final D evalFiniteAssign (Finite.Lhs lhs, Finite.Rhs rhs) {
    C newChildState = childState.eval(fin.assign(lhs, rhs));
    return build(state, newChildState);
  }

  @Override public final D eval (Finite.Test test) {
    C newChildState = childState.eval(test);
    return build(state, newChildState);
  }

  @Override public final D introduce (NumVar numericVariable, Type type, Option<BigInt> value) {
    C newChildState = childState.introduce(numericVariable, type, value);
    return build(state, newChildState);
  }

  @Override public final D project (NumVar variable) {
    C newChildState = childState.project(variable);
    return build(state, newChildState);
  }

  @Override public D substitute (NumVar from, NumVar to) {
    return build(state, childState.substitute(from, to));
  }

  @Override public Option<NumVar> resolveVariable (MemVar region, FiniteRange bits) {
    return pickSpecificField(region, bits);
  }

}
