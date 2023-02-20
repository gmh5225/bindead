package bindead.analyses.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.mutablecollections.CollectionHelpers;
import rreil.disassembler.BlockOfInstructions;
import rreil.disassembler.Instruction;
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
import bindead.analyses.algorithms.data.TransitionSystem;
import bindead.analyses.algorithms.data.Worklist;
import bindead.analyses.warnings.WarningsMap;
import bindead.debug.AnalysisDebugger;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import bindead.exceptions.AnalysisException;
import bindead.exceptions.CallStringAnalysisException;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.UnknownCodeAddressException;
import binparse.Binary;

/**
 * Recursive descent disassembly algorithm. The algorithm
 * disassembles the binary while following <b>statically</b> known jumps.
 */
public class RecursiveDisassembler<D extends RootDomain<D>> extends Analysis<D> {
  private final WarningsMap warningsMap;
  private final TransitionSystem transitions;
  private final BinaryCodeCache binaryCode;
  private final RReilCodeCache rreilCode;
  private RecursiveDisassemblerEvaluator<D> evaluator;
  // below are only for debugging purposes
  private final boolean DEBUGWARNINGS = AnalysisProperties.INSTANCE.debugWarnings.isTrue();
  private final boolean DEBUGEVAL = AnalysisProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean DEBUGOTHER = AnalysisProperties.INSTANCE.debugOther.isTrue();
  private final boolean DISASSEMBLEBLOCKS = AnalysisProperties.INSTANCE.disassembleBlockWise.isTrue();
  private final boolean IGNORENONEXISTENTCODE = AnalysisProperties.INSTANCE.ignoreNonExistentJumpTargets.isTrue();
  private final AnalysisDebugger debugger;
  private final ProgressReporter progressReporter;

  public RecursiveDisassembler (AnalysisEnvironment environment, Binary binary) {
    super(environment);
    transitions = new TransitionSystem();
    warningsMap = new WarningsMap();
    this.binaryCode = new BinaryCodeCache(binary, getPlatform().getDisassembler(), warningsMap);
    this.rreilCode = new RReilCodeCache(binary);
    debugger = new AnalysisDebugger(binaryCode, rreilCode);
    progressReporter = new ProgressReporter(getWarnings());
  }

  @Override public BinaryCodeCache getBinaryCode () {
    return binaryCode;
  }

  @Override public RReilCodeCache getRReilCode () {
    return rreilCode;
  }

  @Override public TransitionSystem getTransitionSystem () {
    return transitions;
  }

  @Override public Option<D> getState (CallString callString, RReilAddr address) {
    return Option.none();
  }

  @Override public WarningsMap getWarnings () {
    return warningsMap;
  }

  @Override public ProgressReporter getProgressMonitoring () {
    return progressReporter;
  }

  @Override public void runFrom (RReilAddr startPoint) {
    assert startPoint.offset() == 0;
    ProgramCtx entry = new ProgramCtx(CallString.root(), startPoint);
    processWorklist(entry);
    debugger.printCode();
  }

  private void processWorklist (ProgramCtx entry) {
    Set<RReilAddr> visited = new HashSet<>();
    Worklist<ProgramCtx> worklist = new Worklist<>();
    worklist.enqueue(entry);
    while (!worklist.isEmpty()) {
      if (DEBUGOTHER)
        System.out.println("\nOld:" + worklist);
      ProgramCtx currentPoint = worklist.dequeue();
      if (DEBUGOTHER)
        System.out.println("CurrentElement: " + currentPoint);
      List<ProgramCtx> successors = resolveSuccesorsWrapper(currentPoint);
      // iterate in reverse order as the enqueue operation below prepends and we want to maintain the order of the successors
      for (ProgramCtx successor : CollectionHelpers.reversedIterable(successors)) {
        if (visited.contains(successor.getAddress()))
          continue;
        visited.add(successor.getAddress());
        worklist.enqueue(successor);
      }
      if (DEBUGOTHER) {
        System.out.println("Successors: " + successors);
        System.out.println("New:" + worklist);
      }
    }
  }

