package bindis.avr8;

/**
 * Concrete implementation of the {@code AVRInsnFactory} interface for building
 * AVR instruction objects.
 *
 * @author mb0
 */
public final class AVRInsnFactoryImpl implements AVRInsnFactory {
  public static final AVRInsnFactoryImpl INSTANCE = new AVRInsnFactoryImpl();

  private AVRInsnFactoryImpl () {
  }

  @Override public AVRInsn makeIllegalInsn (AVRDecodeCtx ctx) {
    return new AVRInsn("{bad opcode}", null, null, null, ctx);
  }

  @Override public AVRInsn make (String name, AVRDecodeCtx ctx) {
    return new AVRInsn(name, null, null, null, ctx);
  }

  @Override public AVRInsn make (String name, AVROpnd op1, AVRDecodeCtx ctx) {
    return new AVRInsn(name, op1, null, null, ctx);
  }

  @Override public AVRInsn make (String name, AVROpnd op1, AVROpnd op2, AVRDecodeCtx ctx) {
    return new AVRInsn(name, op1, op2, null, ctx);
  }

  @Override public AVRInsn make (String name, AVROpnd op1, AVROpnd op2, AVROpnd op3, AVRDecodeCtx ctx) {
    return new AVRInsn(name, op1, op2, op3, ctx);
  }
}
