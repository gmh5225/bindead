package bindis.avr8;

import bindis.Operand;
import bindis.OperandKind;

/**
 *
 * @author mb0
 */
public abstract class AVROpnd extends Operand {
  public AVROpnd (int size, OperandKind kind) {
    super(size, kind);
  }

  public abstract <R, T> R accept (OpndVisitor<R, T> visitor, T data);
}
