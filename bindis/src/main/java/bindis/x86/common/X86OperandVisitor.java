package bindis.x86.common;

/**
 * X86 operand visitor interface.
 * 
 * @author mb0
 */
public interface X86OperandVisitor<R, T> {
  public R visit (X86ImmOpnd opnd, T data);

  public R visit (X86RegOpnd opnd, T data);

  public R visit (X86MemOpnd opnd, T data);
}
