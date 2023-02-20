package bindead.domains.segments.machine;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.term;

import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.FiniteRange;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.ThreeWaySplit;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Lin;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.abstractsyntax.memderef.SymbolicOffset;
import bindead.data.Linear;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.NumVar.FlagVar;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.segments.basics.RegionAccess;
import bindead.domains.segments.basics.SegCompatibleState;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.domains.segments.warnings.BufferOverflow;
import bindead.domains.segments.warnings.FrameAccess;
import bindead.domains.segments.warnings.NonConstantStackOffset;
import bindead.domains.segments.warnings.ReturnAddressOverwritten;
import bindead.domains.segments.warnings.StackUnderflow;
import bindead.domains.segments.warnings.UnknownStackFrame;
import bindead.environment.platform.Platform;
import bindead.exceptions.DomainStateException.UnimplementedMethodException;
import bindead.exceptions.Unreachable;

/**
 * The segment containing stack frames contains a possibly circular linked list
 * of stack frames. Each frame is uniquely identified by the abstract addresses
 * if its stack frame.
 *
 * @param <D> the memory domain
 * @author Axel Simon
 */
public class StackSegment<D extends MemoryDomain<D>> extends Segment<D> {
  public static final String NAME = "Stack";
  private final static FiniteFactory fin = FiniteFactory.getInstance();

  /**
   * This constant is the address of the topmost stack variable. It is re-used
   * for every new stack frame. Once a new function is called the current stack
   * frame obtains a new address and this address is re-used for the new stack
   * frame.
   */
  private final AddrVar stackAddr = NumVar.getSingletonAddress("activeFrame");
  private final MemVar currentBpRelative = MemVar.getVarOrFresh("currBP");
  private final NumVar currentBarrier = NumVar.getSingleton("BP|SP");
  private final MemVar currentSpRelative = MemVar.getVarOrFresh("currSP");

  /**
   * The canonical entry point of the topmost stack frame.
   */
  private final RReilAddr currentFunction;

  /**
   * The object tracking information on the topmost stack frame.
   */
  private final Frame<D> currentFrame;

  /**
   * A map from the entry address of a function to the addresses of a set
   * representing its instances. Each instance corresponds to a call-site inside
   * the function where the stack frame was exited. Each exit point is mapped to
   * the address of the stack frame. This information is flow-sensitive as
   * unreachable stack frames are garbage-collected each time a frame is popped
   * off the stack.
   */
  private final AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> callsites;

  /**
   * A map from return sites to the call sites they belong to. The map is built
   * statically on each call taking the address of the next instruction following the call
   * as the canonical return site. The map is needed to match return points with the stack frames they belong to.
   */
  private final AVLMap<RReilAddr, RReilAddr> returnsites;

  /**
   * A map from addresses of inactive frames to their content.
   */
  private final AVLMap<AddrVar, FramePair<D>> inactiveFrames;

  /**
   * A flow-insensitive map of all functions, indexed by the canonical entry
   * point in each function. The value is a set of the beginning of basic
   * blocks that belong to the same function.
   * The information in this map as well as in {@link #reverse} is tracked at each program point although it is a global
   * program property. The reason for tracking this information per program
   * point is that adding new basic blocks to a function might change its
   * canonical entry point which requires a change in the data structure of the
   * flow-sensitive information.
   */
  private final AVLMap<RReilAddr, AVLSet<RReilAddr>> functions;

  /**
   * The owner function of each basic block. Functions are identified by their entry point.
   * This canonical entry point of each function is mapped to itself.
   *
   * @see #functions
   */
  private final AVLMap<RReilAddr, RReilAddr> reverse;


  /**
   * A designated special call site that is used to express that a function has no predecessor.
   */
  public final static AVLMap<RReilAddr, AddrVar> emptyCallsite = AVLMap.empty();


  public StackSegment () {
    // create a top-level stack frame that has been called at address 0
    currentFunction = RReilAddr.ZERO;
    currentFrame = new Frame<D>(this);

    // create an empty set of inactive stack frames
    callsites = AVLMap.<RReilAddr, AVLMap<RReilAddr, AddrVar>>empty();
    returnsites = AVLMap.<RReilAddr, RReilAddr>empty();
    inactiveFrames = AVLMap.<AddrVar, FramePair<D>>empty();

    // create the initial collection of functions
    reverse = AVLMap.<RReilAddr, RReilAddr>empty().bind(RReilAddr.ZERO, currentFunction);
    AVLSet<RReilAddr> functionBlocks = AVLSet.<RReilAddr>empty().add(RReilAddr.ZERO);
    functions = AVLMap.<RReilAddr, AVLSet<RReilAddr>>empty().bind(currentFunction, functionBlocks);
  }

  private StackSegment (RReilAddr currentFunction, Frame<D> currentFrame,
      AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> callsites,
      AVLMap<RReilAddr, RReilAddr> returnsites,
      AVLMap<AddrVar, FramePair<D>> inactive,
      AVLMap<RReilAddr, AVLSet<RReilAddr>> functions,
      AVLMap<RReilAddr, RReilAddr> reverse) {
    this.currentFunction = currentFunction;
    this.currentFrame = currentFrame;
    this.inactiveFrames = inactive;
    this.callsites = callsites;
    this.returnsites = returnsites;
    this.functions = functions;
    this.reverse = reverse;
  }

  @Override public P3<List<MemVar>, Boolean, D> initialize (D state) {

    state = state.introduce(stackAddr, Type.Address, Option.<BigInt>none());
    state = currentFrame.allocate(state);
    // XXX bm: the trigger mechanism is not used here. Can we remove it?
    return P3.<List<MemVar>, Boolean, D>tuple3(new LinkedList<MemVar>(), Boolean.TRUE, state);
  }

  @Override public SegmentWithState<D> triggerAssignment (Lhs lhs, Rhs rhs, D state) {
    assert false;
    // XXX bm: is this necessary anymore?
    Platform platform = state.getContext().getEnvironment().getPlatform();
    MemVar sp = MemVar.getVarOrNull(platform.getStackPointer());
    if (sp != null && lhs.getRegionId().equals(sp) && lhs.getOffset() == 0) {
      Option<NumVar> spVar = getStackPointer(state);
      if (spVar.isNone())
        return new SegmentWithState<D>(this, state);
      Range offset = state.queryRange(linear(term(spVar.get()), term(BigInt.MINUSONE, stackAddr)));
      if (offset.isConstant() && offset.getConstantOrNull().isZero()) {
        assert false;
        // TODO replace the current frame with a fresh one
      }
    }
    return new SegmentWithState<D>(this, state);
  }

  @Override public MemVarSet getChildSupportSet () {
    MemVarSet css = currentFrame.getChildSupportSet();
    for (P2<AddrVar, FramePair<D>> fp : inactiveFrames)
      css = css.insertAll(fp._2().getChildSupportSet());
    return css;
  }

  @Override public SegmentWithState<D> tryPrimitive (PrimOp prim, D state) {
    if (prim.is("currentStackFrameAddress", 1, 0)) {
      Lhs sp = prim.getOutArg(0);
      state = state.assignSymbolicAddressOf(sp, stackAddr);
      return new SegmentWithState<D>(this, state);
    }
    return null;
  }

