package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.avr8.emitters.CarryShiftFlagEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class RorTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public RorTranslator () {
    super(ReturnType.Register);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    instructions.add(factory.SHRS(env.getNextReilAddress(), dst, src1, factory.immediate(src1.size(), 1)));

    final LowLevelRReilOpnd t0 = env.temporaryRegister(dst.size());
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t0, AVR8Helpers.C_OPERAND));
    instructions.add(factory.SHL(env.getNextReilAddress(), t0, t0, factory.immediate(t0.size(), t0.size() - 1)));

    instructions.add(factory.OR(env.getNextReilAddress(), dst, dst, t0));

    CarryShiftFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }

}
