package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class SwapTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public SwapTranslator () {
    super(ReturnType.Register);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    instructions.add(factory.MOV(env.getNextReilAddress(), dst, src1));
    instructions.add(factory.SHL(env.getNextReilAddress(), dst, dst, factory.immediate(dst.size(), 4)));

    final LowLevelRReilOpnd t0 = env.temporaryRegister(src1.size());
    instructions.add(factory.MOV(env.getNextReilAddress(), t0, src1));
    instructions.add(factory.SHRS(env.getNextReilAddress(), t0, t0, factory.immediate(t0.size(), 4)));

    instructions.add(factory.OR(env.getNextReilAddress(), dst, dst, t0));
  }

}