  private StackSegment<D> addLocationsToCurrentFrame (RReilAddr target) {
    AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> callsites = this.callsites;
    AVLMap<RReilAddr, AVLSet<RReilAddr>> functions = this.functions;
    AVLMap<RReilAddr, RReilAddr> reverse = this.reverse;
    RReilAddr currentFunction = this.currentFunction;
    RReilAddr point = target;
    {
      Option<RReilAddr> ownerFunction = reverse.get(point);
      if (ownerFunction.isNone()) {
        // a basic block we haven't seen before, add it to the current function
        AVLSet<RReilAddr> basicBlocks = functions.get(currentFunction).get();
        functions.bind(currentFunction, basicBlocks.add(point));
        reverse = reverse.bind(point, currentFunction);
      } else if (!ownerFunction.get().equals(currentFunction)) {
        // the address belongs to a different function, merge it with the current function
        RReilAddr from;
        RReilAddr to;
        // functions with entry points at lower addresses will incorporate
        // the current function and made the new current function
        if (currentFunction.base() < ownerFunction.get().base()) {
          to = ownerFunction.get();
          from = currentFunction;
        } else {
          to = currentFunction;
          from = ownerFunction.get();
        }
        currentFunction = to; // update the current function
        AVLSet<RReilAddr> basicBlocks = functions.get(from).get();
        for (RReilAddr block : basicBlocks)
          reverse = reverse.remove(block).bind(block, to);
        basicBlocks = basicBlocks.union(functions.get(to).get());
        functions = functions.remove(from).bind(to, basicBlocks);
        Option<AVLMap<RReilAddr, AddrVar>> callsitesOpt = callsites.get(from);
        if (callsitesOpt.isSome())
          callsites = callsites.remove(from).bind(to, callsitesOpt.get());
      }
    }
    return new StackSegment<D>(currentFunction, currentFrame, callsites, returnsites, inactiveFrames, functions, reverse);
  }

  /**
   * If the value on top of the stack is equal to the address of the instruction following this branch,
   * then it is a call.
   */
  private boolean isCall (BigInt stackOffset, D state, ProgramPoint nextInstruction) {
    Platform platform = state.getContext().getEnvironment().getPlatform();
    BigInt offsetInBits = stackOffset.mul(BigInt.EIGHT);
    FiniteRange access = FiniteRange.of(offsetInBits, platform.defaultArchitectureSize());
    Range value = state.queryRange(currentFrame.getDynamicRegion(), access);
    if (value != null && value.isConstant()) {
      BigInt r = value.getConstantOrNull();
      return r.isEqualTo(BigInt.of(nextInstruction.getAddress().base()));
    }
    return false;
  }

  /**
   * If stack pointer points in front of the current stack frame.
   */
  private static boolean isReturn (BigInt stackOffset) {
    return stackOffset.isPositive();
  }

  /**
   * If the stack offset is 0 then no new stack frame is allocated for a call.
   */
  private static boolean isTailCall (BigInt stackOffset) {
    return stackOffset.isZero();
  }

  @Override public SegmentWithState<D> evalJump (RReilAddr target, D state, ProgramPoint current, ProgramPoint next) {
    NumVar spVar = getStackPointer(state).get();
    // compute the value of the current stack pointer relative to the sp region in the current frame
    Range offsetRange = state.queryRange(linear(
        term(spVar),
        term(BigInt.MINUSONE, stackAddr),
        term(BigInt.MINUSONE, currentFrame.getBarrier())));
    if (!offsetRange.isConstant()) {
      state.getContext().addWarning(new NonConstantStackOffset(spVar, offsetRange));
      return null;
    }

    // the offset of sp is constant which it has to be for all sane stack frames
    BigInt offset = offsetRange.getConstantOrNull();
    if (isCall(offset, state, next)) {
      state = setInstructionPointer(target, state);
      SegmentWithState<D> pair = pushFrame(state, current, next, target);
      return pair;
    }
    if (isReturn(offset)) {
      // remove the current stack frame and make the jumped to one the new current one
      Option<RReilAddr> callsite = returnsites.get(target);
      if (callsite.isNone() || !currentFrame.prevFrames.contains(callsite.get())) {
        state.getContext().addWarning(new UnknownStackFrame(target));
        // TODO bm: actually we see a return used as a call/jump here, so we should treat it like that
        throw new IllegalStateException("We return to a stack frame that we have not seen before.");
      }
      D predState = currentFrame.getPredecessor(callsite.get(), state);
      predState = setInstructionPointer(target, predState);
      P2<StackSegment<D>, D> pair = popFrame(predState, callsite.get());
      StackSegment<D> newSegment = pair._1();
      D newState = pair._2();
      newSegment = newSegment.addLocationsToCurrentFrame(target);
      return new SegmentWithState<D>(newSegment, newState);
    }
    if (isTailCall(offset)) {
      // TODO: implement
      // replace the current stack frame with a fresh one
//      predState = setInstructionPointer(target, predState);
      throw new UnimplementedMethodException("Handling of tail calls not yet implemented.");
    }
    // intra-procedural JUMP: add the targets to the addresses of this function
    D resultState = setInstructionPointer(target, state);
    StackSegment<D> newSegment = addLocationsToCurrentFrame(target);
    return new SegmentWithState<D>(newSegment, resultState);
  }

  private D setInstructionPointer (RReilAddr target, D state) {
    Platform platform = state.getContext().getEnvironment().getPlatform();
    assert target.offset() == 0 : "Native jumps/calls cannot jump to intra-RREIL addresses.";
    MemVar ip = MemVar.getVarOrNull(platform.getInstructionPointer());
    int architectureSize = platform.defaultArchitectureSize();
    FiniteRange access = FiniteRange.of(0, architectureSize - 1);
    Option<NumVar> ipVar = state.pickSpecificField(ip, access);
    Rlin rhs = fin.literal(architectureSize, BigInt.of(target.base()));
    // the alternative to add an offset to IP is not possible as soon as the IP is not a constant value
    // we would introduce spurious jump targets if adding an offset to a range of values
    // TODO: see from where the range of values for IP come from
//    BigInt ipValue = state.queryRange(ipVar.get()).getConstantOrNull();
//    assert ipValue != null : "IP should be constant!";
//    BigInt offset = BigInt.of(target.base()).sub(ipValue);
//    Bin rhs = fin.binary(fin.linear(architectureSize, ipVar.get()), BinOp.Add, fin.literal(architectureSize, offset));
    state = state.evalFiniteAssign(fin.variable(architectureSize, ipVar.get()), rhs);
    return state;
  }

  private Option<NumVar> getStackPointer (D state) {
    Platform platform = state.getContext().getEnvironment().getPlatform();
    MemVar sp = MemVar.getVarOrNull(platform.getStackPointer());
    if (sp == null)
      return Option.none();
    FiniteRange access = FiniteRange.of(0, platform.defaultArchitectureSize() - 1);
    return state.pickSpecificField(sp, access);
  }

