package rreil.cfa;

import java.util.HashMap;
import java.util.Map;

import javalx.data.BigInt;
import rreil.abstractsyntax.RReil.Native;
import rreil.abstractsyntax.RReil.PrimOp;
import rreil.abstractsyntax.Rhs;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.Test;
import rreil.abstractsyntax.util.RReilVisitor;
import rreil.assembler.CompilationUnit;

public class ParseTreeCfaBuilder {
  private final CompilationUnit compilationUnit;
  private final Map<RReilAddr, Cfa.Vertex> vertices = new HashMap<RReilAddr, Cfa.Vertex>();
  private final Cfa cfa;
  private final RReilAddr entryAddress;
  private final RReilAddr exitAddress;

  public ParseTreeCfaBuilder (CompilationUnit compilationUnit) {
    this.compilationUnit = compilationUnit;
    entryAddress = compilationUnit.entryAddress();
    exitAddress = compilationUnit.exitAddress();
    this.cfa = new Cfa(entryAddress);
  }

  public void buildCfa () {
    for (RReilAddr address : compilationUnit.getInstructionMapping().keySet()) {
      if (address.equals(entryAddress))
        vertices.put(address, cfa.getEntry());
      else {
        Cfa.Vertex v = cfa.createVertex();
        vertices.put(address, v);
      }
    }

    final StaticFlowVisitor visitor = new StaticFlowVisitor();

    for (Map.Entry<RReilAddr, Cfa.Vertex> addressAndVertex : vertices.entrySet()) {
      final Cfa.Vertex v = addressAndVertex.getValue();
      final RReilAddr address = addressAndVertex.getKey();
      final RReil insn = compilationUnit.getInstructionMapping().get(address);
      visitor.setVertexBeingProcessed(v);
      insn.accept(visitor, null);
    }
  }

  public Cfa getCfa () {
    return cfa;
  }

  public class StaticFlowVisitor implements RReilVisitor<Void, Void> {
    private Cfa.Vertex vertexBeingProcessed;

    @Override public Void visit (RReil.Assign insn, Void _) {
      visitAssignment(insn);
      return null;
    }

    @Override public Void visit (RReil.Load insn, Void _) {
      visitAssignment(insn);
      return null;
    }

    @Override public Void visit (RReil.Store insn, Void _) {
      visitAssignment(insn);
      return null;
    }

    @Override public Void visit (PrimOp insn, Void data) {
      visitAssignment(insn);
      return null;
    }

    @Override public Void visit (Native insn, Void data) {
      visitAssignment(insn);
      return null;
    }

    private void visitAssignment (RReil.Statement insn) {
      final RReilAddr address = insn.getRReilAddress();
      final Cfa.Vertex targetVertex = findVertex(RReilAddr.valueOf(address.base() + 1));
      final Cfa.Edge edge = cfa.createEdge(vertexBeingProcessed, targetVertex);
      cfa.setTransitionType(edge, Cfa.TransitionType.block(insn));
    }

    @Override public Void visit (RReil.Call insn, Void _) {
      // We assume that a call always returns.
      final RReilAddr address = insn.getRReilAddress();
      final Cfa.Vertex targetVertex = findVertex(RReilAddr.valueOf(address.base() + 1));
      final Cfa.Edge edge = cfa.createEdge(vertexBeingProcessed, targetVertex);
      cfa.setTransitionType(edge, Cfa.TransitionType.call(insn));
      return null;
    }

    @Override public Void visit (RReil.Return insn, Void _) {
      final Cfa.Vertex targetVertex = cfa.getExit();
      final Cfa.Edge edge = cfa.createEdge(vertexBeingProcessed, targetVertex);
      cfa.setTransitionType(edge, Cfa.TransitionType.ret(insn));
      return null;
    }

    @Override public Void visit (RReil.BranchToNative insn, Void _) {
      final RReilAddr address = insn.getRReilAddress();
      final RReilAddr followUpAddress = RReilAddr.valueOf(address.base() + 1);

      final Test takenCondition = compilationUnit.getRReilFactory().testIfNonZero(insn.getCond());
      final Test notTakenCondition = compilationUnit.getRReilFactory().testIfZero(insn.getCond());

      final Cfa.Vertex targetVertexOnTakenBranch;
      final Cfa.Vertex targetVertexOnNotTakenBranch = findVertex(followUpAddress);

      if (isStaticBranch(insn)) {
        final BigInt target = ((Rhs.Rlit) insn.getTarget()).getValue();
        targetVertexOnTakenBranch = findVertex(RReilAddr.valueOf(target));
      } else {
        final Rhs.Rvar target = (Rhs.Rvar) insn.getTarget();
        targetVertexOnTakenBranch = cfa.createVertex();
        Cfa.Vertex computedFlowVertex = cfa.createVertex();
        final Cfa.Edge computedFlowEdge = cfa.createEdge(targetVertexOnTakenBranch, computedFlowVertex);
        cfa.setTransitionType(computedFlowEdge, Cfa.TransitionType.flow(address, target));
      }

      final Cfa.Edge whenTakenEdge = cfa.createEdge(vertexBeingProcessed, targetVertexOnTakenBranch);
      final Cfa.Edge whenNotTakenEdge = cfa.createEdge(vertexBeingProcessed, targetVertexOnNotTakenBranch);
      cfa.setTransitionType(whenTakenEdge, Cfa.TransitionType.test(address, takenCondition));
      cfa.setTransitionType(whenNotTakenEdge, Cfa.TransitionType.test(address, notTakenCondition));
      return null;
    }

    @Override public Void visit (RReil.BranchToRReil insn, Void _) {
      throw new UnsupportedOperationException("Intra-RREIL branches not allowed in rreil-assembly code");
    }

    @Override public Void visit (RReil.Nop stmt, Void data) {
      return null;
    }

    private Cfa.Vertex findVertex (RReilAddr address) {
      if (address.equals(exitAddress))
        return cfa.getExit();

      if (!vertices.containsKey(address))
        throw new RuntimeException(String.format("No instruction at address <%s>", address.toString()));
      return vertices.get(address);
    }

    private boolean isStaticBranch (RReil.BranchToNative insn) {
      return insn.getTarget() instanceof Rhs.Rlit;
    }

    public Cfa.Vertex getVertexBeingProcessed () {
      return vertexBeingProcessed;
    }

    public void setVertexBeingProcessed (Cfa.Vertex vertexBeingProcessed) {
      this.vertexBeingProcessed = vertexBeingProcessed;
    }
  }
}
