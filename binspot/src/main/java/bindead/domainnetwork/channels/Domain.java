package bindead.domainnetwork.channels;

import static bindead.debug.StringHelpers.indentMultiline;
import javalx.numeric.Range;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.SemiLattice;
import bindead.exceptions.Unreachable;

public abstract class Domain<D extends Domain<D>> implements SemiLattice<D>, QueryChannel, PrettyDomain {
  private final boolean DEBUGBINARIES = AnalysisProperties.INSTANCE.debugBinaryOperations.isTrue();
  private final boolean DEBUGSUBSETOREQUAL = AnalysisProperties.INSTANCE.debugSubsetOrEqual.isTrue();
  private final boolean DEBUGWIDENING = AnalysisProperties.INSTANCE.debugWidening.isTrue();
  protected final String name;

  protected Domain (String name) {
    this.name = name;
  }

  /**
   * Convenience method to join two domain states that can be {@code null} (i.e. bottom).
   * Returns {@code null} if both states are {@code null}. Thus clients need to handle
   * the {@code null} return and throw {@link Unreachable} if necessary.
   */
  public static <D extends SemiLattice<D>> D joinNullables (D first, D second) {
    if (first == null && second == null)
      return null;
    if (first == null)
      return second;
    if (second == null)
      return first;
    return first.join(second);
  }

  @Override public final Range queryRange (NumVar variable) {
    return queryRange(Linear.linear(variable));
  }

  @Override public D addToState (D newState, boolean isWideningPoint) {
    return addToStateWithLatticeOps(newState, isWideningPoint);
  }

  /**
   * Perform the numeric join or widening which is implemented as<br>
   *
   * <pre>
   *   if (n ⊑ s)
   *     return null
   *  else
   *     return numericWiden(s, s ⊔ n)
   * </pre>
   *
   * @param newState The new state that is to be added to this state
   * @param isWideningPoint If widening should be applied after joining this state with the new state
   * @return The joined or widened domain.
   */
  protected final D addToStateWithLatticeOps (D newState, boolean isWideningPoint) {
    if (newState == this) {
      debugSubsetOrEqual(isWideningPoint, true);
      return null;
    }
    @SuppressWarnings("unchecked")
    D thisD = (D) this;
    final boolean isSubsetOrEqual = newState.subsetOrEqual(thisD);
    debugSubsetOrEqual(isWideningPoint, isSubsetOrEqual);
    if (isSubsetOrEqual) {
      return null;
    }
    final D joinedState = join(newState);
    if (DEBUGBINARIES)
      debugJoined(joinedState);
    if (!isWideningPoint)
      return joinedState;
    final D widened = widen(joinedState);
    if (DEBUGWIDENING)
      debugWidened(widened);
    return widened;
  }

  protected void debugWidened (final D finalState) {
    System.out.println();
    System.out.println(indentMultiline("  widened:   ", finalState.toString()) + "\n");
  }

  protected void debugJoined (final D joinedState) {
    System.out.println();
    System.out.println(indentMultiline("  joined:    ", joinedState.toString()) + "\n");
  }

  protected void debugSubsetOrEqual (boolean isWideningPoint, boolean isSubsetOrEqual) {
    if (!DEBUGSUBSETOREQUAL)
      return;
    System.out.println();
    System.out.println(AnalysisProperties.NAME + " (subset-or-equal):");
    System.out.println("  subset-or-equal: " + isSubsetOrEqual);
    System.out.println("  widening: " + isWideningPoint);
  }
}
