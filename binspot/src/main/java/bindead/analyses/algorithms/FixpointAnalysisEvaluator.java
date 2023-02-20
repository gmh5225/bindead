package bindead.analyses.algorithms;

import java.util.List;
import java.util.Stack;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.Range;
import rreil.RReilGrammarException;
import rreil.lang.RReil;
import rreil.lang.RReil.Assertion;
import rreil.lang.RReil.Assign;
import rreil.lang.RReil.Branch;
import rreil.lang.RReil.BranchToNative;
import rreil.lang.RReil.BranchToRReil;
import rreil.lang.RReil.Flop;
import rreil.lang.RReil.Load;
import rreil.lang.RReil.Native;
import rreil.lang.RReil.Nop;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Store;
import rreil.lang.RReil.Throw;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.SimpleExpression;
import rreil.lang.Test;
import rreil.lang.util.RReilFactory;
import rreil.lang.util.RReilVisitor;
import bindead.analyses.ProgramAddress;
import bindead.analyses.algorithms.data.Flows;
import bindead.analyses.algorithms.data.Flows.FlowType;
import bindead.analyses.algorithms.data.Flows.Successor;
import bindead.analyses.callback.Callbacks;
import bindead.analyses.systems.SystemModel;
import bindead.analyses.systems.natives.FunctionDefinition;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import bindead.environment.platform.Platform;
import bindead.exceptions.AnalysisException;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.Unreachable;

/**
 * An evaluator for the RReil instructions. The evaluator implements some logic to apply transfer functions
 * and return the result, while at the same time resolving jumps.
 *
 * @param <D> The type of the abstract domain used in the analysis
 *
 * @author Bogdan Mihaila
 */
