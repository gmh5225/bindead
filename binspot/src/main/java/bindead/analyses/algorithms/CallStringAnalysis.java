package bindead.analyses.algorithms;

import java.util.ArrayList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.mutablecollections.CollectionHelpers;
import javalx.numeric.BigInt;
import rreil.disassembler.Instruction;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.LowLevelRReil;
import bindead.analyses.Analysis;
import bindead.analyses.BinaryCodeCache;
import bindead.analyses.ProgressReporter;
import bindead.analyses.RReilCodeCache;
import bindead.analyses.algorithms.data.CallString;
import bindead.analyses.algorithms.data.CallString.Transition;
import bindead.analyses.algorithms.data.Flows;
import bindead.analyses.algorithms.data.Flows.Successor;
import bindead.analyses.algorithms.data.ProgramCtx;
import bindead.analyses.algorithms.data.StateSpace;
import bindead.analyses.algorithms.data.TransitionSystem;
import bindead.analyses.algorithms.data.Worklist;
import bindead.analyses.warnings.WarningsMap;
import bindead.debug.AnalysisDebugger;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import bindead.environment.abi.ABI;
import bindead.exceptions.AnalysisException;
import bindead.exceptions.CallStringAnalysisException;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import binparse.Binary;
import binparse.Segment;

/**
 * Fixpoint iteration using the call-string approach. The fixpoint algorithm
 * disassembles the binary on-the-fly, that is it will never decode unreachable
 * code (esp in the case when visiting conditional branches).
 */
public class CallStringAnalysis<D extends RootDomain<D>> extends Analysis<D> {
  private final D initialState;
  protected final StateSpace<D> states;
  private final TransitionSystem transitions;
  private final BinaryCodeCache binaryCode;
  private final RReilCodeCache rreilCode;
  private FixpointAnalysisEvaluator<D> evaluator;
  private long analysisStartCanary;
  // below are only for debugging purposes
  private final boolean DEBUGWARNINGS = AnalysisProperties.INSTANCE.debugWarnings.isTrue();
  private final boolean DEBUGEVAL = AnalysisProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean DEBUGOTHER = AnalysisProperties.INSTANCE.debugOther.isTrue();
  private final AnalysisDebugger debugger;
  private final ProgressReporter progressReporter;

  public CallStringAnalysis(AnalysisEnvironment environment, Binary binary, D initialState) {
    super(environment);
    states = new StateSpace<D>();
    transitions = new TransitionSystem();
    this.initialState = initialState.setContext(new AnalysisCtx(environment));
    this.binaryCode = new BinaryCodeCache(binary, getPlatform().getDisassembler(), getWarnings());
    this.rreilCode = new RReilCodeCache(binary);
    debugger = new AnalysisDebugger(binaryCode, rreilCode);
    progressReporter = new ProgressReporter(getWarnings());
  }

  @Override
  public BinaryCodeCache getBinaryCode() {
    return binaryCode;
  }

  @Override
  public RReilCodeCache getRReilCode() {
    return rreilCode;
  }

  @Override
  public TransitionSystem getTransitionSystem() {
    return transitions;
  }

  @Override
  public Option<D> getState(CallString callString, RReilAddr address) {
    return states.get(new ProgramCtx(callString, address));
  }

  @Override
  public void runFrom(RReilAddr startPoint) {
    CallString rootCs = CallString.root();
    runFrom(startPoint, rootCs);
  }

  public void runFrom(RReilAddr startPoint, int callstringLength) {
    CallString rootCs = CallString.root(callstringLength);
    runFrom(startPoint, rootCs);
  }

  private void runFrom(RReilAddr startPoint, CallString root) {
    assert startPoint.offset() == 0;
    D state = getPlatform().forwardAnalysisBootstrap().bootstrap(initialState, startPoint.base());
    state = introduceDataSegments(state);
    state = setAnalysisStartStackCanary(state, startPoint);
    RReilAddr artificialStartPoint = RReilAddr.ZERO;
    Transition callTransition = new Transition(artificialStartPoint, startPoint);
    ProgramCtx artificialEntry = new ProgramCtx(root, artificialStartPoint);
    ProgramCtx entry = new ProgramCtx(root.push(callTransition), startPoint);
    transitions.addCallTransition(artificialEntry, entry);
    states.setInitial(entry, state);
    debugger.printWarnings(entry, getWarnings()); // any warnings produced during the bootstrapping
    Worklist<ProgramCtx> queue = new Worklist<>();
    queue.enqueue(entry);
    while (!queue.isEmpty()) {
      if (DEBUGOTHER) {
        System.out.println("\nOld:" + queue);
      }
      ProgramCtx currentPoint = queue.dequeue();
      if (DEBUGOTHER) {
        System.out.println("CurrentElement: " + currentPoint);
      }
      List<ProgramCtx> successors = resolveSuccesorsWrapper(currentPoint);
      // iterate in reverse order as the enqueue operation prepends and we want to maintain the order of the successors
      for (ProgramCtx successor : CollectionHelpers.reversedIterable(successors)) {
        queue.enqueue(successor);
      }
      if (DEBUGOTHER) {
        System.out.println("Successors: " + successors);
        System.out.println("New:" + queue);
      }
    }
    debugger.printSummary(states, getWarnings());
    if (DEBUGWARNINGS) {
      System.out.println(getWarnings());
    }
  }

