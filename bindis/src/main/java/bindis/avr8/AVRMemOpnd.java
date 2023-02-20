package bindis.avr8;

import bindis.OperandKind;

public class AVRMemOpnd extends AVROpnd {
  private final AVRRegOpnd memReg;
  private final AVRRegisterAlterationType altType;
  private final AVRImmOpnd displacement;

  public AVRRegOpnd getMemReg () {
    return memReg;
  }

  public AVRRegisterAlterationType getAltType () {
    return altType;
  }

  public AVRImmOpnd getDisplacement () {
    return displacement;
  }

  public AVRMemOpnd (AVRRegOpnd memReg) {
    this(memReg, AVRRegisterAlterationType.None);
  }

  public AVRMemOpnd (AVRRegOpnd memReg, AVRRegisterAlterationType altType) {
    this(memReg, altType, null);
  }

  public AVRMemOpnd (AVRImmOpnd displacement) {
    this(null, AVRRegisterAlterationType.None, displacement);
  }

  public AVRMemOpnd (AVRRegOpnd memReg, AVRRegisterAlterationType altType, AVRImmOpnd displacement) {
    super(8, OperandKind.MEM);

    if (memReg == null && displacement == null)
      throw new RuntimeException("Either memory register or displacement has to be specified.");

    this.memReg = memReg;
    this.altType = altType;
    this.displacement = displacement;
  }

  public int ptrSize () {
    if (memReg != null)
      return memReg.size();
    if (displacement != null)
      return displacement.size();
    throw new UnsupportedOperationException();
  }

  @Override public <R, T> R accept (OpndVisitor<R, T> visitor, T data) {
    return visitor.visit(this, data);
  }

  @Override public StringBuilder asString (StringBuilder buf) {
    StringBuilder sb = buf.append("[");
    if (memReg != null) {
      if (altType == AVRRegisterAlterationType.PreDecrement)
        sb.append("-");
      sb.append(memReg.toString());
      if (altType == AVRRegisterAlterationType.PostIncrement)
        sb.append("+");
      if (displacement != null)
        sb.append(" + " + displacement);
    } else
      sb.append(displacement.toString());
    sb.append("]");
    return sb;
  }
}
