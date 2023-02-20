package bindis.avr8;

import bindis.OperandKind;

public class AVRImmOpnd extends AVROpnd {
  private int value;

  public int getValue () {
    return value;
  }

  public AVRImmOpnd (int size, int value) {
    super(size, OperandKind.IMM);
    this.value = value;
  }

  @Override public <R, T> R accept (OpndVisitor<R, T> visitor, T data) {
    return visitor.visit(this, data);
  }

  @Override public StringBuilder asString (StringBuilder buf) {
    return buf.append(value);
  }
}
