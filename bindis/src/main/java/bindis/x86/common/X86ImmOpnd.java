package bindis.x86.common;

import bindis.OperandKind;

/**
 * X86 immediate operands.
 *
 * @author mb0
 */
public class X86ImmOpnd extends X86Operand {
  protected final Number imm;

  public X86ImmOpnd (int size, Number imm) {
    super(size, OperandKind.IMM);
    this.imm = imm;
  }

  /**
   * Render this immediate operand to the given string-builder.
   *
   * @param buf The string-builder.
   * @return The updated string-builder.
   */
  @Override public StringBuilder asString (StringBuilder buf) {
    switch (size) {
      case 8:
        buf.append(Long.toHexString(imm.byteValue()));
        break;
      case 16:
        buf.append(Long.toHexString(imm.shortValue()));
        break;
      case 32:
        buf.append(Long.toHexString(imm.intValue()));
        break;
      default:
        // should not happen
        buf.append(Long.toHexString(imm.longValue()));
    }
    return buf;
  }

  @Override public <R, T> R accept (X86OperandVisitor<R, T> visitor, T data) {
    return visitor.visit(this, data);
  }

  /**
   * Return this immediate value as number.
   *
   * @return
   */
  public Number imm () {
    return imm;
  }
}
