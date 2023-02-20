package bindead.analysis.util;

import bindead.domainnetwork.interfaces.RootDomain;
import bindead.exceptions.EvaluationException;
import bindead.exceptions.Unreachable;
import javalx.digraph.Digraph.Edge;
import rreil.abstractsyntax.RReil;
import rreil.cfa.Cfa;
import rreil.cfa.Cfa.CfaComputedFlow;
import rreil.cfa.CfaEdgeTypeVisitor;

public abstract class CfgEdgeTypeVisitorSkeleton<D extends RootDomain<D>> implements CfaEdgeTypeVisitor<D, D> {
  @Override public D visit (Cfa.CfaBlock block, Edge correspondingEdge, D domain) {
    for (RReil.Statement insn : block.getBlock()) {
      try {
        domain = domain.eval(insn);
      } catch (Unreachable bottom) {
        // this exception is needed for control flow and thus needs to be propagated up the call stack
        throw bottom;
      } catch (Throwable cause) {
        // by using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
        // might also be useful to have a partial analysis result even on unexpected errors. Also we need to catch
        // assertion errors.
        throw new EvaluationException(cause, insn.getRReilAddress(), insn.toString());
      }
    }
    return domain;
  }

  @Override public D visit (Cfa.CfaTest test, Edge correspondingEdge, D domain) {
    try {
      return domain.evalTest(test.getTest());
    } catch (Unreachable bottom) {
      // this exception is needed for control flow and thus needs to be propagated up the call stack
      throw bottom;
    } catch (Throwable cause) {
      // by using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
      // might also be useful to have a partial analysis result even on unexpected errors. Also we need to catch
      // assertion errors.
      throw new EvaluationException(cause, test.blockStartAddress(), test.getTest().toString());
    }
  }

  @Override public D visit (Cfa.CfaCall call, Edge correspondingEdge, D domain) {
    try {
      return domain.evalCall(call.getCallInstruction());
    } catch (Unreachable bottom) {
      // this exception is needed for control flow and thus needs to be propagated up the call stack
      throw bottom;
    } catch (Throwable cause) {
      // by using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
      // might also be useful to have a partial analysis result even on unexpected errors. Also we need to catch
      // assertion errors.
      throw new EvaluationException(cause, call.blockStartAddress(), call.getCallInstruction().toString());
    }
  }

  @Override public D visit (Cfa.CfaReturn ret, Edge correspondingEdge, D domain) {
    try {
      return domain.evalReturn(ret.getReturnInstruction());
    } catch (Unreachable bottom) {
      // this exception is needed for control flow and thus needs to be propagated up the call stack
      throw bottom;
    } catch (Throwable cause) {
      // by using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
      // might also be useful to have a partial analysis result even on unexpected errors. Also we need to catch
      // assertion errors.
      throw new EvaluationException(cause, ret.blockStartAddress(), ret.getReturnInstruction().toString());
    }
  }

  @Override public D visit (CfaComputedFlow flow, Edge correspondingEdge, final D domain) {
    try {
      return domain.evalComputedFlow(flow.getVariable());
    } catch (Unreachable bottom) {
      // this exception is needed for control flow and thus needs to be propagated up the call stack
      throw bottom;
    } catch (Throwable cause) {
      // by using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
      // might also be useful to have a partial analysis result even on unexpected errors. Also we need to catch
      // assertion errors.
      throw new EvaluationException(cause, flow.blockStartAddress(), flow.toString());
    }
  }

}
