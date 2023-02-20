package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers._PUSH;

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

public class PushfwTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, OperandSize.WORD);
    instructions.addAll(Arrays.asList(
        factory.MOV(env.getNextReilAddress(), tmp, factory.immediate(tmp.size(), 0)),
        factory.MOV(env.getNextReilAddress(), tmp.withOffset(0, OperandSize.BIT), X86Helpers.BELOW_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), tmp.withOffset(2, OperandSize.BIT), X86Helpers.PARITY_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), tmp.withOffset(4, OperandSize.BIT), X86Helpers.AUXILIARY_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), tmp.withOffset(6, OperandSize.BIT), X86Helpers.EQUAL_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), tmp.withOffset(7, OperandSize.BIT), X86Helpers.SIGN_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), tmp.withOffset(10, OperandSize.BIT), X86Helpers.DIRECTION_FLAG_OPERAND),
        factory.MOV(env.getNextReilAddress(), tmp.withOffset(11, OperandSize.BIT), X86Helpers.OVERFLOW_FLAG_OPERAND)));
    _PUSH(env, tmp, instructions);
  }
}
