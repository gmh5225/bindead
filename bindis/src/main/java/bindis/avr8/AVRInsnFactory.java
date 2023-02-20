package bindis.avr8;

/**
 *
 * @author mb0
 */
public interface AVRInsnFactory {
  public AVRInsn makeIllegalInsn (AVRDecodeCtx ctx);

  public AVRInsn make (String name, AVRDecodeCtx ctx);

  public AVRInsn make (String name, AVROpnd op1, AVRDecodeCtx ctx);

  public AVRInsn make (String name, AVROpnd op1, AVROpnd op2, AVRDecodeCtx ctx);

  public AVRInsn make (String name, AVROpnd op1, AVROpnd op2, AVROpnd op3, AVRDecodeCtx ctx);
}
