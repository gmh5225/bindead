package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.avr8.common.AVR8RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.TranslationHelpers;

public class LacTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public LacTranslator () {
    super(ReturnType.None);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final OperandSize archDefaultSize = TranslationHelpers.architectures.get("AVR");
    final LowLevelRReilOpnd t0 = env.temporaryRegister(archDefaultSize);

    instructions.add(factory.LOAD(env.getNextReilAddress(), t0, AVR8RegisterTranslator.$.translateRegister("Z")));
    instructions.add(factory.XOR(env.getNextReilAddress(), t0, t0, TranslationHelpers.getMaxImmediate(archDefaultSize.getSizeInBytes() * 8)));
    instructions.add(factory.STORE(env.getNextReilAddress(), AVR8RegisterTranslator.$.translateRegister("Z"), src1));
  }
}
