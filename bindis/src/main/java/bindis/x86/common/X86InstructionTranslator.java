package bindis.x86.common;

import java.util.LinkedList;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.Instruction.InstructionBuilder;
import rreil.disassembler.Instruction.InstructionFactory;
import rreil.disassembler.OperandTree;
import rreil.disassembler.OperandTree.Node;
import rreil.disassembler.OperandTree.NodeBuilder;
import rreil.disassembler.OperandTree.Type;

/**
 * Translate disassembled x86 instruction objects into a generic tree structure.
 */
public final class X86InstructionTranslator implements X86OperandVisitor<NodeBuilder, NodeBuilder> {
  private final InstructionFactory factory;

  public X86InstructionTranslator (InstructionFactory factory) {
    this.factory = factory;
  }

  /**
   * Translates a x86 instruction into a tree-like representation.
   *
   * @param insn
   * @return
   */
  public Instruction translate (X86Instruction insn) {
    String mnemonic = insn.mnemonic();
    List<OperandTree> opnds = new LinkedList<OperandTree>();
    for (int i = 0; i < insn.numberOfOperands(); i++) {
      opnds.add(new OperandTree(translate((X86Operand) insn.operand(i))));
    }
    InstructionBuilder builder = new InstructionBuilder(factory);
    builder = builder.address(insn.address()).mnemonic(mnemonic).link(opnds).opcode(insn.opcode());
    return builder.build();
  }

  public Node translate (X86Operand opnd) {
    NodeBuilder root = new NodeBuilder();
    Node child = opnd.accept(this, new NodeBuilder()).build();
    return root.type(Type.Size).data(opnd.size()).link(child).build();
  }

  @Override public NodeBuilder visit (X86ImmOpnd opnd, NodeBuilder root) {
    return root.type(Type.Immi).data(opnd.imm());
  }

  @Override public NodeBuilder visit (X86RegOpnd opnd, NodeBuilder root) {
    return root.type(Type.Sym).data(opnd.name());
  }

  @Override public NodeBuilder visit (X86MemOpnd opnd, NodeBuilder root) {
    root.type(Type.Mem).data(opnd.ptrSize());
    NodeBuilder addressTree = new NodeBuilder().type(Type.Op).data("+");
    linkBase(opnd, addressTree);
    linkIndexAndScale(opnd, addressTree);
    linkDisplacement(opnd, addressTree);
    Node addressNode = addressTree.build();
    if (addressNode.getChildren().size() == 1)
      return root.link(addressNode.getChildren().get(0));
    return root.link(addressTree.build());
  }

  public NodeBuilder linkBase (X86MemOpnd opnd, NodeBuilder derefSubTree) {
    if (opnd.base() != null)
      derefSubTree.link(opnd.base().accept(this, new NodeBuilder()).build());
    return derefSubTree;
  }

  public NodeBuilder linkIndexAndScale (X86MemOpnd opnd, NodeBuilder derefSubTree) {
    if (opnd.index() != null) {
      Node idx = opnd.index().accept(this, new NodeBuilder()).build();
      if (opnd.scale() > 1) {
        Node scale = new NodeBuilder().type(Type.Immi).data(opnd.scale()).build();
        Node idxTree = new NodeBuilder().type(Type.Op).data("*").link(idx).link(scale).build();
        derefSubTree.link(idxTree);
      } else
        derefSubTree.link(idx);
    }
    return derefSubTree;
  }

  public NodeBuilder linkDisplacement (X86MemOpnd opnd, NodeBuilder derefSubTree) {
    if (opnd.disp() != null)
      derefSubTree.link(opnd.disp().accept(this, new NodeBuilder()).build());
    return derefSubTree;
  }
}
