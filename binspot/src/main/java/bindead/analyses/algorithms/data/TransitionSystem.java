package bindead.analyses.algorithms.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import rreil.lang.RReilAddr;
import bindead.domainnetwork.interfaces.ProgramPoint;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A system that stores the intra- and interprocedural control flow of a program.
 *
 * @author Bogdan Mihaila
 */
public class TransitionSystem {
  /**
   * For each callstring the transitions in the tracked procedure.
   */
  private final Map<CallString, ProceduralTransitions> procedures = new HashMap<>();
  /**
   * All the callstrings that occur for a procedure given by its address.
   */
  private final Multimap<RReilAddr, CallString> callstringsForProcedure = HashMultimap.create();
  /**
   * All the call-sites known for a procedure given by its address.
   */
  private final Multimap<RReilAddr, RReilAddr> callsites = HashMultimap.create();
  /**
   * Potential return-sites known for a procedure given by its address.
   * A return site is the instruction following the call instruction.
   */
  private final Multimap<RReilAddr, RReilAddr> returnsites = HashMultimap.create();
  /**
   * This are the over-approximated successors without taking the callstring into account.
   */
  private final Multimap<RReilAddr, RReilAddr> flatSuccessors = HashMultimap.create();

  public static class ProceduralTransitions {

    private final Multimap<ProgramPoint, ProgramPoint> localTransitions = HashMultimap.create();
    private final Multimap<ProgramPoint, ProgramCtx> calls = HashMultimap.create();
    private final Multimap<ProgramPoint, ProgramCtx> returns = HashMultimap.create();

    public void addLocalTransition (ProgramPoint from, ProgramPoint to) {
      localTransitions.put(from, to);
    }

    public void addCall (ProgramCtx from, ProgramCtx to) {
      calls.put(from, to);
    }

    public void addReturn (ProgramCtx from, ProgramCtx to) {
      returns.put(from, to);
    }

    public Multimap<ProgramPoint, ProgramPoint> getLocalTransitions () {
      return localTransitions;
    }

    public Multimap<ProgramPoint, ProgramCtx> getCalls () {
      return calls;
    }

    public Multimap<ProgramPoint, ProgramCtx> getReturns () {
      return returns;
    }

  }

  private ProceduralTransitions getProcedureOrFresh (CallString procedure) {
    ProceduralTransitions intraProceduralTransitions = procedures.get(procedure);
    if (intraProceduralTransitions == null) {
      intraProceduralTransitions = new ProceduralTransitions();
      procedures.put(procedure, intraProceduralTransitions);
      if (!procedure.isRoot()) {
        CallString.Transition lastCall = procedure.peek();
        callstringsForProcedure.put(lastCall.getTarget(), procedure);
      }
    }
    return intraProceduralTransitions;
  }

  public Map<CallString, ProceduralTransitions> getAllProcedures () {
    return procedures;
  }

  public Collection<CallString> getCallstringsForProcedure (RReilAddr procedureEntry) {
    return callstringsForProcedure.get(procedureEntry);
  }

  public Collection<RReilAddr> getCallSitesForProcedure (RReilAddr procedureEntry) {
    return callsites.get(procedureEntry);
  }

  public Collection<RReilAddr> getPotentialReturnSitesForProcedure (RReilAddr procedureEntry) {
    return returnsites.get(procedureEntry);
  }

  /**
   * Return an overapproximation of all possible successors for a given program address. The successors are "flat" in
   * that all successors
   * for all possible callstrings are returned and intra- and interprocedural transitions are not distinguished. Hence,
   * from a given
   * address all the program points that can be reached through any form of control flow are returned.
   */
  public Multimap<RReilAddr, RReilAddr> getAllPossibleTransitions () {
    return flatSuccessors;
  }

  /**
   * Return all the transitions for the given procedure identified by a callstring.
   */
  public ProceduralTransitions getTransitionsFor (CallString procedure) {
    return getProcedureOrFresh(procedure);
  }

  /**
   * Add an intraprocedural transition.
   */
  public void addLocalTransition (CallString procedure, ProgramPoint from, ProgramPoint to) {
    ProceduralTransitions procedureTransitions = getProcedureOrFresh(procedure);
    procedureTransitions.addLocalTransition(from, to);
    flatSuccessors.put(from.getAddress(), to.getAddress());
  }

  /**
   * Add a call transition expressed in tuples of call-strings and program addresses.
   */
  public void addCallTransition (ProgramCtx from, ProgramCtx to) {
    ProceduralTransitions procedureTransitions = getProcedureOrFresh(from.getCallString());
    procedureTransitions.addCall(from, to);
    callsites.put(to.getAddress(), from.getAddress());
    flatSuccessors.put(from.getAddress(), to.getAddress());
  }

  /**
   * Add a call transition expressed in tuples of call-strings and program addresses.
   * This method also records a potential return site for the call.
   */
  public void addCallTransition (ProgramCtx from, ProgramCtx to, RReilAddr fallThroughAddress) {
    addCallTransition(from, to);
    returnsites.put(to.getAddress(), fallThroughAddress);
  }

  /**
   * Add a return transition expressed in tuples of call-strings and program addresses.
   */
  public void addReturnTransition (ProgramCtx from, ProgramCtx to) {
    ProceduralTransitions fromProcedureTransitions = getProcedureOrFresh(from.getCallString());
    fromProcedureTransitions.addReturn(from, to);
    flatSuccessors.put(from.getAddress(), to.getAddress());
  }

}
