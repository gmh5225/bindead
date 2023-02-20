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

public class SahfTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);
    final LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");

    instructions.addAll(Arrays.asList(
        factory.MOV(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, ax.withOffset(8, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.PARITY_FLAG_OPERAND, ax.withOffset(10, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.AUXILIARY_FLAG_OPERAND, ax.withOffset(12, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.EQUAL_FLAG_OPERAND, ax.withOffset(14, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.SIGN_FLAG_OPERAND, ax.withOffset(15, OperandSize.BIT)),
        factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
        factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND),
        factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND)));
  }
}
