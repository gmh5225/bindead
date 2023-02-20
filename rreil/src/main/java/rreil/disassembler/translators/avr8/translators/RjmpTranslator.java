package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class RjmpTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public RjmpTranslator () {
    super(ReturnType.None);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
//    final LowLevelRReilOpnd t0 = env.temporaryRegister(OperandSize.WORD);
    final LowLevelRReilOpnd t0 = env.temporaryRegister(src1.size());
//    instructions.add(factory.MOVSX(env.getNextReilAddress(), t0, src1));
//    instructions.add(factory.SHL(env.getNextReilAddress(), t0, t0, factory.immediate(t0.size(), 1)));
//    instructions.add(factory.ADD(env.getNextReilAddress(), t0, t0, factory.immediate(OperandSize.WORD, env.getBaseAddress())));
    instructions.add(factory.ADD(env.getNextReilAddress(), t0, src1, factory.immediate(src1.size(), 2)));
    instructions.add(factory.GOTONATIVE(env.getNextReilAddress(), t0));
  }
}
