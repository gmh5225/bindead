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
 * Generic wrapper for translating binary operations with register or immediate
 * arguments only.
 *
 * @author mb0
 */
@Deprecated public class AVR8RegRegTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final InsnEmitter emitter;

  public AVR8RegRegTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  @Override public void translate (TranslationCtx ctx, Instruction insn, List<LowLevelRReil> instructions) {
    TranslationState opndState1 = AVR8OperandTranslator.translateOperand(ctx, insn.operand(0));
    TranslationState opndState2 = AVR8OperandTranslator.translateOperand(ctx, insn.operand(1));
    assert opndState1.getInstructionStack().isEmpty() : "non-empty instruction stack for first operand";
    assert opndState2.getInstructionStack().isEmpty() : "non-empty instruction stack for second operand";
    LowLevelRReilOpnd op1 = opndState1.getOperandStack().pop();
    LowLevelRReilOpnd op2 = opndState2.getOperandStack().pop();
    LowLevelRReilOpnd dst = ctx.temporaryRegister(op1.size());
    emitter.emit(ctx, dst, op1, op2, instructions);
    // Result write-back.
    instructions.add(factory.MOV(ctx.getNextReilAddress(), op1, dst));
  }
}
