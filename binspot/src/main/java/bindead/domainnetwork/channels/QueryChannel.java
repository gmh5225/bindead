package bindead.domainnetwork.channels;

import javalx.numeric.Range;
import bindead.data.Linear;
import bindead.data.NumVar;

/**
 * An interface to retrieve information from a domain.
 * Not every domain implements all of these functions, only some are of interest.
 * They are meant as a way to access data throughout the domain stack.
 */
public interface QueryChannel {
  /**
   * Get the range valuation of a linear expression.
   *
   * @param expr A linear expression.
   * @return The value of the linear expression.
   */
  public Range queryRange (Linear expr);

  /**
   * Get the range valuation of a single variable.
   *
   * @param variable A variable.
   * @return The value of the variable.
   */
  // XXX hsi does this make sense for wrapped values? why no bitsize?
  public Range queryRange (NumVar variable);

  /**
   * Get the linear equalities that are known about the variable.
   * Note that this is not supposed to be a complete set of equalities as it depends on the domain
   * answering if all transitive equalities or equalities that would hold when tested appear in the list.
   * Mostly returned set will be a subset of all the existing equalities.
   *
   * @param variable A variable to know the linear equalities for.
   * @return A set of linear equalities.
   */
  public SetOfEquations queryEqualities (NumVar variable);

  /**
   * Get the synthesized channel containing information sent upwards by domains after transfer functions.
   * Contains e.g. new equalities that were inferred for variables since the last domain operation.
   *
   * @return A synthesized-channel object corresponding to this state.
   */
  public SynthChannel getSynthChannel ();

  /**
   * Get the debug channel containing detailed information from some domains.
   * Note that the interface of this channel is subject to frequent changes as it is only intended
   * for the debugging during the development of domains. Do not rely on this interface.
   *
   * @return A debugging-channel object corresponding to this state.
   */
  public DebugChannel getDebugChannel ();

}
