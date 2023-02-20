package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8RegRegWordTranslator;
import rreil.disassembler.translators.avr8.emitters.GenericFlagEmitter;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class AdiwTranslator extends AVR8RegRegWordTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class AdiwFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new AdiwFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      // Zero
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Negative
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Carry
      final LowLevelRReilOpnd t1 = env.temporaryRegister(1);

      instructions.add(factory.XOR(env.getNextReilAddress(), t1, dst.withOffset(15, 1), factory.immediate(t1.size(), (1 << t1.size()) - 1)));
      instructions.add(factory.AND(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, t1, src1.withOffset(15, 1)));

      // Two's complement overflow indicator
      instructions.add(factory.XOR(env.getNextReilAddress(), t1, src1.withOffset(15, 1), factory.immediate(t1.size(), (1 << t1.size()) - 1)));
      instructions.add(factory.AND(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, t1, dst.withOffset(15, 1)));

      GenericFlagEmitter.$.emit(env, dst, src1, src2, instructions);
    }
  }

  public AdiwTranslator () {
    super(new boolean[] { true, false });
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd t0 = env.temporaryRegister(dst.size());
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t0, src2));

    instructions.add(factory.ADD(env.getNextReilAddress(), dst, src1, t0));

    AdiwFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }

}
