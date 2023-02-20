package bindis.gdsl;

import bindis.IOperand;
import rreil.disassembler.OperandTree;
import rreil.disassembler.OperandTree.Type;

public class GdslOperand implements IOperand {
  private gdsl.decoder.NativeInstruction gdslInsn;
  private int id;

  public GdslOperand (gdsl.decoder.NativeInstruction gdslInsn, int id) {
    this.gdslInsn = gdslInsn;
    this.id = id;
  }

  @Override public StringBuilder asString (StringBuilder buf) {
    return buf.append(gdslInsn.operandToString(id));
  }

  @Override public String toString () {
    return asString(new StringBuilder()).toString();
  }

  public OperandTree.Type getType () {
    switch (gdslInsn.operandType(id)) {
    case Immediate:
      return Type.Immi;
    case Register:
      return Type.Sym;
    case Memory:
      return Type.Mem;
    case Linear:
      return Type.Op;
    case Flow:
      return Type.Sym;
    }
    throw new RuntimeException();
  }
}
