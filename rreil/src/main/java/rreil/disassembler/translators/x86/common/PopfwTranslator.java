package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers._POP;

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

public class PopfwTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, OperandSize.WORD);
    _POP(env, tmp, instructions);
    instructions.addAll(Arrays.asList(
        factory.MOV(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, tmp.withOffset(0, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.PARITY_FLAG_OPERAND, tmp.withOffset(2, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.AUXILIARY_FLAG_OPERAND, tmp.withOffset(4, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.EQUAL_FLAG_OPERAND, tmp.withOffset(6, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.SIGN_FLAG_OPERAND, tmp.withOffset(7, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.DIRECTION_FLAG_OPERAND, tmp.withOffset(10, OperandSize.BIT)),
        factory.MOV(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND, tmp.withOffset(11, OperandSize.BIT))));
  }
}
