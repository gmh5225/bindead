package bindis.avr8.decoders;

import bindis.NativeInstruction;
import bindis.avr8.AVRDecodeCtx;
import bindis.avr8.AVROpnd;

/**
 *
 * @author mb0
 */
public final class Decode {
  public static final Decode INSTANCE = new Decode();

  public static InsnDecoder decodeGroup (final int groupIdx) {
    return new NestedDecoder(null) {
      @Override protected InsnDecoder lookupDecoder (AVRDecodeCtx ctx) {
        final int subgroupIdx = Slice.S2.slice(ctx.getInsnWord());
        return OpcodeTables.groupTable[groupIdx][subgroupIdx];
      }
    };
  }

  public static InsnDecoder decodeOverlayGroup (final int groupIdx, final Slice slice) {
    return new NestedDecoder(null) {
      @Override protected InsnDecoder lookupDecoder (AVRDecodeCtx ctx) {
        final int subgroupIdx = slice.slice(ctx.getInsnWord());
        return OpcodeTables.overlayGroupTable[groupIdx][subgroupIdx];
      }
    };
  }

  /**
   * Build a decoder for zero-operand instructions (e.g. "nop").
   *
   * @param mnemonic
   * @return
   */
  public static InsnDecoder decode (String mnemonic) {
    return new InsnDecoder(mnemonic) {
      @Override public NativeInstruction decodeInstruction (AVRDecodeCtx ctx) {
        return factory.make(mnemonic, ctx);
      }
    };
  }

  public static InsnDecoder decode (final String mnemonic, final OpndDecoder decodeOp1, final OpndDecoder decodeOp2) {
    return new InsnDecoder(mnemonic) {
      @Override public NativeInstruction decodeInstruction (AVRDecodeCtx ctx) {
        final AVROpnd op1 = decodeOp1.decode(ctx);
        final AVROpnd op2 = decodeOp2.decode(ctx);
        return factory.make(mnemonic, op1, op2, ctx);
      }
    };
  }

  public static InsnDecoder decode (final String mnemonic, final OpndDecoder decodeOp1) {
    return new InsnDecoder(mnemonic) {
      @Override public NativeInstruction decodeInstruction (AVRDecodeCtx ctx) {
        final AVROpnd op1 = decodeOp1.decode(ctx);
        return factory.make(mnemonic, op1, ctx);
      }
    };
  }
}
