package rreil.cfa.util;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javalx.data.BigInt;
import javalx.data.Option;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReil.Assign;
import rreil.abstractsyntax.RReil.BranchToNative;
import rreil.abstractsyntax.RReil.Call;
import rreil.abstractsyntax.RReil.BranchToRReil;
import rreil.abstractsyntax.RReil.Load;
import rreil.abstractsyntax.RReil.Native;
import rreil.abstractsyntax.RReil.Nop;
import rreil.abstractsyntax.RReil.PrimOp;
import rreil.abstractsyntax.RReil.Return;
import rreil.abstractsyntax.RReil.Statement;
import rreil.abstractsyntax.RReil.Store;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.Rhs;
import rreil.abstractsyntax.Rhs.Rlit;
import rreil.abstractsyntax.Rhs.Rval;
import rreil.abstractsyntax.Rhs.Rvar;
import rreil.abstractsyntax.Test;
import rreil.abstractsyntax.util.CRReilFactory;
import rreil.abstractsyntax.util.RReilFactory;
import rreil.abstractsyntax.util.RReilVisitor;
import rreil.cfa.Cfa;
import rreil.cfa.Cfa.CfaCall;
import rreil.cfa.Cfa.CfaComputedFlow;
import rreil.cfa.Cfa.TransitionType;
import rreil.cfa.util.CfaStateException.ErrObj;
import rreil.disassembly.RReilDisassemblyCtx;

/**
 * A builder for Control flow automatons (CFA).
 *
 * @author Bogdan Mihaila
 */
public class CfaBuilder {
  private final Cfa cfa;
  private final Map<RReilAddr, Vertex> vertices;
  private final Queue<RReilAddr> insnsWorkQueue;
  private final RReilDisassemblyCtx disassembly;

  private CfaBuilder (Cfa cfa, RReilDisassemblyCtx disassembly) {
    this.cfa = cfa;
    this.disassembly = disassembly;
    vertices = new HashMap<RReilAddr, Vertex>(CfaHelpers.getAddressableVertices(cfa));
    insnsWorkQueue = new ArrayDeque<RReilAddr>(); // FIFO
  }

  /**
   * Removes nops and redundant jumps in the CFA.
   *
   * @param cfa The CFA to be worked on
   * @return A new CFA in which the nops and redundant jumps were removed
   */
  public static Cfa cleanup (Cfa cfa) {
    return CfaCleaner.cleanup(cfa);
  }

  /**
   * Replaces the instructions in the CFA at the passed in addresses with no-op jump instructions. Thus the structure of
   * the CFA is not altered.
   *
   * @param cfa The CFA to be worked on
   * @param addresses The instruction addresses of dead code
   * @return A new CFA in which the dead code has been removed
   * @see #removeRedundantJumps(Cfa)
   */
  public static Cfa removeDeadCode (Cfa cfa, Set<RReilAddr> addresses) {
    return CfaCleaner.removeDeadCode(cfa, addresses);
  }

  /**
   * Removes any redundant jump edges in the CFA.
   * Redundant edges are jumps that jump unconditionally
   * to the next instruction. Thus performing a no-op.
   *
   * @param cfa The CFA to be worked on
   * @return A new CFA in which the redundant jump edges were removed
   */
  public static Cfa removeRedundantJumps (Cfa cfa) {
    return CfaCleaner.removeRedundantJumps(cfa);
  }

  /**
   * Removes nops from the CFA.
   *
   * @param cfa The CFA to be worked on
   * @return A new CFA in which nop instructions have been removed
   */
  public static Cfa removeNops (Cfa cfa) {
    return CfaCleaner.removeNops(cfa);
  }

  /**
   * Return a new CFA that contains the same
   * instructions as the passed in one but where
   * paths of consecutive statements are merged
   * together to one basic block.
   */
  public static Cfa makeBasicBlocks (Cfa cfa) {
    return BasicBlocksTool.makeBasicBlocks(cfa);
  }

