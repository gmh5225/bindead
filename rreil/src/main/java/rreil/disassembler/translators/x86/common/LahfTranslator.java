package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class LahfTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");
    final LowLevelRReilOpnd zero = factory.immediate(OperandSize.BIT, 0);
    final LowLevelRReilOpnd one = factory.immediate(OperandSize.BIT, 1);

    instructions.addAll(Arrays.asList(
        factory.MOV(env.getNextReilAddress(), ax.withOffset(8, OperandSize.BIT), X86Helpers.BELOW_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), ax.withOffset(9, OperandSize.BIT), one),
        factory.MOV(env.getNextReilAddress(), ax.withOffset(10, OperandSize.BIT), X86Helpers.PARITY_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), ax.withOffset(11, OperandSize.BIT), zero),
        factory.MOV(env.getNextReilAddress(), ax.withOffset(12, OperandSize.BIT), X86Helpers.AUXILIARY_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), ax.withOffset(13, OperandSize.BIT), zero),
        factory.MOV(env.getNextReilAddress(), ax.withOffset(14, OperandSize.BIT), X86Helpers.EQUAL_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), ax.withOffset(15, OperandSize.BIT), X86Helpers.SIGN_FLAG_OPERAND)));
  }
}