  /**
   * Wraps the {@link #resolveSuccesors(ProgramCtx)} method and saves the analysis results so far on any exception and
   * propagates it up the stack in a defined way for handlers to be able to display these partial results.
   */
  private List<ProgramCtx> resolveSuccesorsWrapper (ProgramCtx point) {
    try {
      if (Thread.interrupted())
        throw new InterruptedException("Analysis was stopped by user.");
      return resolveSuccesors(point);
    } catch (CallStringAnalysisException cause) {
      try {
        debugger.printInstruction(point);
      } catch (Exception e) {
        // the above printer can cause exceptions itself
      }
      if (DEBUGEVAL)
        System.out.println();
      throw cause; // Catch and re-throw to avoid the accumulation of exceptions by putting one into another below.
      // XXX bm: do not catch all for now as it is only useful for the GUI to display the results
      // some tests expect a certain exception type so we cannot wrap it into our own type
      // } catch (Throwable cause) {
      // By using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
      // might also be useful to have a partial analysis result even on unexpected errors (e.g. stack overflows).
      // Also we want to catch assertion errors.
    } catch (RuntimeException cause) {
      try {
        debugger.printInstruction(point);
      } catch (Exception e) {
        // the above printer can cause exceptions itself
      }
      if (DEBUGEVAL)
        System.out.println("\nBAM!\n" + cause + "\n");
      debugger.printCode();
      throw cause;
    } catch (Error cause) {
      try {
        debugger.printInstruction(point);
      } catch (Exception e) {
        // the above printer can cause exceptions itself
      }
      if (DEBUGEVAL)
        System.out.println("\nBAM!\n" + cause + "\n");
      debugger.printCode();
      throw cause;
    } catch (Throwable cause) {
      throw new CallStringAnalysisException(cause, this, point.getCallString(), point.getAddress());
    }
  }

  private List<ProgramCtx> resolveSuccesors (ProgramCtx currentProgramPoint) {
    RReilAddr currentAddress = currentProgramPoint.getAddress();
    P2<RReil, RReilAddr> pair;
    try {
      pair = getInstruction(currentAddress);
    } catch (UnknownCodeAddressException e) {
      if (IGNORENONEXISTENTCODE)
        return Collections.emptyList();
      else
        throw e;
    }
    RReil currentRReilInstruction = pair._1();
    RReilAddr nextAddress = pair._2();
    debugBeforeEval(currentRReilInstruction, currentProgramPoint);
    debugger.printInstruction(currentProgramPoint);
    progressReporter.evaluatingInstruction(currentRReilInstruction);
    RecursiveDisassemblerEvaluator<D> evaluator = getEvaluator();
    P2<ProgramPoint, RReilAddr> ctx = P2.tuple2((ProgramPoint) currentProgramPoint, nextAddress);
    Flows<D> successors = currentRReilInstruction.accept(evaluator, ctx);
    List<P2<Successor<?>, Boolean>> loggedSuccessors = new ArrayList<P2<Successor<?>, Boolean>>();
    List<ProgramCtx> queue = new ArrayList<ProgramCtx>();
    CallString currentCallString = currentProgramPoint.getCallString();
    for (Successor<D> successor : successors) {
      loggedSuccessors.add(new P2<Successor<?>, Boolean>(successor, true));
      switch (successor.getType()) {
      case Call: {
        Transition callTransition = new Transition(currentAddress, successor.getAddress());
        CallString calleeCallString = currentCallString.push(callTransition);
        ProgramCtx to = new ProgramCtx(calleeCallString, successor.getAddress());
        transitions.addCallTransition(currentProgramPoint, to, nextAddress);
        updateWorklist(to, queue);
        break;
      }
      case Return: {
        // We interpret return from the empty call-string as "return-from-main"
        // TODO: what about the possible return targets in this case? Emit info?
        // TODO: check if the actual return address is addressOf(call-site) + length(call-site), otherwise
        // emit info, that return address was modified.
        if (currentCallString.isRoot())
          break;
        CallString callerCallString = currentCallString.unsafePop();
        ProgramCtx to = new ProgramCtx(callerCallString, successor.getAddress());
        transitions.addReturnTransition(currentProgramPoint, to);
        updateWorklist(to, queue);
        break;
      }
      case Next:
      case Jump: {
        ProgramCtx to = new ProgramCtx(currentCallString, successor.getAddress());
        transitions.addLocalTransition(currentCallString, currentProgramPoint, to);
        updateWorklist(to, queue);
        break;
      }
      case Halt:
        break;
      case Error:
        throw new AnalysisException();
      default:
        break;
      }
    }

    debugger.printSuccessors(loggedSuccessors);
    debugAfterEval(currentRReilInstruction, currentProgramPoint, successors);
    debugger.printWarnings(currentProgramPoint, getWarnings());
    debugger.printSeparator();
    if (successors.isEmpty())
      // a program can only be terminated by the halt instruction. It must have at least that as a successor.
      throw new InvariantViolationException("No successors inferred after evaluating last instruction.");
    return queue;
  }