public class FixpointAnalysisEvaluator<D extends RootDomain<D>> implements
    RReilVisitor<Flows<D>, P3<D, ProgramPoint, RReilAddr>> {
  private final long analysisStartCanary;

  public FixpointAnalysisEvaluator (long analysisStartCanary) {
    this.analysisStartCanary = analysisStartCanary;
  }

  /**
   * Execute a callback for the address that is jumped to if there exists one.
   *
   * @return {@code true} if a callback was registered and executed for this address and {@code false} otherwise.
   */
  private boolean tryCallback (RReil insn, P3<D, ProgramPoint, RReilAddr> ctx, Flows<D> flow) {
    AnalysisEnvironment env = ctx._1().getContext().getEnvironment();
    Callbacks hooks = env.getCallbacks();
    SystemModel systemModel = env.getSystemModel();
    if (hooks == null && systemModel == null)
      return false;
    if (hooks != null) {
      boolean handled = hooks.tryCallback(insn, ctx._1(), ctx._2(), env, flow);
      if (handled)
        return true;
    }
    if (systemModel != null) {
      Option<FunctionDefinition> optFunc = systemModel.handleNativeFunction(ctx._2().getAddress(), insn, ctx._1());
      if (optFunc.isNone()) {
        return false;
      } else {
        evaluateFunction(optFunc.get(), ctx, flow);
        return true;
      }
    }
    return false;
  }

  @Override public Flows<D> visit (Assign stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    D domainState = ctx._1();
    domainState = domainState.eval(stmt);
    return Flows.next(ctx._3(), domainState);
  }

  @Override public Flows<D> visit (Load stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    D domainState = ctx._1();
    try {
      domainState = domainState.eval(stmt);
      return Flows.next(ctx._3(), domainState);
    } catch (Unreachable _) {
      return new Flows<D>();
    }
  }

  @Override public Flows<D> visit (Store stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    D domainState = ctx._1();
    domainState = domainState.eval(stmt);
    return Flows.next(ctx._3(), domainState);
  }

  @Override public Flows<D> visit (Nop stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    return Flows.next(ctx._3(), ctx._1());
  }

  @Override public Flows<D> visit (BranchToRReil stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    D domainState = ctx._1();
    Flows<D> flow = new Flows<D>();

    SimpleExpression exp = stmt.getCond();
    RReilFactory rreil = RReilFactory.instance;
    Test notTakenBranch;
    Test takenBranch;

    if (exp instanceof RangeRhs) { // TODO: use a visitor here?
      throw new UnimplementedException(); // TODO: no need to execute test, just go both paths
    } else if (exp instanceof LinRval) {
      Rval condition = ((LinRval) exp).getRval();
      takenBranch = rreil.testIfNonZero(condition);
      notTakenBranch = rreil.testIfZero(condition);
    } else if (exp instanceof Lin) {
      Lin condition = (Lin) exp;
      takenBranch = rreil.testIfNonZero(condition);
      notTakenBranch = rreil.testIfZero(condition);
    } else if (exp instanceof Cmp) {
      Cmp condition = (Cmp) exp;
      takenBranch = rreil.test(condition);
      notTakenBranch = takenBranch.not();
    } else {
      throw new RReilGrammarException();
    }

    Address target = stmt.getTarget();
    // Try walking the "taken" branch
    try {
      D takenState = domainState.eval(takenBranch);
      RReilAddr rreilTarget = target.getAddress();
      flow.addJump(rreilTarget, takenState);
    } catch (Unreachable _) {
    }
    // Try walking the "not-taken" branch
    try {
      D notTakenState = domainState.eval(notTakenBranch);
      flow.addNext(ctx._3(), notTakenState);
    } catch (Unreachable _) {
    }
    if (flow.isEmpty()) {
      flow.addError(domainState);
      String msg =
        String.format("Targets for branch instruction %s: %s could not be resolved.", stmt.getRReilAddress(), stmt);
      throw new AnalysisException(msg);
    }
    return flow;
  }

  @Override public Flows<D> visit (BranchToNative stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    D domainState = ctx._1();
    Flows<D> flow = new Flows<D>();

    SimpleExpression exp = stmt.getCond();
    RReilFactory rreil = RReilFactory.instance;
    Test notTakenBranch;
    Test takenBranch;

    if (exp instanceof RangeRhs) { // TODO: use a visitor here?
      throw new UnimplementedException(); // TODO: no need to execute test, just go both paths
    } else if (exp instanceof LinRval) {
      Rval condition = ((LinRval) exp).getRval();
      takenBranch = rreil.testIfNonZero(condition);
      notTakenBranch = rreil.testIfZero(condition);
    } else if (exp instanceof Lin) {
      Lin condition = (Lin) exp;
      takenBranch = rreil.testIfNonZero(condition);
      notTakenBranch = rreil.testIfZero(condition);
    } else if (exp instanceof Cmp) {
      Cmp condition = (Cmp) exp;
      takenBranch = rreil.test(condition);
      notTakenBranch = takenBranch.not();
    } else {
      throw new RReilGrammarException();
    }

    Lin target = stmt.getTarget();
    // Try walking the "not-taken" branch
    try {
      D resultState = domainState.eval(notTakenBranch);
      flow.addNext(ctx._3(), resultState);
    } catch (Unreachable _) {
    }
    // Try walking the "taken" branch
    try {
      D resultState = domainState.eval(takenBranch);
      Range range = resultState.queryRange(target);
      if (range == null)
        throw new DomainStateException.VariableSupportSetException(target);
      if (!range.isConstant()) {
        throw new UnsupportedOperationException("Non-constant branch target: " + range);
      }
      RReilAddr singleTarget = RReilAddr.valueOf(range.getConstantOrNull());
      // NOTE: the assertion below is to warn as in such cases the StateSpace class does not take the target point
      // as a join point but will overwrite the state when it is updated
      // XXX: if enabled has effects on (delayed) widening and synthesized predicates,
      // thus we use the offset adding method below instead
//      resultState = setInstructionPointerAssign(resultState, singleTarget.base());
      resultState = setInstructionPointerAdd(resultState, singleTarget.base());
      ProgramPoint jumpPoint = new ProgramAddress(singleTarget);
      P3<D, ProgramPoint, RReilAddr> newCtx = P3.tuple3(resultState, jumpPoint, ctx._3());
      boolean handledByCallback = tryCallback(stmt, newCtx, flow);
      if (!handledByCallback)
        flow.addJump(singleTarget, resultState);
    } catch (Unreachable _) {
    }
    if (flow.isEmpty()) {
      flow.addError(domainState);
      String msg =
        String.format("Targets for branch instruction %s: %s could not be resolved.", stmt.getRReilAddress(), stmt);
      throw new AnalysisException(msg);
    }
    return flow;
  }

  @Override public Flows<D> visit (Branch stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
      D domainState = ctx._1();
      ProgramPoint nextInstructionAddress = new ProgramAddress(ctx._3());
      List<P2<RReilAddr, D>> targets = domainState.eval(stmt, ctx._2(), nextInstructionAddress);
      Flows<D> flow = new Flows<D>();
      for (P2<RReilAddr, D> target : targets) {
        RReilAddr targetAddress = target._1();
        ProgramPoint jumpPoint = new ProgramAddress(targetAddress);
        D targetState = target._2();
        // XXX: if enabled has effects on (delayed) widening and synthesized predicates,
        // thus we use the offset adding method below instead
  //      targetState = setInstructionPointerAssign(targetState, targetAddress.base());
        targetState = setInstructionPointerAdd(targetState, targetAddress.base());

        P3<D, ProgramPoint, RReilAddr> newCtx = P3.tuple3(targetState, jumpPoint, ctx._3());
        boolean handledByCallback = tryCallback(stmt, newCtx, flow);
        if (!handledByCallback)
          switch (stmt.getBranchType()) {
          case Call:
            flow.addCall(targetAddress, targetState);
            break;
          case Return:
            // if a return jumps to the canary value then the analysis has popped the stack correctly and should end here
            if (targetAddress.base() == analysisStartCanary)
              flow.addHalt(targetState);
            else
              flow.addReturn(targetAddress, targetState);
            break;
          case Jump:
            flow.addJump(targetAddress, targetState);
            break;
          }
      }
      if (flow.isEmpty()) {
        flow.addError(domainState);
        String msg =
          String.format("Targets for branch instruction %s: %s could not be resolved.", stmt.getRReilAddress(), stmt);
        throw new AnalysisException(msg);
      }
      return flow;
    }

  /**
   * Set the value of the instruction pointer/program counter.
   * Uses an assignment of a constant.
   */
  private D setInstructionPointerAssign (D state, long targetIP) {
    Platform platform = state.getContext().getEnvironment().getPlatform();
    int size = platform.defaultArchitectureSize();
    String ip = platform.getInstructionPointer();
    state = state.eval(String.format("mov.%d %s, %d", size, ip, targetIP));
    return state;
  }

  /**
   * Set the value of the instruction pointer/program counter on jumps.
   * It does this by adding an offset to avoid delaying the widening.
   */
  private D setInstructionPointerAdd (D state, long targetIP) {
    Platform platform = state.getContext().getEnvironment().getPlatform();
    int size = platform.defaultArchitectureSize();
    String ip = platform.getInstructionPointer();
    String tmp = "ipJumpOffsetReg";
    String calcOffset = String.format("sub.%d %s, %d, %s", size, tmp, targetIP, ip);
    String addOffset = String.format("add.%d %s, %s, %s", size, ip, ip, tmp);
    state = state.eval(calcOffset, addOffset);
    return state;
  }

  @Override public Flows<D> visit (PrimOp primOp, P3<D, ProgramPoint, RReilAddr> ctx) {
    D domainState = ctx._1();
    RReilAddr nextInstructionaddress = ctx._3();
    if (primOp.is("halt", 0, 0)) {
      return Flows.halt(domainState);
    }
    try {
      domainState = domainState.eval(primOp);
      return Flows.next(nextInstructionaddress, domainState);
    } catch (Unreachable _) {
      return Flows.error(domainState);
    }
  }

  @Override public Flows<D> visit (Native stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    AnalysisEnvironment env = ctx._1().getContext().getEnvironment();
    SystemModel systemModel = env.getSystemModel();
    if (systemModel != null) {
      final List<Option<FunctionDefinition>> functions = systemModel.handleNative(stmt, ctx._1(), ctx._2());
      final Flows<D> flows = new Flows<D>();
      if (functions.isEmpty()) {
        // Unable to handle native
        flows.addHalt(ctx._1());
        return flows;
      }
      for (Option<FunctionDefinition> optFunc : functions) {
        if (optFunc.isSome()) {
          FunctionDefinition function = optFunc.get();
          evaluateFunction(function, ctx, flows);
        } else {
          // Unhandled native operation: Unreachable!
          // TODO Emit warning???
          flows.addError(ctx._1());
        }
      }
      return flows;
    }
    throw new UnsupportedOperationException("Natives not supported without SystemModel!");
  }

  @Override public Flows<D> visit (Assertion stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    D domainState = ctx._1();
    RReilAddr nextInstructionaddress = ctx._3();
    return Flows.next(nextInstructionaddress, domainState);
  }

  // TODO: This handling of function definitions is a bit hacky...
  /**
   * This method uses this {@link FixpointAnalysisEvaluator} to resolve the functions effects on the domain state in the
   * function-local
   * address space
   *
   * @param function
   * @param ctx
   * @param flows
   */
  private void evaluateFunction (FunctionDefinition function, P3<D, ProgramPoint, RReilAddr> ctx, Flows<D> flows) {
    // final Range test = ctx._1().queryRange(platform.getRvar("ebx"));
    // Start resolving with first instruction
    RReilAddr startAddr = function.getInstructions().firstKey();
    Successor<D> init = new Successor<D>(FlowType.Jump, startAddr, ctx._1());

    Stack<Successor<D>> todo = new Stack<Successor<D>>();
    todo.add(init);

    // Resolve all successors
    while (!todo.isEmpty()) {
      Successor<D> succ = todo.pop();
      switch (succ.getType()) {
      case Return:
        // TODO: Implement a returnf for this?
        // // Okay, passed the function. Step forward using the state modified by the function
        // flows.addNext(succ.getState());
        break;

      case Halt:
        // TODO: Enough/Correct??
        flows.addHalt(ctx._1());
        break;

      case Jump:
      case Next:
        RReilAddr localAddr = succ.getAddress();
        // TODO: Hack until there is a returnf
        if (function.reachedEnd(localAddr)) {
          // When the end of the function is reached: add flow with the current state and we're done
          flows.addNext(ctx._3(), succ.getState());
          continue;
        }

        RReil rreil = function.getInstructions().get(localAddr);
        if (rreil == null) {
          throw new IllegalStateException(
            "Jumps to targets outside the local address space are not allowed inside functions!");
        }

        P3<D, ProgramPoint, RReilAddr> localCtx = P3.tuple3(succ.getState(), ctx._2(), ctx._3());
        Flows<D> nextFlows = rreil.accept(this, localCtx);

        for (Successor<D> nextSucc : nextFlows) {
          todo.add(nextSucc);
        }
        break;

      case Call:
        throw new IllegalStateException("No calls allowed inside native functions!");
      case Error:
        flows.addError(ctx._1());
        break;
      default:
        break;
      }
    }
  }

  @Override public Flows<D> visit (Throw stmt, P3<D, ProgramPoint, RReilAddr> data) {
    throw new UnimplementedException();
  }

  @Override public Flows<D> visit (Flop stmt, P3<D, ProgramPoint, RReilAddr> data) {
    throw new UnimplementedException();
  }

}