  @Override
  public WarningsMap getWarnings() {
    return states.getWarnings();
  }

  @Override
  public ProgressReporter getProgressMonitoring () {
    return progressReporter;
  }

  /**
   * Write a canary value on the stack such that when jumping to that value
   * using a return we know that the analysis ended.
   */
  private D setAnalysisStartStackCanary(D state, RReilAddr startAddress) {
    // TODO: at the moment we pass the start address as the canary.
    // We should maybe use the end of the text segment instead to not
    // mistake a return to that address in the program code as the end of the analysis
    analysisStartCanary = startAddress.base();
    ABI abi = environment.getABI();
    if (abi == null) {
      return state;
    }
    return abi.writeAnalysisStartStackCanary(startAddress, analysisStartCanary, state);
  }

  private D introduceDataSegments(D state) {
    for (Segment segment : binaryCode.getBinary().getSegments()) {
      // need to sanitize the name as some character are not allowed in identifiers in our parser below
      String name = segment.getNameOrAddress().replaceAll("\\.|-", "_");
      BigInt address = BigInt.of(segment.getAddress());
      long size = segment.getSize();
      ContentCtx segmentCtx = new ContentCtx(name, address, size, segment.getPermissions(),
              segment.getData(), segment.getEndianness());
      state = state.introduceRegion(MemVar.fresh(segmentCtx.getName()), new RegionCtx(segmentCtx));
      state = state.eval(String.format("prim fixAtConstantAddress (%s.%d)", segmentCtx.getName(),
              getPlatform().defaultArchitectureSize()));
    }
    return state;
  }

  /**
   * Wraps the {@link #resolveSuccesors(ProgramCtx)} method and saves the
   * analysis results so far on any exception and propagates it up the stack in
   * a defined way for handlers to be able to display these partial results.
   */
  private List<ProgramCtx> resolveSuccesorsWrapper(ProgramCtx point) {
    try {
      if (Thread.interrupted()) {
        throw new InterruptedException("Analysis was stopped by user.");
      }
      return resolveSuccesors(point);
    } catch (CallStringAnalysisException cause) {
      debugger.printInstruction(point);
      if (DEBUGEVAL) {
        System.out.println();
      }
      throw cause; // Catch and re-throw to avoid the accumulation of exceptions by putting one into another below.
      // XXX bm: do not catch all for now as it is only useful for the GUI to display the results
      // some tests expect a certain exception type so we cannot wrap it into our own type
    } catch (Throwable cause) {
      // By using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
      // might also be useful to have a partial analysis result even on unexpected errors (e.g. stack overflows).
      // Also we want to catch assertion errors.
      debugger.printInstruction(point);
      if (DEBUGEVAL) {
        System.out.println("\nBAM!\n");
      }
      // try to save any warnings produced before the exception
      states.putWarnings(point, states.get(point).get().getContext().getWarningsChannel());
      debugger.printSummary(states, getWarnings());
      System.out.println(getWarnings());
      throw new CallStringAnalysisException(cause, this, point.getCallString(), point.getAddress());
    }
  }

