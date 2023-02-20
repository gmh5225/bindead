package bindis.avr8;

import java.nio.ByteOrder;

import bindis.DecodeStream;
import bindis.NativeDisassembler;
import bindis.NativeInstruction;
import bindis.avr8.decoders.InsnDecoder;
import bindis.avr8.decoders.OpcodeTables;
import bindis.avr8.decoders.Slice;
import javalx.exceptions.UnimplementedException;
import rreil.disassembler.BlockOfInstructions;

/**
 */
public final class AVRDis implements NativeDisassembler {
  public static final AVRDis $ = new AVRDis();

  private AVRDis () {
  }

  @Override public NativeInstruction decode (DecodeStream in, long startPc) {
    in.order(ByteOrder.LITTLE_ENDIAN);
    final AVRDecodeCtx ctx = new AVRDecodeCtx(in, (int) startPc);
    InsnDecoder decoder = OpcodeTables.noopTable.get(ctx.getInsnWord());
    if (decoder == null) {
      decoder = lookupDecoder(ctx);
      if (decoder == null)
        return null;
    }
    return decoder.decode(ctx);
  }

  private static InsnDecoder lookupDecoder (AVRDecodeCtx ctx) {
    final int insnWord = ctx.getInsnWord();
    final int idx = Slice.S1.slice(insnWord);
    return OpcodeTables.prefixTable[idx];
  }

  @Override public BlockOfInstructions decodeBlock (DecodeStream in, long pc) {
    throw new UnimplementedException();
  }
}
