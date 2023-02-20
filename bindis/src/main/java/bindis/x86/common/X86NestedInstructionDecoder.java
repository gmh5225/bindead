package bindis.x86.common;

import bindis.NativeInstruction;

/**
 * Nested instruction decoder lookup. Every dispatching decoder should extend this class.
 *
 * @author mb0
 */
public abstract class X86NestedInstructionDecoder extends X86InstructionDecoder {
  public X86NestedInstructionDecoder (final String name) {
    super(name);
  }

  @Override protected final NativeInstruction decodeInstruction (final X86DecodeCtx ctx) {
    // This enables nested group support.
    final X86InstructionDecoder decoder = lookupDecoder(ctx);
    if (decoder != null)
      return decoder.decodeInstruction(ctx);
    return null;
  }

  @Override protected abstract X86InstructionDecoder lookupDecoder (final X86DecodeCtx ctx);
}
