package bindead.domainnetwork.interfaces;

import java.util.List;

/**
 * Operations needed for a semi-lattice and methods for our fixpoint.
 *
 * @param <S> The self type of the lattice
 */
public interface SemiLattice<S extends SemiLattice<S>> {
  /**
   * The partial order operation between {@code this} and {@code other} domains.
   *
   * @param other A domain with which to compare this domain.
   * @return {@code true} if this domain is less or equal to the other domain.
   */
  public boolean subsetOrEqual (S other);

  /**
   * The join (least-upper-bound) operation between {@code this} and {@code other} domains.
   *
   * @param other A domain with which to join this domain.
   * @return The join of both domains.
   */
  public S join (S other);

  /**
   * The widening operation between {@code this} and {@code other} domains,
   * where the {@code other} domain is the bigger one.
   *
   * @param other A domain with which to widen this domain.
   * @return The widened domain.
   */
  public S widen (S other);

  /**
   * Enlarge the state space with another state space.
   *
   * Implementation in class Domain shows how {@link #addToState(SemiLattice, boolean)} and join/widen/subsetOrEqual
   * can be implemented in terms of each other.
   *
   * @param newState The new state that is to be added to this state
   * @param isWideningPoint If widening should be applied after joining this state with the new state
   * @return the joined (or widened, if requested) state or {@code null} if the new state was smaller or equal this state
   */
  public abstract S addToState (S newState, boolean isWideningPoint);

  /**
   * Get the context of the current analysis.
   */
  public AnalysisCtx getContext ();

  /**
   * Add some context of the current analysis to this domain.
   *
   * @return A new domain with the given analysis context.
   */
  public S setContext (AnalysisCtx ctx);

  public List<S> enumerateAlternatives ();


}