  /**
   * Returns a new CFA that contains the same
   * instructions as the passed in one but where
   * the basic blocks are split up to contain
   * only one single instruction per block.
   */
  public static Cfa expandBasicBlocks (Cfa cfa) {
    return BasicBlocksTool.expandBasicBlocks(cfa);
  }

  /**
   * Builds a new CFA from the passed in instructions.
   *
   * @param disassembly Disassembled instructions that should be used to build a CFA
   * @param entryAddress The entry address of the procedure of this CFA
   * @return The resulting CFA
   */
  public static Cfa build (RReilDisassemblyCtx disassembly, RReilAddr entryAddress) {
    CfaBuilder builder = new CfaBuilder(new Cfa(entryAddress), disassembly);
    builder.vertices.put(entryAddress, builder.cfa.getEntry());
    builder.startBuildingFrom(entryAddress);
    return builder.cfa;
  }

  /**
   * Adds new instructions to the CFA as the jump target of a computed jump or call.
   *
   * @param cfa A CFA to be extended
   * @param disassembly Disassembled instructions that should be used to extend the CFA
   * @param extensionSource The edge a computed jump or a call from where to start the extension
   * @param extensionTarget The resolved target address for the above edge from where to continue adding instructions to
   *          the CFA
   * @return <code>true</code> if the CFA was extended
   */
  public static boolean extend (Cfa cfa, RReilDisassemblyCtx disassembly, Edge extensionSource, RReilAddr extensionTarget) {
    // see if there is already an edge for this jump in the CFA before starting any CFA extension
    for (Vertex node : extensionSource.getTarget().successors()) {
      Option<RReilAddr> address = cfa.getAddress(node);
      if (address.isSome() && address.get().equals(extensionTarget))
        return false;
    }
    CfaBuilder builder = new CfaBuilder(cfa, disassembly);
    builder.extendImpl(extensionSource, extensionTarget);
    return true;
  }

  /**
   * Adds the new parts to the CFA after a computed flow edge or a call.
   */
  private void extendImpl (Edge continuationEdge, RReilAddr resolvedJumpTarget) {
    // add the resolved jump edge
    Vertex source = continuationEdge.getTarget();
    Vertex target = getVertex(resolvedJumpTarget);
    Edge jumpEdge = cfa.createEdge(source, target);

    TransitionType instruction = cfa.getTransitionType(continuationEdge);
    if (instruction instanceof CfaCall) {
      CfaCall call = (CfaCall) instruction;
      RReilFactory rreilFactory = CRReilFactory.getInstance();
      Test alwaysTrueCond = rreilFactory.testIfNonZero(rreilFactory.literal(1, BigInt.ONE));
      RReilAddr startAdress = call.blockStartAddress();
      cfa.setTransitionType(jumpEdge, TransitionType.test(startAdress, alwaysTrueCond));
    } else if (instruction instanceof CfaComputedFlow) {
      // assign the target address value to the register that is used for the jump
      CfaComputedFlow flow = (CfaComputedFlow) instruction;
      RReilFactory rreilFactory = CRReilFactory.getInstance();
      Rvar targetAddressVariable = flow.getVariable();
      BigInt resolvedTargetAddress = BigInt.valueOf(resolvedJumpTarget.base());
      Rlit jumpToAddress = rreilFactory.literal(targetAddressVariable.getSize(), resolvedTargetAddress);
      Assign assignment = rreilFactory.assign(null, flow.blockStartAddress(), targetAddressVariable.asLhs(), jumpToAddress);
      cfa.setTransitionType(jumpEdge, Cfa.TransitionType.block(assignment));
    } else {
      throw new CfaStateException(ErrObj.INVARIANT_FAILURE,
          "The extension point for the CFA is neither a call nor a computed flow.");
    }

    // continue extending the CFA from the resolved address on
    startBuildingFrom(resolvedJumpTarget);
  }

