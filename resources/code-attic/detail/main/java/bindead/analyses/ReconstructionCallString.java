package bindead.analyses;

import bindead.exceptions.FixpointException;
import bindead.analysis.util.CallString;
import bindead.analysis.util.ForwardEvaluator;
import bindead.data.Range;
import bindead.domainnetwork.channels.Message;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domainnetwork.interfaces.SegmentCtx;
import bindead.domains.affine.Affine;
import bindead.domains.fields.Fields;
import bindead.domains.intervals.Intervals;
import bindead.domains.narrowing.Narrowing;
import bindead.domains.pointsto.PointsTo;
import bindead.domains.predicate.Predicate;
import bindead.domains.root.Root;
import bindead.domains.wrapping.Wrapping;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.DomainStateException.ErrObj;
import bindead.exceptions.ReconstructionException;
import bindead.platforms.Platforms;
import bindead.platforms.Platforms.AnalysisPlatform;
import binparse.Binary;
import binparse.Segment;
import binparse.Symbol;
import binparse.elf.ElfBinary;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javalx.data.BigInt;
import javalx.data.CollectionHelpers;
import javalx.data.Interval;
import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.Tuple2;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.fn.Fn;
import rreil.abstractsyntax.Field;
import rreil.abstractsyntax.RReil.Return;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.Rhs.Rlit;
import rreil.abstractsyntax.Rhs.Rval;
import rreil.abstractsyntax.Rhs.Rvar;
import rreil.cfa.Cfa;
import rreil.cfa.Cfa.CfaCall;
import rreil.cfa.CompositeCfa;
import rreil.cfa.util.CfaHelpers;
import rreil.cfa.util.CfaStateException;
import rreil.disassembly.DisassemblyProvider;

/**
 * Performs the reconstruction of the CFA for a binary. It uses the Call-String approach to perform an interprocedural
 * analysis.
 */
public class ReconstructionCallString<D extends RootDomain<D>> {
  private final AnalysisCtx<D> intraproceduralAnalyses;
  private final Map<Cfa, Set<CallString>> analysesForCfa;
  private final Binary binary;
  private final DisassemblyProvider disassembler;
  private final AnalysisPlatform platform;
  private final CompositeCfa compositeCfa;
  private CallString currentCallString;
  private static final int defaultCallStringLength = 500;

  protected ReconstructionCallString (Binary binary, CallString callString) {
    this(binary, null, callString);
  }

  protected ReconstructionCallString (Binary binary, CompositeCfa ccfa, CallString callString) {
    this.binary = binary;
    this.platform = Platforms.getPlatformFor(binary);
    byte[] codeSegment = binary.getCodeSegment().getData();
    long baseAddress = binary.getCodeSegment().getAddress();
    this.disassembler = platform.disassemblyProvider(codeSegment, baseAddress);
    this.intraproceduralAnalyses = new AnalysisCtx<D>();
    this.analysesForCfa = new HashMap<Cfa, Set<CallString>>();
    this.currentCallString = callString;
    if (ccfa == null)
      this.compositeCfa = new CompositeCfa(disassembler);
    else
      this.compositeCfa = ccfa;
  }

