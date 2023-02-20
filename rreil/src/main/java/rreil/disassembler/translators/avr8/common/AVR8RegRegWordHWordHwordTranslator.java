package rreil.disassembler.translators.avr8.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 * Generic wrapper for translating binary operations with register or immediate arguments only.
 *
 * @author mb0
 */
public abstract class AVR8RegRegWordHWordHwordTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final LowLevelRReilOpnd dstHigh;
  private final LowLevelRReilOpnd dstLow;

  public AVR8RegRegWordHWordHwordTranslator (LowLevelRReilOpnd dstHigh, LowLevelRReilOpnd dstLow) {
    this.dstLow = dstLow;
    this.dstHigh = dstHigh;
  }

  public abstract void emit (TranslationCtx ctx, LowLevelRReilOpnd dst, LowLevelRReilOpnd LowLevelRReilOpnd, LowLevelRReilOpnd LowLevelRReilOpnd0, List<LowLevelRReil> instructions);

  @Override public void translate (TranslationCtx ctx, Instruction insn, List<LowLevelRReil> instructions) {
    AVR8SregMemTranslator.loadSreg(ctx, instructions);

    TranslationState opndState1 = AVR8OperandTranslator.translateOperand(ctx, insn.operand(0));
    TranslationState opndState2 = AVR8OperandTranslator.translateOperand(ctx, insn.operand(1));
    assert opndState1.getInstructionStack().isEmpty() : "non-empty instruction stack for first operand";
    assert opndState2.getInstructionStack().isEmpty() : "non-empty instruction stack for second operand";
    LowLevelRReilOpnd op1 = opndState1.getOperandStack().pop();
    LowLevelRReilOpnd op2 = opndState2.getOperandStack().pop();
    LowLevelRReilOpnd tempdst = ctx.temporaryRegister(op1.size()*2);

    emit(ctx, tempdst, op1, op2, instructions);

    // Result write-back.
    instructions.addAll(Arrays.asList(factory.CONVERT(ctx.getNextReilAddress(), dstLow, tempdst)));
    instructions.addAll(Arrays.asList(factory.SHRS(ctx.getNextReilAddress(), tempdst, tempdst, factory.immediate(tempdst.size(), dstHigh.size()))));
    instructions.addAll(Arrays.asList(factory.CONVERT(ctx.getNextReilAddress(), dstHigh, tempdst)));

    AVR8SregMemTranslator.storeSreg(ctx, instructions);
  }
}
