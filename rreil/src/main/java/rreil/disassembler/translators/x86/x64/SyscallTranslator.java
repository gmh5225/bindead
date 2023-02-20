package rreil.disassembler.translators.x86.x64;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;

public class SyscallTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override public void translate (TranslationCtx env, Instruction insn, List<LowLevelRReil> instructions) {
//    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    /*
     * TODO What to do with flags??? See doku
     *
     * RCX ←RIP; (* Will contain address of next instruction *)
     * RIP ←IA32_LSTAR;
     * R11 ←RFLAGS;
     * //RFLAGS ←RFLAGS AND NOT(IA32_FMASK);
     */
    instructions.addAll(Arrays.asList(
        factory.NATIVE(env.getNextReilAddress(), insn)
        ));
  }
}