  /**
   * Make the current stack frame inactive and install a fresh stack frame as the current frame.
   *
   * @param state the child state
   * @param exitPoint the program location where the call originated from, serves as secondary key
   * @param returnPoint the program location where the next instruction after the call is--i.e. the point
   *          the call should return to
   * @param entryPoint the function that is being called
   * @return the updated stack segment containing a new frame
   */
  private SegmentWithState<D> pushFrame (D state, ProgramPoint exitPoint, ProgramPoint returnPoint, RReilAddr entryPoint) {
    AVLMap<AddrVar, FramePair<D>> inactiveFrames = this.inactiveFrames;
    AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> callsites = this.callsites;
    AVLMap<RReilAddr, AVLSet<RReilAddr>> functions = this.functions;
    AVLMap<RReilAddr, RReilAddr> reverse = this.reverse;
    RReilAddr currentFunction = this.currentFunction;
    AVLMap<RReilAddr, RReilAddr> returnsites = this.returnsites;

    if (!reverse.contains(exitPoint.getAddress())) {
      reverse = reverse.bind(exitPoint.getAddress(), currentFunction);
      AVLSet<RReilAddr> basicBlocks = functions.get(currentFunction).get();
      functions = functions.bind(currentFunction, basicBlocks.add(exitPoint.getAddress()));
      // One would expect that jumping to a basic block is required before a
      // call in that basic block is seen, thus, the canonical address of the
      // function should be that of the basic block and never that of a function exit.
      assert basicBlocks.getMin().get().base() <= exitPoint.getAddress().base();
    }
    // add a new function if the entry point does not belong to any known function
    if (!reverse.contains(entryPoint)) {
      reverse = reverse.bind(entryPoint, entryPoint);
      functions = functions.bind(entryPoint, AVLSet.<RReilAddr>empty().add(entryPoint));
    }

    Frame<D> inactiveFrame;
    AddrVar inactiveAddr = NumVar.freshAddress("frame_" + exitPoint.getAddress().toShortString());
    {
      Option<NumVar> stackPointer = getStackPointer(state);
      if (stackPointer.isNone())
        throw new IllegalStateException("Stack pointer has not been initialized in the analysis.");
      Platform platform = state.getContext().getEnvironment().getPlatform();
      int spSize = platform.defaultArchitectureSize();
      P2<Frame<D>, D> pair = currentFrame.makeInactive(exitPoint, spSize, stackPointer.get(), inactiveAddr, state);
      inactiveFrame = pair._1();
      state = pair._2();
    }
    // allocate the new frame and make it point to the previous one
    FlagVar flag = NumVar.freshFlag("f(" + entryPoint.toShortString() + "->" +
      exitPoint.getAddress().toShortString() + ")");
    Frame<D> currentFrame = new Frame<D>(this, exitPoint.getAddress(), flag);
    state = currentFrame.allocate(state, exitPoint.getAddress());

    // insert the current stack frame into the map of inactive stack frames
    AVLMap<RReilAddr, AddrVar> currentCallsites = callsites.get(currentFunction).getOrElse(emptyCallsite);
    if (currentCallsites.contains(exitPoint.getAddress())) {
      // the current stack frame was already exited once at this address, so
      // merge it with the existing one
      AddrVar frameAddr = currentCallsites.get(exitPoint.getAddress()).get();
      FramePair<D> fPair = inactiveFrames.get(frameAddr).get();
      P2<D, FramePair<D>> pair = fPair.summarize(inactiveAddr, inactiveFrame, frameAddr, state);
      state = pair._1();
      fPair = pair._2();
      inactiveFrames = inactiveFrames.bind(frameAddr, fPair);
    } else {
      // it is the first instance of this function leaving at exitPoint, so
      // create a new entry
      inactiveFrames = inactiveFrames.bind(inactiveAddr, new FramePair<D>(inactiveFrame));
      currentCallsites = currentCallsites.bind(exitPoint.getAddress(), inactiveAddr);
      callsites = callsites.bind(currentFunction, currentCallsites);
      returnsites = returnsites.bind(returnPoint.getAddress(), exitPoint.getAddress());
    }

    currentFunction = entryPoint;
    StackSegment<D> seg =
      new StackSegment<D>(currentFunction, currentFrame, callsites, returnsites, inactiveFrames, functions, reverse);
    return new SegmentWithState<D>(seg, state);
  }

  /**
   * Remove the current frame and make the previous frame active. Perform a garbage collection on not anymore reachable
   * frames.
   *
   * @param predState The state with the flag set from this frame to the predecessor
   * @param callsite The callsite of the predecessor
   * @return the updated stack segment containing a new frame
   */
  private P2<StackSegment<D>, D> popFrame (D predState, RReilAddr callsite) {
    AVLMap<RReilAddr, AVLSet<RReilAddr>> functions = this.functions;
    AVLMap<RReilAddr, RReilAddr> reverse = this.reverse;

    Frame<D> predFrame = currentFrame;
    AVLMap<AddrVar, FramePair<D>> inactiveFrames = this.inactiveFrames;
    AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> callsites = this.callsites;
    predState = predFrame.deallocate(predState);

    RReilAddr predFunction = reverse.get(callsite).get();
    AddrVar predAddr = callsites.get(predFunction).get().get(callsite).get();
    FramePair<D> predFramePair = inactiveFrames.get(predAddr).get();
    if (predFramePair.materialized == null) {
      // we require a materialized frame
      if (predFramePair.summarized == null) {
        // TODO: raise a warning
        throw new IllegalStateException("The predecessor frame was not found.");
      }
      P2<D, FramePair<D>> resPair = predFramePair.materialize(predAddr, predState);
      predFramePair = resPair._2();
      predState = resPair._1();
    }
    predFrame = predFramePair.materialized;
    predFramePair = predFramePair.claimMaterialized();
    inactiveFrames = inactiveFrames.bind(predAddr, predFramePair);

    Option<NumVar> stackPointer = getStackPointer(predState);
    if (stackPointer.isNone())
      throw new IllegalStateException("Stack pointer has not been initialized in the analysis.");
    Platform platform = predState.getContext().getEnvironment().getPlatform();
    int spSize = platform.defaultArchitectureSize();
    P2<Frame<D>, D> predPair = predFrame.makeActive(spSize, stackPointer.get(), predAddr, predState);
    predFrame = predPair._1();
    predState = predPair._2();

    // perform garbage collection to remove unreachable stack frames by gathering all live exit nodes
    AVLSet<RReilAddr> worklist = predFrame.getAllPredecessors();
    AVLSet<RReilAddr> unreachable = AVLSet.<RReilAddr>empty();

    // commence by setting unreachable to the whole set of frames
    for (P2<RReilAddr, AVLMap<RReilAddr, AddrVar>> p : callsites)
      for (RReilAddr exitAddr : p._2().keys())
        unreachable = unreachable.add(exitAddr);

    while (!worklist.isEmpty()) {
      RReilAddr next = worklist.getMax().get();
      worklist = worklist.remove(next);
      unreachable = unreachable.remove(next);

      RReilAddr entryPoint = reverse.get(next).get();
      AddrVar frameAddr = callsites.get(entryPoint).get().get(next).get();
      FramePair<D> fPair = inactiveFrames.get(frameAddr).get();
      Frame<D> frame = fPair.materialized;
      if (frame != null) {
        for (RReilAddr addr : frame.getAllPredecessors()) {
          if (unreachable.contains(addr))
            worklist = worklist.add(addr);
        }
        frame = fPair.summarized;
        if (frame != null) {
          for (RReilAddr addr : frame.getAllPredecessors()) {
            if (unreachable.contains(addr))
              worklist = worklist.add(addr);
          }
        }
      }
    }
    for (RReilAddr exitAddr : unreachable) {
      RReilAddr entryAddr = reverse.get(exitAddr).get();
      AVLMap<RReilAddr, AddrVar> functionCallsites = callsites.get(entryAddr).get();
      AddrVar var = functionCallsites.get(exitAddr).get();
      functionCallsites = functionCallsites.remove(exitAddr);
      callsites = callsites.bind(entryAddr, functionCallsites);
      FramePair<D> fPair = inactiveFrames.get(var).get();
      if (fPair.materialized != null)
        predState = fPair.materialized.deallocate(predState);
      if (fPair.summarized != null)
        predState = fPair.summarized.deallocate(predState);
      inactiveFrames = inactiveFrames.remove(var);
      predState = predState.project(var);
    }
    StackSegment<D> seg =
      new StackSegment<D>(predFunction, predFrame, callsites, returnsites, inactiveFrames, functions, reverse);
    return P2.tuple2(seg, predState);
  }

