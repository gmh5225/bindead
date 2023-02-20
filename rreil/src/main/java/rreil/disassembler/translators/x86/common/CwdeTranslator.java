package rreil.disassembler.translators.x86.common;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class CwdeTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");
    final LowLevelRReilOpnd eax = registerTranslator.translateRegister("eax");
    final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.DWORD);

    instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), t, ax));

    // TODO: Check implicit zero extension of eax!
    X86Helpers.emitWritebackAndMaybeZeroExtend(env, eax, t, instructions);
  }
}
