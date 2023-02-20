package bindis.avr8.decoders;

import bindis.NativeInstruction;
import bindis.avr8.AVRDecodeCtx;
import bindis.avr8.AVRInsnFactory;
import bindis.avr8.AVRInsnFactoryImpl;

/**
 *
 * @author mb0
 */
public abstract class InsnDecoder {
  protected static final AVRInsnFactory factory = AVRInsnFactoryImpl.INSTANCE;
  protected final String mnemonic;

  public InsnDecoder (String mnemonic) {
    this.mnemonic = mnemonic;
  }

  public final NativeInstruction decode (final AVRDecodeCtx ctx) {
    final InsnDecoder decoder = lookupDecoder(ctx);
    if (decoder != null)
      return decoder.decodeInstruction(ctx);
    else
      return factory.makeIllegalInsn(ctx);
  }

  public abstract NativeInstruction decodeInstruction (AVRDecodeCtx ctx);

  protected InsnDecoder lookupDecoder (final AVRDecodeCtx ctx) {
    return this;
  }
}