  private void derefFrame (List<RegionAccess<D>> res, AbstractPointer dRef, D state, Frame<D> frame) {
    WarningsContainer warn = state.getContext().getWarningsChannel();
    Linear barrier = linear(frame.getBarrier());
    // compute the possible state space for accesses relative to SP
    D dynState = null;
    try {
      dynState = dRef.calcBelowAccess(state, barrier);
    } catch (Unreachable e) {
    }
    // compute the possible state space for accesses relative to BP
    D staticState = state;
    try {
      staticState = dRef.calcBelowAccess(staticState, Linear.ZERO);
      staticState = dRef.calcAboveAccess(staticState, barrier);
    } catch (Unreachable e) {
      staticState = null;
    }
    // compute the possible state space for accesses in the previous stack frames
    D prevState = null;
    try {
      prevState = dRef.calcAboveAccess(state, Linear.ZERO);
    } catch (Unreachable e) {
    }

    if (dynState != null && staticState != null) {
      warn.addWarning(new BufferOverflow(dRef));
      staticState = null;
      prevState = null;
    }

    if (staticState != null && prevState != null) {
      warn.addWarning(new ReturnAddressOverwritten(dRef));
      prevState = null;
    }

    SymbolicOffset offset = new SymbolicOffset(dRef.offset);
    if (dynState != null) {
      MemVar region = frame.getDynamicRegion();
      assert region != null;
      RegionAccess<D> resolveRegion =
        new RegionAccess<D>(new AbstractMemPointer(region, offset), new SegmentWithState<D>(this, dynState));
      RegionAccess<D> rdRef = resolveRegion.addOffset(barrier.negate());
      res.add(rdRef);
    }
    if (staticState != null) {
      MemVar region = frame.getStaticRegion();
      assert region != null;
      RegionAccess<D> rdRef =
        new RegionAccess<D>(new AbstractMemPointer(region, offset),
          new SegmentWithState<D>(this, staticState));
      res.add(rdRef);
    }
    if (prevState == null)
      return;
    // descend into inactive stack frames
    List<P2<RReilAddr, D>> preds = frame.getAllPredecessors(prevState);
    if (preds.isEmpty())
      warn.addWarning(new StackUnderflow(dRef));

    for (P2<RReilAddr, D> pair : preds) {
      RReilAddr exitPoint = pair._1();
      RReilAddr entryPoint = reverse.getOrNull(exitPoint);
      AddrVar prevAddr = callsites.get(entryPoint).get().get(exitPoint).get();
      FramePair<D> fPair = inactiveFrames.get(prevAddr).get();
      // if a materialized frame exists in the pair, create a deref of it,
      // then summarize it and descend;
      // in case no materialized frame exists, materialize one, deref it but
      // don't recurse; these two rules ensure termination
      Frame<D> prevFrame = fPair.materialized;
      boolean descend = true;
      D funState = pair._2();
      StackSegment<D> stack = this;
      if (prevFrame == null) {
        // no materialized frame exists
        descend = false; // TODO: the logic is broken here, as we will never descend if we follow this path; think about
                         // actually computing a fixpoint here
        P2<D, FramePair<D>> matPair = fPair.materialize(prevAddr, funState);
        funState = matPair._1();
        AVLMap<AddrVar, FramePair<D>> inactive = this.inactiveFrames.bind(prevAddr, matPair._2());
        stack =
          new StackSegment<D>(currentFunction, currentFrame, callsites, returnsites, inactive, functions, reverse);
      }
      assert prevFrame != null; // XXX why is it != null?
      AbstractPointer prevFramedRef = dRef.setAddr(prevAddr).addOffset(linear(term(prevFrame.spAtCall)));
      if (descend)
        stack.derefFrame(res, prevFramedRef, funState, prevFrame);
    }
  }

  @Override public List<RegionAccess<D>> dereference (Lin sourcePointerValue, AbstractPointer dRef, D state) {
    if (dRef.isAbsolute())
      return null;
    AddrVar addr = dRef.address;
    if (!(stackAddr.equalTo(addr) || inactiveFrames.contains(addr)))
      return null;

    List<RegionAccess<D>> res = new LinkedList<RegionAccess<D>>();
    if (stackAddr.equalTo(addr)) {
      derefFrame(res, dRef, state, currentFrame);
      return res;
    }

    // the address lies in one of the inactive frames
    FramePair<D> fPair = inactiveFrames.getOrNull(addr);
    Frame<D> frame = fPair.materialized;
    StackSegment<D> localSegment = this;
    if (frame == null) {
      // the inactive frame has only a summary: materialize it and then access it
      P2<D, FramePair<D>> matPair = fPair.materialize(addr, state);
      frame = matPair._2().materialized;
      state = matPair._1();
      AVLMap<AddrVar, FramePair<D>> inactive = this.inactiveFrames.bind(addr, matPair._2());
      localSegment =
        new StackSegment<D>(currentFunction, currentFrame, callsites, returnsites, inactive, functions, reverse);
    }
    localSegment.derefFrame(res, dRef, state, frame);
    return res;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("parents: ");
    builder.append(currentFrame.getAllPredecessors());
    if (!callsites.isEmpty()) {
      builder.append("; ");
      builder.append("callsites: ");
      builder.append(callsites);
    }
    if (!reverse.isEmpty()) {
      builder.append("; ");
      builder.append("frames: ");
      builder.append(functions);
    }
    return StringHelpers.indentMultiline(NAME + ": ", builder.toString());
  }

