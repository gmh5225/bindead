package bindis.gdsl;

import gdsl.translator.Translator;

import java.util.ArrayList;

import bindis.INativeInstruction;
import javalx.exceptions.UnimplementedException;
import rreil.disassembler.Instruction;
import rreil.disassembler.OpcodeFormatter;
import rreil.disassembler.OperandTree;
import rreil.disassembler.OperandTree.NodeBuilder;
import rreil.disassembler.OperandTree.Type;

public class GdslNativeInstruction implements INativeInstruction {
  private final gdsl.decoder.NativeInstruction gdslInsn;
  private final byte[] opcode;
  private final long address;
  private final GdslOperand operands[];
  private final Translator rreilTranslator;

  public GdslNativeInstruction (gdsl.decoder.NativeInstruction gdslInsn, byte[] opcode, long address,
      Translator rreilTranslator) {
    this.gdslInsn = gdslInsn;
    this.opcode = opcode;
    this.address = address;
    this.rreilTranslator = rreilTranslator;

    operands = new GdslOperand[gdslInsn.operands()];
    for (int i = 0; i < operands.length; i++) {
      operands[i] = new GdslOperand(gdslInsn, i);
    }
  }

  @Override public Instruction toTreeInstruction () {
    ArrayList<OperandTree> operands = new ArrayList<OperandTree>();
    for (int i = 0; i < this.operands.length; i++) {
      NodeBuilder builder = new NodeBuilder();
      builder.type(Type.Op).data(this.operands[i]);
      operands.add(new OperandTree(builder.build()));
    }
    return new GdslInstruction(address, opcode, gdslInsn, operands, rreilTranslator);
  }

  @Override public String architecture () {
    // FIXME: either remove this method in interface or return here the right architecture string coming from the platform
    throw new UnimplementedException();
  }

  @Override public String mnemonic () {
    return gdslInsn.mnemonic();
  }

  @Override public byte[] opcode () {
    return opcode;
  }

  @Override public long address () {
    return address;
  }

  @Override public GdslOperand operand (int idx) {
    return operands[idx];
  }

  @Override public int numberOfOperands () {
    return operands.length;
  }

  @Override public GdslOperand[] operands () {
    return operands;
  }

  @Override public int length () {
    return (int) gdslInsn.getSize() * 8;
  }

  @Override public StringBuilder asString (StringBuilder pretty) {
    return pretty.append(gdslInsn.toString());
  }

  @Override public StringBuilder opcode (StringBuilder buf) {
    return OpcodeFormatter.format(opcode, buf);
  }

  @Override public StringBuilder address (StringBuilder buf) {
    return buf.append(String.format("%08x", address));
  }

}
