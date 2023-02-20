package rreil.disassembler.translators.x86.x64;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class CdqeTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    LowLevelRReilOpnd eax = registerTranslator.translateRegister("eax");
    LowLevelRReilOpnd rax = registerTranslator.translateRegister("rax");
    instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), rax, eax));
  }
}