  @Override public SegCompatibleState<D> makeCompatible (Segment<D> otherRaw, D state, D otherState) {
    StackSegment<D> other = (StackSegment<D>) otherRaw;
    AVLMap<RReilAddr, AVLSet<RReilAddr>> functions = this.functions;
    AVLMap<RReilAddr, RReilAddr> reverse = this.reverse;
    RReilAddr currentFunction = this.currentFunction;
    AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> thisCallsites = this.callsites;
    AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> otherCallsites = other.callsites;
    // the return sites are a simple mapping to callsite addresses, thus can just be joined together
    AVLMap<RReilAddr, RReilAddr> returnsites = this.returnsites.union(other.returnsites);

    // Exit points of a stack frame never change. Thus the only properties that have to be adjusted when making two
    // states compatible are the canonical entry point and the abstract addresses. For the latter, it is possible that an
    // inactive stack frame uses a different address in each state, so they have to be adjusted. Moreover, a stack frame
    // might be present in one state but missing in the other in which case it has to be added.

    // Adjust the canonical entry points.
    ThreeWaySplit<AVLMap<RReilAddr, AVLSet<RReilAddr>>> splitFuncs = functions.split(other.functions);


    for (P2<RReilAddr, AVLSet<RReilAddr>> pair : splitFuncs.inBothButDiffering()) {
      // The canonical address is the same, so we just need to merge set of addresses in this function.
      AVLSet<RReilAddr> blocks = other.functions.get(pair._1()).get();
      for (RReilAddr addr : blocks.difference(pair._2()))
        reverse = reverse.bind(addr, pair._1());
      functions = functions.bind(pair._1(), pair._2().union(blocks));
    }

    // add or enlarge partitions in the first domain based on the partitions in the second domain
    for (P2<RReilAddr, AVLSet<RReilAddr>> pair : splitFuncs.onlyInSecond()) {
      RReilAddr otherECR = pair._1();
      AVLSet<RReilAddr> otherBlocks = pair._2();
      if (reverse.contains(otherECR)) {
        RReilAddr thisECR = reverse.get(pair._1()).get();
        AVLSet<RReilAddr> thisBlocks = functions.get(thisECR).get();
        AVLSet<RReilAddr> union = thisBlocks.union(otherBlocks);
        // the equivalence class representative "ECR" of the other block set lies in blocks
        if (otherECR.base() < thisECR.base()) {
          // re-map the elements in thisBlocks to the new ECR otherECR
          for (RReilAddr addr : union)
            reverse = reverse.bind(addr, otherECR);
          functions = functions.remove(thisECR).bind(otherECR, union);
          thisCallsites = thisCallsites.remove(thisECR).bind(otherECR, thisCallsites.get(thisECR).get());
        } else {
          // re-map the new elements of otherBlocks to the old ECR thisECR
          for (RReilAddr addr : otherBlocks.difference(thisBlocks))
            reverse = reverse.bind(thisECR, addr);
          functions = functions.bind(thisECR, union);
          otherCallsites = otherCallsites.remove(otherECR).bind(thisECR, otherCallsites.get(otherECR).get());
        }
      } else {
        // there is no partition in this that overlaps with otherBlocks
        for (RReilAddr addr : otherBlocks)
          reverse = reverse.bind(addr, otherECR);
        functions = functions.bind(otherECR, otherBlocks);
        thisCallsites = thisCallsites.bind(otherECR, emptyCallsite);
      }
    }

    // if merging in the partitions from the second domain has created ECRs that lie in another partition of the first
    // domain, merge them; in order to find these partitions, we merely need to check the partitions in this domain that
    // were not contained in the other domain
    for (P2<RReilAddr, AVLSet<RReilAddr>> pair : splitFuncs.onlyInFirst()) {
      RReilAddr fstECR = pair._1();
      AVLSet<RReilAddr> fstBlocks = pair._2();
      Option<RReilAddr> sndECROpt = other.reverse.get(fstECR);
      if (sndECROpt.isSome()) {
        RReilAddr sndECR = sndECROpt.get();
        assert !fstECR.equals(sndECR);
        // the set previously in the first map has been altered by merging in a set from the second map
        AVLSet<RReilAddr> sndBlocks = functions.get(sndECR).get();
        AVLSet<RReilAddr> union = fstBlocks.union(sndBlocks);
        for (RReilAddr addr : union)
          reverse = reverse.bind(addr, fstECR);
        if (fstECR.base() < sndECR.base()) {
          for (RReilAddr addr : union) {
            reverse = reverse.bind(addr, fstECR);
          }
          functions = functions.bind(fstECR, union);
          thisCallsites = thisCallsites.remove(sndECR).bind(fstECR, thisCallsites.get(sndECR).get());
          otherCallsites = otherCallsites.remove(sndECR).bind(fstECR, otherCallsites.get(sndECR).get());
        } else {
          for (RReilAddr addr : union) {
            reverse = reverse.bind(addr, sndECR);
          }
          functions = functions.bind(sndECR, union);
        }
      } else {
        otherCallsites = otherCallsites.bind(fstECR, emptyCallsite);
      }
    }

    ThreeWaySplit<AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>>> splitCallsites = thisCallsites.split(otherCallsites);
    AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> differingCallsites = splitCallsites.inBothButDiffering();
    // need to add the callsites only existent in one side to the other side
    for (P2<RReilAddr, AVLMap<RReilAddr, AddrVar>> pair : splitCallsites.onlyInFirst()) {
      otherCallsites = otherCallsites.bind(pair._1(), emptyCallsite);
    }
    for (P2<RReilAddr, AVLMap<RReilAddr, AddrVar>> pair : splitCallsites.onlyInSecond()) {
      thisCallsites = thisCallsites.bind(pair._1(), emptyCallsite);
      differingCallsites = differingCallsites.bind(pair._1(), emptyCallsite);
    }

    AVLMap<RReilAddr, AVLMap<RReilAddr, AddrVar>> callsites = thisCallsites;
    AVLMap<AddrVar, FramePair<D>> inactiveFrames = this.inactiveFrames;

    for (P2<RReilAddr, AVLMap<RReilAddr, AddrVar>> incompat : differingCallsites) {
      AVLMap<RReilAddr, AddrVar> thisExits = incompat._2();
      AVLMap<RReilAddr, AddrVar> otherExits = otherCallsites.get(incompat._1()).get();
      ThreeWaySplit<AVLMap<RReilAddr, AddrVar>> splitExits = thisExits.split(otherExits);
      for (P2<RReilAddr, AddrVar> call : splitExits.inBothButDiffering()) {
        AddrVar thisVar = call._2();
        AddrVar otherVar = otherExits.get(call._1()).get();
        if (thisVar.getStamp() < otherVar.getStamp()) {
          otherState = otherState.substitute(otherVar, thisVar);
          FramePair<D> thisPair = inactiveFrames.get(thisVar).get();
          FramePair<D> otherPair = other.inactiveFrames.get(otherVar).get();
          P3<D, D, FramePair<D>> merged = FramePair.merge(state, thisPair, otherState, otherPair);
          state = merged._1();
          otherState = merged._2();
          inactiveFrames = inactiveFrames.bind(thisVar, merged._3());
        } else {
          if (!thisVar.equalTo(otherVar)) {
            state = state.substitute(thisVar, otherVar);
            thisExits = thisExits.bind(call._1(), otherVar);
          }
          FramePair<D> thisPair = inactiveFrames.get(thisVar).get();
          FramePair<D> otherPair = other.inactiveFrames.get(otherVar).get();
          P3<D, D, FramePair<D>> merged = FramePair.merge(state, thisPair, otherState, otherPair);
          state = merged._1();
          otherState = merged._2();
          inactiveFrames = inactiveFrames.remove(thisVar).bind(otherVar, merged._3());
        }
      }

      for (P2<RReilAddr, AddrVar> call : splitExits.onlyInFirst()) {
        AddrVar var = call._2();
        otherState = otherState.introduce(var, Type.Address, Option.<BigInt>none());
        FramePair<D> fPair = inactiveFrames.get(var).get();
        otherState = fPair.allocate(otherState);
      }
      for (P2<RReilAddr, AddrVar> call : splitExits.onlyInSecond()) {
        AddrVar var = call._2();
        state = state.introduce(var, Type.Address, Option.<BigInt>none());
        FramePair<D> fPair = other.inactiveFrames.get(var).get();
        state = fPair.allocate(state);
        inactiveFrames = inactiveFrames.bind(var, fPair);
        thisExits = thisExits.bind(call._1(), var);
      }
      callsites = callsites.bind(incompat._1(), thisExits);
    }

    P3<D, D, Frame<D>> triple = Frame.merge(state, currentFrame, otherState, other.currentFrame);
    state = triple._1();
    otherState = triple._2();
    Frame<D> currentFrame = triple._3();

    StackSegment<D> seg =
      new StackSegment<D>(currentFunction, currentFrame, callsites, returnsites, inactiveFrames, functions, reverse);
    return new SegCompatibleState<D>(seg, state, otherState);
  }

