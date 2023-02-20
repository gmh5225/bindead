package rreil.disassembler.translators.avr8.common;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 * Generic wrapper for translating unary operations with register or immediate
 * arguments only.
 *
 */
@Deprecated
public class AVR8RegTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();
  private final InsnEmitter emitter;

  public AVR8RegTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  @Override public void translate (TranslationCtx ctx, Instruction insn, List<LowLevelRReil> instructions) {
    TranslationState opndState1 = AVR8OperandTranslator.translateOperand(ctx, insn.operand(0));
    assert opndState1.getInstructionStack().isEmpty() : "non-empty instruction stack for first operand";
    LowLevelRReilOpnd op1 = opndState1.getOperandStack().pop();
    LowLevelRReilOpnd dst = ctx.temporaryRegister(op1.size());
    emitter.emit(ctx, dst, op1, null, instructions);
    // Result write-back.
    instructions.add(factory.MOV(ctx.getNextReilAddress(), op1, dst));
  }
}
