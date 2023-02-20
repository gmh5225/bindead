package bindis;

/**
 * Abstract base class for instruction operands.
 *
 * @author mb0
 */
public abstract class Operand implements IOperand {
  protected final int size;
  protected final OperandKind kind;

  public Operand (int size, OperandKind kind) {
    this.size = size;
    this.kind = kind;
  }

  /**
   * Returns the operand kind of this instruction.
   *
   * @return
   */
  public OperandKind kind () {
    return kind;
  }

  /**
   * Returns the size in bits of this operand.
   *
   * @return
   */
  public int size () {
    return size;
  }

  @Override public String toString () {
    return asString(new StringBuilder()).toString();
  }
}
