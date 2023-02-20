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

public class CdqTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (
      final TranslationCtx env,
      final Instruction instruction,
      final List<LowLevelRReil> instructions) {

    //env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final LowLevelRReilOpnd eax = registerTranslator.translateRegister("eax");
    final LowLevelRReilOpnd edx = registerTranslator.translateRegister("edx");
    final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.QWORD);

    instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), t, eax));

    // TODO: Check implicit zero extension of edx!
    X86Helpers.emitWritebackAndMaybeZeroExtend(env, edx, t.withOffset(32, OperandSize.DWORD.getBits()), instructions);
  }
}
