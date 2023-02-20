package bindead.analyses.algorithms.data;

import static bindead.debug.StringHelpers.indentMultiline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javalx.data.Option;
import rreil.lang.RReilAddr;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.analyses.algorithms.data.Flows.FlowType;
import bindead.analyses.warnings.WarningsMap;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/**
 * The state space is the mapping from program points to states, where the state at each program point is actually the
 * state _before_ the program point, i.e. the incoming state.
 * Additionally, the warnings that were produced during the analysis are saved here, too.
 */
public class StateSpace<D extends RootDomain<D>> {
  private final boolean DEBUGBINARIES = AnalysisProperties.INSTANCE.debugBinaryOperations.isTrue();
  private final boolean DEBUGSUBSETOREQUAL = AnalysisProperties.INSTANCE.debugSubsetOrEqual.isTrue();
  private final boolean DEBUGWIDENING = AnalysisProperties.INSTANCE.debugWidening.isTrue();

  private final Map<ProgramPoint, D> states = new HashMap<>();
  private final Multimap<RReilAddr, ProgramPoint> stateSpaceForAddress = HashMultimap.create();
  private final Set<ProgramPoint> junctionPoints = new HashSet<>();
  private final Multimap<ProgramPoint, ProgramPoint> incomingEdges = HashMultimap.create();
  private final WarningsMap warningsMap = new WarningsMap();
  private final Multiset<ProgramPoint> iterationsCounter = HashMultiset.create();
  private final Multiset<ProgramPoint> wideningPoints = HashMultiset.create();

  /**
   * Set the state at a program location. Use only for bootstrapping an analysis,
   * as it bypasses all the dependency computation. During an analysis use
   * {@link #update(ProgramPoint, ProgramPoint, RootDomain, boolean)} instead.
   */
  public void setInitial (ProgramPoint point, D state) {
    assert get(point).isNone();
    putState(point, state);
  }

  /**
   * Set the state at a program location and update any necessary bookkeeping.
   */
  private void putState (ProgramPoint point, D state) {
    // the warnings channel needs to be reset for when the state will be read and evaluated again
    // otherwise we will pass on the warnings from previous program points
    final AnalysisCtx newCtx =
      new AnalysisCtx(Option.<ProgramPoint>some(point), state.getContext().getEnvironment(), new WarningsContainer());
    state = state.setContext(newCtx);
    states.put(point, state);
    iterationsCounter.add(point);
    stateSpaceForAddress.put(point.getAddress(), point);
  }

  /**
   * Update the state space with a new value.
   *
   * @param from The source of the transition
   * @param flow The flow (transition) type
   * @param to The target of the transition
   * @param newState The new state for {@code to}
   * @param useWidening If widening should be applied where necessary or never
   * @return {@code true} if the new state lead to an update or {@code false} if it was smaller than the old state
   */
  public boolean update (ProgramPoint from, FlowType flow, ProgramPoint to, D newState, boolean useWidening) {
    updateTransitionsTopology(from, to, flow);
    final D oldState = get(to).getOrNull();
    // cannot test here also for a junction as the back-edge might not lead into a junction
    final boolean isWideningPoint = useWidening && isBackedge(from, to);
    if (printDebugOutput(isWideningPoint))
      debugOldAndNewState(to, oldState, newState);
    if (isWideningPoint)
      wideningPoints.add(to);
    final D finalState;
    if (oldState == null) {
      // old state is bottom, no need to widen or join
      finalState = newState;
      if (printDebugOutput(isWideningPoint))
        System.out.println("  old state overwritten! Was bottom.");
    } else if (!isJunction(to) && !isWideningPoint) {
      // optimization to overwrite the old state in case we are not at a junction and not at a widening point
      // this has quite some impact of being about 3-5x faster
      finalState = newState;
      if (printDebugOutput(isWideningPoint))
        System.out.println("  old state overwritten! Was not a junction or widening point.");
    } else {
      if (printDebugOutput(isWideningPoint)) {
        final String action = isWideningPoint ? "widened" : "joined";
        System.out.println("  states " + action + ".");
      }
      finalState = oldState.addToState(newState, isWideningPoint);
      if (isWideningPoint && finalState != null) {
        // XXX bm: note that with the heap domain this invariant does not always hold.
        // The heap domain summarizes memory regions on widening so the result might be incomparable with the arguments
        // we would need to trigger here a summary on each state with itself before comparing it but that
        // is not possible currently.
        assert oldState.subsetOrEqual(finalState) : "Widened state is smaller than its arguments!";
        assert newState.subsetOrEqual(finalState) : "Widened state is smaller than its arguments!";
      }
      if (isWideningPoint && printDebugOutput(isWideningPoint)) {
        if (finalState == null)
          System.out.println("new state was smaller or equal to the old state.");
        else
          System.out.println("widened state:\n" + finalState);
      }

    }
    if (finalState == null)
      return false;
    debugWideningCounter(to, isWideningPoint);
    putWarnings(from, newState.getContext().getWarningsChannel());
    putState(to, finalState);
    return true;
  }

