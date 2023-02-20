package bindis.avr8;

/**
 *
 * @author mb0
 */
public interface OpndVisitor<R, T> {
  public R visit (AVRRegOpnd opnd, T data);

  public R visit (AVRImmOpnd opnd, T data);

  public R visit (AVRMemOpnd opnd, T data);
}
