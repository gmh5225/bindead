package bindead.domainnetwork.interfaces;

import java.util.LinkedList;
import java.util.List;

import javalx.exceptions.UnimplementedException;
import rreil.lang.MemVar;
import bindead.data.NumVar;
import bindead.domainnetwork.channels.DebugChannel;
import bindead.domainnetwork.channels.Domain;
import bindead.domainnetwork.channels.SynthChannel;

public abstract class ZenoHeadDomain<D extends ZenoHeadDomain<D>> extends Domain<D> implements ZenoDomain<D> {
  // we only need to hold the context in the head of the hierarchy, but subclasses need to pass it on
  // when set via #setContext()
  private final AnalysisCtx analysisContext;

  public ZenoHeadDomain (String name, AnalysisCtx ctx) {
    super(name);
    analysisContext = ctx;
  }

  /**
   * Currently, this is a no-op on all Zeno domains. Should never be called as it is
   * caught in FiniteZenoFunctor.
   */
  @Override public D assumeConcrete (NumVar var) {
    throw new UnimplementedException("should never be called");
  }

  @Override public AnalysisCtx getContext () {
    return analysisContext;
  }

  @Override public D setContext (AnalysisCtx ctx) {
    throw new UnimplementedException(
      "Needs to be implemented in subclassers, passing the context through the constructor.");
  }

  /**
   * Empty implementation for convenience. Override in subclasses if debug information should be propagated.<br>
   * <br>
   * {@inheritDoc}
   */
  @Override public DebugChannel getDebugChannel () {
    return new DebugChannel();
  }

  /**
   * Empty implementation for convenience. Override in subclasses if synthesized information should be propagated.<br>
   * <br>
   * {@inheritDoc}
   */
  @Override public SynthChannel getSynthChannel () {
    return new SynthChannel();
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    builder.append(var);
  }

  @Override public void toCompactString (StringBuilder builder) {
    builder.append(toString());
  }

  @SuppressWarnings("unchecked")
  @Override public List<D> enumerateAlternatives () {
    List<D> ds = new LinkedList<D>();
    ds.add((D)this);
    return ds;
  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // TODO implement in PrettyDomain
    throw new UnimplementedException();

  }
}
