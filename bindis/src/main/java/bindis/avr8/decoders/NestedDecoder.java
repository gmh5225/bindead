package bindis.avr8.decoders;

import bindis.NativeInstruction;
import bindis.avr8.AVRDecodeCtx;

/**
 *
 * @author mb0
 */
public abstract class NestedDecoder extends InsnDecoder {
  public NestedDecoder (final String name) {
    super(name);
  }

  @Override public final NativeInstruction decodeInstruction (final AVRDecodeCtx ctx) {
    // This enables nested group support.
    final InsnDecoder decoder = lookupDecoder(ctx);
    if (decoder != null)
      return decoder.decodeInstruction(ctx);
    else
      return factory.makeIllegalInsn(ctx);
  }

  @Override protected abstract InsnDecoder lookupDecoder (final AVRDecodeCtx ctx);
}
