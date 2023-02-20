package rreil.disassembler.translators.x86.x64;

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

public class CqoTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);

    final LowLevelRReilOpnd rax = registerTranslator.translateRegister("rax");
    final LowLevelRReilOpnd rdx = registerTranslator.translateRegister("rdx");
    final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.OWORD);

    instructions.addAll(Arrays.asList(
        factory.SIGNEXTEND(env.getNextReilAddress(), t, rax),
        factory.MOV(env.getNextReilAddress(), rdx, t.withOffset(64, OperandSize.QWORD))));
  }
}
