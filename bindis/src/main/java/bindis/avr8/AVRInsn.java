package bindis.avr8;

import bindis.NativeInstruction;
import rreil.disassembler.Instruction;

/**
 * Base class for AVR instructions.
 *
 * @author mb0
 */
public final class AVRInsn extends NativeInstruction {
  private static final String $Architecture = "AVR-8";

  public AVRInsn (String mnemonic, AVRDecodeCtx ctx) {
    super(ctx.getStartPc(), mnemonic, ctx.slice());
  }

  public AVRInsn (String mnemonic, AVROpnd op0, AVRDecodeCtx ctx) {
    super(ctx.getStartPc(), mnemonic, ctx.slice(), op0);
  }

  public AVRInsn (String mnemonic, AVROpnd op0, AVROpnd op1, AVRDecodeCtx ctx) {
    super(ctx.getStartPc(), mnemonic, ctx.slice(), op0, op1);
  }

  public AVRInsn (String mnemonic, AVROpnd op0, AVROpnd op1, AVROpnd op2, AVRDecodeCtx ctx) {
    super(ctx.getStartPc(), mnemonic, ctx.slice(), op0, op1, op2);
  }

  @Override public Instruction toTreeInstruction () {
    return AVRTreeTranslator.$.translate(this);
  }

  @Override public String architecture () {
    return $Architecture;
  }
}