  private void startBuildingFrom (RReilAddr startAddress) {
    insnsWorkQueue.add(startAddress);
    ActualCfaBuilder dispatcher = new ActualCfaBuilder();
    while (!insnsWorkQueue.isEmpty()) {
      RReilAddr nextAddress = insnsWorkQueue.remove();
      if (!instructionAlreadyInCfa(nextAddress)) {
        RReil nextInstr = getInstruction(nextAddress);
        nextInstr.accept(dispatcher, null);
      }
    }
  }

  private RReil getInstruction (RReilAddr address) {
    RReil nextInstr = disassembly.getInstruction(address);
    if (nextInstr == null)
      throw new CfaStateException(ErrObj.MISSING_JUMP_TARGET, "no instruction found for address " + address);
    return nextInstr;
  }

  /**
   * Returns the CFA vertex for an address.
   * Lazily builds a new vertex if there does
   * not exist one already.
   */
  private Vertex getVertex (RReilAddr address) {
    Vertex node = vertices.get(address);
    if (node == null) {
      node = cfa.createVertex();
      vertices.put(address, node);
    }
    return node;
  }

  private boolean instructionAlreadyInCfa (RReilAddr address) {
    Vertex vertex = vertices.get(address);
    return vertex != null && !vertex.outgoing().isEmpty();
  }

  /**
   * Returns the vertex that follows this instruction in the linear flow of control -- i.e. fall-through processing.
   */
  private Vertex getFallThroughSuccessorVertex (RReilAddr address) {
    RReilAddr nextAddress = disassembly.getSuccessorAddress(address);
    return getVertex(nextAddress);
  }

  /**
   * Adds the address following this instruction to the processing queue.
   */
  private void addFallThroughSuccessorToWorkqueue (RReilAddr address) {
    RReilAddr nextAddress = disassembly.getSuccessorAddress(address);
    if (nextAddress != null)
      insnsWorkQueue.add(nextAddress);
    else
      throw new CfaStateException(ErrObj.MISSING_JUMP_TARGET, "no instruction found for address " + nextAddress);
  }

  private class ActualCfaBuilder implements RReilVisitor<Void, Void> {
    @Override public Void visit (Assign insn, Void _) {
      return handleNonJump(insn);
    }

    @Override public Void visit (Load insn, Void _) {
      return handleNonJump(insn);
    }

    @Override public Void visit (Store insn, Void _) {
      return handleNonJump(insn);
    }

    @Override public Void visit (Nop insn, Void _) {
      return handleNonJump(insn);
    }

    @Override public Void visit (PrimOp insn, Void data) {
      return handleNonJump(insn);
    }

    @Override public Void visit (Native insn, Void data) {
      return handleNonJump(insn);
    }

    /**
     * Handles any non branching instruction as e.g. statements that yield only a fall-through edge.
     */
    private Void handleNonJump (Statement insn) {
      RReilAddr insnAddress = insn.getRReilAddress();
      Vertex source = getVertex(insnAddress);
      Vertex target = getFallThroughSuccessorVertex(insnAddress);
      Cfa.Edge edge = cfa.createEdge(source, target);
      cfa.setTransitionType(edge, TransitionType.block(insn));
      addFallThroughSuccessorToWorkqueue(insnAddress);
      return null;
    }

    @Override public Void visit (Call insn, Void _) {
      // assumes that calls always return but the return point does not necessarily have to be the next higher
      // instruction address. Thus leave the it to a reconstruction analysis to find the return point.
      Vertex source = getVertex(insn.getRReilAddress());
      Vertex target = cfa.createVertex();
      Edge edge = cfa.createEdge(source, target);
      cfa.setTransitionType(edge, TransitionType.call(insn));
      return null;
    }

    @Override public Void visit (Return insn, Void _) {
      Vertex source = getVertex(insn.getRReilAddress());
      Vertex target = cfa.getExit();
      Edge edge = cfa.createEdge(source, target);
      cfa.setTransitionType(edge, TransitionType.ret(insn));
      return null;
    }

