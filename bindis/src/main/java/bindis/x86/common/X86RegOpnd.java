package bindis.x86.common;

import bindis.OperandKind;

/**
 * X86 register operands.
 *
 * @author mb0
 */
public class X86RegOpnd extends X86Operand {
  private final String name;

  public X86RegOpnd (String name, int size) {
    super(size, OperandKind.REG);
    this.name = name;
  }

  public String name () {
    return name;
  }

  @Override public StringBuilder asString (StringBuilder buf) {
    return buf.append(name);
  }

  @Override public <R, T> R accept (X86OperandVisitor<R, T> visitor, T data) {
    return visitor.visit(this, data);
  }
}
