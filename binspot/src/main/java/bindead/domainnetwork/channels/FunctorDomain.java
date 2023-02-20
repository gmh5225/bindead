package bindead.domainnetwork.channels;

import java.util.LinkedList;
import java.util.List;

import javalx.data.products.P3;
import javalx.numeric.Range;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.DomainPrintHelpers;
import bindead.debug.DomainPrintProperties;
import bindead.debug.DomainStringBuilder;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.SemiLattice;

import com.jamesmurty.utils.XMLBuilder;

/**
 * A functor domain implements the functionality of the parent and child domain construction.
 *
 * @param <S> the internal state of the functor domain
 * @param <C> the child domain, which has to be a SemiLattice (so that we can join states by joining their child)
 * @param <D> the domain itself, the self-type (so that we can perform type-safe binary operations on sub-classed)
 */
public abstract class FunctorDomain<S extends FunctorState, C extends SemiLattice<C> & QueryChannel & PrettyDomain, D extends FunctorDomain<S, C, D>>
    extends Domain<D> {
  private final boolean compactPrinting = DomainPrintProperties.INSTANCE.printCompact.isTrue();

  public final S state;
  public final C childState;

  public FunctorDomain (String name, S state, C child) {
    super(name);
    this.state = state;
    this.childState = child;
  }

  public abstract D build (S ctx, C childState);

  /**
   * Make the support set of this and other domain compatible. During this process it might be
   * necessary to introduce or project variables from the child domains, thus the method returns the modified
   * children of both domains.
   * FIXME: Are the children still possibly incompatible after this method? And how does that fit with
   * the default implementations of join/widen/subset below?
   *
   * hsi: No, the returned children of type C have the same support set. That's the purpose of making them compatible,
   * because then operations join/widen/subset of the functor domain can simply be performed on the children.
   *
   */
  public abstract P3<S, C, C> makeCompatible (D other, boolean isWideningPoint);

  /* ***********
   * Operations from interface SemiLattice:
   *********** */

  // TODO say why these three functions are redundant

  @Override public D join (D other) {
    P3<S, C, C> p3 = makeCompatible(other, false);
    return build(p3._1(), p3._2().join(p3._3()));
  }

  @Override public D widen (D other) {
    P3<S, C, C> p3 = makeCompatible(other, true);
    return build(p3._1(), p3._2().widen(p3._3()));
  }

  @Override public boolean subsetOrEqual (D other) {
    P3<S, C, C> p3 = makeCompatible(other, false);
    return p3._2().subsetOrEqual(p3._3());
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
  @Override public D addToState (D newState, boolean isWideningPoint) {
    if (newState == this) {
      debugSubsetOrEqual(isWideningPoint, true);
      return null;
    }
    final P3<S, C, C> states = makeCompatible(newState, isWideningPoint);
    C collectedChild = states._2().addToState(states._3(), isWideningPoint);
    if (collectedChild == null)
      return null;
    return build(states._1(), collectedChild);
  }

  @Override public AnalysisCtx getContext () {
    return childState.getContext();
  }

  @Override public D setContext (AnalysisCtx ctx) {
    return build(state, childState.setContext(ctx));
  }

  /* ***********
   * Operations from interface QueryChannel:
   *********** */

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    return childState.queryEqualities(variable);
  }

  @Override public Range queryRange (Linear lin) {
    return childState.queryRange(lin);
  }

  @Override public SynthChannel getSynthChannel () {
    SynthChannel channel = childState.getSynthChannel();
    return channel.removeVariables(localSubset(channel.getVariables()));
  }

  @Override public DebugChannel getDebugChannel () {
    return childState.getDebugChannel();
  }

  /**
   * If a functor domain uses the default {@link querySynth} implementation
   * then this function is called to check with of the passed-in variables are
   * local to the functor domain. A functor domain that introduces its own
   * variables must override this method by one which returns the subset of the
   * input variables that are only known within this domain. This subset is
   * then removed from all information propagated up the synthesized channel.
   *
   * @param toTest the variable set in the synthesized channel
   * @return the subset of {@code toTest} that are owned by this functor domain
   */
  // Default: a domain does not hold local variables
  // REFACTOR hsi: too complicated, just ask domain via something like (bool exportEqualitiedFor(var))
  public VarSet localSubset (VarSet toTest) {
    return VarSet.empty();
  }

  @Override public final XMLBuilder toXML (XMLBuilder builder) {
    builder = state.toXML(builder);
    builder = childState.toXML(builder);
    return builder;
  }

  @Override public final void toString (DomainStringBuilder builder) {
    builder.append(name, DomainPrintHelpers.printState(name, state));
    childState.toString(builder);
  }

  // only overwritten by domains that do not have a state, e.g. wrapping domain
  @Override public String toString () {
    if (compactPrinting) {
      StringBuilder builder = new StringBuilder();
      toCompactString(builder);
      return builder.toString();
    } else {
      return DomainPrintHelpers.printState(name, state) + childState;
    }
  }

  @Override public void toCompactString (StringBuilder builder) {
    state.toCompactString(name, builder, childState);
    childState.toCompactString(builder);
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    childState.varToCompactString(builder, var);
  }

  @Override public List<D> enumerateAlternatives () {
    List<D> alts = new LinkedList<D>();
    List<C> cs = childState.enumerateAlternatives();
    for (C c : cs)
      alts.add(build(state, c));
    return alts;
  }
}