  /**
   * A frame pair can hold a recently-used materialized frame and a summary
   * frame. At least one of them exists in any frame pair.
   *
   * @author Axel Simon
   */
  private static class FramePair<D extends MemoryDomain<D>> {
    private final Frame<D> materialized;
    private final Frame<D> summarized;

    public FramePair (Frame<D> f) {
      materialized = f;
      summarized = null;
    }

    private FramePair (Frame<D> mat, Frame<D> sum) {
      materialized = mat;
      summarized = sum;
    }

    public P2<D, FramePair<D>> summarize (AddrVar inactiveAddr, Frame<D> inactiveFrame,
        AddrVar frameAddr, D state) {
      Frame<D> resMat = materialized;
      Frame<D> resSum = summarized;
      if (resSum == null) {
        resSum = resMat;
        resMat = inactiveFrame;
        state = state.substitute(inactiveAddr, frameAddr);
      } else if (resMat != null) {
        P2<Frame<D>, D> pair = resSum.fold(resMat, state);
        resSum = pair._1();
        resMat = inactiveFrame;
        state = pair._2();
      }
      FramePair<D> res = new FramePair<D>(resMat, resSum);
      return P2.tuple2(state, res);
    }

    /**
     * Create a new materialized frame. If a materialized frame already exists,
     * it is replaced by a new one.
     *
     * @param state the input state
     * @return the resulting state and the frame pair with a fresh materialized frame
     */
    public P2<D, FramePair<D>> materialize (AddrVar frameAddr, D state) {
      Frame<D> prevMat = materialized;
      P2<Frame<D>, D> pair = summarized.expand(state);
      Frame<D> resMat = pair._1();
      Frame<D> resSum = summarized;
      state = pair._2();
      if (prevMat != null) {
        P2<Frame<D>, D> sumPair = summarized.fold(prevMat, state);
        resSum = sumPair._1();
        state = sumPair._2();
      }
      FramePair<D> res = new FramePair<D>(resMat, resSum);
      return P2.tuple2(state, res);
    }

    public static <D extends MemoryDomain<D>> P3<D, D, FramePair<D>> merge (D fstDom, FramePair<D> fst, D sndDom,
        FramePair<D> snd) {
      Frame<D> fstMat = fst.materialized;
      Frame<D> sndMat = snd.materialized;
      Frame<D> fstSum = fst.summarized;
      Frame<D> sndSum = snd.summarized;
      // how we make pairs compatible, (..) denotes folding, .. denotes join
      // fstMat fstSum sndMat sndSum resMat resSum
      // * *                  **
      // * * **
      // * + * + ** ++
      // * + + (*+)+
      // * + * (*+)*
      if (fstMat != null && fstSum != null && sndMat != null && sndSum != null) {
        // both pairs have materialized and summary frames: merge them
        // individually
        P3<D, D, Frame<D>> matTriple = Frame.merge(fstDom, fstMat, sndDom, sndMat);
        P3<D, D, Frame<D>> sumTriple = Frame.merge(matTriple._1(), fstSum, matTriple._2(), sndSum);
        FramePair<D> res = new FramePair<D>(matTriple._3(), sumTriple._3());
        return P3.tuple3(sumTriple._1(), sumTriple._2(), res);
      }
      if (fstMat != null && fstSum == null && sndMat != null && sndSum == null) {
        // both pairs have materialized and materialized frames: merge them
        P3<D, D, Frame<D>> matTriple = Frame.merge(fstDom, fstMat, sndDom, sndMat);
        FramePair<D> res = new FramePair<D>(matTriple._3(), null);
        return P3.tuple3(matTriple._1(), matTriple._2(), res);
      }
      // ensure that both pairs have only summary frames
      if (fstSum == null)
        fstSum = fstMat;
      else if (fstMat != null) {
        P2<Frame<D>, D> pair = fstSum.fold(fstMat, fstDom);
        fstSum = pair._1();
        fstDom = pair._2();
      }
      if (sndSum == null)
        sndSum = sndMat;
      else if (sndMat != null) {
        P2<Frame<D>, D> pair = sndSum.fold(sndMat, sndDom);
        sndSum = pair._1();
        sndDom = pair._2();
      }
      // both pairs have only summary frames: merge them
      P3<D, D, Frame<D>> sumTriple = Frame.merge(fstDom, fstSum, sndDom, sndSum);
      FramePair<D> res = new FramePair<D>(null, sumTriple._3());
      return P3.tuple3(sumTriple._1(), sumTriple._2(), res);
    }

    public D allocate (D state) {
      if (materialized != null)
        state = materialized.allocate(state);
      if (summarized != null)
        state = summarized.allocate(state);
      return state;
    }

    public FramePair<D> claimMaterialized () {
      return new FramePair<D>(null, summarized);
    }

    @Override public String toString () {
      String res = "<mat=";
      if (materialized == null)
        res = res + "no";
      else
        res = res + "callers:" + materialized.prevFrames.size();
      res = res + ",sum=";
      if (summarized == null)
        res = res + "no";
      else
        res = res + "yes";
      return res + ">";
    }

    public MemVarSet getChildSupportSet () {
      MemVarSet css = MemVarSet.empty();
      if (materialized != null)
        css = css.insertAll(materialized.getChildSupportSet());
      if (summarized != null)
        css = css.insertAll(summarized.getChildSupportSet());
      return css;
    }
  }

  private static class Frame<D extends MemoryDomain<D>> {
    private static final AVLMap<RReilAddr, FlagVar> noPreds = AVLMap.empty();
    private final StackSegment<D> stack;
    private final MemVar bpRelative;
    private final NumVar barrier;
    private final MemVar spRelative;

    /**
     * A backup of the stack pointer at the time when the next function is
     * called. This variable is null if this frame is the current frame.
     */
    private final NumVar spAtCall;

    /**
     * Predecessors of this frame are tracked by a map from the call site to a
     * flag indicating if the frame is feasible. This map contains no entry for
     * the top-most frame. For all non-summary frames, this entry contains
     * exactly one entry.
     */
    private final AVLMap<RReilAddr, FlagVar> prevFrames;

