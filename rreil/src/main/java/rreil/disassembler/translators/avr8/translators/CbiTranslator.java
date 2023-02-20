package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class CbiTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public CbiTranslator () {
    super(ReturnType.Memory);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    instructions.add(factory.MOV(env.getNextReilAddress(), dst, src1));

    final LowLevelRReilOpnd t0 = env.temporaryRegister(dst.size());

    final LowLevelRReilOpnd t1 = env.temporaryRegister(dst.size());
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t1, src2));

    instructions.add(factory.MOV(env.getNextReilAddress(), t0, factory.immediate(dst.size(), 1)));
    instructions.add(factory.SHL(env.getNextReilAddress(), t0, t0, t1));
    instructions.add(factory.XOR(env.getNextReilAddress(), t0, t0, factory.immediate(t0.size(), (1 << t0.size()) - 1)));
    instructions.add(factory.AND(env.getNextReilAddress(), dst, t0, dst));
  }
}