  private void debugWideningCounter (ProgramPoint point, boolean isWideningPoint) {
    if (isWideningPoint && DEBUGWIDENING) {
      final int timesWidenened = wideningPoints.count(point);
      System.out.println(AnalysisProperties.NAME + " (widening #" + timesWidenened + ")@" + point + ":");
    }
  }

  private boolean printDebugOutput (boolean isWideningPoint) {
    return DEBUGBINARIES || DEBUGSUBSETOREQUAL || DEBUGWIDENING && isWideningPoint;
  }

  private void debugOldAndNewState (ProgramPoint at, D oldState, D newState) {
    System.out.println();
    System.out.println(AnalysisProperties.NAME + " (updating state@" + at + "):");
    final String oldStateAsText = oldState != null ? oldState.toString() : "_|_";
    System.out.println(indentMultiline("  old-state: ", oldStateAsText) + "\n");
    System.out.println(indentMultiline("  new-state: ", newState.toString()) + "\n");
  }

  /**
   * Add an edge to the table of incoming edges. Thereby we track program points
   * that are junction points. Additionally there might be conditional jumps that jump
   * to the next instruction and thus we need to mark these as junctions even though
   * they have only one successor.
   *
   * @param pred the program point where the edge originates from
   * @param succ the target of the edge
   * @param flow The flow (transition) type
   */
  private void updateTransitionsTopology (ProgramPoint pred, ProgramPoint succ, FlowType flow) {
    // this is still needed as there might be two distinct program points (instructions) that end
    // in the same point and thus reach the successor without a jump
    incomingEdges.put(succ, pred);
    switch (flow) {
    case Call:
    case Jump:
    case Return:
      junctionPoints.add(succ);
      break;
    case Error:
    case Halt:
    case Next:
    default:
      break;
    }
  }

  private boolean isJunction (ProgramPoint point) {
    return incomingEdges.get(point).size() > 1 || junctionPoints.contains(point);
  }

  /**
   * Use a simple widening heuristic. If a path leads from a higher to a lower address then we have a backedge.
   */
  private static boolean isBackedge (ProgramPoint from, ProgramPoint to) {
    return from.getAddress().compareTo(to.getAddress()) > 0;
  }

  /**
   * Retrieve the state at a certain program location.
   */
  public Option<D> get (ProgramPoint point) {
    return Option.fromNullable(states.get(point));
  }

  /**
   * Retrieve the state space at a certain address, i.e. the program points
   * that are associated with the given address. Use these points to retrieve
   * a state for each of them using {@link #get(ProgramPoint)}.
   */
  public Set<ProgramPoint> get (RReilAddr address) {
    return (Set<ProgramPoint>) stateSpaceForAddress.get(address);
  }

  public WarningsMap getWarnings () {
    return warningsMap;
  }

  public void putWarnings (ProgramPoint point, WarningsContainer warnings) {
    // Only store non-empty warnings.
    if (warnings.isEmpty())
      return;
    this.warningsMap.put(point, iterationsCounter.count(point), warnings);
  }

  public Multiset<ProgramPoint> getWideningPoints () {
    return wideningPoints;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Size: ");
    builder.append(states.entrySet());
    builder.append("\n");
    for (Entry<ProgramPoint, D> entry : states.entrySet()) {
      ProgramPoint point = entry.getKey();
      builder.append(point);
      builder.append("\n");
    }
    return builder.toString();
  }
}