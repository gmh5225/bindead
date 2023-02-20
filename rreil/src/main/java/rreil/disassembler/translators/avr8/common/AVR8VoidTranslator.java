package rreil.disassembler.translators.avr8.common;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 * Generic wrapper for translating operations without return value with register or immediate arguments only.
 *
 */
@Deprecated public class AVR8VoidTranslator implements InsnTranslator {
  private final InsnEmitter emitter;

  public AVR8VoidTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  @Override public void translate (TranslationCtx ctx, Instruction insn, List<LowLevelRReil> instructions) {
    TranslationState opndState1 = AVR8OperandTranslator.translateOperand(ctx, insn.operand(0));
    assert opndState1.getInstructionStack().isEmpty() : "non-empty instruction stack for first operand";
    LowLevelRReilOpnd op1 = opndState1.getOperandStack().pop();
    emitter.emit(ctx, null, op1, null, instructions);
  }
}