  protected void run (BigInt startAddress) {
    Cfa entryCfa = compositeCfa.scanCfa(startAddress);
    Analysis<D> initialAnalysis = newIntraproceduralAnalysis(entryCfa, initialDomainHierarchy());
    intraproceduralAnalyses.put(currentCallString, initialAnalysis);
    setSymbolNames(compositeCfa, binary);
    SegmentCtx[] segments = getDataSections(binary).toArray(new SegmentCtx[]{});
    initialAnalysis.bootstrapState(entryCfa.getEntry(), platform.forwardAnalysisBootstrap(), segments);
    trackAnalysesForCfa(entryCfa, initialAnalysis, currentCallString);
    solve(initialAnalysis);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private D initialDomainHierarchy () {
    return (D) new Root(new Fields(new Predicate(new PointsTo(new Wrapping(new Affine(new Narrowing(new Intervals())))))));
  }

  public static ReconstructionCallString<?> run (Binary binary, BigInt startAddress) {
    return run(binary, startAddress, null, defaultCallStringLength);
  }

  public static ReconstructionCallString<?> run (Binary binary, BigInt startAddress, CompositeCfa ccfa, int callStringLength) {
    CallString callString = CallString.withMaxSignificantLength(callStringLength);
    @SuppressWarnings("rawtypes")
    ReconstructionCallString<?> reconstruction = new ReconstructionCallString(binary, ccfa, callString);
    try {
      reconstruction.run(startAddress);
    } catch (FixpointException cause) {
      throw new ReconstructionException(cause, reconstruction, reconstruction.currentCallString);
    } catch (Exception cause) {
      throw new ReconstructionException(cause, reconstruction, reconstruction.currentCallString);
    }
    return reconstruction;
  }

  public static ReconstructionCallString<?> runElf (String file) throws IOException {
    return runElf(file, defaultCallStringLength);
  }

  public static ReconstructionCallString<?> runElf (String file, int callStringLength) throws IOException {
    Binary binary = new ElfBinary(file);
    BigInt startAddress = BigInt.valueOf(binary.getSymbol("main").get().getAddress());
    return run(binary, startAddress, null, callStringLength);
  }

  /**
   * Runs the analysis on an already reconstructed CCFA again.
   */
  public static ReconstructionCallString<?> rerun (Binary binary, BigInt startAddress, CompositeCfa ccfa) {
    return run(binary, startAddress, ccfa, defaultCallStringLength);
  }

  public AnalysisPlatform getPlatform () {
    return platform;
  }

  public Binary getBinary () {
    return binary;
  }

  public CompositeCfa getCompositeCfa () {
    return compositeCfa;
  }

  /**
   * Returns the analysis contexts for the given CFA.
   */
  public AnalysisCtx<D> getAnalysisContexts (Cfa cfa) {
    AnalysisCtx<D> analyses = new AnalysisCtx<D>();
    if (analysesForCfa.containsKey(cfa)) {
      for (CallString cs : analysesForCfa.get(cfa)) {
        analyses.put(cs, intraproceduralAnalyses.get(cs));
      }
    }
    return analyses;
  }

  /**
   * Returns all the analysis contexts.
   */
  public AnalysisCtx<D> getAnalysisContexts () {
    return intraproceduralAnalyses;
  }

  private Analysis<D> newIntraproceduralAnalysis (Cfa cfa, D entryState) {
    Analysis<D> intraproceduralAnalysis;
    CallbackAtCalls evaluator = new CallbackAtCalls(cfa, disassembler);
    intraproceduralAnalysis = new ForwardAnalysis<D>(platform, evaluator, entryState);
    intraproceduralAnalysis.setStartVertex(cfa.getEntry());
    return intraproceduralAnalysis;
  }

  private void trackAnalysesForCfa (Cfa cfa, Analysis<D> analysis, CallString callString) {
    intraproceduralAnalyses.put(callString, analysis);
    Set<CallString> analysesSet = analysesForCfa.get(cfa);
    if (analysesSet == null) {
      analysesSet = new HashSet<CallString>();
      analysesForCfa.put(cfa, analysesSet);
    }
    analysesSet.add(callString);
  }

  private Analysis<D> analyzeProcedure (Cfa cfa, D entryState, CallString callString) {
    Analysis<D> intraproceduralAnalysis = intraproceduralAnalyses.get(callString);
    if (intraproceduralAnalysis == null) {
      intraproceduralAnalysis = newIntraproceduralAnalysis(cfa, entryState);
      intraproceduralAnalyses.put(callString, intraproceduralAnalysis);
    } else {
      // XXX: detect recursion and do something meaningful!
      //D oldEntryState = intraproceduralAnalysis.getState(intraproceduralAnalysis.getStartVertex()).get();
      //D newEntryState = oldEntryState.join(entryState);
      intraproceduralAnalysis.setInitialState(entryState);
      intraproceduralAnalysis.setStartVertex(cfa.getEntry()); // bootstraps the vertex to the initial state
    }
    trackAnalysesForCfa(cfa, intraproceduralAnalysis, callString);
    solve(intraproceduralAnalysis);
    return intraproceduralAnalysis;
  }

  private D enterCall (Cfa caller, CfaCall call, Edge callEdge, D callState) {
    Range savedReturnAddress = getReturnAddressFromStack(callState);
    assert savedReturnAddress.isFinite() && savedReturnAddress.numberOfDiscreteValues() == 1 : "The return address pushed on the stack is not a single value.";

    Rval target = call.getCallInstruction().getTarget();
    List<D> returnValues = new LinkedList<D>();
    for (Cfa callee : getCallees(target, callState)) {
      currentCallString = currentCallString.push(call.getCallInstruction());
      Analysis<D> calleeAnalysis = analyzeProcedure(callee, callState, currentCallString);
      returnValues.add(calleeAnalysis.getState(callee.getExit()).get());
      currentCallString = currentCallString.pop(call.getCallInstruction());
      returnFromCall(caller, callEdge, savedReturnAddress, callee, calleeAnalysis);
    }
    // FIXME: assure that all calls return to the same address to be allowed to merge them
    return merge(returnValues);
  }

  private void returnFromCall (Cfa caller, Edge callEdge, Range savedReturnAddress, Cfa callee, Analysis<D> calleeAnalysis) {
    Range returnAddress = getReturnAddressesFromCallee(calleeAnalysis);
    if (!returnAddress.convexHull().equals(savedReturnAddress.convexHull()))
      addWarning(calleeAnalysis, callee.getExit(), new ReturnAddressModifiedWarning(returnAddress, savedReturnAddress));
    int maxReturnAddresses = 10; // chosen arbitrarily
    if (returnAddress.isFinite() && returnAddress.numberOfDiscreteValues() <= maxReturnAddresses) {
      for (BigInt address : returnAddress) {
        caller.extendFrom(callEdge, RReilAddr.valueOf(address), disassembler);
      }
    } else {
      caller.extendFrom(callEdge, RReilAddr.valueOf(savedReturnAddress.getMin()), disassembler);
    }
  }

  private void addWarning (Analysis<D> analysis, Vertex vertex, Message warning) {
    List<Message> warnings = analysis.getWarnings().get(vertex);
    if (warnings == null) {
      warnings = new ArrayList<Message>();
      analysis.getWarnings().put(vertex, warnings);
    }
    warnings.add(warning);
  }

  private List<Cfa> getCallees (Rval target, D domain) {
    if (target instanceof Rlit) {
      BigInt targetAddress = ((Rlit) target).getValue();
      return Collections.singletonList(compositeCfa.scanCfa(targetAddress));
    }
    Range targetAddresses = domain.queryRange(((Rvar) target).asUnresolvedField());
    int maxAllowedCallTargets = 1; // TODO: raise this as soon as it is possible to handle multiple call targets
    if (!targetAddresses.isFinite())
      throw new CfaStateException(CfaStateException.ErrObj.TOO_MANY_JUMP_TARGETS, "infinitely many");
    if (targetAddresses.numberOfDiscreteValues() > maxAllowedCallTargets)
      throw new CfaStateException(
          CfaStateException.ErrObj.TOO_MANY_JUMP_TARGETS, targetAddresses.numberOfDiscreteValues() + "");

    List<Cfa> callees = new LinkedList<Cfa>();
    for (BigInt calleeAddress : targetAddresses) {
      callees.add(compositeCfa.scanCfa(calleeAddress));
    }
    return callees;
  }

  private Range getReturnAddressFromStack (D state) {
    Range stackOffset = platform.getCurrentStackOffset(state);
    if (stackOffset.numberOfDiscreteValues() != 1)
      throw new DomainStateException(ErrObj.UNIMPLEMENTED);
    return platform.getCurrentReturnAddress(stackOffset.getMin(), state);
  }

  private Range getReturnAddressesFromCallee (Analysis<D> analysis) {
    Cfa callee = analysis.getCfa();
    Set<Return> returns = CfaHelpers.getReturnInstructions(callee);
    assert !returns.isEmpty() : "The callee does not contain any return instructions.";
    Range returnAddresses = null;
    for (Return returnInsn : returns) {
      Field returnRegister = returnInsn.getTarget().asUnresolvedField();
      D stateAtReturnInsn = analysis.getState(returnInsn.getRReilAddress()).get();
      Range returnRange = stateAtReturnInsn.queryRange(returnRegister);
      if (returnAddresses == null)
        returnAddresses = returnRange;
      else
        returnAddresses = returnAddresses.union(returnRange);
    }
    return returnAddresses;
  }

  private D merge (List<D> states) {
    Iterator<D> it = states.iterator();
    D current = it.next();
    while (it.hasNext()) {
      current = current.join(it.next());
    }
    return current;
  }

  private static void solve (Analysis<?> analysis) {
    WorklistSolver solver = new WorklistSolver(analysis);
    solver.solve();
  }

  private static List<SegmentCtx> getDataSections (Binary elf) {
    List<SegmentCtx> sections = new LinkedList<SegmentCtx>();
    for (Segment section : elf.getDataSegments()) {
      BigInt address = BigInt.valueOf(section.getAddress());
      BigInt size = BigInt.valueOf(section.getSize());
      sections.add(new SegmentCtx(section.getNameOrAddress(), address, size, section.getPermissions(), section.getData(), section.getEndianness()));
    }
    return sections;
  }

  @SuppressWarnings("unchecked")
  private static void setSymbolNames (CompositeCfa cfas, Binary elf) {
    List<P2<RReilAddr, String>> symbols = CollectionHelpers.map(elf.getExportedSymbols(),
        new Fn<Symbol, P2<RReilAddr, String>>() {
          @Override public P2<RReilAddr, String> apply (Symbol symbol) {
            return new Tuple2<RReilAddr, String>(RReilAddr.valueOf(symbol.getAddress()), symbol.getNameOrAddress());
          }
        });
//    cfas.setNames(symbols.toArray(new P2[0]));
  }

  private class CallbackAtCalls extends ForwardEvaluator<D> {
    public CallbackAtCalls (Cfa cfa, DisassemblyProvider dis) {
      super(cfa, dis);
    }

    @Override public D visit (CfaCall call, Edge correspondingEdge, D domain) {
      domain = super.visit(call, correspondingEdge, domain);
      return enterCall(getCfa(), call, correspondingEdge, domain);
    }
  }

  /**
   * Returns the domain value of a register variable at a given address if the register has a value.
   */
  public Option<Interval> queryIntervalOfRegisterAt (RReilAddr address, String register) {
    Cfa main = getCompositeCfa().lookupCfa("main").get();
    assert getAnalysisContexts(main).size() == 1;
    Option<D> state = getAnalysisContexts(main).values().iterator().next().getState(address);
    return queryIntervalOfRegisterImpl(register, state);
  }

  /**
   * Returns the domain value of a register at the exit point of the main function if the register has a value.
   */
  public Option<Interval> queryIntervalOfRegisterAtExit (String register) {
    Cfa main = getCompositeCfa().lookupCfa("main").get();
    assert getAnalysisContexts(main).size() == 1;
    Option<D> state = getAnalysisContexts(main).values().iterator().next().getState(main.getExit());
    return queryIntervalOfRegisterImpl(register, state);
  }

  private Option<Interval> queryIntervalOfRegisterImpl (String register, Option<D> stateOption) {
    if (stateOption.isNone())
      return Option.none();
    D state = stateOption.get();
    Rvar var = platform.getDisassembler().translateIdentifier(register);
    return Option.some(state.queryRange(var.asUnresolvedField()).convexHull());
  }
}
