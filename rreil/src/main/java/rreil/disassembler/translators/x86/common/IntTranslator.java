package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;


public class IntTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override public void translate (TranslationCtx env, Instruction insn, List<LowLevelRReil> instructions) {
//    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    /*
     * TODO INT:What to do with flags???
     */
    instructions.addAll(Arrays.asList(
        factory.NATIVE(env.getNextReilAddress(), insn)
        ));
  }
}