    @Override public Void visit (BranchToRReil insn, Void _) {
      handleStaticJump(insn, insn.getCond(), insn.getTarget().getAddress());
      return null;
    }

    @Override public Void visit (BranchToNative insn, Void _) {
      Rval condition = insn.getCond();
      RReilAddr insnAddress = insn.getRReilAddress();
      if (insn.isIndirectBranch()) {
        if (isConditionalJump(condition)) {
          Vertex jumpTakenTarget = cfa.createVertex();
          addConditionalEdge(insn, condition, true, jumpTakenTarget);
          addComputedFlowEdge(insn, jumpTakenTarget);
          addFallThroughEdge(insn, condition);
        } else {
          if (alwaysJumps(condition)) {
            Vertex source = getVertex(insnAddress);
            addComputedFlowEdge(insn, source);
          } else {
            addFallThroughEdge(insn, condition);
          }
        }
      } else {
        BigInt targetAddress = ((Rhs.Rlit) insn.getTarget()).getValue();
        handleStaticJump(insn, condition, RReilAddr.valueOf(targetAddress));
      }
      return null;
    }

    /**
     * Handles jumps for which the target address is known statically from the instruction parameters.
     */
    private void handleStaticJump (RReil insn, Rval condition, RReilAddr targetAddress) {
      if (isConditionalJump(condition)) {
        addJumpEdge(insn, condition, targetAddress);
        addFallThroughEdge(insn, condition);
      } else {
        if (alwaysJumps(condition)) {
          addJumpEdge(insn, condition, targetAddress);
        } else {
          addFallThroughEdge(insn, condition);
        }
      }
    }

    private void addJumpEdge (RReil insn, Rval condition, RReilAddr targetAddress) {
      Vertex jumpTakenTarget = getVertex(targetAddress);
      addConditionalEdge(insn, condition, true, jumpTakenTarget);
      insnsWorkQueue.add(targetAddress);
    }

    private void addComputedFlowEdge (BranchToNative insn, Vertex source) {
      Vertex computedFlowVertex = cfa.createVertex();
      Edge computedFlowEdge = cfa.createEdge(source, computedFlowVertex);
      Rhs.Rvar targetAddressVariable = (Rhs.Rvar) insn.getTarget();
      cfa.setTransitionType(computedFlowEdge, Cfa.TransitionType.flow(insn.getRReilAddress(), targetAddressVariable));
    }

    private void addFallThroughEdge (RReil insn, Rval condition) {
      Vertex jumpNotTakenTarget = getFallThroughSuccessorVertex(insn.getRReilAddress());
      addConditionalEdge(insn, condition, false, jumpNotTakenTarget);
      addFallThroughSuccessorToWorkqueue(insn.getRReilAddress());
    }

    private void addConditionalEdge (RReil insn, Rval condition, boolean isConditionTrue, Vertex target) {
      RReilAddr insnAddress = insn.getRReilAddress();
      RReilFactory rreilFactory = CRReilFactory.getInstance();
      Test test;
      if (isConditionTrue)
        test = rreilFactory.testIfNonZero(condition);
      else
        test = rreilFactory.testIfZero(condition);
      Vertex source = getVertex(insnAddress);
      Edge jumpTakenEdge = cfa.createEdge(source, target);
      cfa.setTransitionType(jumpTakenEdge, Cfa.TransitionType.test(insnAddress, test));
    }

    /**
     * Returns <code>true</code> if this jump depends on a runtime value of a condition.
     */
    private boolean isConditionalJump (Rval condition) {
      return condition instanceof Rvar;
    }

    /**
     * Returns <code>true</code> if the condition always evaluates to a true value or <code>false</code> otherwise.
     */
    private boolean alwaysJumps (Rval condition) {
      BigInt value = ((Rhs.Rlit) condition).getValue();
      return !value.equals(BigInt.ZERO);
    }
  }
}
