package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 * Add instruction emitter.
 *
 * @author mb0
 */
public class BstTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public BstTranslator () {
    super(ReturnType.None);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd t0 = env.temporaryRegister(src1.size());
    final LowLevelRReilOpnd t1 = env.temporaryRegister(src1.size());

    instructions.add(factory.MOV(env.getNextReilAddress(), t0, src1));
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t1, src2));

    instructions.add(factory.SHRS(env.getNextReilAddress(), t0, t0, t1));
    instructions.add(factory.CONVERT(env.getNextReilAddress(), AVR8Helpers.T_OPERAND, t0));
  }
}