    private Frame (StackSegment<D> stack, MemVar bpRelative, NumVar barrier, MemVar spRelative,
        AVLMap<RReilAddr, FlagVar> prevFrames,
        NumVar spAtCall) {
      this.stack = stack;
      this.bpRelative = bpRelative;
      this.barrier = barrier;
      this.spRelative = spRelative;
      this.prevFrames = prevFrames;
      this.spAtCall = spAtCall;
    }

    /**
     * Create an active, top-level frame.
     */
    public Frame (StackSegment<D> stack) {
      this(stack, stack.currentBpRelative, stack.currentBarrier, stack.currentSpRelative, noPreds, null);
    }

    /**
     * Create an active frame with predecessor.
     *
     * @param callsite the point where this function was called
     * @param flag a fresh flag that indicates if this call/return edges is
     *          feasible (should always be set to one initially)
     */
    public Frame (StackSegment<D> stack, RReilAddr callsite, FlagVar flag) {
      this(stack, stack.currentBpRelative, stack.currentBarrier, stack.currentSpRelative, noPreds.bind(callsite, flag),
          null);
    }

    public MemVarSet getChildSupportSet () {
      return MemVarSet.of(bpRelative, spRelative);
    }

    /**
     * When we leave the current function through a call
     * make this current stack frame inactive by renaming its variables and
     * adjusting the frame pointer by the given offset.
     *
     * @param entryPoint the point in this stack frame where the next function is called
     * @param spSize the size of the stack pointer
     * @param spVar the numeric variable containing the stack pointer
     * @param frameAddr the address of this inactive frame, will be allocated in the returned state
     * @param state the domain state
     * @return a tuple of containing the now inactive stack frame and the modified state
     */
    public P2<Frame<D>, D> makeInactive (ProgramPoint entryPoint, int spSize, NumVar spVar,
        AddrVar frameAddr, D state) {
      assert spAtCall == null;
      String pos = entryPoint.getAddress().toShortString();
      MemVar newBpRelative = MemVar.fresh("BP@" + pos);
      NumVar newBarrier = NumVar.fresh("BP|SP@" + pos);
      MemVar newSpRelative = MemVar.fresh("SP@" + pos);
      NumVar newSpAtCall = NumVar.fresh("SPdiff@" + pos);
      state = state.substituteRegion(bpRelative, newBpRelative);
      state = state.substitute(barrier, newBarrier);
      state = state.substituteRegion(spRelative, newSpRelative);
      // rename all the flags to ensure that they don't clash later
      AVLMap<RReilAddr, FlagVar> newPrevFrames = noPreds;
      for (P2<RReilAddr, FlagVar> pred : prevFrames) {
        FlagVar flag = NumVar.freshFlag("f(" + pos + "->" + pred._1().toShortString() + ")");
        state = state.substitute(pred._2(), flag);
        newPrevFrames = newPrevFrames.bind(pred._1(), flag);
      }

      // Make the current stack frame inactive by performing the following actions:
      // 1. Compute the difference between SP and the beginning of the stack frame
      // and store it in spAtCall. This difference is later used to set the SP on return.
      // 2. Make all pointers that currently point to the topmost stackframe point to frameAddr which is the new name for
      // the frame that is being made inactive. This is done by substituting stackAddr with frameAddr.
      // 3. The stack pointer now points to the end of the inactive frame. Create a new address called stackAddr and let
      // SP point to it.
      state = state.introduce(newSpAtCall, Type.Zeno, Option.<BigInt>none());
      Linear spOffset = linear(term(spVar), term(BigInt.MINUSONE, stack.stackAddr));
      state =
        state.evalFiniteAssign(fin.variable(spSize, newSpAtCall), fin.linear(spSize, spOffset));
      state = state.substitute(stack.stackAddr, frameAddr);
      state = state.introduce(stack.stackAddr, Type.Address, Option.<BigInt>none());
      state = state.evalFiniteAssign(fin.variable(spSize, spVar),
          fin.linear(spSize, linear(stack.stackAddr)));
      Frame<D> newFrame = new Frame<D>(stack, newBpRelative, newBarrier, newSpRelative, newPrevFrames, newSpAtCall);
      return P2.tuple2(newFrame, state);
    }

    /**
     * When entering a previous frame through a return.
     * Make the frame active by restoring its stack pointer value
     * and renaming the frame variables to be the current ones.
     *
     * @param spSize the size of the stack pointer
     * @param spVar the numeric variable containing the stack pointer
     * @param frameAddr the address of the inactive frame
     * @param state the domain state
     * @return a tuple of containing the now active stack frame and the modified state
     */
    public P2<Frame<D>, D> makeActive (int spSize, NumVar spVar, AddrVar frameAddr, D state) {
      // Make any pointer that points to the top-level stack frame (at stackAddr) invalid. Then make all pointers
      // that point to the previous frame which will be the new active frame point to stackAddr. We need to
      // re-introduce the variable frameAddr since it is still used as an index into the inactive table.
      // sp = stackAddr + spAtCall + sp

      // save the current stack offset in sp and add it below to the stack offset of the caller
      // sp = sp - stackAddr
      Linear spTerm = linear(term(spVar), term(BigInt.MINUSONE, stack.stackAddr));
      state = state.evalFiniteAssign(
          fin.variable(spSize, spVar), fin.linear(spSize, spTerm));

      state = state.project(stack.stackAddr);
      state = state.substitute(frameAddr, stack.stackAddr);
      state = state.introduce(frameAddr, Type.Address, Option.<BigInt>none());

      spTerm = linear(term(stack.stackAddr), term(spAtCall), term(spVar));
      state = state.evalFiniteAssign(
          fin.variable(spSize, spVar), fin.linear(spSize, spTerm));

      state = state.substituteRegion(bpRelative, stack.currentBpRelative);
      state = state.substitute(barrier, stack.currentBarrier);
      state = state.substituteRegion(spRelative, stack.currentSpRelative);
      state = state.project(spAtCall);
      Frame<D> newFrame =
        new Frame<D>(stack, stack.currentBpRelative, stack.currentBarrier, stack.currentSpRelative, prevFrames, null);
      return P2.tuple2(newFrame, state);
    }

    /**
     * Make the two frames compatible and fold the given one onto this.
     *
     * @param other the frame to be removed from this domain
     * @param state the state on which to operate
     * @return the resulting compatible frame and the new state
     */
    public P2<Frame<D>, D> fold (Frame<D> other, D state) {
      assert false;
      return P2.tuple2(other, state);
    }

    public P2<Frame<D>, D> expand (D state) {
      assert false;
      return P2.tuple2(this, state);
    }

    /**
     * Return a state where the flag to the predecessor for the given callsite address is set to 1
     * and the flags for all other predecessors are set to 0.
     */
    public D getPredecessor (RReilAddr callsite, D domainState) {
      assert prevFrames.contains(callsite);
      D state = domainState;
      for (P2<RReilAddr, FlagVar> pair : prevFrames) {
        try {
          if (pair._1().equals(callsite)) // set the flag to 1 for the frame we return to
            state = state.eval(fin.equalTo(1, linear(pair._2()), Linear.ONE));
          else
            state = state.eval(fin.equalTo(1, linear(pair._2()), Linear.ZERO));
        } catch (Unreachable _) {
          state.getContext().addWarning(new FrameAccess(pair._2()));
        }
      }
      return state;
    }

