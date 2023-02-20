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

public class CwdTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");
    final LowLevelRReilOpnd dx = registerTranslator.translateRegister("dx");
    final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.DWORD);

    instructions.addAll(Arrays.asList(
        factory.SIGNEXTEND(env.getNextReilAddress(), t, ax),
        factory.MOV(env.getNextReilAddress(), dx, t.withOffset(16, OperandSize.WORD.getBits()))));
  }
}
