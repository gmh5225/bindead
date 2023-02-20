package bindis.avr8;

import bindis.OperandKind;

/**
 *
 * @author mb0
 */
public class AVRRegOpnd extends AVROpnd {
  public static enum AVRReg {
    R0("r0", 8),
    R1("r1", 8),
    R2("r2", 8),
    R3("r3", 8),
    R4("r4", 8),
    R5("r5", 8),
    R6("r6", 8),
    R7("r7", 8),
    R8("r8", 8),
    R9("r9", 8),
    R10("r10", 8),
    R11("r11", 8),
    R12("r12", 8),
    R13("r13", 8),
    R14("r14", 8),
    R15("r15", 8),
    R16("r16", 8),
    R17("r17", 8),
    R18("r18", 8),
    R19("r19", 8),
    R20("r20", 8),
    R21("r21", 8),
    R22("r22", 8),
    R23("r23", 8),
    R24("r24", 8),
    R25("r25", 8),
    R26("r26", 8),
    R27("r27", 8),
    R28("r28", 8),
    R29("r29", 8),
    R30("r30", 8),
    R31("r31", 8),
    X("X", 16),
    Y("Y", 16),
    Z("Z", 16);
    private final String name;
    private final int size;

    AVRReg (String name, int size) {
      this.name = name;
      this.size = size;
    }

    public String getName () {
      return name;
    }

    public static AVRReg resolve (int regNo) {
      return AVRReg.values()[regNo];
    }
  }
  private final AVRReg reg;

  public AVRRegOpnd (int size, int regNo) {
    this(AVRReg.resolve(regNo));
    if (reg.size != size)
      throw new RuntimeException("Size and register number do not fit together.");
  }

  public AVRRegOpnd (AVRReg reg) {
    super((byte) reg.size, OperandKind.REG);
    this.reg = reg;
  }

  public String name () {
    return reg.name;
  }

  public int getRegNo () {
    return reg.ordinal(); // FIXME
  }

  public int getSize() {
    return reg.size;
  }

  @Override public StringBuilder asString (StringBuilder buf) {
    return buf.append(reg.name);
  }

  @Override public <R, T> R accept (OpndVisitor<R, T> visitor, T data) {
    return visitor.visit(this, data);
  }
}
