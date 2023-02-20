package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class SbiTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public SbiTranslator () {
    super(ReturnType.Memory);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd t0 = env.temporaryRegister(dst.size());
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t0, src2));

    instructions.add(factory.CONVERT(env.getNextReilAddress(), dst, factory.immediate(dst.size(), 1)));
    instructions.add(factory.SHL(env.getNextReilAddress(), dst, dst, t0));
    instructions.add(factory.OR(env.getNextReilAddress(), dst, dst, src1));
  }

}
