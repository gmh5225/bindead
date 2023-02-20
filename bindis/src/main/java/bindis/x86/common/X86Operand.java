package bindis.x86.common;

import bindis.Operand;
import bindis.OperandKind;

public abstract class X86Operand extends Operand {
  public X86Operand (int size, OperandKind kind) {
    super(size, kind);
  }

  public abstract <R, T> R accept (X86OperandVisitor<R, T> visitor, T data);
}
