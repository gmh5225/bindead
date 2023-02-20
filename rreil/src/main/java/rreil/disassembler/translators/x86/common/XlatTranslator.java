package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class XlatTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);
    final LowLevelRReilOpnd al = registerTranslator.translateRegister("al");
    final LowLevelRReilOpnd ebx = registerTranslator.translateRegister("ebx").withSize(env.getDefaultArchitectureSize());
    final LowLevelRReilOpnd t1 = registerTranslator.temporaryRegister(env, ebx.size());
    final LowLevelRReilOpnd t2 = registerTranslator.temporaryRegister(env, ebx.size());
    final LowLevelRReilOpnd t3 = registerTranslator.temporaryRegister(env, al.size());

    instructions.addAll(Arrays.asList(
        factory.CONVERT(env.getNextReilAddress(), t1, al),
        factory.ADD(env.getNextReilAddress(), t2, ebx, t1),
        factory.LOAD(env.getNextReilAddress(), t3, t2),
        factory.MOV(env.getNextReilAddress(), al, t3)));
  }
}