  private List<ProgramCtx> resolveSuccesors(ProgramCtx currentProgramPoint) {
    RReilAddr currentAddress = currentProgramPoint.getAddress();
    disassemble(currentAddress);
    RReil stmt = rreilCode.getInstruction(currentAddress);
    D domainState = states.get(currentProgramPoint).get();
    debugBeforeEval(stmt, currentProgramPoint, domainState);
    debugger.printInstruction(currentProgramPoint);
    progressReporter.evaluatingInstruction(stmt);
    FixpointAnalysisEvaluator<D> evaluator = getEvaluator();
    RReilAddr nextAddress = nextInstructionAddress(currentAddress);
    P3<D, ProgramPoint, RReilAddr> ctx = P3.tuple3(domainState, (ProgramPoint) currentProgramPoint, nextAddress);
    Flows<D> successors = stmt.accept(evaluator, ctx);
    List<P2<Successor<?>, Boolean>> loggedSuccessors = new ArrayList<>();
    List<ProgramCtx> queue = new ArrayList<>();
    CallString currentCallString = currentProgramPoint.getCallString();
    for (Successor<D> successor : successors) {
      switch (successor.getType()) {
        case Call: {
          Transition callTransition = new Transition(currentAddress, successor.getAddress());
          CallString calleeCallString = currentCallString.push(callTransition);
          ProgramCtx to = new ProgramCtx(calleeCallString, successor.getAddress());
          transitions.addCallTransition(currentProgramPoint, to, nextAddress);
          boolean updated = updateWorklist(currentProgramPoint, to, successor, queue, false);
          loggedSuccessors.add(new P2<Successor<?>, Boolean>(successor, updated));
          break;
        }
        case Return: {
        // We interpret return from the empty call-string as "return-from-main"
          // TODO: what about the possible return targets in this case? Emit info?
          // TODO: check if the actual return address is addressOf(call-site) + length(call-site), otherwise
          // emit info, that return address was modified.
          if (currentCallString.isRoot()) {
            break;
          }
          CallString callerCallString = currentCallString.unsafePop();
          ProgramCtx to = new ProgramCtx(callerCallString, successor.getAddress());
          transitions.addReturnTransition(currentProgramPoint, to);
          boolean updated = updateWorklist(currentProgramPoint, to, successor, queue, false);
          loggedSuccessors.add(new P2<Successor<?>, Boolean>(successor, updated));
          break;
        }
        case Next:
        case Jump: {
          ProgramCtx to = new ProgramCtx(currentCallString, successor.getAddress());
          transitions.addLocalTransition(currentCallString, currentProgramPoint, to);
          boolean updated = updateWorklist(currentProgramPoint, to, successor, queue, true);
          loggedSuccessors.add(new P2<Successor<?>, Boolean>(successor, updated));
          break;
        }
        case Halt:
          // no successor state but we might have new warnings
          states.putWarnings(currentProgramPoint, successor.getState().getContext().getWarningsChannel());
          break;
        case Error:
          states.putWarnings(currentProgramPoint, successor.getState().getContext().getWarningsChannel());
          throw new AnalysisException();
        default:
          break;
      }
    }
    debugger.printSuccessors(loggedSuccessors);
    debugAfterEval(stmt, currentProgramPoint, successors);
    debugger.printWarnings(currentProgramPoint, getWarnings());
    debugger.printSeparator();
    if (successors.isEmpty()) { // a program can only be terminated by the halt instruction. It must have at least that as a successor.
      throw new InvariantViolationException("No successors inferred after evaluating last instruction.");
    }
    return queue;
  }

  private void disassemble(RReilAddr address) {
    if (binaryCode.hasInstruction(address.base())) {
      return;
    }
    Instruction insn = binaryCode.decodeInstruction(address.base());
    for (LowLevelRReil stmt : insn.toRReilInstructions()) {
      rreilCode.addInstruction(stmt.toRReil());
    }
  }

  private RReilAddr nextInstructionAddress(RReilAddr currentInstructionAddress) {
    RReilAddr nextInstructionAddress;
    Option<RReilAddr> nextInstructionOption = rreilCode.getNextInstructionAddressWithSameBase(currentInstructionAddress);
    if (nextInstructionOption.isSome()) {
      nextInstructionAddress = nextInstructionOption.get();
    } else {
      long nextNativeAddress = binaryCode.getNextDisassemblyAddress(currentInstructionAddress.base());
      nextInstructionAddress = RReilAddr.valueOf(nextNativeAddress);
    }
    return nextInstructionAddress;
  }

  protected boolean updateWorklist (ProgramCtx from, ProgramCtx to, Successor<D> successor, List<ProgramCtx> queue, boolean useWidening) {
    disassemble(to.getAddress()); // improves debugging as we know what code parts we reached
    boolean updated = states.update(from, successor.getType(), to, successor.getState(), useWidening);
    if (updated) {
      queue.add(to);
      return true;
    }
    return false;
  }

  protected FixpointAnalysisEvaluator<D> getEvaluator() {
    if (evaluator == null) {
      evaluator = new FixpointAnalysisEvaluator<D>(analysisStartCanary);
    }
    return evaluator;
  }

  private void debugBeforeEval(RReil stmt, ProgramPoint point, D domainState) {
    if (debugHooks != null) {
      debugHooks.beforeEval(stmt, point, domainState, this);
    }
  }

  private void debugAfterEval(RReil stmt, ProgramPoint point, Flows<D> flow) {
    if (debugHooks != null) {
      for (Successor<D> successor : flow) {
        debugHooks.afterEval(stmt, point, successor.getAddress(), successor.getState(), this);
      }
    }
  }

}
