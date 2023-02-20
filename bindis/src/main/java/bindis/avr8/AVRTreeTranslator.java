package bindis.avr8;

import java.util.LinkedList;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.Instruction.InstructionBuilder;
import rreil.disassembler.Instruction.InstructionFactory;
import rreil.disassembler.OperandTree;
import rreil.disassembler.OperandTree.Node;
import rreil.disassembler.OperandTree.NodeBuilder;
import rreil.disassembler.OperandTree.Type;
import rreil.disassembler.translators.common.TranslationException;
import rreil.lang.lowlevel.LowLevelRReil;

public final class AVRTreeTranslator implements OpndVisitor<NodeBuilder, NodeBuilder> {
  private static final AVRInstructionFactory factory = new AVRInstructionFactory();
  public static final AVRTreeTranslator $ = new AVRTreeTranslator();
  public final String $PostIncrementOperator = "++";
  public final String $PreDecrementOperator = "--";

  private AVRTreeTranslator () {
  }

  public Instruction translate (AVRInsn insn) {
    final String mnemonic = insn.mnemonic();
    final List<OperandTree> opnds = new LinkedList<OperandTree>();
    for (int i = 0; i < insn.numberOfOperands(); i++) {
      opnds.add(new OperandTree(translate((AVROpnd) insn.operand(i))));
    }
    return new InstructionBuilder(factory).address(insn.address()).mnemonic(mnemonic).link(opnds).opcode(insn.opcode()).build();
  }

  public Node translate (AVROpnd opnd) {
    final NodeBuilder root = new NodeBuilder();
    final Node child = opnd.accept(this, new NodeBuilder()).build();
    return root.type(Type.Size).data(opnd.size()).link(child).build();
  }

  @Override public NodeBuilder visit (AVRRegOpnd opnd, NodeBuilder root) {
    return root.type(Type.Sym).data(opnd.name());
  }

  @Override public NodeBuilder visit (AVRImmOpnd opnd, NodeBuilder root) {
    return root.type(Type.Immi).data(opnd.getValue());
  }

  @Override public NodeBuilder visit (AVRMemOpnd opnd, NodeBuilder root) {
    root.type(Type.Mem).data(opnd.ptrSize());
    Node reg = null;
    if (opnd.getMemReg() != null) {
      Node node = opnd.getMemReg().accept(new AVRTreeTranslator(), new NodeBuilder()).build();
      switch (opnd.getAltType()) {
        case None: {
          reg = node;
          break;
        }
        case PostIncrement: {
          reg = new NodeBuilder().type(Type.Op).data($PostIncrementOperator).link(node).build();
          break;
        }
        case PreDecrement: {
          reg = new NodeBuilder().type(Type.Op).data($PreDecrementOperator).link(node).build();
        }
      }
    }
    Node imm = null;
    if (opnd.getDisplacement() != null)
      imm = opnd.getDisplacement().accept(this, new NodeBuilder()).build();

    if (reg != null && imm != null) {
      NodeBuilder addressTree = new NodeBuilder().type(Type.Op).data("+").link(reg).link(imm);
      return root.link(addressTree.build());
    } else if (reg != null)
      return root.link(reg);
    else
      return root.link(imm);
  }

  private static class AVRInstructionFactory implements InstructionFactory {
    @Override public Instruction build (long address, byte[] opcode, String mnemonic, List<OperandTree> opnds) {
      return new AVRInstruction(address, opcode, mnemonic, opnds);
    }
  }

  private static class AVRInstruction extends Instruction {
    public AVRInstruction (long address, byte[] opcode, String mnemonic, List<OperandTree> opnds) {
      super(address, opcode, mnemonic, opnds);
    }

    @Override public List<LowLevelRReil> toRReilInstructions () throws TranslationException {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }
}
