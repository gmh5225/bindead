package bindead.domainnetwork.combinators;


import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.FiniteRange;
import javalx.numeric.Range;
import rreil.lang.MemVar;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rval;
import bindead.analyses.RReilRunner;
import bindead.data.NumVar;
import bindead.debug.XmlPrintHelpers;
import bindead.domainnetwork.channels.FunctorDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RootDomain;

/**
 * Abstract base class for domains that reside at the interface from root to memory domains.
 */
public abstract class RootMemoryFunctor<S extends FunctorState, C extends MemoryDomain<C>, D extends RootMemoryFunctor<S, C, D>>
    extends FunctorDomain<S, C, D> implements RootDomain<D> {

  protected RootMemoryFunctor (String name, S state, C childState) {
    super(name, state, childState);
  }

  @SuppressWarnings("unused") private final P2<D, D> makeCompatibleForAxel (D other, boolean isWideningPoint) {
    D s2 = other;
    P3<S, C, C> t = makeCompatible(s2, isWideningPoint);
    return P2.<D, D>tuple2(build(t._1(), t._2()), build(t._1(), t._3()));
  }

  @Override public final Range queryRange (Rval value) {
    return childState.queryRange(value);
  }

  @Override public final Range queryRange (Lin value) {
    return childState.queryRange(value);
  }

  @Override public final Range queryRange (MemVar region, FiniteRange key) {
    return childState.queryRange(region, key);
  }

  @SuppressWarnings("unchecked") @Override final public D eval (String... instructions) {
    return RReilRunner.eval((D) this, instructions);
  }

  @Override public String toXml () {
    return XmlPrintHelpers.asString(name, this);
  }

  @Override public Option<NumVar> resolveVariable (MemVar region, FiniteRange bits) {
    return childState.pickSpecificField(region, bits);
  }

}