  protected void updateWorklist (ProgramCtx to, List<ProgramCtx> queue) {
    try {
      disassemble(to.getAddress()); // improves debugging as we know what code parts we reached
    } catch (UnknownCodeAddressException e) {
      // ignore errors or not?
      if (!IGNORENONEXISTENTCODE)
        throw e;
    }
    queue.add(to);
  }

  private P2<RReil, RReilAddr> getInstruction (RReilAddr address) {
    if (!binaryCode.hasInstruction(address.base()))
      disassemble(address);
    RReilAddr nextInstructionAddress;
    Option<RReilAddr> nextInstructionOption = rreilCode.getNextInstructionAddressWithSameBase(address);
    if (nextInstructionOption.isSome()) {
      nextInstructionAddress = nextInstructionOption.get();
    } else {
      long nextNativeAddress = binaryCode.getNextDisassemblyAddress(address.base());
      nextInstructionAddress = RReilAddr.valueOf(nextNativeAddress);
    }
    RReil rreilInsn = rreilCode.getInstruction(address);
    return P2.tuple2(rreilInsn, nextInstructionAddress);
  }

  private void disassemble (RReilAddr address) {
    // chooses which disassembly should be used. Either instruction wise disassembly or whole instruction blocks
    if (DISASSEMBLEBLOCKS)
      disassembleBlock(address);
    else
      disassembleOne(address);
  }

  private void disassembleOne (RReilAddr address) {
    Instruction insn = binaryCode.decodeInstruction(address.base());
    for (LowLevelRReil stmt : insn.toRReilInstructions()) {
      rreilCode.addInstruction(stmt.toRReil());
    }
  }

  private void disassembleBlock (RReilAddr address) {
    BlockOfInstructions block = binaryCode.decodeBlock(address.base());
    for (LowLevelRReil stmt : block.toRReilInstructions()) {
      rreilCode.addInstruction(stmt.toRReil());
    }
  }

  protected RecursiveDisassemblerEvaluator<D> getEvaluator () {
    if (evaluator == null)
      evaluator = new RecursiveDisassemblerEvaluator<D>(warningsMap);
    return evaluator;
  }

  private void debugBeforeEval (RReil stmt, ProgramPoint point) {
    if (debugHooks != null)
      debugHooks.beforeEval(stmt, point, null, this);
  }

  private void debugAfterEval (RReil stmt, ProgramPoint point, Flows<D> flow) {
    if (debugHooks != null) {
      for (Successor<D> successor : flow) {
        debugHooks.afterEval(stmt, point, successor.getAddress(), successor.getState(), this);
      }
    }
  }

}
