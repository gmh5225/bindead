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
public class BldTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public BldTranslator () {
    super(ReturnType.Register);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd t0 = env.temporaryRegister(dst.size());
    final LowLevelRReilOpnd t1 = env.temporaryRegister(dst.size());

    instructions.add(factory.CONVERT(env.getNextReilAddress(), t1, src2));
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t0, AVR8Helpers.T_OPERAND));
    instructions.add(factory.SHL(env.getNextReilAddress(), t0, t0, t1));

    final LowLevelRReilOpnd t2 = env.temporaryRegister(dst.size());
    instructions.add(factory.MOV(env.getNextReilAddress(), t2, factory.immediate(t2.size(), 0)));
    instructions.add(factory.ADD(env.getNextReilAddress(), t2, t2, factory.immediate(t2.size(), 1)));
    instructions.add(factory.XOR(env.getNextReilAddress(), t2, t2, factory.immediate(t2.size(), (1 << t2.size()) - 1)));
    instructions.add(factory.SHL(env.getNextReilAddress(), t2, t2, t1));

    instructions.add(factory.AND(env.getNextReilAddress(), dst, src1, t2));
    instructions.add(factory.OR(env.getNextReilAddress(), dst, dst, t0));
  }
}