    /**
     * Return a list of predecessors and the corresponding state in which the flag to the predecessor frame is set to 1.
     */
    public List<P2<RReilAddr, D>> getAllPredecessors (D domainState) {
      List<P2<RReilAddr, D>> res = new LinkedList<P2<RReilAddr, D>>();
      for (RReilAddr callsite : getAllPredecessors()) {
        D predState = getPredecessor(callsite, domainState);
        res.add(P2.tuple2(callsite, predState));
      }
      return res;
    }

    /**
     * Return the call sites of all the predecessors.
     */
    public AVLSet<RReilAddr> getAllPredecessors () {
      AVLSet<RReilAddr> res = AVLSet.<RReilAddr>empty();
      for (P2<RReilAddr, FlagVar> pair : prevFrames)
        res = res.add(pair._1());
      return res;
    }

    public NumVar getBarrier () {
      return barrier;
    }

    public MemVar getDynamicRegion () {
      return spRelative;
    }

    public MemVar getStaticRegion () {
      return bpRelative;
    }

    /**
     * Merge two frames, possibly by renaming and introducing variables in the
     * underlying memory regions.
     *
     * @param fstDom the first memory region
     * @param fst the first frame
     * @param sndDom the second memory region
     * @param snd the second frame
     * @return the changed first and second memory region and the merged frame
     */
    public static <D extends MemoryDomain<D>> P3<D, D, Frame<D>> merge (D fstDom, Frame<D> fst, D sndDom, Frame<D> snd) {
      if (fst == snd)
        return P3.<D, D, Frame<D>>tuple3(fstDom, sndDom, fst);
      MemVar bpRelative;
      switch (fst.bpRelative.compareTo(snd.bpRelative)) {
      case 0:
        bpRelative = fst.bpRelative;
        break;
      case 1: {
        bpRelative = snd.bpRelative;
        fstDom = fstDom.substituteRegion(fst.bpRelative, bpRelative);
        break;
      }
      default: {
        bpRelative = fst.bpRelative;
        sndDom = sndDom.substituteRegion(snd.bpRelative, bpRelative);
        break;
      }
      }
      NumVar barrier;
      switch (fst.barrier.compareTo(snd.barrier)) {
      case 0:
        barrier = fst.barrier;
        break;
      case 1: {
        barrier = snd.barrier;
        fstDom = fstDom.substitute(fst.barrier, snd.barrier);
        break;
      }
      default: {
        barrier = fst.barrier;
        sndDom = sndDom.substitute(snd.barrier, fst.barrier);
        break;
      }
      }
      MemVar spRelative;
      switch (fst.spRelative.compareTo(snd.spRelative)) {
      case 0:
        spRelative = fst.spRelative;
        break;
      case 1: {
        spRelative = snd.spRelative;
        fstDom = fstDom.substituteRegion(fst.spRelative, spRelative);
      }
        break;
      default: {
        spRelative = fst.spRelative;
        sndDom = sndDom.substituteRegion(snd.spRelative, spRelative);
      }
        break;
      }
      ThreeWaySplit<AVLMap<RReilAddr, FlagVar>> prevSplit = fst.prevFrames.split(snd.prevFrames);
      AVLMap<RReilAddr, FlagVar> prevFrames = fst.prevFrames;
      for (P2<RReilAddr, FlagVar> pair : prevSplit.inBothButDiffering()) {
        FlagVar fstFlag = fst.prevFrames.get(pair._1()).get();
        FlagVar sndFlag = snd.prevFrames.get(pair._1()).get();
        assert fstFlag.equalTo(pair._2());
        if (fstFlag.compareTo(sndFlag) < 0) {
          sndDom = sndDom.substitute(sndFlag, fstFlag);
        } else {
          fstDom = fstDom.substitute(fstFlag, sndFlag);
          prevFrames = prevFrames.bind(pair._1(), sndFlag);
        }
      }
      for (P2<RReilAddr, FlagVar> pair : prevSplit.onlyInFirst()) {
        sndDom = sndDom.introduce(pair._2(), Type.Bool, Option.some(BigInt.ZERO));
      }
      for (P2<RReilAddr, FlagVar> pair : prevSplit.onlyInSecond()) {
        fstDom = fstDom.introduce(pair._2(), Type.Bool, Option.some(BigInt.ZERO));
        prevFrames = prevFrames.bind(pair._1(), pair._2());
      }
      NumVar spAtCall = null;
      if (fst.spAtCall != null) {
        assert snd.spAtCall != null;
        switch (fst.spAtCall.compareTo(snd.spAtCall)) {
        case 0:
          spAtCall = fst.spAtCall;
          break;
        case 1: {
          spAtCall = snd.spAtCall;
          fstDom = fstDom.substitute(fst.spAtCall, snd.spAtCall);
          break;
        }
        default: {
          spAtCall = fst.spAtCall;
          sndDom = sndDom.substitute(snd.spAtCall, fst.spAtCall);
          break;
        }
        }
      }
      assert fst.stack.stackAddr == snd.stack.stackAddr;
      assert fst.stack.currentBpRelative == snd.stack.currentBpRelative;
      assert fst.stack.currentBarrier == snd.stack.currentBarrier;
      assert fst.stack.currentSpRelative == snd.stack.currentSpRelative;
      Frame<D> frame = new Frame<D>(fst.stack, bpRelative, barrier, spRelative, prevFrames, spAtCall);
      return P3.<D, D, Frame<D>>tuple3(fstDom, sndDom, frame);
    }

    public D allocate (D state, RReilAddr prevFrame) {
      state = state.introduceRegion(bpRelative, RegionCtx.EMPTYSTICKY);
      state = state.introduceRegion(spRelative, RegionCtx.EMPTYSTICKY);
      state = state.introduce(barrier, Type.Zeno, Option.some(BigInt.ZERO));
      for (P2<RReilAddr, FlagVar> pair : prevFrames) {
        BigInt flagVal = prevFrame != null && pair._1().equals(prevFrame) ? BigInt.ONE : BigInt.ZERO;
        state = state.introduce(pair._2(), Type.Bool, Option.some(flagVal));
      }
      if (spAtCall != null)
        state = state.introduce(spAtCall, Type.Zeno, Option.<BigInt>none());
      return state;
    }

    /**
     * Like #allocated, but does not set a predecessor.
     *
     * @param state the underlying domain
     * @return the domain containing the new flags, addresses and regions
     */
    public D allocate (D state) {
      return allocate(state, null);
    }

    /**
     * Remove this stack frame, thereby freeing its resources.
     *
     * @param state the underlying domain
     * @return the domain in which the variables of this frame have been removed
     */
    public D deallocate (D state) {
      state = state.projectRegion(bpRelative);
      state = state.projectRegion(spRelative);
      state = state.project(barrier);
      for (P2<RReilAddr, FlagVar> pair : prevFrames)
        state = state.project(pair._2());
      if (spAtCall != null)
        state = state.project(spAtCall);
      return state;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append("(");
      builder.append(bpRelative);
      builder.append(",");
      builder.append(barrier);
      builder.append(",");
      builder.append(spRelative);
      if (spAtCall != null) {
        builder.append(",");
        builder.append(spAtCall);
      }
      builder.append(")");
      return builder.toString();
    }
  }


  @Override public void toCompactString (StringBuilder builder, PrettyDomain childDomain) {
    // throw new UnimplementedException();
  }

}
